import re

import pytest
from aiohttp import web

from sendr_utils import alist

from hamcrest import assert_that, contains_inanyorder, equal_to, has_entries, has_items, has_properties, not_none

from mail.ipa.ipa.core.entities.enums import EventType, TaskType
from mail.ipa.ipa.core.entities.event import Event
from mail.ipa.ipa.core.entities.password import Password
from mail.ipa.ipa.tests.data.create_import_data import CSV_ERRORS


@pytest.fixture
def request_admin_uid(admin_uid):
    return admin_uid


@pytest.fixture
def general_import_params():
    return {
        'server': 'server.test',
        'port': 993,
        'imap': 1,
        'ssl': 0,
        'mark_archive_read': 1,
        'delete_msgs': 0,
    }


@pytest.fixture
def csv_name():
    return 'csv-name'


@pytest.fixture
def request_params(request_admin_uid, user_ip, general_import_params, csv_name):
    return {
        'admin_uid': request_admin_uid,
        'user_ip': user_ip,
        'name': csv_name,
        **general_import_params,
    }


class EventTestContract:
    @pytest.fixture(autouse=True)
    async def setup_event(self, storage, org_id, organization):
        await storage.event.create(Event(org_id=org_id, revision=1, event_type=EventType.STOP))

    @pytest.mark.asyncio
    async def test_revision_increased(self, response, storage, org_id):
        org = await storage.organization.get(org_id)
        assert_that(org.revision, equal_to(2))

    @pytest.mark.asyncio
    async def test_event_created(self, response, storage, org_id):
        events = await alist(storage.event.find(org_id=org_id, order_by=('-event_id',), limit=1))
        assert_that(events[0].event_type, equal_to(EventType.START))


class TestCreateImportCSV:
    @pytest.fixture
    def ipa_settings(self, ipa_settings):
        ipa_settings.CSV_MAX_SIZE = 1024 * 256
        ipa_settings.CSV_FIELD_YANDEXMAIL_LOGIN = 'ylog'
        ipa_settings.CSV_FIELD_SRC_LOGIN = 'slog'
        ipa_settings.CSV_FIELD_SRC_PASSWORD = 'spwd'
        ipa_settings.CSV_REQUIRED_HEADERS = [
            ipa_settings.CSV_FIELD_SRC_LOGIN,
            ipa_settings.CSV_FIELD_SRC_PASSWORD,
            ipa_settings.CSV_FIELD_YANDEXMAIL_LOGIN,
        ]
        return ipa_settings

    @pytest.fixture
    def csv(self):
        return 'ylog,slog,spwd\nval1,val2,val3'

    @pytest.fixture
    def file_key(self):
        return '123123/keykey'

    @pytest.fixture(autouse=True)
    def setup(self, mock_mds_write, ipa_settings, file_key, organization):
        response = f"""
        <?xml version="1.0" encoding="utf-8"?>
        <post
            obj="filename"
            id="0a443519e96915cfe"
            groups="2"
            size="8"
            key="{file_key}"
        >
            <complete addr="test1.mdst.yandex.net" path="/srv/storage/37/5/data-0.0" group="1395718" status="0"/>
            <complete addr="test2.mdst.yandex.net" path="/srv/storage/15/3/data-0.0" group="1397812" status="0"/>
            <written>2</written>
        </post>
        """.strip()
        path = re.compile(f'/upload-{ipa_settings.MDS_NAMESPACE}/.*')
        mock_mds_write(path, web.Response(body=response))

    @pytest.fixture
    async def response(self, app, org_id, request_params, csv):
        return await app.post(f'/import/{org_id}/',
                              headers={'Content-Type': 'text/csv'},
                              params=request_params,
                              data=csv)

    @pytest.fixture
    async def response_json(self, response):
        return await response.json()

    class TestSuccess:
        @pytest.mark.asyncio
        async def test_success_response(self, response_json):
            assert_that(
                response_json,
                has_entries({
                    'status': 'success',
                    'data': has_entries({
                        'task_id': not_none()
                    })
                })
            )

        @pytest.mark.asyncio
        async def test_success_task_created(self,
                                            response_json,
                                            storage,
                                            org_id,
                                            admin_uid,
                                            user_ip,
                                            file_key,
                                            csv_name,
                                            general_import_params):
            task_id = response_json['data']['task_id']
            task = await storage.task.get(task_id)
            assert_that(
                task,
                has_properties({
                    'params': {
                        'import_params': {
                            'org_id': org_id,
                            'admin_uid': admin_uid,
                            'user_ip': user_ip,
                            **general_import_params,
                        },
                        'csv_key': file_key,
                    },
                    'meta_info': {
                        'name': csv_name,
                    }
                })
            )

        class TestHasStopEvent(EventTestContract):
            pass

    class TestCSVErrors:
        @pytest.mark.asyncio
        @pytest.mark.parametrize('expected_error, expected_params, csv', CSV_ERRORS)
        async def test_raises_csv_error(self, response_json, expected_error, expected_params):
            expected_data = {
                'message': expected_error,
            }
            if expected_params:
                expected_data['params'] = expected_params

            assert_that(
                response_json,
                has_entries({
                    'status': 'fail',
                    'data': has_entries(expected_data)
                })
            )


class TestCreateImportJSON:
    @pytest.fixture
    def request_json(self, rands):
        return {
            'users': [
                {
                    'login': rands(),
                    'password': rands(),
                    'src_login': rands(),
                }
                for _ in range(3)
            ],
        }

    @pytest.fixture
    async def response(self, app, org_id, organization, request_params, request_json, mock_encryptor_iv):
        return await app.post(f'/import/{org_id}/', params=request_params, json=request_json)

    @pytest.fixture
    async def response_json(self, response):
        return await response.json()

    @pytest.fixture
    async def tasks(self, storage):
        return [t async for t in storage.task.find()]

    @pytest.fixture
    async def users(self, storage, org_id):
        return [u async for u in storage.user.find(org_id=org_id)]

    def test_response_status(self, response):
        assert response.status == 200

    def test_response_json(self, response_json):
        assert_that(
            response_json, has_entries({
                'code': 200,
                'status': 'success',
                'data': has_entries({
                    'task_id': not_none()
                })
            })
        )

    @pytest.mark.asyncio
    async def test_creates_organization(self, storage, org_id, response):
        assert await storage.organization.check_exists(org_id)

    @pytest.mark.asyncio
    async def test_creates_task(self,
                                storage,
                                org_id,
                                response,
                                tasks,
                                request_json,
                                request_params,
                                ):
        assert_that(
            tasks,
            has_items(*[
                has_properties({
                    'task_type': TaskType.INIT_IMPORT,
                    'params': has_entries({
                        'users': contains_inanyorder(*[
                            {
                                'src_login': user['src_login'],
                                'login': user['login'].lower(),
                                'password': Password.from_plain(user['password']).encrypted(),
                            }
                            for user in request_json['users']
                        ]),
                        'params': has_entries({
                            'admin_uid': request_params['admin_uid'],
                            'delete_msgs': bool(request_params['delete_msgs']),
                            'imap': bool(request_params['imap']),
                            'mark_archive_read': bool(request_params['mark_archive_read']),
                            'org_id': org_id,
                            'port': request_params['port'],
                            'server': request_params['server'],
                            'ssl': bool(request_params['ssl']),
                            'user_ip': request_params['user_ip'],
                        })
                    }),
                })
            ]),
        )

    class TestHasStopEvent(EventTestContract):
        pass
