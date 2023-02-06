import pytest

from hamcrest import assert_that, contains, equal_to, has_entries, has_properties

from mail.ipa.ipa.interactions.yarm.entities import YarmCollector, YarmCollectorState, YarmCollectorStatus
from mail.ipa.ipa.interactions.yarm.exceptions import YarmDuplicateError


@pytest.fixture
def yarm_client(clients, mock_yarm, response, url):
    mock_yarm(url, response)
    return clients.yarm


@pytest.fixture
def pop_id():
    return 'popid'


@pytest.fixture
def password():
    return 'buzzword'


@pytest.fixture
def src_login():
    return 'mailbox@gmail.test'


@pytest.fixture
def dst_email():
    return 'mailbox@pdd-domain.test'


class TestYarmCreate:
    @pytest.fixture
    def url(self):
        return '/api/create'

    @pytest.fixture
    def request_coro(self, yarm_client, dst_email, user_ip, suid, src_login, password, general_import_params):
        return yarm_client.create(
            src_login=src_login,
            password=password,
            suid=suid,
            user_ip=user_ip,
            dst_email=dst_email,
            params=general_import_params,
        )

    class TestSuccess:
        @pytest.fixture
        def response(self, pop_id):
            return {'popid': pop_id}

        @pytest.fixture(autouse=True)
        async def returned(self, request_coro):
            return await request_coro

        def test_returned(self, returned, pop_id):
            assert_that(returned, equal_to(pop_id))

        def test_params(self, get_last_yarm_request, dst_email, src_login, suid, general_import_params):
            assert_that(
                get_last_yarm_request().query,
                equal_to({
                    'login': src_login,
                    'suid': str(suid),
                    'user': dst_email,
                    'server': general_import_params.server,
                    'port': str(general_import_params.port),
                    'ssl': str(int(general_import_params.ssl)),
                    'imap': str(int(general_import_params.imap)),
                    'no_delete_msgs': str(int(not general_import_params.delete_msgs)),
                    'mark_archive_read': str(int(general_import_params.mark_archive_read)),
                    'mdb': 'pg',
                    'json': '1',
                })
            )

        @pytest.mark.asyncio
        async def test_body(self, get_last_yarm_request, password):
            assert_that(
                await get_last_yarm_request().post(),
                equal_to({
                    'password': password,
                })
            )

        def test_headers(self, get_last_yarm_request, user_ip):
            assert_that(
                get_last_yarm_request().headers,
                has_entries({
                    'x-real-ip': user_ip,
                })
            )

    class TestDuplicate:
        @pytest.fixture
        def response(self, pop_id):
            return {'error': {'reason': 'dublicate error', 'description': 'dublicate error'}}

        @pytest.mark.asyncio
        async def test_raises_duplicate_error(self, request_coro):
            with pytest.raises(YarmDuplicateError):
                await request_coro


class TestYarmList:
    @pytest.fixture
    def url(self):
        return '/api/list'

    @pytest.fixture
    def request_coro(self, yarm_client, suid):
        return yarm_client.list(suid=suid)

    class TestSuccess:
        @pytest.fixture
        def response(self, src_login):
            return {
                'rpops': [
                    {
                        'popid': pop_id,
                        'server': 'server.test',
                        'port': '993',
                        'login': src_login,
                        'email': src_login,
                        'leave_msgs': True,
                        'imap': True,
                        'use_ssl': False,
                        'is_on': '2',
                        'error_status': 'ok',
                    }
                    for pop_id in ('popid1', 'popid2')
                ]
            }

        @pytest.fixture(autouse=True)
        async def returned(self, request_coro):
            return [collector async for collector in request_coro]

        def test_returned(self, returned):
            assert_that([value.pop_id for value in returned], equal_to(['popid1', 'popid2']))

        def test_params(self, get_last_yarm_request, suid):
            assert_that(
                get_last_yarm_request().query,
                equal_to({
                    'suid': str(suid),
                    'mdb': 'pg',
                    'json': '1',
                }),
            )


@pytest.mark.parametrize('enabled', (True, False))
class TestYarmSetEnabled:
    @pytest.fixture
    def url(self):
        return '/api/enable'

    @pytest.fixture
    def request_coro(self, yarm_client, suid, pop_id, enabled):
        return yarm_client.set_enabled(suid=suid, pop_id=pop_id, enabled=enabled)

    @pytest.fixture
    def response(self):
        return {}

    @pytest.fixture(autouse=True)
    async def returned(self, request_coro):
        return await request_coro

    def test_params(self, get_last_yarm_request, suid, pop_id, enabled):
        assert_that(
            get_last_yarm_request().query,
            equal_to({
                'suid': str(suid),
                'popid': str(pop_id),
                'mdb': 'pg',
                'is_on': str(int(enabled)),
                'json': '1',
            })
        )


class TestYarmStatus:
    @pytest.fixture
    def url(self):
        return '/api/status'

    @pytest.fixture
    def request_coro(self, yarm_client, pop_id):
        return yarm_client.status(pop_id=pop_id)

    @pytest.fixture
    def response(self):
        return {
            "folders": [
                {"name": "folder1", "messages": "10", "collected": "10", "errors": "0"},
                {"name": "INBOX", "messages": "11", "collected": "5", "errors": "2"},
            ],
        }

    @pytest.fixture(autouse=True)
    async def returned(self, request_coro):
        return await request_coro

    def test_returned(self, returned):
        assert_that(
            returned,
            has_properties({
                'total': 21,
                'collected': 15,
                'errors': 2,
            }),
        )

    def test_folders(self, returned):
        assert_that(
            returned.folders,
            contains(
                has_properties({'name': 'folder1', 'collected': 10, 'total': 10, 'errors': 0}),
                has_properties({'name': 'INBOX', 'collected': 5, 'total': 11, 'errors': 2}),
            ),
        )

    def test_get_default_status(self):
        assert_that(
            YarmCollectorStatus.get_default_status(),
            has_properties({
                'collected': None,
                'total': None,
                'errors': None,
            }),
        )


class TestYarmEdit:
    @pytest.fixture
    def url(self):
        return '/api/edit'

    @pytest.fixture
    def response(self):
        return {}

    @pytest.fixture
    def request_coro(self, yarm_client, pop_id, suid, dst_email, password):
        return yarm_client.edit(pop_id=pop_id, suid=suid, dst_email=dst_email, password=password)

    @pytest.fixture(autouse=True)
    async def returned(self, request_coro):
        return await request_coro

    def test_params(self, get_last_yarm_request, suid, pop_id, dst_email):
        assert_that(
            get_last_yarm_request().query,
            equal_to({
                'mdb': 'pg',
                'suid': str(suid),
                'popid': pop_id,
                'user': dst_email,
                'json': '1',
            })
        )

    @pytest.mark.asyncio
    async def test_body(self, get_last_yarm_request, password):
        assert_that(
            await get_last_yarm_request().post(),
            equal_to({
                'password': password,
            })
        )


class TestYarmDelete:
    @pytest.fixture
    def url(self):
        return '/api/delete'

    @pytest.fixture
    def response(self):
        return {}

    @pytest.fixture
    def request_coro(self, yarm_client, pop_id, suid):
        return yarm_client.delete_collector(pop_id=pop_id, suid=suid)

    @pytest.fixture(autouse=True)
    async def returned(self, request_coro):
        return await request_coro

    def test_params(self, get_last_yarm_request, suid, pop_id, dst_email):
        assert_that(
            get_last_yarm_request().query,
            equal_to({
                'mdb': 'pg',
                'suid': str(suid),
                'popid': pop_id,
                'json': '1',
            })
        )


@pytest.mark.parametrize('error_status, expected_error_status', (
    (YarmCollector.ERROR_STATUS_OK, YarmCollector.ERROR_STATUS_OK),
    ('', YarmCollector.ERROR_STATUS_OK),
))
def test_entity_from_response(src_login, pop_id, error_status, expected_error_status):
    response = {
        'popid': pop_id,
        'server': 'server.test',
        'port': '993',
        'login': src_login,
        'email': src_login,
        'leave_msgs': True,
        'imap': True,
        'use_ssl': False,
        'is_on': '2',
        'error_status': error_status,
    }

    expected = YarmCollector(
        pop_id=pop_id,
        server='server.test',
        port=993,
        login=src_login,
        email=src_login,
        delete_msgs=False,
        imap=True,
        ssl=False,
        state=YarmCollectorState.TEMPORARY_ERROR,
        error_status=expected_error_status,
    )
    assert expected == YarmCollector.from_response(response)
