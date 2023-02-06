import itertools
from copy import copy
from datetime import datetime, timezone
from decimal import Decimal
from itertools import chain

import pytest

from sendr_utils import alist

from hamcrest import assert_that, contains, contains_inanyorder, has_property

from mail.payments.payments.core.entities.enums import (
    PAY_METHODS, PAYMETHOD_ID_OFFLINE, OrderKind, OrderSource, PayStatus, RefundStatus
)
from mail.payments.payments.core.entities.serial import Serial
from mail.payments.payments.storage.mappers.order.order import SKIP_CONDITION, FindOrderParams


class TestFind:
    @pytest.fixture
    def orders_data(self):
        return [{}, {}, {}]

    @pytest.fixture
    def returned(self, storage):
        async def _inner(**kwargs):
            return await alist(storage.order.find(FindOrderParams(**kwargs)))

        return _inner

    @pytest.fixture
    def returned_found_count(self, storage):
        async def _inner(**kwargs):
            return await storage.order.get_found_count(FindOrderParams(**kwargs))

        return _inner

    @pytest.mark.asyncio
    async def test_empty(self, returned):
        assert await returned() == []

    @pytest.mark.asyncio
    async def test_get_all(self, orders, returned):
        assert_that(await returned(), contains_inanyorder(*orders))

    @pytest.mark.asyncio
    async def test_get_all_count(self, orders, returned_found_count):
        assert len(orders) == await returned_found_count()

    class TestUID:
        @pytest.fixture
        def order_data(self):
            return [{}] * 3

        @pytest.fixture
        async def extra_merchant(self, storage, merchant):
            extra = copy(merchant)
            extra.uid += 1
            extra.merchant_id = str(extra.uid)
            async with storage.conn.begin():
                extra = await storage.merchant.create(extra)
                await storage.serial.create(Serial(extra.uid))
            return extra

        @pytest.fixture
        async def extra_orders(self, storage, extra_merchant, shop_entity, orders):
            extra = []
            shop_entity.uid = extra_merchant.uid
            shop = await storage.shop.create(shop_entity)
            for order in orders:
                new_order = copy(order)
                new_order.uid = extra_merchant.uid
                new_order.shop_id = shop.shop_id
                order_created = await storage.order.create(new_order)
                order_created.shop = shop
                extra.append(order_created)
            return extra

        @pytest.mark.asyncio
        async def test_uid_none(self, orders, extra_orders, returned):
            assert_that(
                await returned(),
                contains_inanyorder(*orders, *extra_orders),
            )

        @pytest.mark.asyncio
        async def test_get_all_count_uid(self, orders, extra_orders, returned_found_count):
            assert len(orders) + len(extra_orders) == await returned_found_count()

        @pytest.mark.asyncio
        async def test_uid(self, merchant, orders, extra_orders, returned):
            assert_that(
                await returned(uid=merchant.uid),
                contains_inanyorder(*orders),
            )

    class TestOrderID:
        @pytest.mark.asyncio
        async def test_order_id(self, order, returned):
            assert await returned(uid=order.uid, order_id=order.order_id) == [order]

        @pytest.mark.asyncio
        async def test_get_all_count_order_id(self, order, returned_found_count):
            assert await returned_found_count(uid=order.uid, order_id=order.order_id) == 1

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
        async def test_original_order_id(self, orders, returned, original_order_id, merchant):
            assert_that(
                await returned(uid=merchant.uid, original_order_id=original_order_id),
                contains_inanyorder(*orders))

        @pytest.mark.asyncio
        async def test_get_all_count_original_order_id(
            self, orders, original_order_id, merchant, returned_found_count
        ):
            assert len(orders) == await returned_found_count(uid=merchant.uid, original_order_id=original_order_id)

        @pytest.mark.asyncio
        async def test_original_order_info_is_included(self, orders, merchant, returned, expected_order_info,
                                                       original_order_id):
            result = await returned(uid=merchant.uid, original_order_id=original_order_id)
            assert_that(
                result,
                contains(has_property('original_order_info', expected_order_info)))

    class TestPriceFromTo:
        @pytest.fixture
        def orders_data(self):
            return [
                {'price': Decimal('100.11')},
                {'price': Decimal('102.11')},
                {'price': Decimal('110')},
                {'price': Decimal('101')},
                {'price': Decimal('200.50')},
            ]

        @pytest.mark.asyncio
        async def test_price_from_to__from(self, orders, returned):
            assert_that(
                await returned(price_from=Decimal('102.11')),
                contains_inanyorder(orders[1], orders[2], orders[4])
            )

        @pytest.mark.asyncio
        async def test_price_from_to__from_count(self, orders, returned_found_count):
            assert await returned_found_count(price_from=Decimal('102.11')) == 3

        @pytest.mark.asyncio
        async def test_to(self, orders, returned):
            assert_that(
                await returned(price_to=Decimal('102.11')),
                contains_inanyorder(orders[0], orders[3])
            )

        @pytest.mark.asyncio
        async def test_price_from_to__to_count(self, orders, returned_found_count):
            assert await returned_found_count(price_to=Decimal('102.11')) == 2

        @pytest.mark.asyncio
        async def test_price_from_to__from_to(self, orders, returned):
            assert_that(
                await returned(price_from=Decimal('102'), price_to=Decimal('102.12')),
                contains_inanyorder(orders[1])
            )

        @pytest.mark.asyncio
        async def test_price_from_to__from_to_count(self, orders, returned_found_count):
            assert await returned_found_count(price_from=Decimal('102'), price_to=Decimal('102.12')) == 1

    class TestHeldAtFromTo:
        @pytest.fixture
        def orders_data(self):
            return [
                {'held_at': datetime(year=1990, month=1, day=11, tzinfo=timezone.utc)},
                {'held_at': datetime(year=1990, month=1, day=1, tzinfo=timezone.utc)},
                {'held_at': datetime(year=1990, month=2, day=1, tzinfo=timezone.utc)},
                {'held_at': datetime(year=1990, month=3, day=1, tzinfo=timezone.utc)},
                {'held_at': datetime(year=1991, month=1, day=28, tzinfo=timezone.utc)},
            ]

        @pytest.mark.asyncio
        async def test_held_at_from_to__from(self, orders, returned):
            assert_that(
                await returned(held_at_from=datetime(year=1990, month=1, day=15, tzinfo=timezone.utc)),
                contains_inanyorder(orders[2], orders[3], orders[4])
            )

        @pytest.mark.asyncio
        async def test_held_at_from_to__from_count(self, orders, returned_found_count):
            assert await returned_found_count(
                held_at_from=datetime(year=1990, month=1, day=15, tzinfo=timezone.utc)
            ) == 3

        @pytest.mark.asyncio
        async def test_held_at_from_to__to(self, orders, returned):
            assert_that(
                await returned(held_at_to=datetime(year=1990, month=2, day=15, tzinfo=timezone.utc)),
                contains_inanyorder(orders[0], orders[1], orders[2])
            )

        @pytest.mark.asyncio
        async def test_held_at_from_to__to_count(self, orders, returned_found_count):
            assert await returned_found_count(
                held_at_to=datetime(year=1990, month=2, day=15, tzinfo=timezone.utc)
            ) == 3

        @pytest.mark.asyncio
        async def test_held_at_from_to__from_to(self, orders, returned):
            assert_that(
                await returned(held_at_from=datetime(year=1990, month=1, day=30, tzinfo=timezone.utc),
                               held_at_to=datetime(year=1990, month=2, day=2, tzinfo=timezone.utc)),
                contains_inanyorder(orders[2])
            )

        @pytest.mark.asyncio
        async def test_held_at_from_to__from_to_count(self, orders, returned_found_count):
            assert await returned_found_count(
                held_at_from=datetime(year=1990, month=1, day=30, tzinfo=timezone.utc),
                held_at_to=datetime(year=1990, month=2, day=2, tzinfo=timezone.utc),
            ) == 1

    class TestKindsAndStatuses:
        @pytest.fixture
        def orders_data(self):
            return [
                {
                    'order_id': 1,
                    'kind': OrderKind.PAY,
                    'original_order_id': None,
                    'pay_status': PayStatus.NEW,
                    'refund_status': None,
                    'active': True,
                },
                {
                    'order_id': 2,
                    'kind': OrderKind.PAY,
                    'original_order_id': None,
                    'pay_status': PayStatus.PAID,
                    'refund_status': None,
                    'active': True,
                },
                {
                    'order_id': 3,
                    'kind': OrderKind.REFUND,
                    'original_order_id': 1,
                    'pay_status': None,
                    'refund_status': RefundStatus.FAILED,
                    'active': True,
                },
                {
                    'order_id': 4,
                    'kind': OrderKind.PAY,
                    'original_order_id': None,
                    'pay_status': PayStatus.NEW,
                    'refund_status': None,
                    'active': False,
                },
                {
                    'order_id': 5,
                    'kind': OrderKind.REFUND,
                    'original_order_id': 4,
                    'pay_status': None,
                    'refund_status': RefundStatus.REQUESTED,
                    'active': True,
                },
            ]

        @pytest.mark.parametrize('kinds,indexes', [
            ([OrderKind.PAY], [0, 1, 3]),
            ([OrderKind.REFUND], [2, 4]),
            ([OrderKind.PAY, OrderKind.REFUND], [0, 1, 2, 3, 4]),
        ])
        @pytest.mark.asyncio
        async def test_kinds(self, orders, returned, kinds, indexes):
            assert_that(
                await returned(kinds=kinds),
                contains_inanyorder(*[
                    orders[index]
                    for index in indexes
                ])
            )

        @pytest.mark.parametrize('kinds,indexes', [
            ([OrderKind.PAY], [0, 1, 3]),
            ([OrderKind.REFUND], [2, 4]),
            ([OrderKind.PAY, OrderKind.REFUND], [0, 1, 2, 3, 4]),
        ])
        @pytest.mark.asyncio
        async def test_kinds_count(self, orders, returned_found_count, kinds, indexes):
            assert await returned_found_count(kinds=kinds) == len(indexes)

        @pytest.mark.parametrize('pay_statuses,indexes', [
            ([PayStatus.NEW], [0]),
            ([PayStatus.NEW, PayStatus.PAID], [0, 1]),
        ])
        @pytest.mark.asyncio
        async def test_pay_statuses(self, orders, returned, pay_statuses, indexes):
            assert_that(
                await returned(pay_statuses=pay_statuses),
                contains_inanyorder(*[
                    orders[index]
                    for index in indexes
                ])
            )

        @pytest.mark.parametrize('pay_statuses,indexes', [
            ([PayStatus.NEW], [0]),
            ([PayStatus.NEW, PayStatus.PAID], [0, 1]),
        ])
        @pytest.mark.asyncio
        async def test_pay_statuses_count(self, orders, returned_found_count, pay_statuses, indexes):
            assert await returned_found_count(pay_statuses=pay_statuses) == len(indexes)

        @pytest.mark.parametrize('refund_statuses,indexes', [
            ([RefundStatus.REQUESTED], [4]),
            ([RefundStatus.REQUESTED, RefundStatus.FAILED], [2, 4]),
        ])
        @pytest.mark.asyncio
        async def test_refund_statuses(self, orders, returned, refund_statuses, indexes):
            assert_that(
                await returned(refund_statuses=refund_statuses),
                contains_inanyorder(*[
                    orders[index]
                    for index in indexes
                ])
            )

        @pytest.mark.parametrize('pay_statuses, refund_statuses, is_active, indexes', [
            ([PayStatus.NEW], None, None, [0]),
            ([PayStatus.NEW], None, False, [0, 3]),
            (None, [RefundStatus.FAILED], False, [2, 3]),
            ([PayStatus.NEW], [RefundStatus.FAILED], None, [0, 2]),
            ([PayStatus.NEW], [RefundStatus.FAILED], True, [0, 1, 2, 4]),
            ([PayStatus.NEW], [RefundStatus.FAILED], False, [0, 2, 3]),
            ([PayStatus.NEW, PayStatus.PAID], [RefundStatus.REQUESTED, RefundStatus.FAILED], None, [0, 1, 2, 4]),
            ([PayStatus.NEW, PayStatus.PAID], [RefundStatus.REQUESTED, RefundStatus.FAILED], False, [0, 1, 2, 3, 4]),
        ])
        @pytest.mark.asyncio
        async def test_all_statuses(self, orders, returned, refund_statuses, pay_statuses, is_active, indexes):
            assert_that(
                await returned(pay_statuses=pay_statuses, refund_statuses=refund_statuses, is_active=is_active),
                contains_inanyorder(*[
                    orders[index]
                    for index in indexes
                ])
            )

        @pytest.mark.parametrize('pay_statuses, refund_statuses, is_active, indexes', [
            ([PayStatus.NEW], None, None, [0]),
            ([PayStatus.NEW], None, False, [0, 3]),
            (None, [RefundStatus.FAILED], False, [2, 3]),
            ([PayStatus.NEW], [RefundStatus.FAILED], None, [0, 2]),
            ([PayStatus.NEW], [RefundStatus.FAILED], True, [0, 1, 2, 4]),
            ([PayStatus.NEW], [RefundStatus.FAILED], False, [0, 2, 3]),
            ([PayStatus.NEW, PayStatus.PAID], [RefundStatus.REQUESTED, RefundStatus.FAILED], None, [0, 1, 2, 4]),
            ([PayStatus.NEW, PayStatus.PAID], [RefundStatus.REQUESTED, RefundStatus.FAILED], False, [0, 1, 2, 3, 4]),
        ])
        @pytest.mark.asyncio
        async def test_all_statuses_count(
            self, orders, returned_found_count, refund_statuses, pay_statuses, is_active, indexes
        ):
            assert await returned_found_count(
                pay_statuses=pay_statuses, refund_statuses=refund_statuses, is_active=is_active
            ) == len(indexes)

    class TestActive:
        @pytest.fixture
        def orders_data(self):
            return [
                {'active': False},
                {'active': True},
                {'active': False},
                {'active': True},
                {'active': True},
            ]

        @pytest.mark.parametrize('is_active,indexes', [
            (True, [1, 3, 4]),
            (False, [0, 2]),
        ])
        @pytest.mark.asyncio
        async def test_active(self, orders, returned, is_active, indexes):
            assert_that(
                await returned(is_active=is_active),
                contains_inanyorder(*[
                    orders[index]
                    for index in indexes
                ]),
            )

        @pytest.mark.parametrize('is_active,indexes', [
            (True, [1, 3, 4]),
            (False, [0, 2]),
        ])
        @pytest.mark.asyncio
        async def test_active_count(self, orders, returned_found_count, is_active, indexes):
            assert await returned_found_count(is_active=is_active) == len(indexes)

    class TestParentOrderId:
        @pytest.fixture
        def parent_order_id(self, orders):
            return orders[0].order_id

        @pytest.mark.asyncio
        async def test_parent_order_id(self, returned, parent_order_id, order):
            assert_that(await returned(parent_order_id=parent_order_id), contains_inanyorder(order))

        @pytest.mark.asyncio
        async def test_parent_order_id_count(self, returned_found_count, parent_order_id, order):
            assert await returned_found_count(parent_order_id=parent_order_id) == 1

        @pytest.mark.asyncio
        async def test_no_parent_order_id(self, returned, parent_order_id, order):
            order_ids = [_.order_id for _ in await returned()]
            assert order.order_id not in order_ids and len(order_ids) > 0

        @pytest.mark.asyncio
        async def test_no_parent_order_id_count(self, returned, returned_found_count, parent_order_id, order):
            order_ids = [_.order_id for _ in await returned()]
            assert len(order_ids) == await returned_found_count() and len(order_ids) > 0

        @pytest.mark.asyncio
        async def test_ignore_parent_order_id(self, returned, parent_order_id, order):
            order_ids = [_.order_id for _ in await returned(parent_order_id=SKIP_CONDITION)]
            assert order.order_id in order_ids and len(order_ids) > 1

        @pytest.mark.asyncio
        async def test_ignore_parent_order_id_count(self, returned, returned_found_count, parent_order_id, order):
            order_ids = [_.order_id for _ in await returned(parent_order_id=SKIP_CONDITION)]
            assert len(order_ids) == await returned_found_count(parent_order_id=SKIP_CONDITION) and len(order_ids) > 0

    class TestPriceOrdering:
        @pytest.fixture
        def orders_data(self):
            return [
                {'price': Decimal(1)},
                {'price': Decimal(4)},
                {'price': Decimal(3)},
                {'price': Decimal(2)},
                {'price': Decimal(5)},
            ]

        @pytest.mark.asyncio
        async def test_price_asc(self, orders, returned):
            assert await returned(sort_by='price') == [
                orders[0],
                orders[3],
                orders[2],
                orders[1],
                orders[4],
            ]

        @pytest.mark.asyncio
        async def test_price_desc(self, orders, returned):
            assert await returned(sort_by='price', descending=True) == [
                orders[4],
                orders[1],
                orders[2],
                orders[3],
                orders[0],
            ]

    class TestStatusOrdering:
        @pytest.fixture
        def orders_data(self, original_order_id):
            return [
                {
                    'kind': OrderKind.PAY,
                    'pay_status': PayStatus.NEW,
                },
                {
                    'kind': OrderKind.PAY,
                    'pay_status': PayStatus.PAID,
                },
                {
                    'kind': OrderKind.REFUND,
                    'pay_status': None,
                    'refund_status': RefundStatus.COMPLETED,
                    'original_order_id': original_order_id,
                },
                {
                    'kind': OrderKind.REFUND,
                    'pay_status': None,
                    'refund_status': RefundStatus.COMPLETED,
                    'original_order_id': original_order_id,
                },
                {
                    'kind': OrderKind.PAY,
                    'pay_status': PayStatus.NEW,
                },
                {
                    'kind': OrderKind.PAY,
                    'pay_status': PayStatus.PAID,
                },
            ]

        @pytest.fixture
        async def db_orders(self, storage, orders):
            return [order async for order in storage.order.find()]

        @pytest.mark.parametrize('desc', (True, False))
        @pytest.mark.asyncio
        async def test_status(self, db_orders, returned, desc, orders_data):
            ret_orders = await returned(sort_by='status', descending=desc)
            ret_statuses = [x.pay_status or x.refund_status for x in ret_orders]
            groupped_ret_statuses = [st for st, _ in itertools.groupby(ret_statuses)]
            orders_data_statuses = set(x.pay_status or x.refund_status for x in db_orders)
            assert len(orders_data_statuses) == len(groupped_ret_statuses)

    class TestLimitOffset:
        @pytest.fixture
        def orders_data(self):
            return [{}] * 10

        @pytest.mark.parametrize('limit,offset', [(1, 5), (5, 2), (10, 0)])
        @pytest.mark.asyncio
        async def test_equal(self, orders, returned, limit, offset, merchant):
            assert await returned(limit=limit,
                                  offset=offset,
                                  sort_by='order_id',
                                  uid=merchant.uid) == orders[offset:(limit + offset)]

        @pytest.mark.asyncio
        async def test_less(self, orders, returned, merchant):
            assert len(await returned(limit=100, uid=merchant.uid)) == 10

    class TestTextQuery:
        @pytest.fixture
        def orders_data(self):
            return [
                {'caption': 'A', 'description': 'B'},
                {'caption': 'yBy', 'description': 'xAx'},
                {'caption': 'C', 'description': 'D'},
                {'caption': 'C', 'description': 'D'},
            ]

        @pytest.mark.asyncio
        async def test_text_query__empty_string(self, orders, returned):
            assert_that(
                await returned(text_query=''),
                contains_inanyorder(*orders),
            )

        @pytest.mark.asyncio
        async def test_text_query__empty_string_count(self, orders, returned_found_count):
            assert await returned_found_count(text_query='') == len(orders)

        @pytest.mark.asyncio
        async def test_text_query__filters(self, orders, returned):
            assert_that(
                await returned(text_query='A'),
                contains_inanyorder(orders[0], orders[1]),
            )

        @pytest.mark.asyncio
        async def test_text_query__filters_count(self, orders, returned_found_count):
            assert await returned_found_count(text_query='A') == 2

    class TestEmailQuery:
        @pytest.fixture
        def orders_data(self):
            return [
                {'user_email': 'ppp@yandex.ru'},
                {'user_email': 'ppp@google.com'},
                {'user_email': 'klklklf@yandex.ru'},
                {'user_email': 'ppopop@google.com'},
            ]

        @pytest.mark.asyncio
        async def test_email_query__empty_string(self, orders, returned):
            assert_that(
                await returned(email_query=''),
                contains_inanyorder(*orders),
            )

        @pytest.mark.asyncio
        async def test_email_query__empty_string_count(self, orders, returned_found_count):
            assert await returned_found_count(email_query='') == len(orders)

        @pytest.mark.asyncio
        async def test_email_query__filters(self, orders, returned):
            assert_that(
                await returned(email_query='ppp@'),
                contains_inanyorder(orders[0], orders[1]),
            )

        @pytest.mark.asyncio
        async def test_email_query__filters_count(self, orders, returned_found_count):
            assert await returned_found_count(email_query='ppp@') == 2

    class TestExcludeStats:
        @pytest.fixture
        def orders_data(self, orders_data, exclude_stats):
            return [{**order, 'exclude_stats': exclude_stats} for order in orders_data]

        @pytest.mark.asyncio
        async def test_negative(self, returned, exclude_stats):
            assert len(await returned(exclude_stats=not exclude_stats)) == 0

        @pytest.mark.asyncio
        async def test_negative_count(self, returned_found_count, exclude_stats):
            assert await returned_found_count(exclude_stats=not exclude_stats) == 0

        @pytest.mark.parametrize('skip', (True, False))
        @pytest.mark.asyncio
        async def test_positive(self, returned, orders, orders_data, exclude_stats, skip):
            kwargs = {}
            if not skip:
                kwargs['exclude_stats'] = exclude_stats

            assert_that(
                await returned(**kwargs),
                contains_inanyorder(*orders),
            )

        @pytest.mark.parametrize('skip', (True, False))
        @pytest.mark.asyncio
        async def test_positive_count(self, returned_found_count, orders, orders_data, exclude_stats, skip):
            kwargs = {}
            if not skip:
                kwargs['exclude_stats'] = exclude_stats

            assert await returned_found_count(**kwargs) == len(orders)

    class TestPayMethod:
        @pytest.fixture
        def orders_data(self):
            return [
                {},
                {'pay_status': PayStatus.PAID},
                {'paymethod_id': PAYMETHOD_ID_OFFLINE},
                {'paymethod_id': PAYMETHOD_ID_OFFLINE, 'pay_status': PayStatus.PAID},
                {'paymethod_id': 'card-xxx'},
                {'paymethod_id': 'card-xxx', 'pay_status': PayStatus.PAID},
            ]

        @pytest.mark.parametrize('pay_method', chain((None,), PAY_METHODS))
        @pytest.mark.asyncio
        async def test_pay_method(self, pay_method, orders, returned):
            assert_that(
                await returned(pay_method=pay_method),
                contains_inanyorder(
                    *[order for order in orders if (order.pay_method == pay_method or pay_method is None)]
                ),
            )

        @pytest.mark.parametrize('pay_method', chain((None,), PAY_METHODS))
        @pytest.mark.asyncio
        async def test_pay_method_count(self, pay_method, orders, returned_found_count):
            assert await returned_found_count(pay_method=pay_method) == len([
                order for order in orders if (order.pay_method == pay_method or pay_method is None)
            ])

    class TestCreatedBySources:
        @pytest.fixture
        def orders_data(self):
            return [
                {'created_by_source': OrderSource.UI},
                {'created_by_source': OrderSource.UI},
                {'created_by_source': OrderSource.SERVICE},
                {'created_by_source': OrderSource.SDK_API},
                {'created_by_source': OrderSource.SERVICE},
                {'created_by_source': OrderSource.UI},
            ]

        @pytest.mark.parametrize('created_by_sources,indexes', [
            ([], []),
            ([OrderSource.SERVICE], [2, 4]),
            ([OrderSource.SDK_API], [3]),
            ([OrderSource.SERVICE, OrderSource.SDK_API], [2, 3, 4]),
        ])
        @pytest.mark.asyncio
        async def test_created_by_source(self, orders, returned, created_by_sources, indexes):
            assert_that(
                await returned(created_by_sources=created_by_sources),
                contains_inanyorder(*[orders[i] for i in indexes])
            )

        @pytest.mark.parametrize('created_by_sources,count', [
            ([], 0),
            ([OrderSource.SERVICE], 2),
            ([OrderSource.SDK_API], 1),
            ([OrderSource.SERVICE, OrderSource.SDK_API], 3),
        ])
        @pytest.mark.asyncio
        async def test_created_by_source_count(self, orders, returned_found_count, created_by_sources, count):
            assert await returned_found_count(created_by_sources=created_by_sources) == count

    class TestServiceIds:
        @pytest.fixture
        async def service_merchants(self, create_service_merchant, create_service):
            async def _create():
                service = await create_service()
                service_merchant = await create_service_merchant(service_id=service.service_id)
                service_merchant.service = service
                return service_merchant

            return [await _create() for i in range(3)]

        @pytest.fixture
        def orders_data(self, service_merchants):
            return [{'service_merchant_id': s.service_merchant_id} for s in service_merchants] * 2

        @pytest.fixture
        def orders(self, orders, service_merchants):
            for order in orders:
                order.service_merchant = next(filter(
                    lambda s: s.service_merchant_id == order.service_merchant_id,
                    service_merchants
                ))
            return orders

        @pytest.mark.asyncio
        async def test_service_ids(self, orders, returned, service_merchants):
            for s in service_merchants:
                actual = await returned(service_ids=[s.service.service_id])
                assert actual == list(filter(lambda o: o.service_merchant_id == s.service_merchant_id, orders))

        @pytest.mark.asyncio
        async def test_service_ids_count(self, orders, returned_found_count, service_merchants):
            for s in service_merchants:
                assert await returned_found_count(service_ids=[s.service.service_id]) == 2

        @pytest.mark.asyncio
        async def test_service_ids_empty(self, orders, returned):
            assert await returned(service_ids=[]) == []

        @pytest.mark.asyncio
        async def test_service_ids_empty_count(self, orders, returned_found_count):
            assert await returned_found_count(service_ids=[]) == 0

        @pytest.mark.asyncio
        async def test_service_ids_several(self, orders, returned, service_merchants):
            service_ids = [service_merchants[0].service_id, service_merchants[1].service_id]
            actual = await returned(service_ids=service_ids)
            assert actual == list(filter(lambda o: o.service_merchant.service_id in service_ids, orders))

        @pytest.mark.asyncio
        async def test_service_ids_several_count(self, orders, returned_found_count, service_merchants):
            service_ids = [service_merchants[0].service_id, service_merchants[1].service_id]
            actual = await returned_found_count(service_ids=service_ids)
            assert actual == 4

    class TestCustomerSubscription:
        @pytest.fixture
        def orders_data(self, customer_subscription):
            return [{}]

        @pytest.mark.asyncio
        async def test_customer_subscription__without_subscription(self, order_with_customer_subscription, orders,
                                                                   returned):
            actual = await returned(with_customer_subscription=False, select_customer_subscription=False)
            assert actual == orders

        @pytest.mark.asyncio
        async def test_customer_subscription__with_subscription(self, order_with_customer_subscription, orders,
                                                                returned):
            actual = await returned(with_customer_subscription=True, select_customer_subscription=True)
            assert actual == [order_with_customer_subscription]

        @pytest.mark.asyncio
        async def test_customer_subscription__both(self, order_with_customer_subscription, orders, returned):
            actual = await returned(with_customer_subscription=True, select_customer_subscription=None)
            assert_that(actual, contains_inanyorder(*orders, order_with_customer_subscription))

    class TestFindByCustomerSubscription:
        @pytest.mark.asyncio
        async def test_find_by_customer_subscription__found(self, order_with_customer_subscription, orders, returned):
            actual = await returned(
                select_customer_subscription=True,
                customer_subscription_id=order_with_customer_subscription.customer_subscription_id
            )
            assert actual == [order_with_customer_subscription]

        @pytest.mark.asyncio
        async def test_find_by_customer_subscription__not_found(self, order_with_customer_subscription, orders,
                                                                returned):
            actual = await returned(
                select_customer_subscription=True,
                customer_subscription_id=order_with_customer_subscription.customer_subscription_id + 1
            )
            assert actual == []

    class TestFindByCustomerSubscriptionTransaction:
        @pytest.fixture(autouse=True)
        async def setup(self, storage, order_with_customer_subscription, customer_subscription_transaction):
            order_with_customer_subscription.customer_subscription_tx_purchase_token = \
                customer_subscription_transaction.purchase_token
            await storage.order.save(order_with_customer_subscription)

        @pytest.mark.asyncio
        async def test_find_by_customer_subscription_tx__found(self,
                                                               order_with_customer_subscription,
                                                               customer_subscription_transaction,
                                                               orders,
                                                               returned):
            actual = await returned(
                select_customer_subscription=True,
                customer_subscription_id=order_with_customer_subscription.customer_subscription_id,
                customer_subscription_tx_purchase_token=customer_subscription_transaction.purchase_token
            )
            assert actual == [order_with_customer_subscription]

        @pytest.mark.asyncio
        async def test_find_by_customer_subscription_tx__not_found(self,
                                                                   order_with_customer_subscription,
                                                                   customer_subscription_transaction,
                                                                   orders,
                                                                   returned):
            actual = await returned(
                select_customer_subscription=True,
                customer_subscription_id=order_with_customer_subscription.customer_subscription_id,
                customer_subscription_tx_purchase_token=customer_subscription_transaction.purchase_token + '_1'
            )
            assert actual == []

        @pytest.mark.asyncio
        async def test_find_by_customer_subscription_tx__fail(self,
                                                              order_with_customer_subscription,
                                                              customer_subscription_transaction,
                                                              orders,
                                                              returned):
            with pytest.raises(AssertionError):
                await returned(
                    select_customer_subscription=True,
                    customer_subscription_id=None,
                    customer_subscription_tx_purchase_token=customer_subscription_transaction.purchase_token
                )
