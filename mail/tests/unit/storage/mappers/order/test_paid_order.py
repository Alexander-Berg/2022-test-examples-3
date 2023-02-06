import pytest

from mail.payments.payments.core.entities.enums import PaidOrderStatType, PayStatus


class TestPaidOrder:
    @pytest.fixture
    def amount(self, exclude_stats, orders):
        return 0 if exclude_stats else int(len(orders) / 3)

    @pytest.fixture
    def orders_data(self, exclude_stats, multi_order, customer_subscription):
        data = [{} for _ in range(3)] + \
               [{'parent_order_id': multi_order.order_id} for _ in range(3)] + \
               [{'customer_subscription_id': customer_subscription.customer_subscription_id} for _ in range(3)]

        return [{**_, 'pay_status': PayStatus.PAID, 'exclude_stats': exclude_stats} for _ in data]

    @pytest.mark.asyncio
    async def test_order_count(self, storage, exclude_stats, amount):
        assert (await storage.order.paid_count(PaidOrderStatType.ORDER)) == amount

    @pytest.mark.asyncio
    async def test_order_from_multi_count(self, storage, amount):
        assert (await storage.order.paid_count(PaidOrderStatType.ORDER_FROM_MULTI_ORDER)) == amount

    @pytest.mark.asyncio
    async def test_subs(self, storage, amount):
        assert (await storage.order.paid_count(PaidOrderStatType.SUBSCRIPTION)) == amount
