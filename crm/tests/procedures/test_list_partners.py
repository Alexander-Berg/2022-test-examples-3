import pytest
from crm.agency_cabinet.grants.common import structs
from crm.agency_cabinet.grants.server.src.procedures.partner_manager import ListPartners


@pytest.fixture
def procedure():
    return ListPartners()


async def test_list_partners(procedure: ListPartners, fixture_users, fixture_partners_map,
                             fixture_partners, fixture_user_permissions):
    got = await procedure(structs.ListPartnersInput())

    expected = structs.ListPartnersResponse(partners=[structs.Partner(
        external_id=fixture_partner.external_id,
        partner_id=fixture_partner.id,
        type=fixture_partner.type,
        name=fixture_partner.name
    ) for fixture_partner in fixture_partners])
    assert got == expected
