from datetime import datetime, timedelta, timezone

import pytest

from sendr_utils import alist, utcnow

from hamcrest import assert_that, contains_inanyorder

from mail.payments.payments.core.entities.enums import OrderKind, PayStatus, RefundStatus
from mail.payments.payments.storage.exceptions import OrderNotFound


class TestGet:
    @pytest.fixture(params=(True, False))
    def with_customer_subscription(self, request):
        return request.param

    @pytest.mark.asyncio
    async def test_not_found(self, storage, merchant):
        with pytest.raises(OrderNotFound):
            await storage.order.get(merchant.uid, -1)

    @pytest.mark.asyncio
    async def test_kind_not_found(self, storage, order, merchant):
        with pytest.raises(OrderNotFound):
            await storage.order.get(merchant.uid, order.order_id, kind=OrderKind.MULTI)

    @pytest.mark.parametrize('field', ('order_id', 'customer_subscription_id'))
    @pytest.mark.asyncio
    async def test_invalid_call(self, storage, field, merchant):
        with pytest.raises(RuntimeError):
            await storage.order.get(merchant.uid, **{field: None})

    @pytest.mark.asyncio
    async def test_get(self, storage, order, merchant, with_customer_subscription):
        assert order == await storage.order.get(
            merchant.uid,
            order.order_id,
            with_customer_subscription=with_customer_subscription
        )

    @pytest.mark.asyncio
    async def test_get_related_shop(self, storage, order, shop, with_customer_subscription):
        order_fetched = await storage.order.get(
            order.uid,
            order.order_id,
            with_customer_subscription=with_customer_subscription,
        )
        assert order_fetched.shop == shop

    @pytest.mark.asyncio
    async def test_customer_subscription(self, storage, merchant, order_with_customer_subscription,
                                         with_customer_subscription, customer_subscription):
        customer_subscription_id = order_with_customer_subscription.customer_subscription_id
        order = await storage.order.get(merchant.uid,
                                        customer_subscription_id=customer_subscription_id,
                                        with_customer_subscription=with_customer_subscription)

        assert all((
            merchant.uid == order_with_customer_subscription.uid,
            order.order_id == order_with_customer_subscription.order_id,
            customer_subscription == order.customer_subscription
            if with_customer_subscription else
            order.customer_subscription is None
        ))

    class TestActive:
        @pytest.mark.asyncio
        async def test_active(self, storage, order):
            assert order == await storage.order.get(order.uid, order.order_id, active=True)

        @pytest.mark.asyncio
        async def test_not_active(self, storage, order):
            with pytest.raises(OrderNotFound):
                await storage.order.get(order.uid, order.order_id, active=False)

    class TestOriginalOrder:
        @pytest.fixture
        def orders_data(self, original_order_id):
            return [
                {
                    'order_id': 3,
                    'kind': OrderKind.REFUND,
                    'original_order_id': original_order_id,
                    'pay_status': None,
                    'refund_status': RefundStatus.FAILED,
                }
            ]

        @pytest.mark.asyncio
        async def test_original_order__success(self, storage, orders, original_order_id, merchant, expected_order_info):
            returned = await storage.order.get(merchant.uid, orders[0].order_id, original_order_id=original_order_id)
            assert returned == orders[0] and returned.original_order_info == expected_order_info

        @pytest.mark.asyncio
        async def test_original_order__not_found(self, storage, orders, original_order_id, merchant):
            with pytest.raises(OrderNotFound):
                await storage.order.get(merchant.uid, orders[0].order_id, original_order_id=original_order_id + 1)

    class TestCustomerId:
        @pytest.mark.asyncio
        async def test_customer_id__success(self, storage, order, merchant):
            assert all((
                order.customer_uid,
                order == await storage.order.get(order.uid, order.order_id, customer_uid=order.customer_uid)
            ))


class TestGetUnfinishedRefund:
    @pytest.mark.asyncio
    async def test_not_found(self, storage):
        with pytest.raises(OrderNotFound):
            await storage.order.get_unfinished_refund()

    @pytest.mark.parametrize('existing_refunds_data', (
        [
            {'refund_status': RefundStatus.COMPLETED},
            {'refund_status': RefundStatus.CREATED},
            {'refund_status': RefundStatus.FAILED},
        ],
    ))
    @pytest.mark.asyncio
    async def test_only_gets_requested(self, storage, existing_refunds):
        with pytest.raises(OrderNotFound):
            await storage.order.get_unfinished_refund()

    @pytest.mark.parametrize('existing_refunds_data', (
        [{'refund_status': RefundStatus.REQUESTED}],
    ))
    @pytest.mark.asyncio
    async def test_gets_refund(self, storage, existing_refunds):
        assert await storage.order.get_unfinished_refund() == existing_refunds[0]

    @pytest.mark.parametrize('existing_refunds_data', (
        [{'refund_status': RefundStatus.REQUESTED}],
    ))
    @pytest.mark.asyncio
    async def test_gets_refund_with_shop(self, storage, shop, existing_refunds):
        refund = await storage.order.get_unfinished_refund()
        assert refund.shop == shop

    @pytest.mark.parametrize('existing_refunds_data', (
        [{'refund_status': RefundStatus.REQUESTED}],
        [{'refund_status': RefundStatus.REQUESTED}],
        [{'refund_status': RefundStatus.REQUESTED}],
    ))
    @pytest.mark.asyncio
    async def test_gets_first_updated_refund(self, storage, existing_refunds):
        assert await storage.order.get_unfinished_refund() == existing_refunds[0]

    class TestDelay:
        @pytest.fixture
        def delay(self):
            return timedelta(seconds=30)

        @pytest.fixture
        def delayed_now(self, mocker, delay):
            from mail.payments.payments.utils.datetime import utcnow
            return mocker.mock_module.patch(
                'mail.payments.payments.storage.mappers.order.order.utcnow',
                return_value=utcnow() + delay,
            )

        @pytest.mark.parametrize('existing_refunds_data', ([{'refund_status': RefundStatus.REQUESTED}],))
        @pytest.mark.asyncio
        async def test_get_with_delay(self, storage, existing_refunds, delay, delayed_now):
            with delayed_now:
                assert await storage.order.get_unfinished_refund(delay) == existing_refunds[0]


class TestGetRefundsForOrders:
    @pytest.mark.asyncio
    async def test_get_refunds(self, storage, order_with_refunds, existing_refunds):
        uid_order_id = [(order_with_refunds.uid, order_with_refunds.order_id)]
        returned = await alist(storage.order.get_refunds_for_orders(uid_order_id))

        assert_that(
            returned,
            contains_inanyorder(*existing_refunds)
        )

    @pytest.mark.asyncio
    async def test_get_refunds_absent(self, storage, order_with_refunds):
        uid_order_id = [(order_with_refunds.uid, order_with_refunds.order_id)]
        returned = await alist(storage.order.get_refunds_for_orders(uid_order_id))
        assert len(returned) == 0


class TestGetAbandonedOrder:
    @pytest.mark.asyncio
    async def test_not_found(self, order, storage):
        with pytest.raises(OrderNotFound):
            await storage.order.get_abandoned_order()

    @pytest.fixture
    async def abandoned_order(self, order, storage):
        order.offline_abandon_deadline = utcnow() - timedelta(minutes=1)
        order = await storage.order.save(order)
        return await storage.order.get(order.uid, order.order_id)

    @pytest.mark.asyncio
    async def test_get(self, storage, abandoned_order):
        assert await storage.order.get_abandoned_order() == abandoned_order

    @pytest.mark.asyncio
    async def test_get_with_shop_entity(self, storage, abandoned_order, shop):
        fetched = await storage.order.get_abandoned_order()
        assert fetched.shop == shop


class TestGetOldestNonTerminalPayStatusUpdated:
    @pytest.fixture
    def get_updated_at_by_pay_status(self, storage):
        async def _inner(uid_black_list=[]):
            result = dict()
            async for pay_status, pay_status_updated_at in (
                storage.order.get_oldest_non_terminal_pay_status_updated(uid_black_list)
            ):
                result[pay_status] = pay_status_updated_at
            return result
        return _inner

    @pytest.fixture
    def create_order(self, order_entity, storage):
        async def _inner(pay_status, pay_status_updated_at):
            order_entity.pay_status = pay_status
            order_entity.pay_status_updated_at = pay_status_updated_at
            await storage.order.create(order_entity)
        return _inner

    @pytest.mark.asyncio
    async def test_empty(self, get_updated_at_by_pay_status):
        assert not await get_updated_at_by_pay_status()

    @pytest.mark.asyncio
    async def test_older_pay_status(self, create_order, get_updated_at_by_pay_status):
        await create_order(PayStatus.IN_PROGRESS, datetime(2020, 12, 23, tzinfo=timezone.utc))
        await create_order(PayStatus.IN_PROGRESS, datetime(2020, 12, 21, tzinfo=timezone.utc))
        await create_order(PayStatus.IN_PROGRESS, datetime(2020, 12, 25, tzinfo=timezone.utc))
        assert {
            PayStatus.IN_PROGRESS: datetime(2020, 12, 21, tzinfo=timezone.utc),
        } == await get_updated_at_by_pay_status()

    @pytest.mark.asyncio
    async def test_several_pay_statuses(self, create_order, get_updated_at_by_pay_status):
        await create_order(PayStatus.IN_PROGRESS, datetime(2020, 12, 23, tzinfo=timezone.utc))
        await create_order(PayStatus.IN_MODERATION, datetime(2020, 12, 21, tzinfo=timezone.utc))
        assert {
            PayStatus.IN_PROGRESS: datetime(2020, 12, 23, tzinfo=timezone.utc),
            PayStatus.IN_MODERATION: datetime(2020, 12, 21, tzinfo=timezone.utc),
        } == await get_updated_at_by_pay_status()

    @pytest.mark.asyncio
    async def test_filter_uid_from_black_list(self, create_order, get_updated_at_by_pay_status, merchant_uid):
        await create_order(PayStatus.IN_PROGRESS, datetime(2020, 12, 23, tzinfo=timezone.utc))
        await create_order(PayStatus.IN_MODERATION, datetime(2020, 12, 21, tzinfo=timezone.utc))
        assert not await get_updated_at_by_pay_status([merchant_uid])
