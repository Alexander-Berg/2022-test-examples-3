import pytest

from sendr_utils import json_value

from mail.payments.payments.tests.base import BaseAcquirerTest


@pytest.mark.usefixtures('moderation')
class BaseTestSubscriptions(BaseAcquirerTest):
    @pytest.fixture(autouse=True)
    def product_subscription_create(self, shop_type, trust_client_mocker):
        with trust_client_mocker(shop_type, 'product_subscription_create', {'status': 'success'}) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def get_region(self, geobase_client_mocker):
        with geobase_client_mocker('get_region', {"id": 225, "type": 3}) as mock:
            yield mock

    @pytest.fixture
    def subscription_func(self, client, service_merchant, subscription_data, tvm):
        async def _inner():
            return await client.post(
                f'/v1/internal/subscription/{service_merchant.service_merchant_id}',
                json=json_value(subscription_data),
            )

        return _inner

    @pytest.fixture
    async def subscription(self, subscription_func, service_merchant, storage):
        r = await subscription_func()
        assert r.status == 200
        subscription_id = (await r.json())['data']['subscription_id']
        return await storage.subscription.get(uid=service_merchant.uid,
                                              subscription_id=subscription_id,
                                              service_merchant_id=service_merchant.service_merchant_id)


class TestGetListSubscriptions(BaseTestSubscriptions):
    @pytest.fixture
    async def subscription_response(self, client, subscription, service_merchant, tvm):
        r = await client.get(f'/v1/internal/subscription/{service_merchant.service_merchant_id}')
        assert r.status == 200
        return await r.json()

    def test_response_data(self, subscription_response, subscription):
        assert subscription_response['data'][0]['subscription_id'] == subscription.subscription_id


class TestMinPeriod(BaseTestSubscriptions):
    @pytest.fixture
    def period_amount(self, payments_settings):
        return payments_settings.CUSTOMER_SUBSCRIPTION_MIN_PERIOD - 1

    @pytest.mark.asyncio
    async def test_min_period(self, period_amount, subscription_func):
        msg = f'period should be more then {period_amount + 1} seconds'

        r = await subscription_func()
        response_json = await r.json()
        assert all((
            response_json['data']['params']['period_amount'] == [msg],
            r.status == 400,
        ))


class TestDeleteSubscription:
    @pytest.fixture
    def subscription_response_func(self, client, service_merchant, subscription, tvm):
        async def _inner():
            return await client.delete(
                f'/v1/internal/subscription/{service_merchant.service_merchant_id}/{subscription.subscription_id}'
            )

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
