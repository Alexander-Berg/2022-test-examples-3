import pytest

from sendr_interactions import exceptions as interaction_errors

from mail.payments.payments.core.actions.merchant.oauth_complete import OAuthCompleteMerchantAction
from mail.payments.payments.core.entities.enums import AcquirerType, MerchantOAuthMode, ShopType
from mail.payments.payments.core.entities.merchant_oauth import OAuthToken
from mail.payments.payments.core.exceptions import (
    ChildMerchantError, KassaMeError, MerchantNotFoundError, OAuthAlreadyExistsError, OAuthCodeError
)
from mail.payments.payments.interactions.exceptions import OAuthClientError
from mail.payments.payments.tests.base import BaseTestParent


class TestOAuthCompleteMerchantAction(BaseTestParent):
    @pytest.fixture
    def shop_id(self, default_merchant_shops, is_test, shop):
        return default_merchant_shops[ShopType.TEST if is_test else ShopType.PROD].shop_id

    @pytest.fixture
    def param_uid(self, merchant):
        return merchant.uid

    @pytest.fixture
    def oauth_token(self, rands, randn):
        return OAuthToken(token_type='bearer', access_token=rands(), refresh_token=rands(), expires_in=randn())

    @pytest.fixture
    def expected_oauth_mode(self, me_result):
        return MerchantOAuthMode.TEST if me_result['test'] else MerchantOAuthMode.PROD

    @pytest.fixture
    def code(self, rands):
        return rands()

    @pytest.fixture
    def get_token_exc(self):
        return None

    @pytest.fixture
    def me_exc(self):
        return None

    @pytest.fixture(params=(True, False))
    def is_test(self, request):
        return request.param

    @pytest.fixture
    def me_result(self, is_test, rands):
        return {'test': is_test, rands(): rands()}

    @pytest.fixture(autouse=True)
    def oauth_get_token_mock(self, oauth_client_mocker, get_token_exc, oauth_token):
        with oauth_client_mocker('get_token', result=oauth_token, exc=get_token_exc) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def kassa_me_mock(self, kassa_client_mocker, me_result, me_exc):
        with kassa_client_mocker('me', result=me_result, exc=me_exc) as mock:
            yield mock

    @pytest.fixture
    def params(self, param_uid, code, shop_id):
        return {'uid': param_uid, 'code': code, 'shop_id': shop_id}

    @pytest.fixture
    def with_parent(self):
        return False

    @pytest.fixture
    def action(self, params):
        return OAuthCompleteMerchantAction(**params)

    @pytest.fixture
    async def returned(self, action):
        return await action.run()

    @pytest.mark.asyncio
    async def test_create_token(self, returned, storage, param_uid, default_merchant_shops, expected_oauth_mode):
        shop = default_merchant_shops[ShopType.from_oauth_mode(expected_oauth_mode)]
        from_db = await storage.merchant_oauth.get_by_shop_id(uid=param_uid, shop_id=shop.shop_id)
        assert from_db == returned

    @pytest.mark.asyncio  # TODO: PAYBACK-670: выпилить этот тест
    async def test_mode(self, returned, expected_oauth_mode):
        assert returned.mode == expected_oauth_mode

    @pytest.mark.asyncio
    async def test_data(self, returned, me_result):
        assert returned.data == me_result

    def test_code(self, returned, oauth_get_token_mock, code):
        oauth_get_token_mock.assert_called_once_with(code)

    def test_kassa(self, returned, kassa_me_mock, oauth_token):
        kassa_me_mock.assert_called_once_with(oauth_token.access_token)

    @pytest.mark.parametrize('param_uid', (-1,))
    @pytest.mark.asyncio
    async def test_merchant_not_found(self, action):
        with pytest.raises(MerchantNotFoundError):
            await action.run()

    @pytest.mark.parametrize('me_exc', (interaction_errors.BaseInteractionError(service=None, method=None),))
    @pytest.mark.asyncio
    async def test_kassa_error(self, action):
        with pytest.raises(KassaMeError):
            await action.run()

    @pytest.mark.parametrize('get_token_exc', (OAuthClientError(method=None),))
    @pytest.mark.asyncio
    async def test_code_error(self, action):
        with pytest.raises(OAuthCodeError):
            await action.run()

    @pytest.mark.parametrize('acquirer', [AcquirerType.KASSA])
    @pytest.mark.asyncio
    async def test_already_exist(self, param_uid, is_test, create_merchant_oauth, action):
        await create_merchant_oauth(param_uid, mode=MerchantOAuthMode.TEST if is_test else MerchantOAuthMode.PROD)
        with pytest.raises(OAuthAlreadyExistsError):
            await action.run()

    class TestParent:
        @pytest.fixture
        def with_parent(self):
            return True

        @pytest.mark.asyncio
        async def test_parent(self, action):
            with pytest.raises(ChildMerchantError):
                await action.run()
