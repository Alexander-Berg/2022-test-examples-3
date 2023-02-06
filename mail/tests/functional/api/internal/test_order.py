from datetime import timezone

import pytest

from hamcrest import (
    assert_that, contains, contains_inanyorder, greater_than, has_entries, has_properties, match_equality
)

from mail.payments.payments.core.entities.enums import PayStatus, ReceiptType, TransactionStatus
from mail.payments.payments.core.entities.transaction import Transaction


@pytest.fixture
def payment_get_mock(shop_type, trust_client_mocker, randn):
    with trust_client_mocker(shop_type, 'payment_get', {'uid': randn()}) as mock:
        yield mock


@pytest.fixture
def payment_deliver_mock(shop_type, trust_client_mocker):
    with trust_client_mocker(shop_type, 'payment_deliver', {}) as mock:
        yield mock


@pytest.fixture
def refund_create_mock(shop_type, trust_client_mocker, rands):
    with trust_client_mocker(shop_type, 'refund_create', rands()) as mock:
        yield mock


@pytest.fixture
async def order(client, moderation, storage, service_merchant, order_data, tvm):
    r = await client.post(
        f'/v1/internal/order/{service_merchant.service_merchant_id}',
        json=order_data,
    )
    assert r.status == 200
    order_id = (await r.json())['data']['order_id']
    return await storage.order.get(uid=service_merchant.uid,
                                   order_id=order_id,
                                   service_merchant_id=service_merchant.service_merchant_id)


@pytest.fixture
async def refund(payment_get_mock, refund_create_mock, client, order, transaction, storage, service_merchant,
                 order_data, tvm):
    order.pay_status = PayStatus.PAID
    order = await storage.order.save(order)

    r = await client.post(
        f'/v1/internal/order/{service_merchant.service_merchant_id}/{order.order_id}/refund',
        json=order_data,
    )

    assert r.status == 200
    order_id = (await r.json())['data']['order_id']
    return await storage.order.get(uid=service_merchant.uid,
                                   order_id=order_id,
                                   service_merchant_id=service_merchant.service_merchant_id)


class TestInternalOrderList:
    @pytest.fixture
    def params(self):
        return {}

    @pytest.fixture
    async def response(self, client, transaction, order, params, service_merchant, tvm):
        r = await client.get(
            f'/v1/internal/order/{service_merchant.service_merchant_id}',
            params=params
        )
        assert r.status == 200
        return await r.json()

    def test_response(self, order, refund, response):
        assert_that(
            response['data'],
            contains_inanyorder(
                has_entries({
                    'kind': 'pay',
                    'order_id': order.order_id,
                    'receipt_type': order.data.receipt_type.value,
                }),
                has_entries({
                    'kind': 'refund',
                    'order_id': refund.order_id,
                    'original_order_id': order.order_id,
                })
            )
        )

    class TestFilterByOriginalOrderId:
        @pytest.fixture
        def params(self, order):
            return {'original_order_id': order.order_id}

        def test_filter_by_original_order_id__response(self, order, refund, response):
            assert_that(
                response['data'],
                contains_inanyorder(
                    has_entries({
                        'kind': 'refund',
                        'order_id': refund.order_id,
                        'original_order_id': order.order_id,
                    })
                )
            )


class TestInternalOrderGet:
    @pytest.fixture
    def params(self):
        return {}

    @pytest.fixture
    async def order_response(self, client, transaction, order, params, service_merchant, tvm):
        r = await client.get(f'/v1/internal/order/{service_merchant.service_merchant_id}/{order.order_id}',
                             params=params)
        assert r.status == 200
        return await r.json()

    @pytest.mark.usefixtures('moderation')
    class TestTrustRespCode:
        @pytest.mark.asyncio
        async def test_trust_resp_code__response_data(self, order_response, trust_resp_code):
            assert order_response['data']['trust_resp_code'] == trust_resp_code

    @pytest.mark.usefixtures('moderation')
    class TestPayToken:
        def test_pay_token__hashes(self, crypto_mock, order_response, merchant, payments_settings):
            data = order_response['data']
            pay_token = data['pay_token'][len(payments_settings.PAY_TOKEN_PREFIX):]

            with crypto_mock.decrypt_payment(pay_token) as order:
                assert_that(order, has_entries({
                    'uid': merchant.uid,
                    'order_id': data['order_id'],
                }))

    @pytest.mark.usefixtures('moderation')
    class TestWithRefunds:
        @pytest.fixture
        def with_refunds(self):
            return True

        @pytest.fixture
        def params(self, with_refunds):
            return {'with_refunds': f'{with_refunds}'.lower()}

        @pytest.mark.asyncio
        async def test_with_refunds__response_data(self, refund, order, order_response):
            assert_that(
                order_response['data']['refunds'],
                contains_inanyorder(has_entries({
                    'kind': 'refund',
                    'order_id': refund.order_id,
                    'original_order_id': order.order_id
                }))
            )

        @pytest.mark.asyncio
        async def test_with_refunds__no_refunds(self, order_response):
            assert order_response['data']['refunds'] == []

        @pytest.mark.asyncio
        @pytest.mark.parametrize('with_refunds', [False])
        async def test_with_refunds__none_refunds(self, order_response):
            assert order_response['data']['refunds'] is None

    @pytest.mark.usefixtures('moderation')
    class TestGetOrderWithTrustUrl:
        @pytest.fixture
        def transaction_status(self):
            return TransactionStatus.ACTIVE

        @pytest.mark.asyncio
        async def test_get_order_with_trust_url__response_data(self, order_response, payment_url):
            assert order_response['data']['trust_url'] == payment_url

    @pytest.mark.usefixtures('moderation')
    class TestGetOrderWithoutTrustUrl:
        @pytest.fixture(params=[
            None,
            TransactionStatus.FAILED,
            TransactionStatus.CANCELLED,
            TransactionStatus.CLEARED,
            TransactionStatus.HELD,
        ])
        def transaction_status(self, request):
            return request.param

        @pytest.mark.asyncio
        async def test_get_order_without_trust_url__response_data(self, order_response):
            assert order_response['data']['trust_url'] is None

    @pytest.mark.usefixtures('moderation')
    class TestWithCustomerSubscription:
        def test_order_with_customer_subscription(self,
                                                  order_with_customer_subscription,
                                                  order_response,
                                                  customer_subscription):
            actual = order_response['data']['customer_subscription']
            assert customer_subscription.customer_subscription_id == actual['customer_subscription_id']
            assert customer_subscription.subscription_id == actual['subscription']['subscription_id']


@pytest.mark.usefixtures('moderation', 'balance_person_mock')
class TestPayOfflineOrder:
    @pytest.fixture
    def returned_func(self, client, order, service_merchant, tvm):
        async def _inner(**kwargs):
            return await client.post(
                f'/v1/internal/order/{service_merchant.service_merchant_id}/{order.order_id}/pay_offline',
                json=kwargs,
            )

        return _inner

    @pytest.fixture
    async def returned_json(self, returned):
        return await returned.json()

    def test_pay_offline(self, order, returned, returned_json):
        assert_that(
            (returned.status, returned_json['data']),
            contains(200, has_entries({
                'paymethod_id': 'offline',
                'pay_method': 'offline',
                'pay_status': 'paid',
                'revision': greater_than(order.revision),
                'updated': greater_than(order.updated.astimezone(timezone.utc).isoformat()),
            }))
        )

    @pytest.mark.asyncio
    async def test_service_data(self, rands, returned_func):
        service_data = {rands(): rands()}
        response = await returned_func(service_data=service_data)
        assert_that(
            (await response.json())['data'],
            has_entries({
                'service_data': service_data,
            })
        )


class TestInternalUpdateServiceDataOrder:
    @pytest.fixture
    async def order(self, service_merchant, create_order):
        return await create_order(service_merchant_id=service_merchant.service_merchant_id)

    @pytest.fixture
    def service_data(self, rands):
        return {rands(): rands()}

    @pytest.fixture
    def response_func(self, client, tvm, order, service_data):
        async def _inner(**kwargs):
            kwargs = {'service_data': service_data, **kwargs}
            return await client.post(
                f'/v1/internal/order/{order.service_merchant_id}/{order.order_id}/service_data',
                json=kwargs,
            )

        return _inner

    @pytest.mark.asyncio
    async def test_response(self, service_data, response):
        assert_that(
            (await response.json())['data'],
            has_entries({
                'service_data': service_data,
            })
        )

    @pytest.mark.asyncio
    async def test_updates_order(self, storage, order, service_data, response):
        order = await storage.order.get(order.uid, order.order_id)
        assert service_data == order.data.service_data


class TestInternalReceiptCloseOrder:
    @pytest.fixture
    async def order(self, storage, transaction, order, shop):
        order.pay_status = PayStatus.PAID
        order.data.receipt_type = ReceiptType.PREPAID
        order = await storage.order.save(order)
        order.shop = shop
        return order

    @pytest.fixture
    def response_func(self, storage, client, tvm, items, order):
        async def _inner():
            return await client.post(
                f'/v1/internal/order/{order.service_merchant_id}/{order.order_id}/receipt/close',
                json={'items': [{'product_id': item.product_id} for item in items]},
            )

        return _inner

    @pytest.mark.asyncio
    async def test_response(self, payment_deliver_mock, response):
        assert (await response.json())['data'] == {}

    @pytest.mark.asyncio
    async def test_trust_call(self, storage, order, payment_deliver_mock, items, transaction, merchant, response):
        payment_deliver_mock.assert_called_once_with(
            uid=order.uid,
            acquirer=merchant.acquirer,
            purchase_token=transaction.trust_purchase_token,
            order=match_equality(has_properties({
                'order_id': order.order_id,
                'customer_uid': order.customer_uid,
            })),
            items=items
        )


class TestInternalOrderCancel:
    @pytest.fixture
    def response_func(self, storage, client, tvm, items, order):
        async def _inner():
            return await client.post(
                f'/v1/internal/order/{order.service_merchant_id}/{order.order_id}/cancel',
            )

        return _inner

    @pytest.mark.asyncio
    async def test_response(self, payment_deliver_mock, response, service_merchant, order, storage):
        order = await storage.order.get(uid=service_merchant.uid,
                                        order_id=order.order_id,
                                        service_merchant_id=service_merchant.service_merchant_id)
        assert_that((await response.json())['data'], has_entries({
            'uid': order.uid,
            'order_id': order.order_id,
            'pay_status': PayStatus.CANCELLED.value,
            'closed': order.closed.astimezone(timezone.utc).isoformat()
        }))


class TestInternalOrderPayoutInfo:
    @pytest.fixture
    def response_func(self, client, order, transaction):
        async def _inner():
            return await client.get(
                f'/v1/internal/order/{order.service_merchant_id}/{order.order_id}/payout_info',
            )

        return _inner

    @pytest.fixture
    def main_trust_purchase_token(self, rands):
        return rands()

    @pytest.fixture
    async def transaction(self, storage, order, main_trust_purchase_token):
        return await storage.transaction.create(Transaction(
            uid=order.uid,
            order_id=order.order_id,
            status=TransactionStatus.CLEARED,
            trust_purchase_token=main_trust_purchase_token,
            trust_payment_url="test_payment_url",
            trust_resp_code="test_trust_resp_code",
            trust_payment_id="test_trust_payment_id",
        ))

    @pytest.fixture
    def get_payouts_by_purchase_token_mock(
        self,
        mocker,
        main_trust_purchase_token,
        payouts_data_composite,
        payouts_data
    ):
        def get_payouts_by_purchase_token(purchase_token):
            if purchase_token == main_trust_purchase_token:
                return payouts_data_composite
            else:
                return payouts_data

        mocker.patch(
            'mail.payments.payments.interactions.balance_http.BalanceHttpClient.get_payouts_by_purchase_token',
            side_effect=get_payouts_by_purchase_token
        )

    @pytest.mark.asyncio
    async def test_response(
        self,
        get_payouts_by_purchase_token_mock,
        response,
        payouts_data,
    ):
        assert_that((await response.json()), has_entries({'data': payouts_data['payouts'] * 2}))
