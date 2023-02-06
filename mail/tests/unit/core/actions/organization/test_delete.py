import pytest

from sendr_utils import alist

from mail.beagle.beagle.core.actions.organization import DeleteOrganizationAction
from mail.beagle.beagle.storage.exceptions import OrganizationNotFound


@pytest.mark.asyncio
class TestDeleteOrganization:
    @pytest.fixture
    def returned_func(self, mail_list):
        async def _inner():
            return await DeleteOrganizationAction(org_id=mail_list.org_id).run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    async def test_deleted_org(self, storage, mail_list, returned):
        with pytest.raises(OrganizationNotFound):
            await storage.organization.get(mail_list.org_id)

    async def test_deleted_ml(self, storage, mail_list, returned):
        mail_lists = await alist(storage.mail_list.find(org_id=mail_list.org_id))
        assert len(mail_lists) == 0
