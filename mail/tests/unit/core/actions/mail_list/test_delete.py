import pytest

from mail.beagle.beagle.core.actions.mail_list.delete import DeleteMailListAction
from mail.beagle.beagle.core.exceptions import MailListNotFoundError
from mail.beagle.beagle.storage.exceptions import MailListNotFound


@pytest.mark.asyncio
class TestDeleteMailList:
    @pytest.fixture
    def returned_func(self, org):
        async def _inner(mail_list_id):
            return await DeleteMailListAction(org_id=org.org_id, mail_list_id=mail_list_id).run()

        return _inner

    async def test_delete(self, storage, org, returned_func, mail_list):
        await returned_func(mail_list.mail_list_id)
        with pytest.raises(MailListNotFound):
            await storage.mail_list.get(org.org_id, mail_list.mail_list_id)

    async def test_org_not_found(self, returned_func, randn):
        with pytest.raises(MailListNotFoundError):
            await returned_func(randn())
