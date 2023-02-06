import pytest

from mail.beagle.beagle.core.actions.user.delete import DeleteUserAction, GetDependentUIDsUserAction
from mail.beagle.beagle.storage.exceptions import UserNotFound


class TestDeleteUserAction:
    @pytest.fixture
    def affected_uids(self, randn):
        return [randn() for _ in range(5)]

    @pytest.fixture(autouse=True)
    def get_dependent_uids_mock(self, mock_action, affected_uids):
        return mock_action(GetDependentUIDsUserAction, affected_uids)

    @pytest.fixture
    def returned_func(self, user):
        async def _inner():
            return await DeleteUserAction(user).run()

        return _inner

    @pytest.mark.asyncio
    async def test_deletes(self, storage, user, returned):
        with pytest.raises(UserNotFound):
            await storage.user.get(user.org_id, user.uid)

    def test_returns_affected_uids(self, affected_uids, returned):
        assert returned == affected_uids
