import pytest

from sendr_utils import json_value

from mail.payments.payments.tests.base import BaseAcquirerTest
from mail.payments.payments.utils.datetime import utcnow

from ..v1.test_pay_order import TestEmptyReqParams as BaseTestEmptyReqParams
from ..v1.test_pay_order import TestExistingTransaction as BaseTestExistingTransaction
from ..v1.test_pay_order import TestMissingTrustParams as BaseTestMissingTrustParams
from ..v1.test_pay_order import TestNewTransaction as BaseTestNewTransaction
from ..v1.test_pay_order import TestUpdatesTransaction as BaseTestUpdatesTransaction


class BaseTestCustomerSubscription(BaseAcquirerTest):
    @pytest.fixture(autouse=True)
    def get_parents(self, geobase_client_mocker, customer_subscription_data):
        with geobase_client_mocker('get_parents', [customer_subscription_data['region_id']]) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def subscription_create(self, shop_type, trust_client_mocker):
        with trust_client_mocker(shop_type, 'subscription_create', []) as mock:
            yield mock

    @pytest.fixture
    def subscription_acquirer(self, acquirer):
        return acquirer

    @pytest.fixture
    async def response_data(self, moderation, moderation_subscription, client, subscription,
                            customer_subscription_data, tvm, service_merchant):
        r = await client.post(
            f'/v1/internal/customer_subscription/{service_merchant.service_merchant_id}',
            json={
                'subscription_id': subscription.subscription_id,
                **json_value(customer_subscription_data)
            },
        )
        assert r.status == 200
        return (await r.json())['data']

    @pytest.fixture
    async def customer_subscription(self, storage, service_merchant, response_data):
        customer_subscription_id = response_data['customer_subscription']['customer_subscription_id']
        return await storage.customer_subscription.get(uid=service_merchant.uid,
                                                       customer_subscription_id=customer_subscription_id)

    @pytest.fixture
    async def order(self, storage, service_merchant, response_data):
        customer_subscription_id = response_data['customer_subscription']['customer_subscription_id']
        return await storage.order.get(uid=service_merchant.uid,
                                       customer_subscription_id=customer_subscription_id,
                                       with_customer_subscription=True)


class TestGetCustomerSubscription(BaseTestCustomerSubscription):
    @pytest.fixture
    async def customer_list_subscription_response(self, client, subscription, customer_subscription, tvm):
        r = await client.get(f'/v1/internal/customer_subscription/{customer_subscription.service_merchant_id}')
        assert r.status == 200
        return await r.json()

    @pytest.fixture
    def customer_subscription_response_func(self, client, subscription, customer_subscription, tvm):
        async def _inner():
            service_merchant_id = customer_subscription.service_merchant_id
            customer_subscription_id = customer_subscription.customer_subscription_id
            r = await client.get(f'/v1/internal/customer_subscription/{service_merchant_id}/{customer_subscription_id}')
            assert r.status == 200
            return await r.json()

        return _inner

    @pytest.fixture
    async def customer_subscription_response(self, customer_subscription_response_func):
        return await customer_subscription_response_func()

    @pytest.mark.asyncio
    async def test_list_response_data(self, customer_list_subscription_response, customer_subscription):
        customer_subscription_id = customer_subscription.customer_subscription_id
        order_id = customer_subscription.order_id

        returned_customer_subscription = customer_list_subscription_response['data'][0]['customer_subscription']
        returned_order = customer_list_subscription_response['data'][0]['order']

        assert all((
            returned_customer_subscription['customer_subscription_id'] == customer_subscription_id,
            returned_order['order_id'] == order_id,
        ))

    @pytest.mark.asyncio
    async def test_response_data(self, customer_subscription_response, customer_subscription):
        customer_subscription_id = customer_subscription.customer_subscription_id
        returned = customer_subscription_response['data']['customer_subscription']['customer_subscription_id']
        assert returned == customer_subscription_id

    @pytest.mark.parametrize('set_finish', (True, False))
    @pytest.mark.asyncio
    async def test_disable_on_finish(self, customer_subscription, storage, customer_subscription_response_func,
                                     set_finish):
        customer_subscription.enabled = True
        if set_finish:
            customer_subscription.time_finish = utcnow()
        await storage.customer_subscription.save(customer_subscription)

        customer_subscription_response = await customer_subscription_response_func()
        assert customer_subscription_response['data']['customer_subscription']['enabled'] is not set_finish


class TestStartCustomerSubscription(BaseTestCustomerSubscription):
    @pytest.fixture
    def start_path(self, customer_subscription):
        service_merchant_id = customer_subscription.service_merchant_id
        customer_subscription_id = customer_subscription.customer_subscription_id
        return f'/v1/internal/customer_subscription/{service_merchant_id}/{customer_subscription_id}/start'

    class TestCustomerSubscriptionNewTransaction(BaseTestNewTransaction):
        @pytest.fixture
        def path(self, start_path):
            return start_path

    class TestCustomerSubscriptionExistingTransaction(BaseTestExistingTransaction):
        @pytest.fixture
        def path(self, start_path):
            return start_path

    class TestCustomerSubscriptionUpdatesTransaction(BaseTestUpdatesTransaction):
        @pytest.fixture
        def path(self, start_path):
            return start_path

    class TestCustomerSubscriptionEmptyReqParams(BaseTestEmptyReqParams):
        @pytest.fixture
        def path(self, start_path):
            return start_path

        def test_response_status(self, response, payment_status, payment_url):
            assert response['data']['message'] == 'SUBSCRIPTION_REQUIRES_CUSTOMER_UID'

    class TestCustomerSubscriptionMissingTrustParams(BaseTestMissingTrustParams):
        @pytest.fixture
        def path(self, start_path):
            return start_path


class TestCancelCustomerSubscription(BaseTestCustomerSubscription):
    @pytest.fixture(autouse=True)
    def subscription_cancel(self, shop_type, trust_client_mocker):
        with trust_client_mocker(shop_type, 'subscription_cancel', {}) as mock:
            yield mock

    @pytest.fixture
    def enabled(self):
        return True

    @pytest.fixture()
    async def customer_subscription_set_enabled(self, storage, merchant, response_data, enabled):
        customer_subscription = await storage.customer_subscription.get(
            uid=merchant.uid,
            customer_subscription_id=response_data['customer_subscription']['customer_subscription_id']
        )
        customer_subscription.enabled = enabled
        await storage.customer_subscription.save(customer_subscription)

    @pytest.fixture
    def make_cancel(self, moderation, moderation_subscription, customer_subscription,
                    customer_subscription_set_enabled, client, tvm, service_merchant):
        async def _inner():
            service_merchant_id = service_merchant.service_merchant_id
            customer_subscription_id = customer_subscription.customer_subscription_id

            return await client.post(
                f'/v1/internal/customer_subscription/{service_merchant_id}/{customer_subscription_id}/cancel'
            )

        return _inner

    @pytest.mark.asyncio
    async def test_response_data(self, make_cancel):
        customer_subscription_response = await make_cancel()
        assert (await customer_subscription_response.json())['data']['customer_subscription']['enabled'] is False

    @pytest.mark.asyncio
    async def test_twice_cancel(self, make_cancel):
        await make_cancel()
        customer_subscription_response = await make_cancel()
        assert customer_subscription_response.status == 400

    @pytest.mark.parametrize('enabled', (False,))
    @pytest.mark.asyncio
    async def test_cancel_not_enabled(self, make_cancel):
        customer_subscription_response = await make_cancel()
        assert customer_subscription_response.status == 400
