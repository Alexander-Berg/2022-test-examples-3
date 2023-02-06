import pytest

from mail.payments.payments.core.actions.order.get import GetOrderByCustomerSubscriptionIdAction
from mail.payments.payments.core.exceptions import OrderNotFoundError
from mail.payments.payments.tests.base import BaseTestOrderAction


class TestGetOrderByCustomerSubscriptionIdAction(BaseTestOrderAction):
    @pytest.fixture
    def items(self):
        return 'fake items'

    @pytest.fixture
    def params(self, merchant, order_with_customer_subscription, crypto):
        GetOrderByCustomerSubscriptionIdAction.context.crypto = crypto
        return {
            'customer_subscription_id': order_with_customer_subscription.customer_subscription_id,
            'uid': merchant.uid,
        }

    @pytest.fixture
    def returned_func(self, params):
        async def _inner():
            return await GetOrderByCustomerSubscriptionIdAction(**params).run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    @pytest.mark.asyncio
    async def test_not_found(self, returned_func, params):
        params['customer_subscription_id'] += 1
        with pytest.raises(OrderNotFoundError):
            await returned_func()

    @pytest.mark.asyncio
    async def test_returns_order(self, order_with_customer_subscription, items, some_hash, returned):
        order_with_customer_subscription.items = items
        order_with_customer_subscription.order_hash = some_hash
        assert returned == order_with_customer_subscription
