from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.common import structs as grants_structs
from crm.agency_cabinet.agencies.common import structs as agencies_structs


URL = '/api/agencies/{agency_id}/clients'


async def test_access_deny(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.DENY
    await client.get(URL.format(agency_id=1), expected_status=403)


async def test_list_clients(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.ALLOW
    service_discovery.agencies.get_clients_info.return_value = [
        agencies_structs.ClientInfo(
            id=1,
            name='Клиент',
            login='login'
        ),
        agencies_structs.ClientInfo(
            id=2,
            name='Клиент 2',
            login='login 2'
        )
    ]
    got = await client.get(URL.format(agency_id=1), expected_status=200)

    expected = {
        'clients': [
            {
                'client_id': 1,
                'name': 'Клиент',
                'login': 'login'
            },
            {
                'client_id': 2,
                'name': 'Клиент 2',
                'login': 'login 2'
            }
        ]
    }

    assert got == expected

    service_discovery.grants.check_access_level.assert_awaited_with(yandex_uid=yandex_uid, agency_id=1)
