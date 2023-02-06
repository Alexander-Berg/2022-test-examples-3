import pytest
from crm.agency_cabinet.grants.common import structs
from crm.agency_cabinet.grants.server.src.procedures.partner_manager import GetPartnerID


@pytest.fixture
def procedure():
    return GetPartnerID()


async def test_get_partner_id(procedure: GetPartnerID, fixture_users, fixture_partners_map,
                              fixture_partners, fixture_user_permissions):
    got = await procedure(structs.GetPartnerIDInput(
        yandex_uid=fixture_users[2].yandex_uid,
        external_id=fixture_partners[0].external_id,
        type=fixture_partners[0].type
    ))

    expected = structs.GetPartnerIDResponse(partner_id=fixture_partners[0].id)
    assert got == expected
