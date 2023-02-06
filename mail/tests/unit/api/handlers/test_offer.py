import pytest

from hamcrest import assert_that, has_entries


class TestOfferGet:
    @pytest.fixture
    def headers(self):
        return {'some': 'value'}

    @pytest.fixture
    def data(self):
        return b'test-offer-get-data'

    @pytest.fixture(autouse=True)
    def action(self, mock_action, headers, data):
        from mail.payments.payments.core.actions.offer import GetOfferAction
        return mock_action(GetOfferAction, (headers, data))

    @pytest.fixture
    async def response(self, payments_client, merchant):
        return await payments_client.get(f'/v1/offer/{merchant.uid}')

    def test_params(self, merchant, response, action):
        action.assert_called_once_with(uid=merchant.uid)

    def test_response_headers(self, headers, response):
        assert_that(response.headers, has_entries(headers))

    @pytest.mark.asyncio
    async def test_response_body(self, data, response):
        assert await response.read() == data
