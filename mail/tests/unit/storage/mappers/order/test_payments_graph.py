from datetime import timedelta
from decimal import Decimal

import pytest

from sendr_utils import alist

from mail.payments.payments.core.entities.enums import (
    PAY_METHOD_OFFLINE, PAY_METHODS, PAYMETHOD_ID_OFFLINE, GroupType, OrderKind, PayStatus, RefundStatus
)
from mail.payments.payments.storage.mappers.order.order import FindOrderParams
from mail.payments.payments.utils.datetime import utcnow


class TestPaymentsGraph:
    @pytest.fixture(params=(None, *PAY_METHODS))
    def pay_method(self, request):
        return request.param

    @pytest.fixture
    def paymethod_id(self, rands, pay_method):
        if pay_method == PAY_METHOD_OFFLINE:
            return PAYMETHOD_ID_OFFLINE
        return rands()

    @pytest.fixture
    def inverse_paymethod_id(self, rands, pay_method):
        if pay_method == PAY_METHOD_OFFLINE:
            return rands()
        return PAYMETHOD_ID_OFFLINE

    @pytest.fixture
    def orders_data(self, original_order_id, paymethod_id, inverse_paymethod_id, exclude_stats):
        return [
            {
                'kind': OrderKind.PAY,
                'pay_status': PayStatus.NEW,
                'exclude_stats': exclude_stats,
            },
            {
                'kind': OrderKind.PAY,
                'pay_status': PayStatus.PAID,
                'exclude_stats': exclude_stats,
                'paymethod_id': inverse_paymethod_id
            },
            {
                'kind': OrderKind.PAY,
                'pay_status': PayStatus.PAID,
                'exclude_stats': exclude_stats,
                'paymethod_id': paymethod_id,
            },
            {
                'kind': OrderKind.REFUND,
                'pay_status': None,
                'refund_status': RefundStatus.COMPLETED,
                'original_order_id': original_order_id,
                'exclude_stats': exclude_stats,
            },
            {
                'kind': OrderKind.PAY,
                'pay_status': PayStatus.NEW,
                'exclude_stats': exclude_stats,
            }
        ]

    @pytest.fixture
    async def db_orders(self, storage, orders):
        return await alist(storage.order.find(FindOrderParams(exclude_stats=False)))

    @pytest.fixture
    def lower_dt(self):
        return (utcnow() - timedelta(days=1)).isoformat()

    @pytest.fixture
    def upper_dt(self):
        return (utcnow() + timedelta(days=1)).isoformat()

    class TestPaymentsCount:
        @pytest.mark.asyncio
        async def test_all_payments_count(self, storage, exclude_stats, shop, merchant, db_orders, lower_dt, pay_method,
                                          upper_dt):
            points = await alist(
                storage.order.payments_count(lower_dt, upper_dt, merchant.uid, GroupType.DAY,
                                             shop_id=shop.shop_id,
                                             pay_method=pay_method)
            )
            expected_payments_count = sum(
                order.kind != OrderKind.REFUND and (order.pay_method == pay_method or pay_method is None)
                for order in db_orders
            )
            assert len(points) == 0 if exclude_stats and pay_method else points[0].y == expected_payments_count

        @pytest.mark.asyncio
        async def test_paid_count(self, storage, exclude_stats, merchant, shop, db_orders, pay_method, lower_dt,
                                  upper_dt):
            points = await alist(
                storage.order.payments_count(lower_dt, upper_dt, merchant.uid, GroupType.DAY, PayStatus.PAID,
                                             OrderKind.PAY, shop_id=shop.shop_id, pay_method=pay_method)
            )
            expected_payments_count = sum(
                order.pay_status == PayStatus.PAID and (order.pay_method == pay_method or pay_method is None)
                for order in db_orders
            )
            assert len(points) == 0 if exclude_stats else points[0].y == expected_payments_count

        @pytest.mark.asyncio
        async def test_refund_count(self, storage, merchant, db_orders, shop, exclude_stats):
            lower_dt = (utcnow() - timedelta(days=1)).isoformat()
            upper_dt = (utcnow() + timedelta(days=1)).isoformat()
            points = await alist(
                storage.order.payments_count(lower_dt, upper_dt, merchant.uid, GroupType.DAY,
                                             order_kind=OrderKind.REFUND,
                                             shop_id=shop.shop_id,
                                             refund_status=RefundStatus.COMPLETED)
            )
            expected_payments_count = sum(order.kind == OrderKind.REFUND for order in db_orders)
            assert len(points) == 0 if exclude_stats else points[0].y == expected_payments_count

    class TestPaymentsSum:
        @pytest.mark.asyncio
        async def test_sum_paid(self, storage, merchant, exclude_stats, db_orders, pay_method, lower_dt, upper_dt):
            expected_sum_paid = 0
            for order in db_orders:
                async for item in storage.item.get_for_order(merchant.uid, order.order_id):
                    if order.pay_status == PayStatus.PAID and (order.pay_method == pay_method or pay_method is None):
                        expected_sum_paid += item.amount * item.price
            points = await alist(
                storage.order.payments_sum(
                    lower_dt, upper_dt, merchant.uid, GroupType.DAY, pay_status=PayStatus.PAID, pay_method=pay_method
                )
            )
            assert len(points) == 0 if exclude_stats else points[0].y == expected_sum_paid

        @pytest.mark.asyncio
        async def test_sum_refund(self, storage, merchant, exclude_stats, db_orders, lower_dt, upper_dt):
            expected_refund_sum = 0
            for order in db_orders:
                async for item in storage.item.get_for_order(merchant.uid, order.order_id):
                    if order.kind == OrderKind.REFUND and order.refund_status == RefundStatus.COMPLETED:
                        expected_refund_sum += item.amount * item.price
            points = await alist(
                storage.order.payments_sum(lower_dt, upper_dt, merchant.uid,
                                           GroupType.DAY, order_kind=OrderKind.REFUND,
                                           refund_status=RefundStatus.COMPLETED)
            )

            assert len(points) == 0 if exclude_stats else points[0].y == expected_refund_sum

    class TestAverageBill:
        @pytest.mark.asyncio
        async def test_avg_paid(self, storage, exclude_stats, merchant, db_orders, pay_method, lower_dt, upper_dt):
            expected_sum_paid = Decimal()
            count = 0
            for order in db_orders:
                if order.pay_status == PayStatus.PAID and (order.pay_method == pay_method or pay_method is None):
                    async for item in storage.item.get_for_order(merchant.uid, order.order_id):
                        expected_sum_paid += item.amount * item.price
                        count += 1

            points = await alist(
                storage.order.average_bill(
                    lower_dt, upper_dt, merchant.uid, GroupType.DAY, pay_status=PayStatus.PAID, pay_method=pay_method
                )
            )

            assert len(points) == 0 if exclude_stats else Decimal(f'{points[0].y}') == (expected_sum_paid / count)
