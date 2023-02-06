from datetime import timedelta
from urllib.parse import urlencode

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, contains, equal_to, has_entries

from mail.payments.payments.core.entities.enums import AcquirerType, MerchantOAuthMode, ShopType
from mail.payments.payments.core.entities.merchant_oauth import MerchantOAuth, OAuthToken
from mail.payments.payments.core.entities.shop import Shop
from mail.payments.payments.interactions.exceptions import OAuthClientError
from mail.payments.payments.utils.helpers import without_none


class TestMerchantOAuthStart:
    @pytest.fixture(autouse=True)
    def setup(self, mocker):
        mocker.patch(
            'mail.payments.payments.core.actions.merchant.oauth_start.uuid_hex', mocker.Mock(return_value='mocked')
        )

    @pytest.fixture
    def state(self, rands):
        return rands()

    @pytest.fixture
    def response_func(self, merchant, client, tvm, state):
        async def _inner():
            return await client.get(f'/v1/merchant/{merchant.uid}/oauth/start', params={'state': state})

        return _inner

    @pytest.fixture
    async def response(self, response_func):
        return await response_func()

    @pytest.fixture
    async def response_data(self, response):
        return await response.json()

    @pytest.mark.asyncio
    async def test_status(self, payments_settings, merchant, response, rands, state):
        params = {
            'response_type': 'code',
            'client_id': payments_settings.OAUTH_APP_ID,
            'device_id': 'payments-mocked',
            'state': state
        }
        url = f"{payments_settings.OAUTH_URL.rstrip('/')}/authorize?" + urlencode(without_none(params))

        assert_that(
            (response.status, await response.json()),
            contains(
                equal_to(200),
                has_entries({
                    'code': 200,
                    'data': {'url': url},
                    'status': 'success'
                })
            )
        )


@pytest.mark.parametrize('merchant_oauth_mode', list(MerchantOAuthMode))
class TestMerchantOAuth:
    @pytest.fixture
    def acquirer(self):
        return AcquirerType.KASSA

    @pytest.fixture
    def response_func(self, merchant, client, tvm, merchant_oauth_mode):
        async def _inner():
            return await client.delete(
                f'/v1/merchant/{merchant.uid}/oauth',
                json={
                    'mode': merchant_oauth_mode.value
                }
            )

        return _inner

    @pytest.fixture
    async def response(self, response_func):
        return await response_func()

    @pytest.fixture
    async def response_data(self, response):
        return await response.json()

    def test_status(self, response):
        assert response.status == 200


class TestMerchantOAuthComplete:
    @pytest.fixture
    def get_token_exc(self):
        return None

    @pytest.fixture
    def me_exc(self):
        return None

    @pytest.fixture
    def merchant_oauth_mode(self):
        return MerchantOAuthMode.TEST

    @pytest.fixture
    def code(self, rands):
        return rands()

    @pytest.fixture
    def now(self, mocker):
        now = utcnow()
        mocker.patch(
            'mail.payments.payments.core.actions.merchant.oauth_complete.utcnow',
            mocker.Mock(return_value=now)
        )
        return now

    @pytest.fixture
    def oauth_token(self, rands, randn):
        return OAuthToken(token_type=rands(), access_token=rands(), refresh_token=rands(), expires_in=randn())

    @pytest.fixture(autouse=True)
    def oauth_mock(self, oauth_client_mocker, merchant_oauth_mode, oauth_token, get_token_exc):
        with oauth_client_mocker('get_token', oauth_token, exc=get_token_exc) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def kassa_mock(self, kassa_client_mocker, oauth_token, me_exc, merchant_oauth_mode, rands):
        response = {'test': merchant_oauth_mode == MerchantOAuthMode.TEST, rands(): rands()}
        with kassa_client_mocker('me', response, exc=me_exc) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    async def shop(self, merchant, storage, merchant_oauth_mode):
        return await storage.shop.create(
            Shop(
                uid=merchant.uid,
                shop_type=ShopType.from_oauth_mode(merchant_oauth_mode),
                is_default=False,
                name='Новый'
            ),
        )

    @pytest.fixture
    def shop_id(self, shop):
        return shop.shop_id

    @pytest.fixture
    def response_func(self, merchant, client, tvm, shop_id, code):
        async def _inner():
            return await client.post(
                f'/v1/merchant/{merchant.uid}/oauth/complete',
                json={
                    'code': code,
                    'shop_id': shop_id
                }
            )

        return _inner

    @pytest.fixture
    async def response(self, response_func):
        return await response_func()

    @pytest.fixture
    async def response_data(self, response):
        return await response.json()

    def test_status(self, response):
        assert response.status == 200

    @pytest.mark.asyncio
    async def test_token_creation(self,
                                  response_func,
                                  merchant_oauth_mode,
                                  oauth_token,
                                  now,
                                  merchant,
                                  storage,
                                  shop,
                                  default_merchant_shops,
                                  ):
        # Проверяем, что нет токена
        with pytest.raises(MerchantOAuth.DoesNotExist):
            await storage.merchant_oauth.get_by_shop_id(uid=shop.uid, shop_id=shop.shop_id)

        # Дергаем ручку
        await response_func()

        # Получаем токен и дефолтный шоп
        merchant_oauth = await storage.merchant_oauth.get_by_shop_id(uid=shop.uid, shop_id=shop.shop_id)
        default_shop = default_merchant_shops[ShopType.from_oauth_mode(merchant_oauth_mode)]

        # Проверяем, что токен привязался к заданному магазину, а не дефолтному, и параметры токена
        assert all((
            merchant_oauth.shop_id != default_shop.shop_id,
            oauth_token.access_token == merchant_oauth.decrypted_access_token,
            oauth_token.refresh_token == merchant_oauth.decrypted_refresh_token,
            now + timedelta(seconds=oauth_token.expires_in) == merchant_oauth.expires,
        ))

    @pytest.mark.parametrize('get_token_exc', (OAuthClientError(method=None),))
    def test_error(self, response):
        assert response.status == 400
