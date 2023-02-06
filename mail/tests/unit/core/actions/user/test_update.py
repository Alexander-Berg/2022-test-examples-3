from copy import copy

import pytest

from mail.beagle.beagle.core.actions.user.update import UpdateUserAction, UserUpdateError


class TestUpdateUserAction:
    @pytest.fixture
    def user_entity(self, user, rands):
        user_entity = copy(user)
        user_entity.first_name = rands()
        user_entity.last_name = rands()
        return user_entity

    @pytest.fixture
    def returned_func(self, user, user_entity):
        async def _inner():
            return await UpdateUserAction(user, user_entity).run()

        return _inner

    @pytest.mark.asyncio
    async def test_updates_user_name(self, storage, user, user_entity, returned):
        user_from_db = await storage.user.get(user.org_id, user.uid)
        assert all((
            returned == user_from_db,
            returned.first_name == user_entity.first_name,
            returned.last_name == user_entity.last_name,
        ))

    @pytest.mark.asyncio
    class TestChecks:
        async def test_checks_org_id(self, user, user_entity, returned_func):
            user_entity.org_id += 1
            with pytest.raises(UserUpdateError):
                await returned_func()

        async def test_checks_uid(self, user, user_entity, returned_func):
            user_entity.uid += 1
            with pytest.raises(UserUpdateError):
                await returned_func()

        async def test_checks_username(self, user, user_entity, returned_func):
            user_entity.username += '2'
            with pytest.raises(UserUpdateError):
                await returned_func()
