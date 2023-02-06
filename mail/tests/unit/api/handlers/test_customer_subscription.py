import pytest

from .base import BaseCustomerSubscriptionCancelHandlerPostTest, BaseCustomerSubscriptionHandlerPostTest


@pytest.fixture
def extra_call_args(merchant):
    return {
        'uid': merchant.uid,
    }


class TestCustomerSubscriptionHandlerPost(BaseCustomerSubscriptionHandlerPostTest):
    @pytest.fixture
    def action_cls(self):
        from mail.payments.payments.core.actions.customer_subscription.create import CreateCustomerSubscriptionAction
        return CreateCustomerSubscriptionAction

    @pytest.fixture
    def request_path(self, merchant):
        return f'/v1/customer_subscription/{merchant.uid}'


class TestCustomerSubscriptionCancelHandlerPost(BaseCustomerSubscriptionCancelHandlerPostTest):
    @pytest.fixture
    def action_cls(self):
        from mail.payments.payments.core.actions.customer_subscription.cancel import CancelCustomerSubscriptionAction
        return CancelCustomerSubscriptionAction

    @pytest.fixture
    def request_path(self, merchant, customer_subscription):
        customer_subscription_id = customer_subscription.customer_subscription_id

        return f'/v1/customer_subscription/{merchant.uid}/{customer_subscription_id}/cancel'
