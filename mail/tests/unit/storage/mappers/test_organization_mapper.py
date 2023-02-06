import pytest

from hamcrest import assert_that, equal_to

from mail.ipa.ipa.core.entities.org import Org


@pytest.fixture
def org_entity(org_id):
    DEFAULT_REVISION = 1
    return Org(org_id=org_id, revision=DEFAULT_REVISION)


@pytest.fixture
async def org(org_entity, storage):
    return await storage.organization.create(org_entity)


@pytest.mark.asyncio
class TestOrgIdIsNew:
    async def test_ensure(self, storage, dbconn, org_id):
        await storage.organization.ensure_exists(org_id)

        result = await dbconn.execute('select org_id from ipa.organizations')
        rows = [row['org_id'] async for row in result]
        assert_that(rows, equal_to([org_id]))


@pytest.mark.asyncio
class TestOrgIdAlreadyExists:
    @pytest.fixture(autouse=True)
    async def insert_org_id(self, org_id, storage):
        await storage.organization.ensure_exists(org_id)

    async def test_ensure(self, storage, dbconn, org_id):
        await storage.organization.ensure_exists(org_id)

        result = await dbconn.execute('select org_id from ipa.organizations')
        rows = [row['org_id'] async for row in result]
        assert_that(rows, equal_to([org_id]))


@pytest.mark.asyncio
class TestCheckExists:
    @pytest.fixture(autouse=True)
    async def setup(self, org_id, storage):
        await storage.organization.ensure_exists(org_id)

    async def test_returns_false_if_not_found(self, storage, org_id, other_org_id):
        assert not (await storage.organization.check_exists(other_org_id))

    async def test_returns_true_if_found(self, storage, org_id):
        assert await storage.organization.check_exists(org_id)


@pytest.mark.asyncio
class TestGetOrCreate:
    async def test_get_or_create_new_org(self, storage, org_entity):
        org = await storage.organization.get_or_create(org_entity.org_id)
        assert_that(org, equal_to(org_entity))

    async def test_get_or_create_when_org_already_exists(self, storage, org):
        assert_that(
            await storage.organization.get_or_create(org.org_id),
            equal_to(org)
        )


@pytest.mark.asyncio
async def test_save(org, storage):
    org.revision += 1
    await storage.organization.save(org)
    assert_that(
        await storage.organization.get(org.org_id),
        equal_to(org),
    )
