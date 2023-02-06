import pytest

from sendr_utils import json_value

from mail.payments.payments.core.entities.enums import MerchantRole
from mail.payments.payments.tests.base import BaseTestMerchantRoles


@pytest.mark.usefixtures('moderation')
class BaseCustomerSubscriptionTest(BaseTestMerchantRoles):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
        MerchantRole.OPERATOR,
    )

    @pytest.fixture(autouse=True)
    def mock_geobase_get_parents(self, geobase_client_mocker, customer_subscription_data):
        with geobase_client_mocker('get_parents', result=[customer_subscription_data['region_id']]) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def mock_subscription_create(self, shop_type, trust_client_mocker):
        with trust_client_mocker(shop_type, 'subscription_create', []) as mock:
            yield mock

    @pytest.fixture
    async def create_customer_subscription_response(self,
                                                    moderation,
                                                    moderation_subscription,
                                                    subscription,
                                                    tvm,
                                                    client,
                                                    merchant,
                                                    customer_subscription_data):
        return await client.post(
            f'/v1/customer_subscription/{merchant.uid}',
            json=json_value({
                **customer_subscription_data,
                'subscription_id': subscription.subscription_id,
            }),
        )

    @pytest.fixture
    async def returned_customer_subscription(self, create_customer_subscription_response):
        json = await create_customer_subscription_response.json()
        return json['data']['customer_subscription']


class TestCreateCustomerSubscription(BaseCustomerSubscriptionTest):
    def test_response_200(self, create_customer_subscription_response):
        assert create_customer_subscription_response.status == 200

    @pytest.mark.asyncio
    async def test_response_stored(self, storage, merchant, subscription, returned_customer_subscription):
        stored_customer_subscription = await storage.customer_subscription.get(
            uid=merchant.uid,
            customer_subscription_id=returned_customer_subscription['customer_subscription_id']
        )
        assert stored_customer_subscription.subscription_id == subscription.subscription_id


class TestCancelCustomerSubscription(BaseCustomerSubscriptionTest):
    @pytest.fixture(autouse=True)
    def mock_subscription_cancel(self, shop_type, trust_client_mocker):
        with trust_client_mocker(shop_type, 'subscription_cancel', {}) as mock:
            yield mock

    @pytest.fixture
    def enabled(self):
        return True

    @pytest.fixture()
    async def customer_subscription_set_enabled(self, storage, merchant, returned_customer_subscription, enabled):
        customer_subscription = await storage.customer_subscription.get(
            uid=merchant.uid,
            customer_subscription_id=returned_customer_subscription['customer_subscription_id']
        )
        customer_subscription.enabled = enabled
        await storage.customer_subscription.save(customer_subscription)

    @pytest.fixture
    def make_request(self,
                     moderation,
                     moderation_subscription,
                     customer_subscription,
                     customer_subscription_set_enabled,
                     returned_customer_subscription,
                     tvm,
                     client,
                     merchant):
        async def _inner():
            customer_subscription_id = returned_customer_subscription['customer_subscription_id']
            return await client.post(f'/v1/customer_subscription/{merchant.uid}/{customer_subscription_id}/cancel')
        return _inner

    @pytest.fixture
    async def response(self, make_request):
        return await make_request()

    def test_cancel_customer_subscription_status(self, response):
        assert response.status == 200

    @pytest.mark.asyncio
    async def test_cancel_customer_subscription_response(self, response):
        json = await response.json()
        assert json['data']['customer_subscription']['enabled'] is False

    @pytest.mark.asyncio
    async def test_cancel_customer_subscription_twice(self, make_request):
        await make_request()  # first call is success
        second = await make_request()
        assert second.status == 400

    @pytest.mark.parametrize('enabled', (False,))
    def test_cancel_customer_subscription_not_enabled(self, response):
        assert response.status == 400
