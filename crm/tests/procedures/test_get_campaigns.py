import pytest

from crm.agency_cabinet.ord.common import structs
from crm.agency_cabinet.ord.server.src import procedures
from crm.agency_cabinet.ord.server.tests.procedures.conftest import AGENCY_ID


@pytest.fixture
def procedure():
    return procedures.GetCampaigns()


async def test_get_campaigns(procedure, fixture_campaign2, fixture_clients2):
    result = await procedure(structs.GetCampaignsInput(
        agency_id=AGENCY_ID,
        report_id=fixture_campaign2[0].report_id,
        client_id=fixture_clients2[0].id
    ))

    assert result == structs.CampaignList(
        campaigns=[
            structs.Campaign(
                id=fixture_campaign2[0].id,
                campaign_eid=fixture_campaign2[0].campaign_eid,
                name=fixture_campaign2[0].name,
            )
        ],
        size=1
    )
