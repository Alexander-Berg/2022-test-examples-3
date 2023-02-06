import pytest

from hamcrest import assert_that, has_properties

from mail.payments.payments.core.actions.arbitrage.get_order import GetOrderWithCurrentArbitrage
from mail.payments.payments.core.actions.order.get import CoreGetOrderAction
from mail.payments.payments.core.exceptions import OrderInvalidKind, OrderNotFoundError
from mail.payments.payments.tests.base import BaseTestOrderAction


class TestGetOrderWithCurrentArbitrage(BaseTestOrderAction):
    @pytest.fixture
    def customer_uid(self, randn):
        return randn()

    @pytest.fixture
    def order_id(self, order):
        return order.order_id

    @pytest.fixture
    def order_data(self, customer_uid):
        return {
            'customer_uid': customer_uid
        }

    @pytest.fixture
    def returned_func(self, order, crypto, order_id, customer_uid):
        async def _inner():
            CoreGetOrderAction.context.crypto = crypto

            return await GetOrderWithCurrentArbitrage(uid=order.uid, order_id=order_id, customer_uid=customer_uid).run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    @pytest.mark.asyncio
    @pytest.mark.parametrize('order_id', (-1,))
    async def test_not_found(self, returned_func):
        with pytest.raises(OrderNotFoundError):
            await returned_func()

    @pytest.mark.asyncio
    @pytest.mark.parametrize('dialogs_org_id', (None, '123'))
    async def test_returned(self, storage, merchant, order, arbitrage, dialogs_org_id, returned_func):
        merchant.dialogs_org_id = dialogs_org_id
        await storage.merchant.save(merchant)
        returned = await returned_func()

        assert_that(returned, has_properties({
            'uid': order.uid,
            'order_id': order.order_id,
            'current_arbitrage': arbitrage,
            'is_create_arbitrage_available': dialogs_org_id is not None
        }))

    class TestInvalidKind:
        @pytest.fixture
        def order_id(self, multi_order):
            return multi_order.order_id

        @pytest.mark.asyncio
        async def test_invalid_kind(self, returned_func):
            with pytest.raises(OrderInvalidKind):
                await returned_func()

    class TestInvalidKindSubscription:
        @pytest.fixture
        def order_id(self, order_with_customer_subscription):
            return order_with_customer_subscription.order_id

        @pytest.mark.asyncio
        async def test_invalid_kind_subscription(self, returned_func):
            with pytest.raises(OrderInvalidKind):
                await returned_func()
