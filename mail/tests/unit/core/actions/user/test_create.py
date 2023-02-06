import pytest

from mail.beagle.beagle.core.actions.user.create import CreateUserAction, OrganizationNotFoundError, User


class TestCreateUserAction:
    @pytest.fixture
    def user_entity(self, org, randn, rands):
        return User(
            org_id=org.org_id,
            uid=randn(),
            username=rands(),
            first_name=rands(),
            last_name=rands(),
        )

    @pytest.fixture
    def returned_func(self, user_entity):
        async def _inner():
            return await CreateUserAction(user_entity).run()

        return _inner

    def test_returns_user(self, user_entity, returned):
        user_entity.created = returned.created
        user_entity.updated = returned.updated
        assert returned == user_entity

    @pytest.mark.asyncio
    async def test_creates_user(self, storage, user_entity, returned):
        assert await storage.user.get(user_entity.org_id, user_entity.uid) == returned

    @pytest.mark.asyncio
    async def test_organization_does_not_exist(self, user_entity, returned_func):
        user_entity.org_id += 1
        with pytest.raises(OrganizationNotFoundError):
            await returned_func()
