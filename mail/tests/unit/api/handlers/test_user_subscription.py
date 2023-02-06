import pytest

from mail.beagle.beagle.core.actions.user_subscription.create import CreateUserSubscriptionAction
from mail.beagle.beagle.core.actions.user_subscription.delete import DeleteUserSubscriptionAction
from mail.beagle.beagle.core.actions.user_subscription.get import GetUserSubscriptionAction
from mail.beagle.beagle.core.entities.enums import SubscriptionType


class TestGetUserSubscription:
    @pytest.fixture
    def action(self, mock_action):
        return mock_action(GetUserSubscriptionAction)

    @pytest.fixture
    def query_params(self, user):
        return {
            'limit': 20,
            'offset': 1
        }

    @pytest.fixture
    async def response(self, app, org, mail_list, query_params, tvm):
        return await app.get(f'/api/v1/mail_list/{org.org_id}/{mail_list.mail_list_id}/user_subscription',
                             params=query_params)

    def test_params(self, action, org, mail_list, query_params, response):
        action.assert_called_once_with(org_id=org.org_id, mail_list_id=mail_list.mail_list_id, **query_params)


class TestPostUserSubscription:
    @pytest.fixture
    def action(self, mock_action):
        return mock_action(CreateUserSubscriptionAction)

    @pytest.fixture
    def json_params(self, user):
        return {'uid': user.uid,
                'subscription_type': SubscriptionType.INBOX.value}

    @pytest.fixture
    def tvm_default_uid(self):
        return 100

    @pytest.fixture
    def tvm(self, mocker, randn, tvm_default_uid):
        mocker.patch('sendr_tvm.qloud_async_tvm.TicketCheckResult.default_uid', new_callable=mocker.PropertyMock,
                     return_value=tvm_default_uid)
        mocker.src = randn()
        mocker.default_uid = tvm_default_uid
        return mocker

    @pytest.fixture
    async def response(self, app, org, mail_list, json_params, tvm_default_uid, tvm):
        return await app.post(f'/api/v1/mail_list/{org.org_id}/{mail_list.mail_list_id}/user_subscription',
                              json=json_params)

    def test_params(self, action, org, mail_list, json_params, tvm_default_uid, tvm, response):
        json_params.update({'subscription_type': SubscriptionType.INBOX})
        action.assert_called_once_with(
            inviter_uid=tvm_default_uid,
            org_id=org.org_id,
            mail_list_id=mail_list.mail_list_id,
            **json_params
        )


class TestDeleteUserSubscription:
    @pytest.fixture
    def action(self, mock_action):
        return mock_action(DeleteUserSubscriptionAction)

    @pytest.fixture
    def json_params(self, user):
        return {'uid': user.uid}

    @pytest.fixture
    async def response(self, app, org, mail_list, json_params, tvm):
        return await app.delete(f'/api/v1/mail_list/{org.org_id}/{mail_list.mail_list_id}/user_subscription',
                                json=json_params)

    def test_params(self, action, org, mail_list, json_params, response):
        action.assert_called_once_with(org_id=org.org_id, mail_list_id=mail_list.mail_list_id, **json_params)
