import pytest

from mail.payments.payments.core.actions.customer_subscription.get_list import (
    GetCustomerSubscriptionListServiceMerchantAction
)
from mail.payments.payments.tests.base import BaseTestOrderAction


class TestGetCustomerSubscriptionListServiceMerchantAction(BaseTestOrderAction):
    @pytest.fixture
    def params(self, service_merchant, crypto, service_client):
        return {
            'service_merchant_id': service_merchant.service_merchant_id,
            'service_tvm_id': service_client.tvm_id,
        }

    @pytest.fixture
    def action(self, params):
        return GetCustomerSubscriptionListServiceMerchantAction(**params)

    @pytest.fixture
    def returned_func(self, action):
        async def _inner():
            return await action.run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    @pytest.mark.asyncio
    async def test_get(self, customer_subscription, order_with_customer_subscription, returned):
        order_with_customer_subscription.order_hash = returned[0][1].order_hash
        order_with_customer_subscription.items = returned[0][1].items
        order_with_customer_subscription.refunds = returned[0][1].refunds
        assert returned == [(customer_subscription, order_with_customer_subscription)]

    def test_empty(self, returned):
        assert returned == []
