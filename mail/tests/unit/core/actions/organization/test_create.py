import pytest

from mail.beagle.beagle.core.actions.organization import CreateOrganizationAction


@pytest.mark.asyncio
class TestCreateOrganization:
    @pytest.fixture
    def org_id(self, randn):
        return randn()

    @pytest.fixture
    def returned_func(self, org_id):
        async def _inner():
            return await CreateOrganizationAction(org_id=org_id).run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    async def test_create(self, storage, returned, org_id):
        created = await storage.organization.get(org_id=org_id)
        assert returned == created
