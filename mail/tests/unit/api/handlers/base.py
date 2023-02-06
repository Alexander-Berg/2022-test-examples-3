from datetime import timezone
from decimal import Decimal
from random import choice

import pytest

from sendr_utils import without_none

from hamcrest import assert_that, has_entries

from mail.payments.payments.core.entities.enums import NDS, MerchantOAuthMode, PeriodUnit


class BaseTestSubscriptionGet:
    @pytest.fixture
    def action_cls(self):
        raise NotImplementedError

    @pytest.fixture
    def request_path(self):
        raise NotImplementedError

    @pytest.fixture(autouse=True)
    def action(self, action_cls, mock_action, subscription):
        return mock_action(action_cls, [subscription])

    @pytest.fixture
    def offset(self):
        return None

    @pytest.fixture
    def limit(self):
        return None

    @pytest.fixture
    def params(self, limit, offset):
        return without_none({'limit': limit, 'offset': offset})

    @pytest.fixture
    async def response(self, payments_client, params, request_path):
        return await payments_client.get(request_path, params=params)

    @pytest.fixture
    def extra_call_args(self):
        return {}

    class TestSuccessRequest:
        @pytest.mark.parametrize('limit', (1, 10, None))
        @pytest.mark.parametrize('offset', (0, 10, None))
        def test_params(self, limit, offset, response, action, extra_call_args):
            action.assert_called_once_with(
                offset=0 if offset is None else offset,
                limit=100 if limit is None else limit,
                **extra_call_args
            )


class BaseTestSubscriptionPost:
    @pytest.fixture
    def action_cls(self):
        raise NotImplementedError

    @pytest.fixture
    def request_path(self):
        raise NotImplementedError

    @pytest.fixture(autouse=True)
    def action(self, action_cls, subscription, mock_action):
        return mock_action(action_cls)

    @pytest.fixture
    def nds(self):
        return choice(list(NDS))

    @pytest.fixture
    def period_unit(self):
        return choice(list(PeriodUnit))

    @pytest.fixture
    def currency(self):
        return 'RUB'

    @pytest.fixture
    def prices(self, randn, randdecimal, currency):
        return [
            {
                "price": str(randdecimal()),
                "currency": currency,
                "region_id": randn(max=225),
            }
        ]

    @pytest.fixture
    def fast_moderation(self, randbool):
        return randbool()

    @pytest.fixture
    def request_json(self, randitem, nds, prices, rands, period_unit, randn, fast_moderation):
        return {
            "title": rands(),
            "fiscal_title": rands(),
            "nds": nds.value,
            "period_amount": randn(max=120),
            "period_units": period_unit.value,
            "trial_period_amount": randn(max=120),
            "trial_period_units": period_unit.value,
            "prices": prices,
            "mode": randitem(MerchantOAuthMode).value,
            "fast_moderation": fast_moderation,
        }

    @pytest.fixture
    async def response(self, payments_client, request_path, request_json):
        return await payments_client.post(request_path, json=request_json)

    @pytest.fixture
    async def response_json(self, response):
        return await response.json()

    @pytest.fixture
    def extra_call_args(self):
        return {}

    class TestSuccessRequest:
        def test_200(self, response):
            assert response.status == 200

        def test_params(self, request_json, period_unit, nds, currency, response,
                        fast_moderation, action, extra_call_args):
            action.assert_called_once_with(
                title=request_json['title'],
                fiscal_title=request_json['fiscal_title'],
                period_amount=request_json['period_amount'],
                period_units=period_unit,
                prices=[{
                    'price': Decimal(request_json['prices'][0]['price']),
                    'currency': currency,
                    'region_id': request_json['prices'][0]['region_id']
                }],
                nds=nds,
                trial_period_amount=request_json['trial_period_amount'],
                trial_period_units=period_unit,
                merchant_oauth_mode=MerchantOAuthMode(request_json['mode']),
                fast_moderation=fast_moderation,
                **extra_call_args
            )

    class TestBadRequest:
        @pytest.mark.parametrize('nds', (PeriodUnit.DAY,))
        def test_nds_400(self, response):
            assert response.status == 400

        @pytest.mark.parametrize('period_unit', (NDS.NDS_10,))
        def test_period_unit_400(self, response):
            assert response.status == 400

        @pytest.mark.parametrize('currency', (PeriodUnit.DAY.value,))
        def test_currency_400(self, response):
            assert response.status == 400

        @pytest.mark.parametrize('prices', (None, []))
        def test_prices_400(self, response):
            assert response.status == 400

    class TestUniqueRegionId:
        @pytest.fixture
        def prices(self, randn, randdecimal, currency):
            region_id = randn(max=225)
            return [
                {
                    "price": str(randdecimal()),
                    "currency": currency,
                    "region_id": region_id,
                } for _ in range(2)
            ]

        def test_unique_region_id__400(self, response, response_json):
            assert all((
                response.status == 400,
                response_json['data']['params']['_schema'] == ["region_id in prices must be unique"]
            ))

    class TestInvalidTrialOptions:
        @pytest.fixture(params=('trial_period_amount', 'trial_period_units'), autouse=True)
        def setup(self, request, request_json):
            request_json.pop(request.param)

        def test_invalid_trial_options__400(self, response, response_json):
            msg = 'trial_period_amount and trial_period_units must be not empty or empty simultaneously'

            assert all((
                response.status == 400,
                response_json['data']['params']['_schema'] == [msg]
            ))


class BaseTestSubscriptionIdGet:
    @pytest.fixture
    def action_cls_get(self):
        raise NotImplementedError

    @pytest.fixture
    def action_cls_delete(self):
        raise NotImplementedError

    @pytest.fixture
    def request_path(self):
        raise NotImplementedError

    @pytest.fixture
    def extra_call_args(self):
        return {}

    @pytest.fixture(autouse=True)
    def action_get(self, action_cls_get, subscription, mock_action):
        return mock_action(action_cls_get, subscription)

    @pytest.fixture(autouse=True)
    def action_delete(self, action_cls_delete, subscription, mock_action):
        return mock_action(action_cls_delete, subscription)

    @pytest.fixture
    async def response_get(self, payments_client, request_path):
        return await payments_client.get(request_path)

    @pytest.fixture
    async def response_delete(self, payments_client, request_path):
        return await payments_client.delete(request_path)

    @pytest.fixture
    def expected_response_data(self, subscription):
        return {
            'created': subscription.created.astimezone(timezone.utc).isoformat(),
            'enabled_customer_subscriptions': subscription.enabled_customer_subscriptions,
            'deleted': subscription.deleted,
            'fiscal_title': subscription.fiscal_title,
            'mode': 'prod',
            'moderation': None,
            'nds': str(subscription.nds.value),
            'period_amount': subscription.period_amount,
            'period_units': str(subscription.period_units.value),
            'prices': [
                {
                    'currency': price.currency,
                    'region_id': price.region_id,
                    'price': float(price.price),
                }
                for price in subscription.prices
            ],
            'subscription_id': subscription.subscription_id,
            'title': subscription.title,
            'trial_period_amount': subscription.trial_period_amount,
            'trial_period_units': str(subscription.trial_period_units.value),
            'uid': subscription.uid,
            'updated': subscription.updated.astimezone(timezone.utc).isoformat(),
            'fast_moderation': subscription.fast_moderation,
        }

    class TestSuccessGet:
        def test_success_get__200(self, response_get):
            assert response_get.status == 200

        def test_success_get__params(self, response_get, action_get, subscription, extra_call_args):
            action_get.assert_called_once_with(
                subscription_id=subscription.subscription_id,
                **extra_call_args
            )

        @pytest.mark.asyncio
        async def test_success_get__response(self, expected_response_data, response_get):
            data = (await response_get.json())['data']
            assert expected_response_data == data

    class TestSuccessDelete:
        def test_success_delete__200(self, response_delete):
            assert response_delete.status == 200

        def test_success_delete__params(self, response_delete, action_delete, subscription, extra_call_args):
            action_delete.assert_called_once_with(
                subscription_id=subscription.subscription_id,
                **extra_call_args
            )

        @pytest.mark.asyncio
        async def test_success_delete__response(self, expected_response_data, response_delete):
            data = (await response_delete.json())['data']
            assert expected_response_data == data


class BaseCustomerSubscriptionHandlerPostTest:
    @pytest.fixture
    def action_cls(self):
        raise NotImplementedError

    @pytest.fixture
    def request_path(self):
        raise NotImplementedError

    @pytest.fixture
    def extra_call_args(self):
        return {}

    @pytest.fixture
    def action(self, action_cls, mock_action):
        return mock_action(action_cls, (None, None))

    @pytest.fixture
    def request_json(self, randn):
        return {
            'subscription_id': randn(),
            'customer_uid': randn(),
            'user_ip': '127.0.0.1',
            'region_id': 225,
            'quantity': 1,
            'paymethod_id': 'trust_web_page',
        }

    @pytest.fixture
    async def response(self, action, payments_client, request_path, request_json):
        return await payments_client.post(request_path, json=request_json)

    class TestSuccessRequest:
        def test_customer_subscription_handler_200(self, response):
            assert response.status == 200

        def test_customer_subscription_handler_context(self, request_json, response, action, extra_call_args):
            assert_that(
                action.call_args[1],
                has_entries({
                    'subscription_id': request_json['subscription_id'],
                    'quantity': request_json['quantity'],
                    'customer_uid': request_json['customer_uid'],
                    'user_ip': request_json['user_ip'],
                    'region_id': request_json['region_id'],
                    **extra_call_args,
                })
            )


class BaseCustomerSubscriptionCancelHandlerPostTest:
    @pytest.fixture
    def action_cls(self):
        raise NotImplementedError

    @pytest.fixture
    def request_path(self):
        raise NotImplementedError

    @pytest.fixture
    def extra_call_args(self):
        return {}

    @pytest.fixture
    def action(self, action_cls, mock_action):
        return mock_action(action_cls, (None, None))

    @pytest.fixture
    async def response(self, action, payments_client, request_path):
        return await payments_client.post(request_path)

    class TestSuccessRequest:
        def test_cancel_customer_subscription_200(self, response):
            assert response.status == 200

        def test_cancel_customer_subscription_params(self, customer_subscription, extra_call_args, response, action):
            assert_that(
                action.call_args[1],
                has_entries({
                    'customer_subscription_id': customer_subscription.customer_subscription_id,
                })
            )
