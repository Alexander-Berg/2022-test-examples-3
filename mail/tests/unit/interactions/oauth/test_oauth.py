import pytest

from hamcrest import assert_that, has_entries

from mail.payments.payments.core.entities.merchant_oauth import OAuthToken
from mail.payments.payments.interactions.exceptions import OAuthClientError


class BaseOAuthTest:
    @pytest.fixture(autouse=True)
    def spy_post(self, oauth_client, mocker):
        mocker.spy(oauth_client, 'post')
        return oauth_client

    @pytest.fixture(autouse=True)
    def response_json(self, randn, rands):
        return {
            'token_type': 'bearer',
            'access_token': rands(),
            'refresh_token': rands(),
            'expires_in': randn()
        }

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    class TestError:
        @pytest.fixture(autouse=True)
        def response_json(self, randn, rands):
            return {
                'error_description': 'expired_token',
                'error': 'invalid'
            }

        @pytest.mark.asyncio
        async def test_error(self, returned_func, response_json):
            with pytest.raises(OAuthClientError) as exc_info:
                await returned_func()
            assert_that(exc_info.value.params, has_entries(response_json))


class TestGetToken(BaseOAuthTest):
    @pytest.fixture
    def code(self, rands):
        return rands()

    @pytest.fixture
    def returned_func(self, oauth_client, code):
        async def _inner():
            return await oauth_client.get_token(code)

        return _inner

    @pytest.mark.usefixtures('returned')
    def test_call_post(self, payments_settings, oauth_client, code):
        oauth_client.post.assert_called_with(
            interaction_method='_get_token',
            url=f'{payments_settings.OAUTH_URL.rstrip("/")}/token',
            data={"grant_type": "authorization_code", "code": code},
        )

    @pytest.mark.asyncio
    async def test_returned(self, returned, response_json):
        assert returned == OAuthToken(**response_json)


class TestRefreshToken(BaseOAuthTest):
    @pytest.fixture
    def refresh_token(self, rands):
        return rands()

    @pytest.fixture
    def returned_func(self, oauth_client, refresh_token):
        async def _inner():
            return await oauth_client.refresh_token(refresh_token)

        return _inner

    @pytest.mark.usefixtures('returned')
    def test_call_post(self, payments_settings, oauth_client, refresh_token):
        oauth_client.post.assert_called_with(
            interaction_method='_get_token',
            url=f'{payments_settings.OAUTH_URL.rstrip("/")}/token',
            data={"grant_type": "refresh_token", "refresh_token": refresh_token},
        )

    @pytest.mark.asyncio
    async def test_returned(self, returned, response_json):
        assert returned == OAuthToken(**response_json)
