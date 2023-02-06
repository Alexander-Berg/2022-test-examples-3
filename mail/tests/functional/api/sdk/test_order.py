import pytest

from mail.payments.payments.core.actions.order.get_trust_env import GetOrderTrustEnvAction
from mail.payments.payments.core.entities.enums import AcquirerType, MerchantOAuthMode, TrustEnv
from mail.payments.payments.tests.base import BaseSdkOrderTest


@pytest.mark.parametrize(['preregister', 'trust_env'], (
    pytest.param(False, TrustEnv.PROD, id='full registration, prod'),
    pytest.param(False, TrustEnv.SANDBOX, id='full registration, sandbox'),
    pytest.param(True, TrustEnv.SANDBOX, id='preregistered, sandbox'),
))
class TestStart(BaseSdkOrderTest):
    @pytest.fixture
    async def merchant(self, preregister, create_merchant, create_merchant_oauth, create_preregistered_merchant):
        if preregister:
            return await create_preregistered_merchant()
        else:
            m = await create_merchant()
            m.load_data()
            m.load_parent()
            if m.acquirer == AcquirerType.KASSA:
                await create_merchant_oauth(m.merchant_id, mode=MerchantOAuthMode.PROD)
            return m

    @pytest.fixture(autouse=True)
    def setup(self, moderation, balance_person_mock):
        pass

    @pytest.fixture(autouse=True)
    async def merchant_options(self, merchant, storage):
        merchant.options.payment_systems.apple_pay_enabled = True
        merchant.options.payment_systems.google_pay_enabled = False
        await storage.merchant.save(merchant)
        return merchant.options

    @pytest.fixture
    def email(self, randmail):
        return randmail()

    @pytest.fixture
    def purchase_token(self, rands):
        return rands()

    @pytest.fixture
    def customer_uid(self, randn):
        return randn()

    @pytest.fixture(autouse=True)
    def get_order_trust_env_action_mock(self, mock_action, trust_env):
        return mock_action(GetOrderTrustEnvAction, trust_env)

    @pytest.fixture(autouse=True)
    def orders_create(self, shop_type, trust_client_mocker):
        with trust_client_mocker(shop_type, 'orders_create', []) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def payment_create(self, shop_type, trust_client_mocker, payment_create_result):
        with trust_client_mocker(shop_type, 'payment_create', payment_create_result) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def payment_start(self, trust_client_mocker, shop_type, rands, purchase_token):
        with trust_client_mocker(shop_type, 'payment_start',
                                 {'purchase_token': purchase_token, 'payment_url': rands()}) as mock:
            yield mock

    @pytest.fixture
    def response_func(self, client, moderation, customer_uid, order_response, tvm):
        async def _inner(**kwargs):
            data = {
                'pay_token': order_response['data']['pay_token'],
                'customer_uid': customer_uid,
                **kwargs,
            }
            return await client.post('/v1/sdk/order/start', json=data)

        return _inner

    def test_response_status(self, response):
        assert response.status == 200

    @pytest.mark.asyncio
    async def test_set_customer_uid(self, storage, merchant, order_response, customer_uid, response):
        order = await storage.order.get(merchant.uid, order_response['data']['order_id'])
        assert order.customer_uid == customer_uid

    def test_response_data(self, merchant, stored_person_entity, acquirer, trust_env, merchant_options, purchase_token,
                           response_data, preregister):
        if preregister:
            merchant_response = {
                'name': merchant.name,
                'ogrn': None,
                'schedule_text': None,
                'legal_address': None,
            }
        else:
            merchant_response = {
                'name': merchant.name,
                'ogrn': stored_person_entity.ogrn,
                'schedule_text': merchant.organization.schedule_text,
                'legal_address': {
                    'city': stored_person_entity.legal_address_city,
                    'country': 'RUS',
                    'home': stored_person_entity.legal_address_home,
                    'street': stored_person_entity.legal_address_street,
                    'zip': stored_person_entity.legal_address_postcode,
                },
            }
        assert response_data == {
            'purchase_token': purchase_token,
            'acquirer': acquirer.value if not preregister else AcquirerType.TINKOFF.value,  # Forced for test orders
            'environment': trust_env.value,
            'payment_systems_options': {
                'apple_pay_enabled': merchant_options.payment_systems.apple_pay_enabled,
                'google_pay_enabled': merchant_options.payment_systems.google_pay_enabled,
            },
            'merchant': merchant_response,
        }

    @pytest.mark.asyncio
    @pytest.mark.parametrize('turboapp_id', (None, 'abc.com'))
    async def test_turboapp_service_id_not_set(self, payments_settings, response_func, turboapp_id):
        payments_settings.TURBOAPP_SERVICE_ID = None
        response = await response_func(turboapp_id=turboapp_id)
        assert response.status == 200

    @pytest.mark.asyncio
    @pytest.mark.parametrize('email', (None, 'test@test.ru'))
    async def test_email(self, payments_settings, response_func, email):
        payments_settings.TURBOAPP_SERVICE_ID = None
        response = await response_func(email=email)
        assert response.status == 200

    @pytest.mark.asyncio
    async def test_turboapp_id(self, payments_settings, storage, service_merchant, order, response_func):
        payments_settings.TURBOAPP_SERVICE_ID = service_merchant.service_id
        await response_func(turboapp_id=service_merchant.entity_id)
        updated_order = await storage.order.get(order.uid, order.order_id)
        assert (
            order.service_merchant_id is None
            and updated_order.service_merchant_id == service_merchant.service_merchant_id
        )
