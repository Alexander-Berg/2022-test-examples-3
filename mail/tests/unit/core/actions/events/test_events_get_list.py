import pytest

from mail.ipa.ipa.core.actions.events.get_list import GetEventsListAction
from mail.ipa.ipa.tests.contracts import ActionTestContract


class TestGetEventsListAction(ActionTestContract):
    @pytest.fixture
    def action_class(self):
        return GetEventsListAction

    @pytest.fixture
    async def event(self, create_event, organization, existing_user, pop_id):
        return await create_event(org_id=organization.org_id)

    @pytest.fixture
    def params(self, org_id):
        return {
            'org_id': org_id,
        }

    @pytest.mark.asyncio
    async def test_result(self, event, returned):
        assert returned == [event]

    class TestOffset:
        @pytest.fixture
        def params(self, org_id, event):
            return {
                'org_id': org_id,
                'offset': 1
            }

        @pytest.mark.asyncio
        async def test_result(self, returned):
            assert returned == []
