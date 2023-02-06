import pytest


class BaseKassaTest:
    @pytest.fixture(autouse=True)
    def spy_get(self, kassa_client, mocker):
        mocker.spy(kassa_client, 'get')
        return kassa_client

    @pytest.fixture(autouse=True)
    def response_json(self, randn):
        return {
            'test': True,
            'account_id': randn(),
        }

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()


class TestMe(BaseKassaTest):
    @pytest.fixture
    def access_token(self, rands):
        return rands()

    @pytest.fixture
    def returned_func(self, kassa_client, access_token):
        async def _inner():
            return await kassa_client.me(access_token)

        return _inner

    @pytest.mark.usefixtures('returned')
    def test_call_get(self, payments_settings, kassa_client, access_token):
        kassa_client.get.assert_called_with(
            interaction_method='me',
            url=f'{payments_settings.KASSA_URL.rstrip("/")}/api/v3/me',
            headers={'Authorization': f'Bearer {access_token}'},
        )

    @pytest.mark.asyncio
    async def test_returned(self, returned, response_json):
        assert returned == response_json
