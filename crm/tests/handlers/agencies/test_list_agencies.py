import pytest

from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.agencies.common import structs as agencies_structs
from crm.agency_cabinet.grants.common import structs as grants_structs


URL = '/api/agencies'


async def test_returns_empty_for_no_agencies_access(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int):
    service_discovery.grants.get_access_scope.return_value = (grants_structs.AccessScopeType.BY_ID, [])
    got = await client.get(URL, expected_status=200)

    assert got == {
        'agencies': []
    }

    service_discovery.grants.get_access_scope.assert_awaited_with(yandex_uid)
    assert not service_discovery.agencies.get_agencies_info.called


@pytest.mark.parametrize(
    ('grants_return_value', 'agencies_return_value', 'expected'),
    [
        (
            (grants_structs.AccessScopeType.BY_ID, [1, 2, 3]),
            [
                agencies_structs.AgencyInfo(1, 'агентство 1', 'phone_1', 'agency_1@yandex.ru', '', '', ''),
                agencies_structs.AgencyInfo(2, 'name 2', 'phone_2', 'agency_2@yandex.ru', '', '', ''),
            ],
            {'agencies': [{'agency_id': 1, 'name': 'агентство 1'}, {'agency_id': 2, 'name': 'name 2'}]}
        ),
    ]
)
async def test_list_agencies(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid, grants_return_value, agencies_return_value, expected):
    service_discovery.grants.get_access_scope.return_value = grants_return_value
    service_discovery.agencies.get_agencies_info.return_value = agencies_return_value
    got = await client.get(URL, expected_status=200)

    assert got == expected

    service_discovery.grants.get_access_scope.assert_awaited_with(yandex_uid)
    service_discovery.agencies.get_agencies_info.assert_awaited_with(grants_return_value[1])


@pytest.mark.parametrize(
    ('grants_return_value', 'agencies_return_value', 'expected'),
    [
        (
            (grants_structs.AccessScopeType.ALL, []),
            [
                agencies_structs.AgencyInfo(1, 'агентство 1', 'phone_1', 'agency_1@yandex.ru', '', '', ''),
                agencies_structs.AgencyInfo(2, 'name 2', 'phone_2', 'agency_2@yandex.ru', '', '', ''),
            ],
            {'agencies': [{'agency_id': 1, 'name': 'агентство 1'}, {'agency_id': 2, 'name': 'name 2'}]}
        ),
    ]
)
async def test_list_agencies_for_yandex(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid, grants_return_value, agencies_return_value, expected):
    service_discovery.grants.get_access_scope.return_value = grants_return_value
    service_discovery.agencies.get_all_agencies_info.return_value = agencies_return_value
    got = await client.get(URL, expected_status=200)

    assert got == expected

    service_discovery.grants.get_access_scope.assert_awaited_with(yandex_uid)
    service_discovery.agencies.get_all_agencies_info.assert_awaited()
