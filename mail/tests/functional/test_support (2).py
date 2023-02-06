from dataclasses import asdict
from datetime import timezone

import pytest

from sendr_utils import enum_name, enum_value

from hamcrest import assert_that, contains_inanyorder, has_entries

from mail.ipa.ipa.core.entities.enums import UserImportError
from mail.ipa.ipa.interactions import YarmClient
from mail.ipa.ipa.interactions.yarm.exceptions import YarmAuthError, YarmConnectError, YarmLoginError
from mail.ipa.ipa.storage.exceptions import CollectorNotFound


class BaseTestSupport:
    @pytest.fixture
    async def events(self, organization, create_event):
        return [await create_event() for _ in range(3)]

    @pytest.fixture
    async def collectors(self, organization, randn, randitem, create_user, create_collector, rands):
        result = []
        errors = [
            YarmAuthError.CODE,
            YarmConnectError.CODE,
            YarmLoginError.CODE
        ]

        for _ in range(randn(min=3, max=3)):
            user = await create_user(organization.org_id, suid=randn())
            collector = await create_collector(user_id=user.user_id,
                                               status=(randitem(errors)),
                                               pop_id=rands())
            collector.user = user
            result.append(collector)

        return result

    @pytest.fixture
    async def response_json(self, response):
        return await response.json()

    def test_response_status(self, response):
        assert response.status == 200


class TestSupportCollectors(BaseTestSupport):
    @pytest.fixture
    async def response(self, app, org_id):
        return await app.get(f'/support/{org_id}/collectors/')

    def test_response_json(self, collectors, response_json):
        assert_that(
            response_json,
            has_entries({
                'data': contains_inanyorder(*[
                    has_entries({
                        'uid': collector.uid,
                        'collector_id': collector.collector_id,
                        'login': collector.login,
                        'total': collector.total,
                        'collected': collector.collected,
                        'errors': collector.errors,
                        'error': enum_value(UserImportError.get_error(collector_status=collector.status)),
                        'pop_id': collector.pop_id,
                        'params': has_entries(asdict(collector.params))
                    })
                    for collector in collectors
                ]),
                'code': 200,
                'status': 'success',
            })
        )


class TestSupportCollectorsStatuses(BaseTestSupport):
    @pytest.fixture
    async def response(self, app, org_id):
        return await app.get(f'/support/{org_id}/collectors-statuses/')

    def test_response_json(self, collectors, response_json):
        assert_that(
            response_json,
            has_entries({
                'data': contains_inanyorder(*set([collector.status for collector in collectors])),
                'code': 200,
                'status': 'success',
            })
        )


class TestSupportCollector:
    @pytest.fixture
    async def collector(self, randn, organization, create_user, create_collector, rands):
        user = await create_user(organization.org_id, suid=randn())
        collector = await create_collector(user_id=user.user_id,
                                           status=YarmAuthError.CODE,
                                           pop_id=rands())
        collector.user = user
        return collector

    @pytest.fixture(autouse=True)
    def mock_yarm_status(self, mocker, coromock, yarm_collector_status):
        return mocker.patch.object(YarmClient, 'status', coromock(yarm_collector_status))

    @pytest.fixture(autouse=True)
    def mock_yarm_get_collector(self, mocker, coromock, yarm_collector):
        return mocker.patch.object(YarmClient, 'get_collector', coromock(yarm_collector))

    @pytest.fixture(autouse=True)
    def mock_yarm_delete_collector(self, mocker, coromock, yarm_collector):
        return mocker.patch.object(YarmClient, 'delete_collector', coromock())

    @pytest.fixture(autouse=True)
    def mock_yarm_set_enabled(self, mocker, coromock, yarm_collector):
        return mocker.patch.object(YarmClient, 'set_enabled', coromock())

    class TestGet(BaseTestSupport):
        @pytest.fixture
        async def response(self, app, org_id, collector):
            return await app.get(f'/support/{org_id}/collectors/{collector.collector_id}/')

        def test_response_json(self, collector, yarm_collector, response_json):
            assert_that(
                response_json,
                has_entries({
                    'data': has_entries({
                        'uid': collector.uid,
                        'collector_id': collector.collector_id,
                        'login': collector.login,
                        'total': collector.total,
                        'collected': collector.collected,
                        'errors': collector.errors,
                        'error': enum_value(UserImportError.get_error(collector_status=collector.status)),
                        'pop_id': collector.pop_id,
                        'params': has_entries(asdict(collector.params)),
                        'yarm_collector': {
                            'pop_id': yarm_collector.pop_id,
                            'server': yarm_collector.server,
                            'port': yarm_collector.port,
                            'login': yarm_collector.login,
                            'ssl': yarm_collector.ssl,
                            'email': yarm_collector.email,
                            'error_status': yarm_collector.error_status,
                            'imap': yarm_collector.imap,
                            'state': enum_name(yarm_collector.state),
                            'delete_msgs': yarm_collector.delete_msgs,
                            'status': {
                                'collected': yarm_collector.status.collected,
                                'errors': yarm_collector.status.errors,
                                'total': yarm_collector.status.total,
                                'folders': [
                                    {
                                        'name': folder.name,
                                        'collected': folder.collected,
                                        'errors': folder.errors,
                                        'total': folder.total,
                                    } for folder in yarm_collector.status.folders
                                ]
                            }
                        }
                    }),
                    'code': 200,
                    'status': 'success',
                })
            )

    class TestDelete(BaseTestSupport):
        @pytest.fixture
        async def response(self, app, org_id, collector):
            return await app.delete(f'/support/{org_id}/collectors/{collector.collector_id}/')

        @pytest.mark.asyncio
        async def test_delete(self, storage, collector, response):
            with pytest.raises(CollectorNotFound):
                await storage.collector.get(collector.collector_id)

        def test_response_json(self, response_json):
            assert_that(
                response_json,
                has_entries({
                    'data': None,
                    'code': 200,
                    'status': 'success',
                })
            )

    class TestPut(BaseTestSupport):
        @pytest.fixture(params=(True, False))
        def enabled(self, request):
            return request.param

        @pytest.fixture
        async def response(self, app, org_id, enabled, collector):
            return await app.put(f'/support/{org_id}/collectors/{collector.collector_id}/', json={'enabled': enabled})

        @pytest.mark.asyncio
        async def test_put(self, storage, collector, enabled, response):
            collector = await storage.collector.get(collector.collector_id)
            assert collector.enabled == enabled

        def test_response_json(self, response_json):
            assert_that(
                response_json,
                has_entries({
                    'data': None,
                    'code': 200,
                    'status': 'success',
                })
            )


class TestSupportEvents(BaseTestSupport):
    @pytest.fixture
    async def response(self, app, org_id):
        return await app.get(f'/support/{org_id}/events/')

    def test_response_json(self, events, response_json):
        assert_that(
            response_json,
            has_entries({
                'data': contains_inanyorder(*[
                    has_entries({
                        'event_type': enum_value(event.event_type),
                        'org_id': event.org_id,
                        'revision': event.revision,
                        'event_id': event.event_id,
                        'data': event.data,
                        'created_at': event.created_at.astimezone(timezone.utc).isoformat(),
                    })
                    for event in events
                ]),
                'code': 200,
                'status': 'success',
            })
        )


class TestSupportUsers(BaseTestSupport):
    @pytest.fixture
    async def response(self, app, org_id):
        return await app.get(f'/support/{org_id}/users/')

    def test_response_json(self, existing_user, response_json):
        assert_that(
            response_json,
            has_entries({
                'data': contains_inanyorder(*[
                    has_entries({
                        'uid': existing_user.uid,
                        'suid': existing_user.suid,
                        'user_id': existing_user.user_id,
                        'login': existing_user.login,
                        'created_at': existing_user.created_at.astimezone(timezone.utc).isoformat(),
                        'error': existing_user.error
                    })
                ]),
                'code': 200,
                'status': 'success',
            })
        )
