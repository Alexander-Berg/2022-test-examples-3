from copy import copy

import pytest

from sendr_utils import enum_value

from hamcrest import all_of, assert_that, contains_inanyorder, has_entries, has_item

from mail.payments.payments.core.entities.enums import (
    PAY_METHODS, FunctionalityType, MerchantRole, ModerationType, OrderKind, OrderSource, PayStatus, RefundStatus,
    TransactionStatus
)
from mail.payments.payments.core.entities.moderation import Moderation
from mail.payments.payments.core.entities.order import Order
from mail.payments.payments.core.entities.service import ServiceMerchant
from mail.payments.payments.core.entities.transaction import Transaction
from mail.payments.payments.tests.base import BaseTestMerchantRoles
from mail.payments.payments.tests.utils import check_order
from mail.payments.payments.utils.helpers import without_none

from .base import BaseTestOrder


@pytest.mark.usefixtures('moderation')
class BaseTestGetOrderList(BaseTestOrder):
    @pytest.fixture
    def service_merchant_uid(self, randn):
        return randn()

    @pytest.fixture
    async def service_merchant_merchant(self, _create_merchant, service_merchant_uid, acquirer,
                                        merchant_documents):
        return await _create_merchant(service_merchant_uid, acquirer, merchant_documents)

    @pytest.fixture
    async def service_merchant_moderation(self, storage, service_merchant_merchant):
        await storage.moderation.create(Moderation(
            uid=service_merchant_merchant.uid,
            revision=service_merchant_merchant.revision,
            moderation_type=ModerationType.MERCHANT,
            functionality_type=FunctionalityType.PAYMENTS,
            approved=True,
        ))

    @pytest.fixture
    async def service_merchant(self, storage, service_merchant_merchant, service_client):
        service_merchant = ServiceMerchant(uid=service_merchant_merchant.uid,
                                           service_id=service_client.service_id,
                                           entity_id=f'some_entity {service_client.tvm_id}',
                                           description='some description',
                                           enabled=True)
        return await storage.service_merchant.create(service_merchant)

    @pytest.fixture
    def kind(self):
        return None

    @pytest.fixture
    def order_data_list(self, kind):
        return [
            without_none({
                'kind': enum_value(kind),
                'caption': f'cap_{n}',
                'description': f'des_{n}',
                'items': [
                    {
                        'name': f'nam_{n}_{i}',
                        'nds': 'nds_10',
                        'price': 10 * n,
                        'amount': n,
                        'currency': 'RUB',
                    }
                    for i in range(2)
                ]
            })
            for n in range(1, 6)
        ]

    @pytest.fixture
    async def orders(self, no_merchant_user_check, storage, client, merchant, order_data_list):
        with no_merchant_user_check():
            order_ids = []
            for order_data in order_data_list:
                r = await client.post(f'/v1/order/{merchant.uid}/', json=order_data)
                assert r.status == 200
                order_ids.append((await r.json())['data']['order_id'])
        return [
            await storage.order.get(uid=merchant.uid, order_id=order_id)
            for order_id in order_ids
        ]

    async def _make_refunds(self, storage, orders):
        refunds = []
        for order in orders:
            order.refunds = []
            refund = await storage.order.create(Order(
                uid=order.uid,
                original_order_id=order.order_id,
                shop_id=order.shop_id,
                kind=OrderKind.REFUND,
                pay_status=None,
                refund_status=RefundStatus.CREATED,
            ))
            refund = await storage.order.get(refund.uid, refund.order_id)
            order.refunds.append(refund)
            refunds.append(refund)
        return refunds

    @pytest.fixture
    def refunds(self):
        return []

    @pytest.fixture
    async def service_merchant_orders(self, storage, client, service_merchant, order_data_list, tvm,
                                      service_merchant_moderation):
        order_ids = []
        for order_data in order_data_list:
            r = await client.post(f'/v1/internal/order/{service_merchant.service_merchant_id}/', json=order_data)
            assert r.status == 200
            order_ids.append((await r.json())['data']['order_id'])
        return [
            await storage.order.get(uid=service_merchant.uid,
                                    order_id=order_id,
                                    service_merchant_id=service_merchant.service_merchant_id)
            for order_id in order_ids
        ]

    @pytest.fixture
    def service_merchant_order_test_data(self, service_merchant, service_merchant_orders):
        return {'path': f'/v1/internal/order/{service_merchant.service_merchant_id}/',
                'orders': service_merchant_orders}

    @pytest.fixture
    def uid_order_test_data(self, orders, merchant):
        return {'path': f'/v1/order/{merchant.uid}/',
                'orders': orders}

    @pytest.fixture
    def send_request(self, client, test_data):
        async def _inner(params=None):
            return await client.get(test_data['path'], params=params)

        return _inner

    @pytest.fixture
    async def orders_response(self, send_request):
        r = await send_request()
        return (await r.json())['data']

    @pytest.fixture(autouse=True)
    async def setup(self, storage, moderation, test_data, kind, trust_resp_code, trust_payment_id):
        for order in test_data['orders']:
            await storage.transaction.create(Transaction(
                uid=order.uid,
                order_id=order.order_id,
                status=TransactionStatus.FAILED,
                trust_resp_code=trust_resp_code,
                trust_payment_id=trust_payment_id,
            ))
            if order.kind == OrderKind.PAY:
                order.pay_status = PayStatus.PAID
            await storage.order.save(order)

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

    @pytest.mark.asyncio
    async def test_offset(self, send_request, test_data):
        offset = 1
        r = await send_request(params={'offset': offset})
        r_orders = (await r.json())['data']
        assert len(r_orders) == len(test_data['orders']) - 1

    @pytest.mark.asyncio
    async def test_sort_by(self, storage, send_request, test_data):
        test_data['orders'][3].pay_status = PayStatus.PAID
        test_data['orders'][3] = await storage.order.save(test_data['orders'][3])
        r = await send_request(params={'sort_by': 'created', 'desc': 'true'})
        r_orders = (await r.json())['data']
        r_order_ids = [
            r_order['order_id']
            for r_order in r_orders
        ]
        assert r_order_ids == [
            order.order_id
            for order in reversed(test_data['orders'])
        ]

    @pytest.mark.parametrize('kind', (OrderKind.PAY, OrderKind.MULTI))
    @pytest.mark.asyncio
    async def test_kinds(self, send_request, kind, test_data):
        r = await send_request(params={'kinds[]': kind.value})
        r_orders = (await r.json())['data']
        r_order_ids = [r_order['order_id'] for r_order in r_orders]
        assert r_order_ids == [order.order_id for order in test_data['orders'] if order.kind == kind]

    @pytest.mark.parametrize('kind', (OrderKind.PAY, OrderKind.MULTI))
    @pytest.mark.asyncio
    async def test_refunds(self, send_request, kind, refunds):
        r = await send_request(params={'kinds[]': kind.value, 'with_refunds': 1})
        r_orders = (await r.json())['data']
        r_refund_ids = [r_refund['order_id'] for r_order in r_orders for r_refund in r_order['refunds']]
        assert r_refund_ids == [refund.order_id for refund in refunds]

    @pytest.mark.parametrize('pay_status', (PayStatus.NEW,))
    @pytest.mark.asyncio
    async def test_pay_statuses(self, send_request, storage, pay_status, test_data):
        test_data['orders'][1].active = False
        test_data['orders'][1] = await storage.order.save(test_data['orders'][1])

        r = await send_request(params={'pay_statuses[]': pay_status.value})
        r_orders = (await r.json())['data']
        r_order_ids = [r_order['order_id'] for r_order in r_orders]
        assert r_order_ids == [order.order_id for order
                               in test_data['orders'] if order.pay_status == pay_status and order.active]

    @pytest.mark.asyncio
    async def test_trust_resp_code(self, orders_response, trust_resp_code):
        assert all(order_r['trust_resp_code'] == trust_resp_code for order_r in orders_response)

    @pytest.mark.parametrize('pay_method', PAY_METHODS)
    @pytest.mark.asyncio
    async def test_pay_methods(self, send_request, storage, pay_method, test_data):
        test_data['orders'][1].paymethod_id = pay_method
        test_data['orders'][1].pay_status = PayStatus.PAID
        test_data['orders'][1] = await storage.order.save(test_data['orders'][1])

        r = await send_request(params={'pay_method': pay_method})
        r_orders = (await r.json())['data']
        r_order_ids = [r_order['order_id'] for r_order in r_orders]
        assert r_order_ids == [order.order_id for order in test_data['orders'] if order.pay_method == pay_method]

    @pytest.mark.parametrize('created_by_source', list(OrderSource))
    @pytest.mark.asyncio
    async def test_created_by_sources(self, send_request, storage, created_by_source, test_data):
        # udpate() doesn't rewrite `created_by_source`, so create one more order
        test_data['orders'].append(copy(test_data['orders'][-1]))
        test_data['orders'][-1].created_by_source = created_by_source
        test_data['orders'][-1] = await storage.order.create(test_data['orders'][-1])

        r = await send_request(params={'created_by_sources[]': created_by_source.value})
        r_orders = (await r.json())['data']
        r_order_ids = [r_order['order_id'] for r_order in r_orders]
        assert len(r_order_ids) > 0 and r_order_ids == [
            order.order_id for order in test_data['orders']
            if order.created_by_source == created_by_source
        ]

    @pytest.mark.asyncio
    async def test_service_ids(self, send_request, service_merchant):
        r = await send_request(params={'service_ids[]': service_merchant.service_id + 1})
        r_orders = (await r.json())['data']
        assert r_orders == []

    class TestSubscription:
        @pytest.fixture
        def order(self, test_data):
            return test_data['orders'][0]

        @pytest.fixture
        async def orders_subscription(self, storage, test_data, customer_subscription):
            test_data['orders'][0].customer_subscription_id = customer_subscription.customer_subscription_id
            test_data['orders'][0] = await storage.order.save(test_data['orders'][0])
            return test_data['orders']

        @pytest.mark.parametrize('subscription_param', (None, 'false'))
        @pytest.mark.asyncio
        async def test_no_customer_subscription(self, orders_subscription, send_request, subscription_param):
            params = {'subscription': subscription_param} if subscription_param is not None else {}
            r = await send_request(params=params)
            r_orders = (await r.json())['data']
            r_order_ids = [r_order['order_id'] for r_order in r_orders]

            assert len(r_orders) == len(orders_subscription) - 1 \
                and r_order_ids == [o.order_id for o in orders_subscription if o.customer_subscription_id is None]

        @pytest.mark.parametrize('subscription_param', ('true', 'null'))
        @pytest.mark.asyncio
        async def test_customer_subscription(self, orders_subscription, send_request, subscription_param,
                                             customer_subscription, subscription):
            r = await send_request(params={'subscription': subscription_param})
            r_orders = (await r.json())['data']
            order_ids = [
                o.order_id for o in orders_subscription
                if subscription_param == 'null' or o.customer_subscription_id is not None
            ]

            assert_that(r_orders, all_of(
                contains_inanyorder(*[has_entries({'order_id': order_id}) for order_id in order_ids]),
                has_item(
                    has_entries({
                        'customer_subscription': has_entries({
                            'customer_subscription_id': customer_subscription.customer_subscription_id,
                            'subscription': has_entries({
                                'subscription_id': subscription.subscription_id,
                            })
                        })
                    })
                )
            ))


class TestGetOrderList(BaseTestMerchantRoles, BaseTestGetOrderList):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
        MerchantRole.OPERATOR,
        MerchantRole.VIEWER,
    )

    @pytest.fixture
    def id_type(self):
        return 'uid'

    @pytest.fixture
    async def refunds(self, storage, orders):
        return await self._make_refunds(storage, orders)


class TestGetOrderListInternal(BaseTestGetOrderList):
    @pytest.fixture
    def id_type(self):
        return 'service_merchant'

    @pytest.fixture
    async def refunds(self, storage, service_merchant_orders):
        return await self._make_refunds(storage, service_merchant_orders)
