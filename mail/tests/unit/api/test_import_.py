import pytest
import ujson
from aiohttp import StreamReader, web

from hamcrest import assert_that, has_entries, instance_of, match_equality

from mail.ipa.ipa.core.actions.import_.json import CreateImportFromJSONAction
from mail.ipa.ipa.core.entities.enums import UserImportError
from mail.ipa.ipa.core.entities.import_params import GeneralInitImportParams
from mail.ipa.ipa.core.entities.password import Password
from mail.ipa.ipa.core.entities.stat import ImportStat
from mail.ipa.ipa.core.entities.user_info import UserInfo


class TestCreateImport:
    @pytest.fixture
    def task_id(self):
        return 3

    @pytest.fixture
    def import_params_json(self):
        return {
            'server': 'server.test',
            'port': 993,
            'imap': 1,
            'ssl': 0,
            'mark_archive_read': 0,
            'delete_msgs': 1,
        }

    @pytest.fixture
    def import_params(self, import_params_json, admin_uid, user_ip, org_id):
        return GeneralInitImportParams(
            server=import_params_json['server'],
            port=import_params_json['port'],
            ssl=bool(import_params_json['ssl']),
            delete_msgs=bool(import_params_json['delete_msgs']),
            mark_archive_read=bool(import_params_json['mark_archive_read']),
            imap=bool(import_params_json['imap']),
            admin_uid=admin_uid,
            user_ip=user_ip,
            org_id=org_id,
        )

    @pytest.fixture
    def request_params(self, admin_uid, user_ip, import_params_json):
        return {
            'admin_uid': admin_uid,
            'user_ip': user_ip,
            **import_params_json,
        }

    @pytest.fixture
    def data(self):
        return None

    @pytest.fixture
    async def response(self, app, headers, org_id, request_params, data):
        return await app.post(f'/import/{org_id}/',
                              headers=headers,
                              params=request_params,
                              data=data)

    @pytest.fixture
    async def response_json(self, response):
        return await response.json()

    class TestCSV:
        @pytest.fixture(autouse=True)
        def create_import_from_csv_mock(self, mock_action, task_id):
            from mail.ipa.ipa.core.actions.import_.csv import CreateImportFromCSVAction
            return mock_action(CreateImportFromCSVAction, task_id)

        @pytest.fixture
        def data(self):
            return b'sample;csv'

        @pytest.fixture
        def csv_name(self):
            return 'csv-name'

        @pytest.fixture
        def request_params(self, request_params, csv_name):
            request_params['name'] = csv_name
            return request_params

        @pytest.fixture
        def headers(self):
            content_type = 'text/csv'
            return {'Content-Type': content_type}

        def test_create_import_from_csv_call(self,
                                             create_import_from_csv_mock,
                                             response,
                                             org_id,
                                             admin_uid,
                                             user_ip,
                                             import_params,
                                             csv_name,
                                             ):
            create_import_from_csv_mock.assert_called_once_with(
                name=csv_name,
                stream=match_equality(instance_of(StreamReader)),
                import_params=import_params,
            )

        def test_csv_response(self, response_json, task_id):
            assert_that(
                response_json,
                has_entries({
                    'status': 'success',
                    'data': has_entries({
                        'task_id': str(task_id),
                    }),
                })
            )

        class TestSchemaErrorEmptyDomain:
            @pytest.fixture
            def request_params(self, request_params):
                request_params['server'] = ''
                return request_params

            @pytest.mark.asyncio
            async def test_csv_empty_domain(self, response_json):
                assert_that(
                    response_json,
                    has_entries({
                        'status': 'fail',
                    })
                )

        class TestSchemaErrorInvalidPort:
            @pytest.fixture
            def request_params(self, request_params):
                request_params['port'] = 65537
                return request_params

            @pytest.mark.asyncio
            async def test_csv_invalid_port(self, response_json):
                assert_that(
                    response_json,
                    has_entries({
                        'status': 'fail',
                    })
                )

    class TestJSON:
        @pytest.fixture(autouse=True)
        def create_import_from_json_mock(self, mock_action):
            return mock_action(CreateImportFromJSONAction)

        @pytest.fixture
        def headers(self):
            return {'Content-Type': 'application/json'}

        @pytest.fixture
        def users(self, rands):
            return [
                UserInfo(
                    login=rands(),
                    password=Password.from_plain(rands()),
                    src_login=rands(),
                )
                for _ in range(3)
            ]

        @pytest.fixture
        def data(self, users):
            return ujson.dumps({
                'users': [
                    {
                        'login': user.login,
                        'password': user.password.value(),
                        'src_login': user.src_login,
                    }
                    for user in users
                ]
            })

        def test_create_import_from_json_call(self, create_import_from_json_mock, import_params, users, response):
            create_import_from_json_mock.assert_called_once_with(params=import_params, users=users)

        class TestSchemaErrorEmptyDomain:
            @pytest.fixture
            def request_params(self, request_params):
                request_params['server'] = ''
                return request_params

            @pytest.mark.asyncio
            async def test_json_empty_domain(self, response_json):
                assert_that(
                    response_json,
                    has_entries({
                        'status': 'fail',
                    })
                )

        class TestSchemaErrorInvalidPort:
            @pytest.fixture
            def request_params(self, request_params):
                request_params['port'] = 65537
                return request_params

            @pytest.mark.asyncio
            async def test_json_invalid_port(self, response_json):
                assert_that(
                    response_json,
                    has_entries({
                        'status': 'fail',
                    })
                )

    class TestUnknownMimeError:
        @pytest.fixture
        def headers(self):
            return {}

        def test_unknown_mime(self, response_json):
            assert_that(
                response_json,
                has_entries({
                    'code': 415,
                    'data': has_entries({
                        'message': 'Unsupported media type',
                    }),
                })
            )


class TestStatImport:
    @pytest.fixture
    def import_stat(self, randn):
        return ImportStat(
            total=randn(),
            errors=randn(),
            finished=randn(),
        )

    @pytest.fixture(autouse=True)
    def action(self, mock_action, import_stat):
        from mail.ipa.ipa.core.actions.stats.summary import GetImportStatAction
        return mock_action(GetImportStatAction, import_stat)

    @pytest.fixture
    async def response(self, org_id, app):
        return await app.get(f'/import/{org_id}/stat/')

    @pytest.fixture
    async def response_data(self, response):
        return (await response.json())['data']

    def test_response_data(self, import_stat, response_data):
        assert response_data == {
            'total': import_stat.total,
            'errors': import_stat.errors,
            'finished': import_stat.finished,
        }

    def test_action_call(self, org_id, action, response):
        action.assert_called_once_with(org_id=org_id)


class TestInfoImport:
    @pytest.fixture
    def has_more(self):
        return True

    @pytest.fixture
    async def result(self, org_id, create_user, create_collector, has_more):
        result = [
            (await create_user(org_id), None, UserImportError.UNKNOWN_ERROR)
            for _ in range(3)
        ]
        for _ in range(3):
            user = await create_user(org_id)
            collector = await create_collector(user_id=user.user_id)
            error = UserImportError.UNKNOWN_ERROR
            result.append((user, collector, error))
        return result, has_more

    @pytest.fixture(autouse=True)
    def action(self, mock_action, result):
        from mail.ipa.ipa.core.actions.stats.info import GetImportInfoAction
        return mock_action(GetImportInfoAction, result)

    @pytest.fixture
    def only_errors(self):
        return True

    @pytest.fixture
    async def response(self, app, org_id, only_errors):
        params = {}
        if only_errors is not None:
            params['only_errors'] = str(only_errors).lower()
        return await app.get(f'/import/{org_id}/', params=params)

    @pytest.fixture
    async def response_json(self, response):
        return await response.json()

    def test_response_status(self, response):
        assert response.status == 200

    @pytest.mark.parametrize('has_more', (True, False))
    def test_response(self, result, response_json):
        assert response_json == {
            'code': 200,
            'status': 'success',
            'data': {
                'has_more': result[1],
                'collectors': [
                    {
                        'uid': user.uid,
                        'login': user.login,
                        'error': error.value,
                        **(
                            {} if collector is None else {
                                'collected': collector.collected,
                                'total': collector.collected,
                                'errors': collector.collected,
                                'params': {
                                    'delete_msgs': collector.params.delete_msgs,
                                    'imap': collector.params.imap,
                                    'mark_archive_read': collector.params.mark_archive_read,
                                    'port': collector.params.port,
                                    'server': collector.params.server,
                                    'ssl': collector.params.ssl,
                                    'src_login': collector.params.src_login,
                                }
                            }
                        )
                    }
                    for user, collector, error in result[0]
                ],
            }
        }

    @pytest.mark.parametrize('only_errors', (None, True, False))
    def test_call(self, org_id, action, response, only_errors):
        action.assert_called_once_with(org_id=org_id, only_errors=only_errors or False)


class TestReportImportHandler:
    @pytest.fixture
    def csv_content(self):
        return '1,2,3\n4,5,6\n'

    @pytest.fixture(autouse=True)
    def action(self, mock_action, csv_content):
        async def dummy_run(self):
            await self._init_kwargs['output'].write(csv_content.encode('utf-8'))

        from mail.ipa.ipa.core.actions.report import WriteCSVReportAction
        return mock_action(WriteCSVReportAction, action_func=dummy_run)

    @pytest.fixture
    async def response(self, org_id, app):
        return await app.get(f'/import/{org_id}/report/')

    @pytest.fixture
    async def response_body(self, response):
        return await response.text()

    def test_response_status(self, response):
        assert response.status == 200

    def test_response_body(self, csv_content, response_body):
        assert response_body == csv_content

    def test_response_headers(self, response):
        assert_that(
            response.headers,
            has_entries({
                'Content-Type': 'text/csv; charset=utf-8',
                'Content-Disposition': 'attachment; filename="report.csv"',
            })
        )

    def test_action_call(self, org_id, action, response):
        action.assert_called_once_with(
            org_id=org_id,
            output=match_equality(instance_of(web.StreamResponse)),
        )
