import pytest
from crm.agency_cabinet.grants.common import structs
from crm.agency_cabinet.grants.server.src.procedures.partner_manager import ListAvailablePartners


@pytest.fixture
def procedure():
    return ListAvailablePartners()


async def test_list_available_partners(procedure: ListAvailablePartners, fixture_users, fixture_partners_map,
                                       fixture_partners, fixture_user_permissions):
    got = await procedure(structs.ListAvailablePartnersInput(
        yandex_uid=fixture_users[2].yandex_uid
    ))

    partners = [
        structs.Partner(
            partner_id=fixture_partners[0].id,
            external_id=fixture_partners[0].external_id,
            type=fixture_partners[0].type,
            name=fixture_partners[0].name
        )
    ]
    expected = structs.ListAvailablePartnersResponse(partners=partners)
    assert got == expected
