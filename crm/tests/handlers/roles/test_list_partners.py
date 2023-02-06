import pytest

from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.client import NoSuchUserException
from crm.agency_cabinet.grants.common import structs as grants_structs


URL = '/api/partners'


@pytest.mark.parametrize(
    (
        'partners_list',
    ),
    [
        (
            [
                grants_structs.Partner(
                    partner_id=1,
                    external_id='123',
                    type='agency',
                    name='Test'
                ),
                grants_structs.Partner(
                    partner_id=2,
                    external_id='124',
                    type='agency',
                    name='Test2'
                )
            ],
        ),
    ]
)
async def test_list_partners(
    client: BaseTestClient,
    service_discovery: ServiceDiscovery,
    yandex_uid: int,
    partners_list
):
    service_discovery.grants.list_available_partners.return_value = grants_structs.ListAvailablePartnersResponse(
        partners=partners_list)

    got = await client.get(URL, expected_status=200)

    assert got == {
        'partners': [
            {'partner_id': 1, 'external_id': '123', 'type': 'agency', 'name': 'Test'},
            {'partner_id': 2, 'external_id': '124', 'type': 'agency', 'name': 'Test2'}
        ],
        'size': 2
    }


async def test_list_partners_no_user(client: BaseTestClient, service_discovery: ServiceDiscovery):
    service_discovery.grants.list_available_partners.side_effect = NoSuchUserException()

    got = await client.get(URL, expected_status=200)

    assert got == {
        'partners': [],
        'size': 0
    }
