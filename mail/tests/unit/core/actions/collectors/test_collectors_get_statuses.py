import pytest

from mail.ipa.ipa.core.actions.collectors.get_statuses import GetCollectorsStatusesAction
from mail.ipa.ipa.tests.contracts import ActionTestContract


class TestGetCollectorsStatusesAction(ActionTestContract):
    @pytest.fixture
    def action_class(self):
        return GetCollectorsStatusesAction

    @pytest.fixture
    async def collector(self, create_collector, existing_user, pop_id):
        return await create_collector(existing_user.user_id, pop_id=pop_id, status='UNKNOWN_ERROR')

    @pytest.fixture
    def params(self, org_id):
        return {
            'org_id': org_id,
        }

    @pytest.mark.asyncio
    async def test_result(self, collector, returned):
        assert returned == [collector.status]
