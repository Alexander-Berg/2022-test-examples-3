import pytest

from hamcrest import assert_that, equal_to

from mail.ipa.ipa.core.actions.users.remove_collectors import RemoveUserCollectorsAction
from mail.ipa.ipa.interactions.yarm import YarmClient
from mail.ipa.ipa.tests.contracts import ActionTestContract


class TestRemoveUserCollectors(ActionTestContract):
    @pytest.fixture
    def action_class(self):
        return RemoveUserCollectorsAction

    @pytest.fixture
    def params(self, existing_user):
        return {'user_id': existing_user.user_id}

    @pytest.fixture
    def existing_pop_ids(self):
        return ('pop1', 'pop2', 'pop3', 'pop4', 'pop5')

    @pytest.fixture(autouse=True)
    async def collectors(self, create_collector, existing_user, existing_pop_ids):
        return [
            await create_collector(existing_user.user_id, pop_id=popid) for popid in existing_pop_ids
        ] + [await create_collector(existing_user.user_id, pop_id=None)]

    @pytest.fixture(autouse=True)
    async def mock_yarm(self, mocker, coromock):
        return mocker.patch.object(YarmClient, 'delete_collector', coromock())

    @pytest.mark.asyncio
    async def test_collectors_deleted(self, storage, existing_user, returned):
        assert_that(
            [c async for c in storage.collector.find(user_id=existing_user.user_id)],
            equal_to([]),
        )

    def test_yarm_called(self, mocker, mock_yarm, existing_pop_ids, existing_user, returned):
        mock_yarm.assert_has_calls(
            [mocker.call(suid=existing_user.suid, pop_id=pop_id) for pop_id in existing_pop_ids],
            any_order=True,
        )
