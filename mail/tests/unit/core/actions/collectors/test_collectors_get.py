import pytest

from mail.ipa.ipa.core.actions.collectors.get import GetCollectorAction
from mail.ipa.ipa.interactions import YarmClient
from mail.ipa.ipa.tests.contracts import ActionTestContract


class TestGetCollectorAction(ActionTestContract):
    @pytest.fixture
    def action_class(self):
        return GetCollectorAction

    @pytest.fixture
    async def collector(self, create_collector, existing_user, pop_id):
        return await create_collector(existing_user.user_id, pop_id=pop_id, status='UNKNOWN_ERROR')

    @pytest.fixture
    def params(self, org_id, collector):
        return {
            'org_id': org_id,
            'collector_id': collector.collector_id
        }

    @pytest.fixture(autouse=True)
    def mock_yarm_status(self, mocker, coromock, yarm_collector_status):
        return mocker.patch.object(YarmClient, 'status', coromock(yarm_collector_status))

    @pytest.fixture(autouse=True)
    def mock_yarm_get_collector(self, mocker, coromock, yarm_collector):
        return mocker.patch.object(YarmClient, 'get_collector', coromock(yarm_collector))

    @pytest.mark.asyncio
    async def test_result(self, collector, existing_user, yarm_collector, returned):
        collector.user = existing_user
        collector.yarm_collector = yarm_collector
        assert returned == collector
