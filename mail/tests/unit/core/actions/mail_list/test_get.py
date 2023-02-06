import pytest

from mail.beagle.beagle.core.actions.mail_list.get import GetMailListAction
from mail.beagle.beagle.core.exceptions import MailListNotFoundError


@pytest.mark.asyncio
class TestGetMailList:
    @pytest.fixture
    def returned_func(self, org):
        async def _inner(mail_list_id):
            return await GetMailListAction(org_id=org.org_id, mail_list_id=mail_list_id).run()

        return _inner

    async def test_get(self, storage, mail_list, returned_func, org):
        returned = await returned_func(mail_list.mail_list_id)
        mail_list = await storage.mail_list.get(org.org_id, returned.mail_list_id)
        assert returned == mail_list

    async def test_org_not_found(self, returned_func, randn):
        with pytest.raises(MailListNotFoundError):
            await returned_func(randn())
