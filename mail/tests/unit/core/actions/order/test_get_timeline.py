from datetime import timedelta
from decimal import Decimal

import pytest

from hamcrest import assert_that, contains

from mail.payments.payments.core.actions.order.get_timeline import GetOrderTimelineAction
from mail.payments.payments.core.entities.customer_subscription_transaction import CustomerSubscriptionTransaction
from mail.payments.payments.core.entities.enums import OrderKind, PayStatus, RefundStatus, TransactionStatus
from mail.payments.payments.core.entities.order import Order, OrderTimelineEvent, OrderTimelineEventType
from mail.payments.payments.utils.datetime import utcnow


class TestGetOrderTimeline:
    @pytest.fixture
    async def paid_order(self, order, items):
        order.pay_status = PayStatus.PAID
        order.closed = utcnow() + timedelta(days=1)
        return order

    @pytest.fixture
    async def returned(self, order):
        return await GetOrderTimelineAction(order).run()

    @pytest.fixture
    def refund_data(self):
        return {
            'refund_status': RefundStatus.COMPLETED,
            'closed': utcnow() + timedelta(days=2)
        }

    @pytest.fixture
    def timeline_event_created(self, order):
        assert order.created is not None
        return OrderTimelineEvent(
            event_type=OrderTimelineEventType.CREATED,
            date=order.created
        )

    @pytest.fixture
    def timeline_event_paid(self, order):
        assert order.closed is not None
        return OrderTimelineEvent(
            event_type=OrderTimelineEventType.PAID,
            date=order.closed
        )

    @pytest.fixture(params=(False, True))
    def refunds_fetched(self, request):
        return request.param

    class TestEmptyOrder:
        @pytest.fixture
        def order(self, order, refunds_fetched):
            if refunds_fetched:
                order.refunds = []
            return order

        def test_empty_order(self, returned, timeline_event_created):
            assert_that(returned, contains(
                timeline_event_created
            ))

    class TestPaidOrder:
        @pytest.fixture
        def order(self, paid_order, refunds_fetched):
            paid_order.refunds = [] if refunds_fetched else None
            return paid_order

        def test_paid_order(self, returned, timeline_event_created, timeline_event_paid):
            assert_that(returned, contains(
                timeline_event_created,
                timeline_event_paid
            ))

    class TestFullRefund:
        @pytest.fixture
        def order(self, paid_order, refund, refunds_fetched):
            paid_order.refunds = [refund] if refunds_fetched else None
            return paid_order

        def test_full_refund(self, returned, timeline_event_created, timeline_event_paid, refund, items):
            assert_that(returned, contains(
                timeline_event_created,
                timeline_event_paid,
                OrderTimelineEvent(
                    date=refund.closed,
                    event_type=OrderTimelineEventType.REFUNDED,
                    extra={
                        'refund_amount': sum([item.total_price for item in items])
                    }
                )
            ))

    class TestManyRefunds:
        @pytest.fixture
        async def order(self, paid_order, create_refund, refund_data, items, refunds_fetched):
            first_refund = await create_refund(items=[items[0]])
            refund_data_second = refund_data.copy()
            refund_data_second['closed'] += timedelta(days=1)
            second_refund = await create_refund(items=[items[1]], refund_data=refund_data_second)
            paid_order.refunds = [first_refund, second_refund] if refunds_fetched else None
            return paid_order

        def test_many_refunds(self, returned, timeline_event_created, timeline_event_paid, items, refund_data):
            assert_that(returned, contains(
                timeline_event_created,
                timeline_event_paid,
                OrderTimelineEvent(
                    date=refund_data['closed'],
                    event_type=OrderTimelineEventType.PARTIALLY_REFUNDED,
                    extra={
                        'refund_amount': items[0].total_price
                    }
                ),
                OrderTimelineEvent(
                    date=refund_data['closed'] + timedelta(days=1),
                    event_type=OrderTimelineEventType.PARTIALLY_REFUNDED,
                    extra={
                        'refund_amount': items[1].total_price
                    }
                )
            ))

    class TestPeriodic:
        @pytest.fixture
        def create_transaction(self, storage):
            async def _inner(entity):
                result, _ = await storage.customer_subscription_transaction.create_or_update(entity)
                return result
            return _inner

        @pytest.mark.asyncio
        async def test_periodic(self,
                                create_transaction,
                                customer_subscription,
                                order,
                                timeline_event_created,
                                randn,
                                rands):
            expected = [timeline_event_created]
            order.customer_subscription_id = customer_subscription.subscription_id
            for transaction_status, amount in [(TransactionStatus.ACTIVE, '12.34'),
                                               (TransactionStatus.FAILED, '100'),
                                               (TransactionStatus.HELD, None)]:
                transaction = await create_transaction(
                    CustomerSubscriptionTransaction(
                        uid=order.uid,
                        customer_subscription_id=order.customer_subscription_id,
                        purchase_token=rands(),
                        payment_status=transaction_status,
                        data={'amount': amount} if amount else dict(),
                    )
                )
                expected.append(
                    OrderTimelineEvent(
                        date=transaction.created,
                        event_type=OrderTimelineEventType.from_transaction_status(transaction_status),
                        extra={
                            'periodic_amount': Decimal(amount) if amount else None,
                            'tx_id': {
                                'uid': transaction.uid,
                                'customer_subscription_id': transaction.customer_subscription_id,
                                'purchase_token': transaction.purchase_token
                            }
                        },
                    )
                )
            actual = await GetOrderTimelineAction(order).run()
            # default sort by `date` does not define order, because all items created in same time
            actual.sort(key=lambda item: item.event_type.name)
            assert expected == actual

        @pytest.mark.asyncio
        async def test_periodic_with_refunds(self,
                                             storage,
                                             create_transaction,
                                             customer_subscription,
                                             order_with_customer_subscription,
                                             timeline_event_created,
                                             randn,
                                             rands):
            expected = [timeline_event_created]
            transaction = await create_transaction(
                CustomerSubscriptionTransaction(
                    uid=order_with_customer_subscription.uid,
                    customer_subscription_id=order_with_customer_subscription.customer_subscription_id,
                    purchase_token=rands(),
                    payment_status=TransactionStatus.CLEARED,
                    data={'amount': randn()},
                )
            )
            expected.append(
                OrderTimelineEvent(
                    date=transaction.created,
                    event_type=OrderTimelineEventType.from_transaction_status(TransactionStatus.CLEARED),
                    extra={
                        'periodic_amount': Decimal(transaction.data['amount']),
                        'tx_id': {
                            'uid': transaction.uid,
                            'customer_subscription_id': transaction.customer_subscription_id,
                            'purchase_token': transaction.purchase_token
                        }
                    },
                )
            )
            refund = await storage.order.create(
                Order(
                    uid=order_with_customer_subscription.uid,
                    original_order_id=order_with_customer_subscription.order_id,
                    shop_id=order_with_customer_subscription.shop_id,
                    kind=OrderKind.REFUND,
                    pay_status=None,
                    refund_status=RefundStatus.COMPLETED,
                    closed=transaction.created + timedelta(seconds=1),
                    acquirer=order_with_customer_subscription.acquirer,
                    customer_subscription_id=customer_subscription.customer_subscription_id,
                    customer_subscription_tx_purchase_token=transaction.purchase_token
                )
            )
            expected.append(
                OrderTimelineEvent(
                    date=refund.closed,
                    event_type=OrderTimelineEventType.PERIODIC_REFUNDED,
                    extra={
                        'refund_amount': Decimal(transaction.data['amount'])
                    },
                )
            )
            actual = await GetOrderTimelineAction(order_with_customer_subscription).run()
            # default sort by `date` does not define order, because all items created in same time
            actual.sort(key=lambda item: item.event_type.name)
            assert expected == actual
