import pytest

from hamcrest import assert_that, equal_to


@pytest.fixture
def clients(client, admin_client):
    return {'api': client, 'admin': admin_client}


@pytest.fixture
def swagger_api_client(clients, app_type):
    return clients[app_type]


@pytest.fixture(autouse=True)
def swagger_turn_on(payments_settings):
    payments_settings.SWAGGER_ENABLED = True


@pytest.mark.parametrize('app_type, url', (
    ('api', '/api/swagger.json'),
    ('admin', '/admin/api/swagger.json'),
))
@pytest.mark.asyncio
async def test_swagger(swagger_api_client, url):
    response = await swagger_api_client.get(url)
    assert_that(response.status, equal_to(200))
