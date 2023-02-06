import pytest
from crm.agency_cabinet.grants.common import structs
from crm.agency_cabinet.grants.server.src.procedures.partner_manager import GetPartner


@pytest.fixture
def procedure():
    return GetPartner()


async def test_get_partner(procedure: GetPartner, fixture_users, fixture_partners_map,
                                       fixture_partners, fixture_user_permissions):
    got = await procedure(structs.GetPartnerInput(
        yandex_uid=fixture_users[2].yandex_uid,
        partner_id=fixture_partners[0].id,
    ))

    expected = structs.Partner(
        external_id=fixture_partners[0].external_id,
        partner_id=fixture_partners[0].id,
        type=fixture_partners[0].type,
        name=fixture_partners[0].name
    )
    assert got == expected
