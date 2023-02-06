import pytest

from mail.beagle.beagle.core.actions.mail_list_responsible.create import CreateMailListResponsibleAction
from mail.beagle.beagle.core.actions.mail_list_responsible.delete import DeleteMailListResponsibleAction
from mail.beagle.beagle.core.actions.mail_list_responsible.get import GetMailListResponsibleAction


@pytest.fixture
def json_params(user):
    return {'uid': user.uid}


class TestGetMailListResponsible:
    @pytest.fixture
    def action(self, mock_action):
        return mock_action(GetMailListResponsibleAction)

    @pytest.fixture
    def query_params(self, user):
        return {
            'limit': 20,
            'offset': 1
        }

    @pytest.fixture
    async def response(self, app, org, mail_list, query_params):
        return await app.get(f'/api/v1/mail_list/{org.org_id}/{mail_list.mail_list_id}/responsible',
                             params=query_params)

    def test_params(self, action, org, mail_list, query_params, response):
        action.assert_called_once_with(org_id=org.org_id, mail_list_id=mail_list.mail_list_id, **query_params)


class TestPostMailListResponsible:
    @pytest.fixture
    def action(self, mock_action):
        return mock_action(CreateMailListResponsibleAction)

    @pytest.fixture
    async def response(self, app, org, mail_list, json_params):
        return await app.post(f'/api/v1/mail_list/{org.org_id}/{mail_list.mail_list_id}/responsible', json=json_params)

    def test_params(self, action, org, mail_list, json_params, response):
        action.assert_called_once_with(org_id=org.org_id, mail_list_id=mail_list.mail_list_id, **json_params)


class TestDeleteMailListResponsible:
    @pytest.fixture
    def action(self, mock_action):
        return mock_action(DeleteMailListResponsibleAction)

    @pytest.fixture
    async def response(self, app, org, mail_list, json_params):
        return await app.delete(f'/api/v1/mail_list/{org.org_id}/{mail_list.mail_list_id}/responsible',
                                json=json_params
                                )

    @pytest.mark.asyncio
    async def test_params(self, action, org, mail_list, json_params, response):
        action.assert_called_once_with(org_id=org.org_id, mail_list_id=mail_list.mail_list_id, **json_params)
