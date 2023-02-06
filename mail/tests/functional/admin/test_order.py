import itertools
from decimal import Decimal
from typing import Optional

import pytest

from hamcrest import assert_that, contains_inanyorder

from mail.payments.payments.core.entities.enums import NDS, PAY_METHODS, OrderKind, OrderSource, PayStatus, RefundStatus
from mail.payments.payments.core.entities.item import Item
from mail.payments.payments.core.entities.order import Order
from mail.payments.payments.core.entities.product import Product
from mail.payments.payments.storage.mappers.order.order import FindOrderParams
from mail.payments.payments.tests.utils import check_order

from ..api.v1.order.test_get_list import BaseTestGetOrderList
from .base import BaseTestNotAuthorized


class TestGetOrderListAdmin(BaseTestNotAuthorized, BaseTestGetOrderList):
    @pytest.fixture
    def request_url(self):
        return '/admin/api/v1/order'

    @pytest.fixture(params=('assessor', 'admin'))
    def acting_manager(self, request, managers):
        return managers[request.param]

    @pytest.fixture
    def tvm_uid(self, acting_manager):
        return acting_manager.uid

    @pytest.fixture
    async def response(self, send_request):  # для BaseTestNotAuthorized
        return await send_request()

    @pytest.fixture
    def some_hash(self):
        return 'abcde_some_hash'

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
    def subscription_uid(self, merchant):
        return merchant.uid

    @pytest.fixture
    @pytest.mark.asyncio
    async def test_data(self, storage, merchant, service_merchant, some_hash, orders_data, shop):
        orders = []
        for _ in orders_data:
            orders.append(await storage.order.create(Order(
                uid=merchant.uid,
                service_merchant_id=service_merchant.service_merchant_id,
                shop_id=shop.shop_id,
            )))
            orders[-1].order_hash = some_hash
        return {'orders': orders}

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
                currency='RUB',
            )))
        return products

    @pytest.fixture
    async def _items(self, storage, merchant, orders_data, test_data, _products):
        items = []
        for order_data, order in zip(orders_data, test_data['orders']):
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
        return items

    @pytest.fixture
    def send_request(self, admin_client, tvm, test_data, request_url, _items, merchant):
        async def _inner(params: Optional[dict] = None):
            if params is None:
                params = {'merchant_uid': merchant.uid}
            elif params.get('merchant_uid') is None:
                params['merchant_uid'] = merchant.uid
            return await admin_client.get(request_url, params=params)
        return _inner

    @pytest.mark.asyncio
    async def test_empty_response(self, send_request, randn):
        r = await send_request(params={'merchant_uid': randn(), 'order_id': randn()})
        r_orders = (await r.json())['data']
        assert r_orders == []

    @pytest.mark.asyncio
    async def test_orders_match(self, orders_response, test_data):
        assert_that(
            [
                {'uid': r_order['uid'], 'order_id': r_order['order_id']}
                for r_order in orders_response
            ],
            contains_inanyorder(*[
                {'uid': order.uid, 'order_id': order.order_id}
                for order in test_data['orders']
            ])
        )

    @pytest.mark.asyncio
    async def test_orders_data(self, orders_response, test_data):
        orders_response.sort(key=lambda order: order['order_id'])
        test_data['orders'].sort(key=lambda order: order.order_id)
        for r_order, order in zip(orders_response, test_data['orders']):
            check_order(order, r_order, {'currency': 'RUB'})

    @pytest.mark.asyncio
    async def test_limit(self, send_request, test_data):
        limit = 3
        r = await send_request(params={'limit': limit})
        r_orders = (await r.json())['data']
        assert len(r_orders) == limit

    @pytest.mark.parametrize('sort_by', ('created', 'updated'))
    @pytest.mark.parametrize('descending', (True, False))
    @pytest.mark.asyncio
    async def test_sort_by(self, storage, send_request, test_data, sort_by, descending):
        test_data['orders'][3].pay_status = PayStatus.PAID
        test_data['orders'][3] = await storage.order.save(test_data['orders'][3])

        r = await send_request(params={'sort_by': sort_by, 'descending': str(descending).lower()})

        r_orders = (await r.json())['data']
        r_order_ids = [r_order['order_id'] for r_order in r_orders]

        orders = sorted(test_data['orders'], key=lambda o: str(getattr(o, sort_by)), reverse=descending)

        assert r_order_ids == [order.order_id for order in orders]

    @pytest.mark.parametrize('desc', (True, False))
    @pytest.mark.asyncio
    async def test_sort_by_status(self, storage, send_request, test_data, merchant, service_merchant, shop, desc):
        test_data['orders'][3].pay_status = PayStatus.PAID
        test_data['orders'][3] = await storage.order.save(test_data['orders'][3])

        for i in range(2):
            new_order = Order(
                uid=merchant.uid,
                service_merchant_id=service_merchant.service_merchant_id,
                kind=OrderKind.REFUND,
                pay_status=None,
                refund_status=RefundStatus.REQUESTED,
                original_order_id=test_data['orders'][i].order_id,
                shop_id=shop.shop_id,
            )
            new_order = await storage.order.create(new_order)
            test_data['orders'].append(new_order)
        test_data['orders'].insert(0, test_data['orders'].pop())
        r = await send_request(params={'sort_by': 'status', 'desc': str(desc).lower()})

        r_orders = (await r.json())['data']

        r_statuses = [x.get('pay_status', None) or x.get('refund_status', None) for x in r_orders]
        groupped_r_statuses = [st for st, _ in itertools.groupby(r_statuses)]
        test_data_statuses = set(x.pay_status or x.refund_status for x in test_data['orders'])
        assert len(test_data_statuses) == len(groupped_r_statuses)

    @pytest.mark.asyncio
    async def test_invalid_sort_by(self, send_request, rands):
        r = await send_request(params={'sort_by': rands()})
        assert r.status == 400

    @pytest.mark.parametrize('pay_status', list(PayStatus))
    @pytest.mark.asyncio
    async def test_param_pay_statuses(self, send_request, pay_status):
        r = await send_request(params={'pay_statuses[]': pay_status.value})
        assert r.status == 200

    @pytest.mark.asyncio
    async def test_invalid_param_pay_statuses(self, send_request, rands):
        r = await send_request(params={'pay_statuses[]': rands()})
        assert r.status == 400

    @pytest.mark.parametrize('refund_status', list(RefundStatus))
    @pytest.mark.asyncio
    async def test_param_refund_statuses(self, send_request, refund_status):
        r = await send_request(params={'refund_statuses[]': refund_status.value})
        assert r.status == 200

    @pytest.mark.asyncio
    async def test_invalid_param_refund_statuses(self, send_request, rands):
        r = await send_request(params={'refund_statuses[]': rands()})
        assert r.status == 400

    @pytest.mark.parametrize('kind', list(OrderKind))
    @pytest.mark.asyncio
    async def test_param_pay_kinds(self, send_request, kind):
        r = await send_request(params={'kinds[]': kind.value})
        assert r.status == 200

    @pytest.mark.asyncio
    async def test_invalid_param_kinds_statuses(self, send_request, rands):
        r = await send_request(params={'kinds[]': rands()})
        assert r.status == 400

    @pytest.mark.asyncio
    async def test_invalid_pay_method(self, send_request, rands):
        r = await send_request(params={'pay_method': rands()})
        assert r.status == 400

    @pytest.mark.asyncio
    @pytest.mark.parametrize('pay_method', PAY_METHODS)
    async def test_pay_method(self, send_request, pay_method):
        r = await send_request(params={'pay_method': pay_method})
        assert r.status == 200

    @pytest.mark.asyncio
    @pytest.mark.parametrize('created_by_source', list(OrderSource))
    async def test_created_by_sources(self, send_request, created_by_source):
        r = await send_request(params={'created_by_sources[]': created_by_source.value})
        assert r.status == 200

    @pytest.mark.asyncio
    async def test_invalid_created_by_sources(self, send_request, rands):
        r = await send_request(params={'created_by_sources[]': rands()})
        assert r.status == 400

    @pytest.mark.asyncio
    async def test_service_ids(self, send_request):
        r = await send_request(params={'service_ids[]': 1})
        assert r.status == 200

    @pytest.mark.asyncio
    async def test_invalid_service_ids(self, send_request):
        r = await send_request(params={'service_ids[]': 'junk'})
        assert r.status == 400

    @pytest.mark.asyncio
    @pytest.mark.parametrize('subscription', ('false', 'true', 'null'))
    async def test_subscription(self, send_request, subscription):
        r = await send_request(params={'subscription': subscription})
        assert r.status == 200

    @pytest.mark.asyncio
    async def test_invalid_subscription(self, send_request):
        r = await send_request(params={'subscription': 'junk'})
        assert r.status == 400

    class TestGetOrderListAdminV2:
        @pytest.fixture
        def request_url(self):
            return '/admin/api/v2/order'

        @pytest.fixture
        def request_params(self):
            return {
                'limit': 3,
                'kinds[]': 'pay'
            }

        @pytest.fixture
        def find_params(self, merchant):
            return {
                'kinds': [OrderKind.PAY],
                'uid': merchant.uid
            }

        @pytest.mark.asyncio
        async def test_stats(self, storage, send_request, request_params, find_params, test_data):
            r = await send_request(params=request_params)
            response_data = (await r.json())['data']
            found = await storage.order.get_found_count(FindOrderParams(**find_params))
            # total = await storage.order.get_found_count()
            total = 0  # OPLATASUPPORT-71, PAYBACK-917
            assert all((
                response_data['found'] == found,
                response_data['total'] == total
            ))

        @pytest.mark.asyncio
        async def test_should_respond_trust_payment_id(self, orders_response, trust_payment_id):
            assert all(order_r['trust_payment_id'] == trust_payment_id for order_r in orders_response['orders'])
