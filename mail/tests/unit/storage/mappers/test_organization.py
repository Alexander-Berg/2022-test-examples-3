from datetime import timedelta

import pytest

from mail.beagle.beagle.core.entities.organization import Organization
from mail.beagle.beagle.storage.exceptions import OrganizationAlreadyExists, OrganizationNotFound


@pytest.mark.asyncio
class TestOrganizationMapper:
    @pytest.fixture
    def org_id(self):
        return 1234

    @pytest.fixture
    def revision(self):
        return 5678

    @pytest.fixture
    def org_entity(self, org_id, revision):
        return Organization(
            org_id=org_id,
            revision=revision,
        )

    @pytest.fixture
    async def org(self, storage, org_entity):
        return await storage.organization.create(org_entity)

    @pytest.fixture
    def func_now(self, mocker, now):
        mocker.patch('mail.beagle.beagle.storage.mappers.organization.func.now', mocker.Mock(return_value=now))
        return now

    async def test_create(self, storage, org_entity, func_now):
        org = await storage.organization.create(org_entity)
        org_entity.created = org_entity.updated = func_now
        assert org_entity == org

    async def test_create_duplicate(self, storage, org_entity, func_now):
        await storage.organization.create(org_entity)
        with pytest.raises(OrganizationAlreadyExists):
            await storage.organization.create(org_entity)

    async def test_get(self, storage, org_id, org):
        assert org == await storage.organization.get(org_id)

    async def test_get_not_found(self, storage):
        with pytest.raises(OrganizationNotFound):
            await storage.organization.get(-1)

    async def test_get_with_deleted(self, storage, org_id, org):
        await storage.organization.delete(org)
        deleted = await storage.organization.get(org.org_id, skip_deleted=False)
        assert deleted.org_id == org_id

    async def test_deleted_not_found(self, storage, org):
        await storage.organization.delete(org)
        with pytest.raises(OrganizationNotFound):
            await storage.organization.get(org.org_id)

    async def test_save(self, storage, org_id, org, func_now):
        created_before = org.created
        org.revision += 1
        org.created += timedelta(seconds=10)
        org.updated += timedelta(seconds=20)
        updated = await storage.organization.save(org)
        org.created = created_before
        org.updated = func_now
        assert org == updated \
            and org == await storage.organization.get(org_id)
