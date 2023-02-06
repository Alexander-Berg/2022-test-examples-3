import pytest

from sendr_utils import json_value

from hamcrest import assert_that, has_entries

from mail.payments.payments.core.entities.enums import MerchantRole
from mail.payments.payments.tests.base import BaseAcquirerTest, BaseTestMerchantRoles


@pytest.mark.usefixtures('moderation')
class BaseTestSubscriptions(BaseTestMerchantRoles, BaseAcquirerTest):
    @pytest.fixture(autouse=True)
    def product_subscription_create(self, shop_type, trust_client_mocker):
        with trust_client_mocker(shop_type, 'product_subscription_create', {'status': 'success'}) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def get_region(self, geobase_client_mocker):
        with geobase_client_mocker('get_region', {"id": 225, "type": 3}) as mock:
            yield mock

    @pytest.fixture(params=(False, True))
    def fast_moderation(self, request):
        return request.param

    @pytest.fixture
    async def subscription_created_response(self, subscription_data, fast_moderation, client, merchant, tvm):
        subscription_data['fast_moderation'] = fast_moderation
        r = await client.post(
            f'/v1/subscription/{merchant.uid}',
            json=json_value(subscription_data),
        )
        assert r.status == 200
        return (await r.json())['data']

    @pytest.fixture
    async def subscription_created(self, storage, merchant, subscription_created_response):
        subscription_id = subscription_created_response['subscription_id']
        return await storage.subscription.get(uid=merchant.uid,
                                              subscription_id=subscription_id)


class TestCreateSubscription(BaseTestSubscriptions):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
        MerchantRole.OPERATOR,
    )

    def test_create_subscription_returned(self, subscription_created_response, merchant, fast_moderation):
        assert_that(subscription_created_response, has_entries({
            'uid': merchant.uid,
            'fast_moderation': fast_moderation,
        }))

    def test_create_subscription_created(self, subscription_created, merchant):
        subscription_created.uid == merchant.uid


class TestGetListSubscriptions(BaseTestSubscriptions):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
        MerchantRole.OPERATOR,
        MerchantRole.VIEWER,
    )

    @pytest.fixture
    async def subscription_response(self, client, subscription, merchant, tvm):
        r = await client.get(f'/v1/subscription/{merchant.uid}')
        assert r.status == 200
        return await r.json()

    def test_response_data(self, subscription_response, subscription):
        assert subscription_response['data'][0]['subscription_id'] == subscription.subscription_id


class TestGetSubscription(BaseTestSubscriptions):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
        MerchantRole.OPERATOR,
        MerchantRole.VIEWER,
    )

    @pytest.fixture
    async def subscription_response(self, client, subscription, merchant, tvm):
        r = await client.get(f'/v1/subscription/{merchant.uid}/{subscription.subscription_id}')
        assert r.status == 200
        return await r.json()

    def test_response_data(self, subscription_response, subscription):
        assert subscription_response['data']['subscription_id'] == subscription.subscription_id


class TestDeleteSubscription(BaseTestSubscriptions):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
        MerchantRole.OPERATOR,
    )

    @pytest.fixture
    def subscription_response_func(self, client, subscription, tvm):
        async def _inner():
            return await client.delete(f'/v1/subscription/{subscription.uid}/{subscription.subscription_id}')

        return _inner

    @pytest.fixture
    async def subscription_response(self, subscription_response_func):
        return await subscription_response_func()

    @pytest.mark.asyncio
    async def test_response_data(self, subscription_response, subscription):
        data = (await subscription_response.json())['data']
        assert all((
            subscription_response.status == 200,
            data['subscription_id'] == subscription.subscription_id,
            data['deleted'] is True
        ))

    @pytest.mark.asyncio
    async def test_already_deleted(self, subscription_response, subscription_response_func):
        r = await subscription_response_func()
        assert r.status == 404
