import pytest

from hamcrest import assert_that, contains, has_entries

from mail.payments.payments.core.entities.enums import OrderSource, TrustEnv
from mail.payments.payments.utils.helpers import without_none


class TestOrderStartHandler:
    @pytest.fixture
    def purchase_token(self, rands):
        return rands()

    @pytest.fixture
    def payment_systems_options(self, mocker):
        return mocker.Mock(google_pay_enabled=True, apple_pay_enabled=True)

    @pytest.fixture
    def customer_uid(self, randn):
        return randn()

    @pytest.fixture
    def email(self, randmail):
        return f' {randmail()} '

    @pytest.fixture
    def trust_env(self, randitem):
        return randitem(TrustEnv)

    @pytest.fixture
    def pay_token(self, rands, payments_settings):
        payments_settings.PAY_TOKEN_PREFIX = f'{rands()}:'
        return f'{payments_settings.PAY_TOKEN_PREFIX}{rands()}'

    @pytest.fixture
    def turboapp_id(self, rands):
        return rands()

    @pytest.fixture
    def tsid(self, rands):
        return rands()

    @pytest.fixture
    def psuid(self, rands):
        return rands()

    @pytest.fixture(autouse=True)
    def action(self, rands, mock_action, trust_env, purchase_token, merchant, order, payment_systems_options):
        from mail.payments.payments.core.actions.order.pay import StartOrderByPayTokenAction
        return mock_action(
            StartOrderByPayTokenAction,
            {
                'purchase_token': purchase_token,
                'payment_url': rands(),
                'acquirer': merchant.acquirer,
                'environment': trust_env,
                'payment_systems_options': payment_systems_options,
            }
        )

    @pytest.fixture
    def request_json(self, pay_token, customer_uid, email, turboapp_id, tsid, psuid):
        return without_none({
            'pay_token': pay_token,
            'email': email,
            'customer_uid': customer_uid,
            'turboapp_id': turboapp_id,
            'tsid': tsid,
            'psuid': psuid,
        })

    @pytest.fixture
    def response_func(self, payments_client, request_json):
        async def _inner():
            return await payments_client.post('/v1/sdk/order/start', json=request_json)

        return _inner

    @pytest.mark.asyncio
    async def test_response(self, merchant, trust_env, purchase_token, response, payment_systems_options):
        assert (
            response.status == 200
            and await response.json() == {
                'code': 200,
                'status': 'success',
                'data': {
                    'purchase_token': purchase_token,
                    'acquirer': merchant.acquirer.value,
                    'environment': trust_env.value,
                    'payment_systems_options': {
                        'apple_pay_enabled': payment_systems_options.apple_pay_enabled,
                        'google_pay_enabled': payment_systems_options.google_pay_enabled,
                    }
                },
            }
        )

    @pytest.mark.parametrize('pop_key', ['pay_token'])
    @pytest.mark.asyncio
    async def test_bad_request(self, request_json, response_func, pop_key):
        request_json.pop(pop_key)
        response = await response_func()
        assert_that(
            (response.status, await response.json()),
            contains(400, has_entries({'status': 'fail', 'code': 400}))
        )

    @pytest.mark.parametrize('key', ['pay_token'])
    @pytest.mark.asyncio
    async def test_invalid_data(self, rands, request_json, key, response_func):
        request_json[key] = rands()
        response = await response_func()
        assert_that(
            (response.status, await response.json()),
            contains(400, has_entries({'status': 'fail', 'code': 400}))
        )

    @pytest.mark.parametrize('customer_uid,turboapp_id,tsid,psuid', (
        pytest.param(None, None, None, None, id='no optional params'),
        pytest.param(1, None, None, None, id='customer_uid'),
        pytest.param(None, 'abc.com', None, None, id='turboapp_id'),
        pytest.param(None, None, 'turboapp_session_id', None, id='tsid'),
        pytest.param(None, None, None, 'publisher_specific_user_identifier', id='psuid'),
    ))
    def test_context(self, action, pay_token, email, customer_uid, turboapp_id, tsid, psuid, response):
        action.assert_called_once_with(**without_none(dict(
            pay_token=pay_token,
            email=email.strip(),
            customer_uid=customer_uid,
            pay_by_source=OrderSource.SDK_API,
            turboapp_id=turboapp_id,
            tsid=tsid,
            psuid=psuid,
        )))
