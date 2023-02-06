import pytest

from aiohttp.test_utils import TestServer

from crm.agency_cabinet.gateway_internal.server.src import setup_app
from crm.agency_cabinet.gateway_internal.server.src.config import GatewayInternalConfig
from crm.agency_cabinet.gateway_internal.server.tests.conftest import AuthTestClient
from crm.agency_cabinet.common.server.common.tvm import TvmClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.common import structs as grants_structs


@pytest.fixture
async def client(event_loop, service_discovery: ServiceDiscovery, tvm_client: TvmClient):
    environ_dict = {
        'TVM2_ASYNC': '1',
        'TVM2_CLIENT_ID': '123',
        'IDM_CLIENT_ID' : '456',
        'AMQP_URL': 'localhost'
    }

    config = GatewayInternalConfig.from_environ(environ_dict)

    class TicketMock:
        src = int(environ_dict['IDM_CLIENT_ID'])

    tvm_client.parse_service_ticket.return_value = TicketMock()

    app = await setup_app(config, service_discovery, tvm_client)
    server = TestServer(app, loop=event_loop)
    client = AuthTestClient(environ_dict['IDM_CLIENT_ID'], server, loop=event_loop)

    await client.start_server()
    yield client
    await client.close()


async def test_get_info(client: AuthTestClient, service_discovery: ServiceDiscovery):
    result = await client.get('idm/info/', expected_status=200)

    assert result == {
        'code': 0,
        'roles': {
            'slug': 'role',
            'name': {
                'en': 'Role',
                'ru': 'Роль'
            },
            'values': {
                'manager': {
                    'name': {
                        'ru': 'Менеджер',
                        'en': 'Manager',
                   },
                    'help': {
                        'ru': 'Доступ ко всем агентствам',
                        'en': 'Access to all agencies'
                    }
                }
            }
        },
        'fields': [
            {
                'slug': 'passport-login',
                'name': {
                    'en': 'Passport login',
                    'ru': 'Паспортный логин',
                },
                'type': 'passportlogin',
                'required': True,
            }
        ]
    }


async def test_get_all_roles(client: AuthTestClient, service_discovery: ServiceDiscovery):
    service_discovery.grants.get_all_internal_roles.return_value = [
        grants_structs.InternalRole(
            staff_login='test_login',
            email='test_email@yandex.ru'
        )
    ]
    result = await client.get('idm/get-all-roles/', expected_status=200)
    assert result == {
        'code': 0,
        'users': [
            {
                'login': 'test_login',
                'roles': [
                    [
                        {'role': 'manager'},
                        {'passport-login': 'test_email'}
                    ]
                ]
            }
        ]}


async def test_add_role(client, service_discovery: ServiceDiscovery):
    service_discovery.grants.add_internal_role.return_value = None

    result = await client.post(
        'idm/add-role/',
        data={'login': 'test', 'fields': '{"passport-login": "test"}'},
        expected_status=200
    )
    assert result == {'code': 0, 'data': {"passport-login": "test"}}


async def test_remove_role(client, service_discovery: ServiceDiscovery):
    service_discovery.grants.add_internal_role.return_value = None

    result = await client.post(
        'idm/remove-role/',
        data={'login': 'test', 'fields': '{"passport-login": "test"}'},
        expected_status=200
    )
    assert result == {'code': 0}
