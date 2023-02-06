import pytest

from mail.beagle.beagle.core.actions.mail_list_responsible.delete import DeleteMailListResponsibleAction
from mail.beagle.beagle.core.exceptions import MailListResponsibleNotFoundError
from mail.beagle.beagle.tests.unit.core.actions.base import *  # noqa


class TestDeleteMailListResponsible(BaseTestNotFound):  # noqa
    @pytest.fixture
    def action(self, params, mocker):
        return DeleteMailListResponsibleAction

    @pytest.fixture
    def params(self, org, mail_list_responsible):
        return {
            'org_id': mail_list_responsible.org_id,
            'mail_list_id': mail_list_responsible.mail_list_id,
            'uid': mail_list_responsible.uid
        }

    class TestNotFound:
        @pytest.mark.asyncio
        async def test_responsible_not_found(self, storage, mail_list_responsible, params, action):
            await storage.mail_list_responsible.delete(mail_list_responsible)
            with pytest.raises(MailListResponsibleNotFoundError):
                await action(**params).run()
