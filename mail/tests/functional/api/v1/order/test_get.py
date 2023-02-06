from datetime import timedelta, timezone
from urllib.parse import urlencode

import pytest

from hamcrest import assert_that, contains_inanyorder, has_entries, has_items, is_

from mail.payments.payments.core.entities.enums import (
    MerchantRole, OrderKind, PayStatus, RefundStatus, TransactionStatus
)
from mail.payments.payments.core.entities.transaction import Transaction
from mail.payments.payments.tests.base import BaseTestMerchantRoles, parametrize_acquirer
from mail.payments.payments.tests.utils import check_order, items_price, items_with_product_id
from mail.payments.payments.utils.helpers import without_none

from .base import BaseTestOrder


@pytest.mark.usefixtures('moderation')
class BaseTestGetOrder(BaseTestOrder):
    @pytest.fixture(params=(OrderKind.PAY,))
    def kind(self, request):
        return request.param

    @pytest.fixture(autouse=True)
    async def setup(self,
                    storage,
                    moderation,
                    test_data,
                    kind,
                    trust_resp_code):
        await storage.transaction.create(Transaction(
            uid=test_data['order'].uid,
            order_id=test_data['order'].order_id,
            status=TransactionStatus.FAILED,
            trust_resp_code=trust_resp_code,
        ))
        if test_data['order'].kind == OrderKind.PAY:
            test_data['order'].pay_status = PayStatus.PAID
        await storage.order.save(test_data['order'])

    @pytest.fixture
    def with_timeline(self):
        return False

    @pytest.fixture
    def params(self, with_timeline):
        return {
            'with_timeline': str(with_timeline)
        }

    @pytest.fixture
    async def order_response(self, client, test_data, params):
        r = await client.get(f"{test_data['path']}?{urlencode(without_none(params))}")
        assert r.status == 200
        return await r.json()

    @pytest.fixture
    def expected_timeline(self, order, refund):
        return [
            {
                'date': order.created.astimezone(timezone.utc).isoformat(),
                'event_type': 'created',
                'extra': {}
            },
            {
                'date': refund.closed.astimezone(timezone.utc).isoformat(),
                'event_type': 'partially_refunded',
                'extra': {
                    'refund_amount': 433.0
                }
            }
        ]

    def test_response_format(self, order_response):
        assert_that(
            order_response,
            has_entries({
                'code': 200,
                'status': 'success',
                'data': is_(dict),
            })
        )

    @parametrize_acquirer
    def test_response_data(self, order_data, test_data, trust_resp_code, order_response):
        check_order(
            test_data['order'],
            order_response['data'],
            {
                'items': contains_inanyorder(*items_with_product_id(order_data['items'])),
                'price': items_price(order_data['items']),
                'currency': 'RUB',
                'trust_resp_code': trust_resp_code,
                'shop': has_entries({
                    'uid': test_data['order'].shop.uid,
                    'shop_id': test_data['order'].shop.shop_id,
                    'name': test_data['order'].shop.name,
                    'is_default': test_data['order'].shop.is_default,
                    'shop_type': test_data['order'].shop.shop_type.value,
                    'created': test_data['order'].shop.created.astimezone(timezone.utc).isoformat(),
                    'updated': test_data['order'].shop.updated.astimezone(timezone.utc).isoformat(),
                })
            }
        )

    def test_hashes(self, crypto_mock, order_response, check_hashes):
        check_hashes(crypto_mock, order_response['data'])

    def test_order_with_customer_subscriptions_response_data(self,
                                                             order_with_customer_subscription,
                                                             order_response,
                                                             customer_subscription):
        actual = order_response['data']['customer_subscription']
        assert customer_subscription.customer_subscription_id == actual['customer_subscription_id']
        assert customer_subscription.subscription_id == actual['subscription']['subscription_id']

    class TestServiceMerchant:
        @pytest.fixture
        def order(self, service_merchant_order):
            return service_merchant_order

        @parametrize_acquirer
        def test_service_merchant__response_data(self, order_data, test_data, trust_resp_code, order_response):
            check_order(
                test_data['order'],
                order_response['data'],
                {
                    'service_merchant': has_entries({
                        'enabled': test_data['order'].service_merchant.enabled,
                        'entity_id': test_data['order'].service_merchant.entity_id,
                        'deleted': test_data['order'].service_merchant.deleted,
                        'created': test_data['order'].service_merchant.updated.astimezone(timezone.utc).isoformat(),
                        'service': has_entries({
                            'service_id': test_data['order'].service_merchant.service.service_id,
                            'name': test_data['order'].service_merchant.service.name,
                        }),
                    }),
                }
            )

    class TestWithTimeline:
        @pytest.fixture
        def with_timeline(self):
            return True

        @pytest.fixture
        async def refund(self, storage, order, refund):
            refund.refund_status = RefundStatus.COMPLETED
            refund.closed = order.created + timedelta(days=1)
            await storage.order.save(refund)
            return refund

        @pytest.fixture
        def run_check_order(self, test_data, order_response):
            def _inner(expected_timeline):
                check_order(
                    test_data['order'],
                    order_response['data'],
                    {
                        'timeline': expected_timeline
                    }
                )
            return _inner

        def test_with_timeline__response_data(self, expected_timeline, run_check_order):
            run_check_order(expected_timeline)

        def test_customer_subscription_with_timeline(self,
                                                     order_with_customer_subscription,
                                                     expected_timeline,
                                                     customer_subscription_transaction,
                                                     run_check_order):
            if expected_timeline:
                expected_timeline.insert(1, {
                    'date': customer_subscription_transaction.updated.astimezone(timezone.utc).isoformat(),
                    'event_type': 'periodic_held',
                    'extra': {
                        'periodic_amount': 183.48,
                        'tx_id': {
                            'uid': customer_subscription_transaction.uid,
                            'customer_subscription_id': customer_subscription_transaction.customer_subscription_id,
                            'purchase_token': customer_subscription_transaction.purchase_token
                        }
                    }
                })
            run_check_order(expected_timeline)


class TestGetOrderByUID(BaseTestMerchantRoles, BaseTestGetOrder):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
        MerchantRole.OPERATOR,
        MerchantRole.VIEWER,
    )

    @pytest.fixture
    def id_type(self, tvm):
        return 'uid'


class TestGetOrderByServiceMerchantId(BaseTestGetOrder):
    @pytest.fixture
    def id_type(self):
        return 'service_merchant'

    @pytest.fixture
    def expected_timeline(self):
        return None

    @pytest.fixture
    def order(self, service_merchant_order):
        return service_merchant_order  # redefine order fixture, for order_with_customer_subscription use


@pytest.mark.usefixtures('moderation')
class TestGetBadOrder(BaseTestOrder):
    @pytest.fixture(params=('bad_service_merchant', 'bad_uid', 'bad_service_merchant_order_id', 'bad_uid_order_id'))
    def test_data(self, request, order, bad_merchant_uid, bad_order_id, bad_service_merchant_id, merchant,
                  service_merchant):
        data = {'bad_uid': [f'/v1/order/{bad_merchant_uid}/{order.order_id}', 403],
                'bad_uid_order_id': [f'/v1/order/{merchant.uid}/{bad_order_id}', 403],
                'bad_service_merchant': [f'/v1/internal/order/{bad_service_merchant_id}/{order.order_id}', 403],
                'bad_service_merchant_order_id':
                    [f'/v1/internal/order/{service_merchant.service_merchant_id}/{bad_order_id}', 404]}
        return data[request.param]

    @pytest.fixture
    async def order_error_response(self, client, test_data, tvm):
        r = await client.get(test_data[0])
        assert r.status == test_data[1]
        return await r.json()

    def test_error_response(self, order_error_response, test_data):
        assert_that(
            order_error_response,
            has_entries({
                'code': test_data[1],
                'status': 'fail',
                'data': has_items('message'),
            }),
        )
