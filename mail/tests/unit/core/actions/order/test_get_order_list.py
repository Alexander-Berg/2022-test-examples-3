from copy import copy
from decimal import Decimal
from typing import Union

import pytest

from hamcrest import assert_that, contains, contains_inanyorder, has_properties, has_property

from mail.payments.payments.core.actions.order.get_list import GetOrderListAction, GetServiceMerchantOrderListAction
from mail.payments.payments.core.entities.enums import NDS, OrderKind, PayStatus, RefundStatus
from mail.payments.payments.core.entities.item import Item
from mail.payments.payments.core.entities.order import Order, OriginalOrderInfo
from mail.payments.payments.core.entities.product import Product
from mail.payments.payments.tests.base import BaseTestOrderAction


def get_action_class(params) -> Union[GetServiceMerchantOrderListAction, GetOrderListAction]:
    if params.get('service_merchant_id'):
        return GetServiceMerchantOrderListAction(**params)
    return GetOrderListAction(**params)


class TestGetOrderListAction(BaseTestOrderAction):
    @pytest.fixture
    def items(self):
        return 'fake items'

    @pytest.fixture
    def find_mock_calls(self, mocker):
        calls = []

        async def dummy(*args, **kwargs):
            nonlocal calls
            calls.append((args, kwargs))
            return
            yield

        mocker.patch('mail.payments.payments.storage.mappers.order.order.OrderMapper.find', dummy)
        return calls

    @pytest.fixture
    def orders_data(self):
        return [
            {'items_count': 1},
            {'items_count': 4},
            {'items_count': 3},
            {'items_count': 2},
            {'items_count': 5},
        ]

    @pytest.fixture
    async def orders(self, storage, merchant, service_merchant, shop, some_hash, orders_data):
        orders = []
        for _ in orders_data:
            order = await storage.order.create(Order(
                uid=merchant.uid,
                service_merchant_id=service_merchant.service_merchant_id,
                shop_id=shop.shop_id,
            ))
            orders.append(await storage.order.get(order.uid, order.order_id))
            orders[-1].order_hash = some_hash
            orders[-1].refunds = list()
        return orders

    @pytest.fixture
    async def _products(self, storage, merchant, orders_data):
        max_item_count = max([order_data['items_count'] for order_data in orders_data])
        products = []
        for i in range(max_item_count):
            products.append(await storage.product.create(Product(
                uid=merchant.uid,
                name=f'product {i}',
                price=Decimal(f'11.{i}'),
                nds=NDS.NDS_18,
            )))
        return products

    @pytest.fixture
    async def _items(self, storage, merchant, orders_data, orders, _products):
        items = []
        for order_data, order in zip(orders_data, orders):
            order.items = []
            for i in range(order_data.get('items_count', 0)):
                item = await storage.item.create(Item(
                    uid=merchant.uid,
                    order_id=order.order_id,
                    product_id=_products[i].product_id,
                    amount=Decimal('33.44'),
                ))
                item.product = _products[i]
                order.items.append(item)
                items.append(item)
        return items

    @pytest.fixture
    async def _refunds(self, storage, merchant, orders, _items):
        refunds = []
        for order in orders:
            order.refunds = []
            refund = await storage.order.create(Order(
                uid=merchant.uid,
                original_order_id=order.order_id,
                shop_id=order.shop_id,
                kind=OrderKind.REFUND,
                pay_status=None,
                refund_status=RefundStatus.CREATED,
            ))
            refund = await storage.order.get(refund.uid, refund.order_id)
            refund.items = []
            for item in _items:
                if item.order_id == order.order_id:
                    item = copy(item)
                    item.order_id = refund.order_id
                    await storage.item.create(item)
                    refund.items.append(item)
            order.refunds.append(refund)
        return refunds

    @pytest.fixture(params=('uid', 'service_merchant'))
    def params(self, request, merchant, service_client, service_merchant):
        data = {'uid': {'uid': merchant.uid},
                'service_merchant': {'service_tvm_id': service_client.tvm_id,
                                     'service_merchant_id': service_merchant.service_merchant_id}}
        return data[request.param]

    @pytest.mark.asyncio
    async def test_get_all(self, orders, _items, params):
        returned = await get_action_class(params).run()
        assert returned == orders

    @pytest.mark.asyncio
    async def test_mocked_find_call(self, find_mock_calls, params, merchant):
        params.update({
            key: i for i, key in enumerate([
                'created_from',
                'created_to',
                'price_from',
                'price_to',
                'held_at_from',
                'held_at_to',
                'kinds',
                'parent_order_id',
                'pay_statuses',
                'refund_statuses',
                'is_active',
                'text_query',
                'email_query',
                'sort_by',
                'descending',
                'limit',
                'created_by_sources',
                'service_ids',
            ])
        })
        await get_action_class(params).run()
        first_call_args = find_mock_calls[0][0]
        assert_that(
            first_call_args[1],
            has_properties({
                'uid': merchant.uid,
                'created_from': params['created_from'],
                'created_to': params['created_to'],
                'price_from': params['price_from'],
                'price_to': params['price_to'],
                'held_at_from': params['held_at_from'],
                'held_at_to': params['held_at_to'],
                'kinds': params['kinds'],
                'parent_order_id': params['parent_order_id'],
                'pay_statuses': params['pay_statuses'],
                'refund_statuses': params['refund_statuses'],
                'is_active': params['is_active'],
                'sort_by': params['sort_by'],
                'descending': params['descending'],
                'limit': params['limit'],
                'text_query': params['text_query'],
                'email_query': params['email_query'],
                'created_by_sources': params['created_by_sources'],
                'service_ids': params['service_ids'],
            })
        )

    @pytest.mark.asyncio
    async def test_fetches_items(self, orders, params, _items):
        returned = await get_action_class(params).run()
        assert_that(
            returned,
            contains(*[
                has_properties({
                    'items': contains_inanyorder(*order.items)
                })
                for order in orders
            ])
        )

    @pytest.mark.asyncio
    async def test_fetches_refunds(self, orders, params, _refunds):
        params['with_refunds'] = True
        params['kinds'] = [OrderKind.PAY]  # do not fetch refunds as usual orders
        returned = await get_action_class(params).run()

        assert_that(
            returned,
            contains(*[
                has_properties({
                    'refunds': contains_inanyorder(*order.refunds)
                })
                for order in orders
            ])
        )

    @pytest.mark.asyncio
    async def test_not_found(self, params):
        assert await get_action_class(params).run() == []


class TestOriginalOrderInfo:
    @pytest.fixture
    async def new_order(self, storage, merchant, shop, original_order) -> Order:
        order = Order(uid=merchant.uid, shop_id=shop.shop_id, order_id=2, original_order_id=original_order.order_id,
                      kind=OrderKind.REFUND, pay_status=None, refund_status=RefundStatus.COMPLETED)
        await storage.order.create(order)
        return order

    @pytest.fixture
    async def original_order(self, storage, merchant, shop, order) -> Order:
        order = Order(uid=merchant.uid, shop_id=shop.shop_id, order_id=1, kind=OrderKind.PAY,
                      pay_status=PayStatus.CANCELLED)
        await storage.order.create(order)
        return order

    @pytest.fixture(autouse=True)
    def mock_hashes(self, mocker):
        mock = mocker.Mock()
        mock.encrypt_order.return_value = "hash"
        mock.encrypt_payment.return_value = "payment_hash"
        GetOrderListAction.context.crypto = mock

    @pytest.mark.asyncio
    async def test_should_return_with_original_order(self, original_order, new_order):
        action = get_action_class({
            "uid": new_order.uid,
            "order_id": new_order.order_id
        })
        result = await action.handle()
        assert_that(result, contains(has_property("original_order_info",
                                                  OriginalOrderInfo(pay_status=original_order.pay_status,
                                                                    paymethod_id=original_order.paymethod_id))))

    @pytest.mark.asyncio
    async def test_should_return_none_if_no_original_order(self, original_order):
        action = get_action_class({
            "uid": original_order.uid,
            "order_id": original_order.order_id
        })
        result = await action.handle()
        assert_that(result, contains(has_property("original_order_info", None)))
