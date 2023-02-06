import pytest
from aiohttp.test_utils import TestClient

from mail.payments.payments.interactions.developer.exceptions import DeveloperKeyAccessDeny


@pytest.fixture
async def sdk_client(aiohttp_client, sdk_app) -> TestClient:
    return await aiohttp_client(sdk_app)


@pytest.mark.parametrize(('apikey', 'expected_message'), (
    pytest.param('', 'DEVELOPER_KEY_ABSENT', id='empty_key'),
    pytest.param('forbidden_key', 'DEVELOPER_KEY_ACCESS_DENY', id='forbidden_key'),
))
class TestDeveloperKeyAuthentication:
    @pytest.fixture(autouse=True)
    def setup(self, developer_client_mocker):
        with developer_client_mocker('check_key', exc=DeveloperKeyAccessDeny) as mock:
            yield mock

    @pytest.fixture
    async def response(self, sdk_client, order, apikey):
        r = await sdk_client.get(f'/v1/order/{order.order_id}',
                                 headers={'Authorization': f'{apikey}'})

        return r

    @pytest.mark.asyncio
    async def test_key(self, response, expected_message):
        assert response.status == 403
        response_json = await response.json()
        assert response_json['data']['message'] == expected_message
