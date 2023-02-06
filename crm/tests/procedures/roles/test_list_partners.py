import pytest
from crm.agency_cabinet.gateway.server.src.procedures.roles import ListPartners
from crm.agency_cabinet.grants.common import structs as grants_structs


@pytest.fixture
def procedure(service_discovery):
    return ListPartners(service_discovery)


async def test_returns_partners_list(
    procedure, service_discovery
):
    partners = grants_structs.ListAvailablePartnersResponse(
        partners=[
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
        ])

    service_discovery.grants.list_available_partners.return_value = partners

    got = await procedure(yandex_uid=123)

    assert got == partners.partners
