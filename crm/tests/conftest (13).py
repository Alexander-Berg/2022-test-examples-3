from collections import defaultdict

import pytest
from aiohttp.test_utils import TestServer
from tvm2.ticket import ServiceTicket, UserTicket

from crm.agency_cabinet.common.server.common.tvm import TvmClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.gateway.server.src import setup_app
from crm.agency_cabinet.gateway.server.src.config import GatewayConfig


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


class GatewayTestClient(BaseTestClient):
    def __init__(self, service_ticket, user_ticket, *args, **kwargs):
        super().__init__(*args, **kwargs)

        self.service_ticket = service_ticket
        self.user_ticket = user_ticket

    async def request(self, method, path, **kwargs):
        service_ticket = kwargs.pop('service_ticket', self.service_ticket)
        user_ticket = kwargs.pop('user_ticket', self.user_ticket)

        kwargs['auth_headers'] = {
            'X-Ya-Service-Ticket': service_ticket,
            'X-Ya-User-Ticket': user_ticket,
        }

        return await super().request(method, path, **kwargs)


@pytest.fixture
async def frontend_client_id():
    return 123


@pytest.fixture
async def yandex_uid():
    return 42


@pytest.fixture
async def client(event_loop, service_discovery: ServiceDiscovery, tvm_client: TvmClient, frontend_client_id: int, yandex_uid: int):
    environ_dict = {
        'TVM2_ASYNC': '1',
        'TVM2_CLIENT_ID': '123',
        'ALLOWED_CLIENTS': str(frontend_client_id),
        'AMQP_URL': 'localhost',
        'MDS_ENDPOINT_URL': 'http://mds.endpoint.url',
        'MDS_ACCESS_KEY_ID': 1,
        'MDS_SECRET_ACCESS_KEY': 1
    }

    config = GatewayConfig.from_environ(environ_dict)

    tvm_client.parse_service_ticket.return_value = ServiceTicket(defaultdict(int, src=frontend_client_id))
    tvm_client.parse_user_ticket.return_value = UserTicket(defaultdict(int, default_uid=yandex_uid))

    app = await setup_app(config, service_discovery, tvm_client)
    server = TestServer(app, loop=event_loop)
    client = GatewayTestClient('service_tocket', 'user_ticket', server, loop=event_loop)

    await client.start_server()
    yield client
    await client.close()


@pytest.fixture(autouse=True)
def patch_setup_s3_client(mocker, request):
    if 'no_patch_setup_s3_client' in request.keywords:
        return
    mocker.patch(
        "crm.agency_cabinet.gateway.server.src.setup_s3_client",
        return_value=None,
    )
