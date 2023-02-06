import pytest

from mail.payments.payments.core.actions.customer_subscription.get import GetCustomerSubscriptionServiceMerchantAction
from mail.payments.payments.core.exceptions import CustomerSubscriptionNotFoundError
from mail.payments.payments.tests.base import BaseTestOrderAction
from mail.payments.payments.utils.datetime import utcnow


class TestGetCustomerSubscriptionServiceMerchantAction(BaseTestOrderAction):
    @pytest.fixture
    def customer_subscription_id(self, order_with_service_and_customer_subscription):
        return order_with_service_and_customer_subscription.customer_subscription_id

    @pytest.fixture
    def params(self, customer_subscription_id, service_merchant, service_client):
        return {
            'service_merchant_id': service_merchant.service_merchant_id,
            'service_tvm_id': service_client.tvm_id,
            'customer_subscription_id': customer_subscription_id,
        }

    @pytest.fixture
    def action(self, params, crypto):
        GetCustomerSubscriptionServiceMerchantAction.context.crypto = crypto
        return GetCustomerSubscriptionServiceMerchantAction(**params)

    @pytest.fixture
    def returned_func(self, action):
        async def _inner():
            return await action.run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    def test_get(self, customer_subscription, order_with_service_and_customer_subscription, returned):
        returned_customer_subscription, returned_order = returned

        order_with_service_and_customer_subscription.order_hash = returned_order.order_hash
        order_with_service_and_customer_subscription.items = returned_order.items

        assert all((
            customer_subscription == returned_customer_subscription,
            order_with_service_and_customer_subscription == returned_order,
        ))

    class TestNotFound:
        @pytest.fixture
        def customer_subscription_id(self, customer_subscription):
            return customer_subscription.customer_subscription_id + 1

        @pytest.mark.asyncio
        async def test_not_found(self, returned_func):
            with pytest.raises(CustomerSubscriptionNotFoundError):
                await returned_func()

    class TestResetEnabled:
        @pytest.mark.parametrize('set_finish', (True, False))
        @pytest.mark.asyncio
        async def test_reset_enabled(self, storage, customer_subscription, returned_func, set_finish):
            customer_subscription.enabled = True
            if set_finish:
                customer_subscription.time_finish = utcnow()
            await storage.customer_subscription.save(customer_subscription)

            customer_subscription, _ = await returned_func()
            assert customer_subscription.enabled is not set_finish
