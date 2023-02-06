import pytest

from hamcrest import assert_that, equal_to

from mail.ipa.ipa.core.actions.collectors.remove import RemoveCollectorAction
from mail.ipa.ipa.interactions.yarm import YarmClient
from mail.ipa.ipa.tests.contracts import ActionTestContract


class RemoveCollectorActionContract(ActionTestContract):
    @pytest.fixture
    def action_class(self):
        return RemoveCollectorAction

    @pytest.fixture
    def params(self, existing_user, user, collector):
        return {'collector': collector}

    @pytest.fixture(autouse=True)
    async def mock_yarm(self, mocker, coromock):
        return mocker.patch.object(YarmClient, 'delete_collector', coromock())

    @pytest.mark.asyncio
    async def test_collectors_deleted(self, storage, existing_user, returned):
        assert_that(
            [c async for c in storage.collector.find(user_id=existing_user.user_id)],
            equal_to([]),
        )


class TestRemoveCollectorActionWithPopId(RemoveCollectorActionContract):
    @pytest.fixture
    async def collector(self, create_collector, existing_user, pop_id):
        return await create_collector(user_id=existing_user.user_id, pop_id=pop_id, with_user=True)

    def test_calls_yarm(self, returned, mock_yarm, pop_id, existing_user):
        mock_yarm.assert_called_once_with(suid=existing_user.suid, pop_id=pop_id)


class TestRemoveCollectorActionWithoutPopId(RemoveCollectorActionContract):
    @pytest.fixture
    async def collector(self, create_collector, existing_user):
        return await create_collector(user_id=existing_user.user_id, pop_id=None, with_user=True)

    @pytest.fixture
    def params(self, existing_user, collector):
        return {'collector': collector}

    def test_not_calls_yarm(self, returned, mock_yarm):
        mock_yarm.assert_not_called()
