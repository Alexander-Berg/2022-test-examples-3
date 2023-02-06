import pytest

from .base import BaseTestSubscriptionGet, BaseTestSubscriptionIdGet, BaseTestSubscriptionPost


class TestSubscriptionGet(BaseTestSubscriptionGet):
    @pytest.fixture
    def action_cls(self):
        from mail.payments.payments.core.actions.subscription.get_list import GetSubscriptionListAction
        return GetSubscriptionListAction

    @pytest.fixture
    def request_path(self, merchant):
        return f'v1/subscription/{merchant.uid}'

    @pytest.fixture
    def extra_call_args(self, merchant):
        return {
            'uid': merchant.uid
        }


class TestSubscriptionPost(BaseTestSubscriptionPost):
    @pytest.fixture(autouse=True)
    def action_cls(self, mock_action, subscription):
        from mail.payments.payments.core.actions.subscription.create import CreateSubscriptionAction
        return CreateSubscriptionAction

    @pytest.fixture
    def request_path(self, merchant):
        return f'/v1/subscription/{merchant.uid}'

    @pytest.fixture
    def extra_call_args(self, merchant):
        return {
            'uid': merchant.uid
        }


class TestSubscriptionIdGet(BaseTestSubscriptionIdGet):
    @pytest.fixture
    def action_cls_get(self):
        from mail.payments.payments.core.actions.subscription.get import GetSubscriptionAction
        return GetSubscriptionAction

    @pytest.fixture
    def action_cls_delete(self):
        from mail.payments.payments.core.actions.subscription.delete import DeleteSubscriptionAction
        return DeleteSubscriptionAction

    @pytest.fixture
    def request_path(self, merchant, subscription):
        return f'/v1/subscription/{merchant.uid}/{subscription.subscription_id}'

    @pytest.fixture
    def extra_call_args(self, merchant):
        return {
            'uid': merchant.uid
        }
