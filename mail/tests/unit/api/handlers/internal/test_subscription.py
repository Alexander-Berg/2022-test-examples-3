import pytest

from ..base import BaseTestSubscriptionGet, BaseTestSubscriptionIdGet, BaseTestSubscriptionPost
from .base import BaseInternalHandlerTvmTest


class TestSubscriptionGet(BaseTestSubscriptionGet, BaseInternalHandlerTvmTest):
    @pytest.fixture
    def action_cls(self):
        from mail.payments.payments.core.actions.subscription.get_list import GetSubscriptionListServiceMerchantAction
        return GetSubscriptionListServiceMerchantAction

    @pytest.fixture
    def request_path(self, service_merchant):
        return f'/v1/internal/subscription/{service_merchant.service_merchant_id}'

    @pytest.fixture
    def extra_call_args(self, service_merchant, service_client):
        return {
            'service_merchant_id': service_merchant.service_merchant_id,
            'service_tvm_id': service_client.tvm_id,
        }


class TestSubscriptionPost(BaseTestSubscriptionPost, BaseInternalHandlerTvmTest):
    @pytest.fixture(autouse=True)
    def action_cls(self, mock_action, subscription):
        from mail.payments.payments.core.actions.subscription.create import CreateSubscriptionServiceMerchantAction
        return CreateSubscriptionServiceMerchantAction

    @pytest.fixture
    def request_path(self, service_merchant):
        return f'/v1/internal/subscription/{service_merchant.service_merchant_id}'

    @pytest.fixture
    def extra_call_args(self, service_merchant, service_client):
        return {
            'service_merchant_id': service_merchant.service_merchant_id,
            'service_tvm_id': service_client.tvm_id,
        }


class TestSubscriptionIdGet(BaseTestSubscriptionIdGet):
    @pytest.fixture
    def action_cls_get(self):
        from mail.payments.payments.core.actions.subscription.get import GetSubscriptionServiceMerchantAction
        return GetSubscriptionServiceMerchantAction

    @pytest.fixture
    def action_cls_delete(self):
        from mail.payments.payments.core.actions.subscription.delete import DeleteSubscriptionServiceMerchantAction
        return DeleteSubscriptionServiceMerchantAction

    @pytest.fixture
    def request_path(self, service_merchant, subscription):
        return f'/v1/internal/subscription/{service_merchant.service_merchant_id}/{subscription.subscription_id}'

    @pytest.fixture
    def extra_call_args(self, service_merchant, service_client):
        return {
            'service_merchant_id': service_merchant.service_merchant_id,
            'service_tvm_id': service_client.tvm_id,
        }
