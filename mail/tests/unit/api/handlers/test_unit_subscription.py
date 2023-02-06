import pytest

from mail.beagle.beagle.core.actions.unit_subscription.create import CreateUnitSubscriptionAction
from mail.beagle.beagle.core.actions.unit_subscription.delete import DeleteUnitSubscriptionAction
from mail.beagle.beagle.core.actions.unit_subscription.get import GetUnitSubscriptionAction
from mail.beagle.beagle.core.entities.enums import SubscriptionType


class TestGetUnitSubscription:
    @pytest.fixture
    def action(self, mock_action):
        return mock_action(GetUnitSubscriptionAction)

    @pytest.fixture
    def query_params(self):
        return {
            'limit': 20,
            'offset': 1
        }

    @pytest.fixture
    async def response(self, app, org, mail_list, query_params):
        return await app.get(f'/api/v1/mail_list/{org.org_id}/{mail_list.mail_list_id}/unit_subscription',
                             params=query_params)

    def test_params(self, action, org, mail_list, query_params, response):
        action.assert_called_once_with(org_id=org.org_id, mail_list_id=mail_list.mail_list_id, **query_params)


class TestPostUnitSubscription:
    @pytest.fixture
    def action(self, mock_action):
        return mock_action(CreateUnitSubscriptionAction)

    @pytest.fixture
    def json_params(self, unit):
        return {'unit_id': unit.unit_id,
                'subscription_type': SubscriptionType.INBOX.value}

    @pytest.fixture
    async def response(self, app, org, mail_list, json_params):
        return await app.post(f'/api/v1/mail_list/{org.org_id}/{mail_list.mail_list_id}/unit_subscription',
                              json=json_params)

    def test_params(self, action, org, mail_list, json_params, response):
        json_params.update({'subscription_type': SubscriptionType.INBOX})
        action.assert_called_once_with(org_id=org.org_id, mail_list_id=mail_list.mail_list_id, **json_params)


class TestDeleteUnitSubscription:
    @pytest.fixture
    def action(self, mock_action):
        return mock_action(DeleteUnitSubscriptionAction)

    @pytest.fixture
    def json_params(self, unit):
        return {'unit_id': unit.unit_id}

    @pytest.fixture
    async def response(self, app, org, mail_list, json_params):
        return await app.delete(f'/api/v1/mail_list/{org.org_id}/{mail_list.mail_list_id}/unit_subscription',
                                json=json_params
                                )

    @pytest.mark.asyncio
    async def test_params(self, action, org, mail_list, json_params, response):
        action.assert_called_once_with(org_id=org.org_id, mail_list_id=mail_list.mail_list_id, **json_params)
