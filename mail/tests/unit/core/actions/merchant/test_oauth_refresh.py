import pytest

from mail.payments.payments.core.actions.merchant.oauth_refresh import OAuthRefreshMerchantAction
from mail.payments.payments.core.entities.merchant_oauth import OAuthToken
from mail.payments.payments.core.exceptions import OAuthCodeError
from mail.payments.payments.interactions.exceptions import OAuthClientError


class TestOAuthRefreshMerchantAction:
    @pytest.fixture
    def refresh_token(self, merchant_oauth):
        return merchant_oauth.decrypted_refresh_token

    @pytest.fixture
    def refresh_token_exc(self):
        return None

    @pytest.fixture
    def oauth_token(self, rands, randn):
        return OAuthToken(token_type='bearer', access_token=rands(), refresh_token=rands(), expires_in=randn())

    @pytest.fixture(autouse=True)
    def oauth_refresh_token_mock(self, oauth_client_mocker, refresh_token_exc, oauth_token):
        with oauth_client_mocker('refresh_token', result=oauth_token, exc=refresh_token_exc) as mock:
            yield mock

    @pytest.fixture
    def params(self, merchant_oauth):
        return {'merchant_oauth': merchant_oauth}

    @pytest.fixture
    def action(self, params):
        return OAuthRefreshMerchantAction(**params)

    @pytest.fixture
    async def returned(self, action):
        return await action.run()

    @pytest.mark.asyncio
    async def test_returned_token(self, returned, storage, merchant_oauth):
        from_db = await storage.merchant_oauth.get_by_shop_id(merchant_oauth.uid, merchant_oauth.shop_id)
        assert from_db == returned

    @pytest.mark.asyncio
    async def test_update_token(self, returned, storage, merchant_oauth):
        assert merchant_oauth != returned

    def test_refresh_token(self, refresh_token, oauth_refresh_token_mock, returned):
        oauth_refresh_token_mock.assert_called_once_with(refresh_token)

    @pytest.mark.parametrize('refresh_token_exc', (OAuthClientError(method=None),))
    @pytest.mark.asyncio
    async def test_refresh_token_error(self, action):
        with pytest.raises(OAuthCodeError):
            await action.run()
