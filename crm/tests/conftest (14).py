import pytest
from aiohttp.test_utils import TestServer

from crm.agency_cabinet.common.blackbox import BlackboxClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.gateway_external.server.src import setup_app
from crm.agency_cabinet.gateway_external.server.src.config import GatewayExternalConfig


pytest_plugins = [
    'smb.common.rmq.rpc.pytest.plugin',
    'crm.agency_cabinet.agencies.client.pytest.plugin',
    'crm.agency_cabinet.common.server.common.pytest.plugin',
    'crm.agency_cabinet.common.service_discovery.pytest.plugin',

]


def pytest_configure(config):
    config.addinivalue_line(
        "markers", "no_patch_setup_s3_client: skip setup step for s3_client"
    )


def pytest_collection_modifyitems(items):
    for item in items:
        item.add_marker(pytest.mark.asyncio)


class GatewayExternalTestClient(BaseTestClient):
    def __init__(self, service_ticket, app_client_id, *args, **kwargs):
        super().__init__(*args, **kwargs)

        self.service_ticket = service_ticket
        self.app_client_id = app_client_id

    async def request(self, method, path, **kwargs):
        service_ticket = kwargs.pop('service_ticket', self.service_ticket)
        app_client_id = kwargs.pop('oauth_token', self.app_client_id)

        kwargs['auth_headers'] = {
            'X-Ya-Service-Ticket': service_ticket,
            'Authorization OAuth': app_client_id,
            'X-Forwarded-For-Y': 'test'
        }

        return await super().request(method, path, **kwargs)


@pytest.fixture
async def frontend_client_id():
    return 123


@pytest.fixture
async def app_client_id():
    return '42'


@pytest.fixture
async def client(event_loop,
                 service_discovery: ServiceDiscovery,
                 blackbox_client: BlackboxClient,
                 frontend_client_id: int,
                 app_client_id: str,
                 ):
    environ_dict = {
        'TVM2_ASYNC': '1',
        'TVM2_CLIENT_ID': '123',
        'AMQP_URL': 'localhost',
        'BLACKBOX_URL': 'http://blackbox.endpoint.url',
    }

    config = GatewayExternalConfig.from_environ(environ_dict)

    blackbox_client.get_app_client_id_by_oauth_token.return_value = '123123123'

    app = await setup_app(config, service_discovery, blackbox_client)
    server = TestServer(app, loop=event_loop)
    client = GatewayExternalTestClient('service_ticket', 'app_client_id', server, loop=event_loop)

    await client.start_server()
    yield client
    await client.close()
