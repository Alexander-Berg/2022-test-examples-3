import pytest

from sendr_utils import alist

from hamcrest import assert_that, contains_inanyorder

from mail.beagle.beagle.core.entities.user import User
from mail.beagle.beagle.storage.exceptions import UserNotFound


@pytest.mark.asyncio
class TestUserMapper:
    @pytest.fixture
    def user_entity(self, randn, rands, org):
        return User(
            org_id=org.org_id,
            uid=randn(),
            username=rands(),
            first_name=rands(),
            last_name=rands(),
        )

    @pytest.fixture
    async def user(self, storage, user_entity):
        return await storage.user.create(user_entity)

    @pytest.fixture
    def func_now(self, mocker, now):
        mocker.patch('mail.beagle.beagle.storage.mappers.user.func.now', mocker.Mock(return_value=now))
        return now

    async def test_create(self, storage, user_entity, func_now):
        user = await storage.user.create(user_entity)
        user_entity.created = user_entity.updated = func_now
        assert user_entity == user

    async def test_get(self, storage, user):
        assert user == await storage.user.get(user.org_id, user.uid)

    async def test_get_not_found(self, storage, randn):
        with pytest.raises(UserNotFound):
            await storage.user.get(randn(), randn())

    async def test_delete(self, storage, user):
        await storage.user.delete(user)
        with pytest.raises(UserNotFound):
            await storage.user.get(user.org_id, user.uid)

    async def test_save(self, rands, storage, user, func_now):
        user.username = rands()
        user.first_name = rands()
        user.last_name = rands()
        updated = await storage.user.save(user)
        user.updated = func_now
        assert user == updated \
            and user == await storage.user.get(user.org_id, user.uid)

    @pytest.mark.asyncio
    class TestFind:
        async def test_org_id_filter(self, storage, org_id, users_with_other_org):
            assert_that(
                await alist(storage.user.find(org_id=org_id)),
                contains_inanyorder(*[user for user in users_with_other_org if user.org_id == org_id])
            )
