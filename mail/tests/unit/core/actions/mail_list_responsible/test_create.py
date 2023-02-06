import pytest

from mail.beagle.beagle.core.actions.mail_list_responsible.create import CreateMailListResponsibleAction
from mail.beagle.beagle.core.exceptions import MailListResponsibleAlreadyExistsError, UserNotFoundError
from mail.beagle.beagle.tests.unit.core.actions.base import *  # noqa


class TestCreateMailListResponsible(BaseTestNotFound):  # noqa
    @pytest.fixture
    def action(self, params, mocker):
        return CreateMailListResponsibleAction

    @pytest.fixture
    def params(self, org, mail_list, user):
        return {
            'org_id': org.org_id,
            'mail_list_id': mail_list.mail_list_id,
            'uid': user.uid
        }

    class TestNotFound:
        @pytest.mark.asyncio
        async def test_user_not_found(self, params, randn, action):
            params.update({'uid': randn()})
            with pytest.raises(UserNotFoundError):
                await action(**params).run()

    class TestAlreadyExists:
        @pytest.mark.asyncio
        async def test_resp_already_exists(self, params, mail_list_responsible, action):
            params.update({'uid': mail_list_responsible.uid})
            with pytest.raises(MailListResponsibleAlreadyExistsError):
                await action(**params).run()
