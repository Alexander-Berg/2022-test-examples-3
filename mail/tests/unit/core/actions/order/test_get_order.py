from datetime import timedelta
from typing import Union

import pytest

from hamcrest import assert_that, has_properties

from mail.payments.payments.core.actions.order.get import GetOrderAction, GetOrderServiceMerchantAction
from mail.payments.payments.core.entities.enums import OrderKind, TransactionStatus
from mail.payments.payments.core.entities.transaction import Transaction
from mail.payments.payments.core.exceptions import OrderNotFoundError
from mail.payments.payments.tests.base import BaseTestOrderAction
from mail.payments.payments.utils.datetime import utcnow


def get_action_class(params) -> Union[GetOrderServiceMerchantAction, GetOrderAction]:
    if params.get('service_merchant_id'):
        return GetOrderServiceMerchantAction(**params)
    return GetOrderAction(**params)


class TestGetOrderAction(BaseTestOrderAction):
    @pytest.fixture
    def transaction(self, order):
        return Transaction(
            uid=order.uid,
            order_id=order.order_id,
            tx_id=111,
            revision=222,
            trust_purchase_token='xxx',
            trust_payment_url='yyy',
            trust_failed_result='zzz',
            trust_resp_code='ttt',
            trust_payment_id='vvv',
            status=TransactionStatus.FAILED.value,
            check_at=utcnow() + timedelta(days=1),
            check_tries=333,
        )

    @pytest.fixture
    def order_data_extra(self):
        return {}

    @pytest.fixture
    def with_refunds(self):
        return False

    @pytest.fixture
    def order_data(self, params_key, service_merchant, order_data_extra):
        if params_key == 'service_merchant':
            return {'service_merchant_id': service_merchant.service_merchant_id, **order_data_extra}
        return order_data_extra

    @pytest.fixture
    def refund_data(self, params_key, service_merchant):
        if params_key == 'service_merchant':
            return {'service_merchant_id': service_merchant.service_merchant_id}
        return {}

    @pytest.fixture(params=('uid', 'service_merchant'))
    def params_key(self, request):
        return request.param

    @pytest.fixture
    def params(self, params_key, with_refunds, crypto, merchant, order, service_client, service_merchant):
        GetOrderAction.context.crypto = crypto

        data = {
            'uid': {'uid': merchant.uid},
            'service_merchant': {
                'service_tvm_id': service_client.tvm_id,
                'service_merchant_id': service_merchant.service_merchant_id
            },
        }
        return {
            'order_id': order.order_id,
            'with_refunds': with_refunds,
            **data[params_key],
        }

    @pytest.fixture
    def returned_func(self, params):
        async def _inner():
            return await get_action_class(params).run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    class TestOrdinary:
        @pytest.mark.asyncio
        async def test_ordinary__not_found(self, params, returned_func):
            params['order_id'] += 1
            with pytest.raises(OrderNotFoundError):
                await returned_func()

    @pytest.mark.asyncio
    async def test_returns_order(self, order, shop, items, some_hash, returned):
        order.items = items
        order.order_hash = some_hash
        order.shop = shop
        assert returned == order

    @pytest.mark.asyncio
    async def test_trust_resp_code(self, storage, returned_func, transaction):
        transaction = await storage.transaction.create(transaction)
        returned = await returned_func()
        assert_that(returned, has_properties({'trust_resp_code': transaction.trust_resp_code}))

    @pytest.mark.asyncio
    async def test_trust_payment_id(self, storage, returned_func, transaction):
        transaction = await storage.transaction.create(transaction)
        returned = await returned_func()
        assert_that(returned, has_properties({'trust_payment_id': transaction.trust_payment_id}))

    def test_refunds_none(self, returned):
        assert returned.refunds is None

    class TestWithRefunds:
        @pytest.fixture
        def with_refunds(self):
            return True

        def test_with_refunds__refunds_not_none(self, returned):
            assert returned.refunds == []

        def test_with_refunds__refunds_not_empty(self, refund, returned):
            assert returned.refunds == [refund]

    class TestSkipCrypto:
        @pytest.mark.asyncio
        async def test_returned(self, params, add_crypto_mock, returned_func):
            params['skip_add_crypto'] = True
            await returned_func()
            add_crypto_mock.assert_not_called()

    class TestKind:
        @pytest.mark.asyncio
        async def test_kind__not_found(self, params, returned_func):
            params['kind'] = OrderKind.MULTI
            with pytest.raises(OrderNotFoundError):
                await returned_func()

    class TestSelectCustomerSubscription:
        @pytest.mark.asyncio
        async def test_select_customer_subscription__not_found(self, params, returned_func):
            params['select_customer_subscription'] = True
            with pytest.raises(OrderNotFoundError):
                await returned_func()

        @pytest.mark.asyncio
        async def test_select_customer_subscription__found(self,
                                                           params,
                                                           order_with_customer_subscription,
                                                           returned_func):
            params['select_customer_subscription'] = True
            returned = await returned_func()
            order_with_customer_subscription.items = returned.items
            order_with_customer_subscription.order_hash = returned.order_hash
            order_with_customer_subscription.customer_subscription = returned.customer_subscription
            order_with_customer_subscription.shop = returned.shop
            assert order_with_customer_subscription == returned

    class TestOriginalOrderId:
        @pytest.fixture(autouse=True)
        async def setup(self, storage, refund, params):
            params['order_id'] = refund.order_id

        @pytest.mark.asyncio
        async def test_original_order_id__not_found(self, params, order, returned_func):
            params['original_order_id'] = order.order_id + 1
            with pytest.raises(OrderNotFoundError):
                await returned_func()

        @pytest.mark.asyncio
        async def test_original_order_id__found(self, params, refund, order, returned_func):
            params['original_order_id'] = order.order_id
            returned = await returned_func()
            refund.items = returned.items
            refund.order_hash = returned.order_hash
            refund.shop = returned.shop
            assert refund == returned

    class TestCustomerUid:
        @pytest.fixture
        def order_data_extra(self, randn):
            return {
                'customer_uid': randn()
            }

        @pytest.mark.asyncio
        async def test_customer_uid__found(self, params, order, returned_func):
            params['customer_uid'] = order.customer_uid
            returned = await returned_func()
            order.items = returned.items
            order.order_hash = returned.order_hash
            order.shop = returned.shop
            assert order == returned

        @pytest.mark.asyncio
        async def test_customer_uid__not_found(self, params, order, returned_func):
            params['customer_uid'] = order.customer_uid + 1
            with pytest.raises(OrderNotFoundError):
                await returned_func()
