import pytest

from crm.agency_cabinet.ord.common import consts, structs
from crm.agency_cabinet.ord.server.src import procedures


@pytest.fixture
def procedure():
    return procedures.GetOrganizations()


async def test_get_organizations(procedure, fixture_organizations):
    got = await procedure(structs.GetOrganizationsInput(
        partner_id=1,
        limit=2
    ))

    assert got == structs.OrganizationsList(
        size=3,
        organizations=[
            structs.Organization(
                id=fixture_organizations[2].id,
                type=consts.OrganizationType.fl,
                mobile_phone='1234567890',
            ),
            structs.Organization(
                id=fixture_organizations[1].id,
                type=consts.OrganizationType.ip,
                inn='contractor inn',
            )
        ]
    )


async def test_get_organizations_empty(procedure, fixture_organizations):
    got = await procedure(structs.GetOrganizationsInput(
        partner_id=99,
        limit=2
    ))

    assert got == structs.OrganizationsList(
        size=0,
        organizations=[]
    )


async def test_get_organizations_search_query(procedure, fixture_organizations):
    got = await procedure(structs.GetOrganizationsInput(
        partner_id=1,
        search_query='123456'
    ))

    assert got == structs.OrganizationsList(
        size=1,
        organizations=[
            structs.Organization(
                id=fixture_organizations[2].id,
                type=consts.OrganizationType.fl,
                mobile_phone='1234567890',
            ),
        ]
    )
