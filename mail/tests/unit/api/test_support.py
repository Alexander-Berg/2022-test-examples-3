from datetime import datetime

import pytest

from sendr_utils import enum_name, enum_value, utcnow

from hamcrest import assert_that, contains_inanyorder, has_entries

from mail.ipa.ipa.core.entities.collector import Collector
from mail.ipa.ipa.core.entities.enums import EventType, UserImportError
from mail.ipa.ipa.core.entities.event import Event
from mail.ipa.ipa.core.entities.user import User
from mail.ipa.ipa.interactions.yarm.exceptions import YarmConnectError


class BaseSupportTest:
    @pytest.fixture
    def org_id(self, randn):
        return randn()

    @pytest.fixture
    def result(self):
        raise NotImplementedError

    @pytest.fixture
    def result_json(self, result):
        return result

    @pytest.fixture
    async def response_json(self, response):
        return await response.json()

    def test_response(self, result, response_json, result_json):
        assert_that(response_json['data'], contains_inanyorder(*result_json))


class TestCollectorsListHandler(BaseSupportTest):
    @pytest.fixture
    def params(self, rands, randn):
        return {
            'offset': randn(min=1, max=20),
            'limit': randn(min=1, max=20),
            'login': rands(),
            'status': rands(),
            'created_from': utcnow().isoformat(),
            'created_to': utcnow().isoformat()
        }

    @pytest.fixture
    def result(self, randn, rands, randbool):
        return [
            Collector(
                collector_id=randn(),
                user=User(
                    uid=randn(),
                    login=rands(),
                    org_id=randn()
                ),
                user_id=randn(),
                total=randn(),
                collected=randn(),
                errors=randn(),
                status=YarmConnectError.CODE,
                pop_id=rands(),
                params={
                    'src_login': rands(),
                    'server': rands(),
                    'port': randn(),
                    'imap': randbool(),
                    'ssl': randbool(),
                    'mark_archive_read': randbool(),
                    'delete_msgs': randbool(),
                },
            )
        ]

    @pytest.fixture(autouse=True)
    def action(self, mock_action, result):
        from mail.ipa.ipa.core.actions.collectors.get_list import GetCollectorsListAction
        return mock_action(GetCollectorsListAction, result)

    @pytest.fixture
    def result_json(self, result):
        return [
            {
                'uid': item.user.uid,
                'login': item.user.login,
                'params': item.params,
                'collector_id': item.collector_id,
                'total': item.total,
                'collected': item.collected,
                'errors': item.errors,
                'error': UserImportError.COLLECTOR_CONNECT_ERROR.value,
                'pop_id': item.pop_id,
            } for item in result
        ]

    @pytest.fixture
    async def response(self, app, org_id, params):
        return await app.get(f'/support/{org_id}/collectors/', params=params)

    def test_params(self, action, org_id, response, params):
        action.assert_called_once_with(
            org_id=org_id,
            offset=params['offset'],
            login=params['login'],
            status=params['status'],
            limit=params['limit'],
            created_from=datetime.fromisoformat(params['created_from']),
            created_to=datetime.fromisoformat(params['created_to'])
        )


class TestCollectorsStatusesHandler(BaseSupportTest):
    @pytest.fixture
    def result(self, randn, rands, randbool):
        return [rands() for _ in range(randn(min=5, max=10))]

    @pytest.fixture(autouse=True)
    def action(self, mock_action, result):
        from mail.ipa.ipa.core.actions.collectors.get_statuses import GetCollectorsStatusesAction
        return mock_action(GetCollectorsStatusesAction, result)

    @pytest.fixture
    async def response(self, app, org_id):
        return await app.get(f'/support/{org_id}/collectors-statuses/')

    def test_params(self, action, org_id, response):
        action.assert_called_once_with(org_id=org_id)


class TestCollectorHandler:
    @pytest.fixture
    def collector_id(self, randn):
        return randn()

    class TestGet(BaseSupportTest):
        @pytest.fixture
        def result(self, randn, rands, randitem, yarm_collector, randbool):
            return Collector(
                collector_id=randn(),
                user=User(
                    uid=randn(),
                    login=rands(),
                    org_id=randn()
                ),
                user_id=randn(),
                total=randn(),
                collected=randn(),
                errors=randn(),
                status=YarmConnectError.CODE,
                pop_id=rands(),
                yarm_collector=yarm_collector,
                params={
                    'src_login': rands(),
                    'server': rands(),
                    'port': randn(),
                    'imap': randbool(),
                    'ssl': randbool(),
                    'mark_archive_read': randbool(),
                    'delete_msgs': randbool(),
                },
            )

        @pytest.fixture
        def result_json(self, result):
            return {
                'uid': result.user.uid,
                'login': result.user.login,
                'params': result.params,
                'collector_id': result.collector_id,
                'total': result.total,
                'collected': result.collected,
                'errors': result.errors,
                'error': enum_value(UserImportError.COLLECTOR_CONNECT_ERROR),
                'pop_id': result.pop_id,
                'yarm_collector': {
                    'pop_id': result.yarm_collector.pop_id,
                    'server': result.yarm_collector.server,
                    'port': result.yarm_collector.port,
                    'login': result.yarm_collector.login,
                    'ssl': result.yarm_collector.ssl,
                    'email': result.yarm_collector.email,
                    'error_status': result.yarm_collector.error_status,
                    'imap': result.yarm_collector.imap,
                    'state': enum_name(result.yarm_collector.state),
                    'delete_msgs': result.yarm_collector.delete_msgs,
                    'status': {
                        'collected': result.yarm_collector.status.collected,
                        'errors': result.yarm_collector.status.errors,
                        'total': result.yarm_collector.status.total,
                        'folders': [
                            {
                                'name': folder.name,
                                'collected': folder.collected,
                                'errors': folder.errors,
                                'total': folder.total,
                            } for folder in result.yarm_collector.status.folders
                        ]
                    }
                }
            }

        @pytest.fixture(autouse=True)
        def action(self, mock_action, result):
            from mail.ipa.ipa.core.actions.collectors.get import GetCollectorAction
            return mock_action(GetCollectorAction, result)

        @pytest.fixture
        async def response(self, app, org_id, collector_id):
            return await app.get(f'/support/{org_id}/collectors/{collector_id}/')

        def test_response(self, result, response_json, result_json):
            assert_that(response_json['data'], has_entries(result_json))

        def test_params(self, action, org_id, collector_id, response):
            action.assert_called_once_with(org_id=org_id, collector_id=collector_id)

    class TestPut:
        @pytest.fixture
        def result(self):
            return None

        @pytest.fixture
        def enabled(self, randbool):
            return randbool()

        @pytest.fixture(autouse=True)
        def action(self, mock_action, result):
            from mail.ipa.ipa.core.actions.collectors.set_enabled import SetCollectorEnabledAction
            return mock_action(SetCollectorEnabledAction, result)

        @pytest.fixture
        async def response(self, app, org_id, collector_id, enabled):
            return await app.put(f'/support/{org_id}/collectors/{collector_id}/', json={'enabled': enabled})

        def test_params(self, action, org_id, collector_id, enabled, response):
            action.assert_called_once_with(org_id=org_id, collector_id=collector_id, enabled=enabled)

    class TestDelete:
        @pytest.fixture
        def result(self):
            return None

        @pytest.fixture(autouse=True)
        def action(self, mock_action, result):
            from mail.ipa.ipa.core.actions.collectors.remove import RemoveCollectorAction
            return mock_action(RemoveCollectorAction, result)

        @pytest.fixture
        async def response(self, app, org_id, collector_id):
            return await app.delete(f'/support/{org_id}/collectors/{collector_id}/')

        def test_params(self, action, org_id, collector_id, response):
            action.assert_called_once_with(org_id=org_id, collector_id=collector_id)


class TestEventsListHandler(BaseSupportTest):
    @pytest.fixture
    def params(self, randn, randitem):
        return {
            'offset': randn(min=1, max=20),
            'limit': randn(min=1, max=20),
            'created_from': utcnow().isoformat(),
            'created_to': utcnow().isoformat(),
            'event_type': enum_value(randitem(list(EventType))),
        }

    @pytest.fixture
    def result(self, randn, randitem, rands, randbool):
        return [
            Event(
                event_type=randitem(list(EventType)),
                org_id=randn(),
                revision=randn(),
                event_id=randn(),
                data={rands(): rands() for _ in range(randn(min=5, max=10))}
            )
        ]

    @pytest.fixture
    def result_json(self, result):
        return [
            {
                'event_type': enum_value(item.event_type),
                'org_id': item.org_id,
                'revision': item.revision,
                'event_id': item.event_id,
                'created_at': item.created_at.isoformat(),
                'data': item.data,
            } for item in result
        ]

    @pytest.fixture(autouse=True)
    def action(self, mock_action, result):
        from mail.ipa.ipa.core.actions.events.get_list import GetEventsListAction
        return mock_action(GetEventsListAction, result)

    @pytest.fixture
    async def response(self, app, org_id, params):
        return await app.get(f'/support/{org_id}/events/', params=params)

    def test_params(self, action, org_id, response, params):
        action.assert_called_once_with(
            org_id=org_id,
            offset=params['offset'],
            limit=params['limit'],
            created_from=datetime.fromisoformat(params['created_from']),
            created_to=datetime.fromisoformat(params['created_to']),
            event_type=EventType(params['event_type'])
        )


class TestUsersListHandler(BaseSupportTest):
    @pytest.fixture
    def params(self, randn, rands):
        return {
            'offset': randn(min=1, max=20),
            'limit': randn(min=1, max=20),
            'login': rands(),
        }

    @pytest.fixture
    def result(self, randn, rands):
        return [
            User(
                org_id=randn(),
                login=rands(),
                user_id=randn(),
                uid=randn(),
                suid=randn(),
            )
        ]

    @pytest.fixture
    def result_json(self, result):
        return [
            {
                'uid': item.uid,
                'suid': item.suid,
                'user_id': item.user_id,
                'login': item.login,
                'created_at': item.created_at.isoformat(),
                'error': item.error,
            }
            for item in result
        ]

    @pytest.fixture(autouse=True)
    def action(self, mock_action, result):
        from mail.ipa.ipa.core.actions.users.get_list import GetUsersListAction
        return mock_action(GetUsersListAction, result)

    @pytest.fixture
    async def response(self, app, org_id, params):
        return await app.get(f'/support/{org_id}/users/', params=params)

    def test_params(self, action, org_id, response, params):
        action.assert_called_once_with(
            org_id=org_id,
            offset=params['offset'],
            limit=params['limit'],
            login=params['login']
        )
