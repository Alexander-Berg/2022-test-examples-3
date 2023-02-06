import pytest
import crm.agency_cabinet.ord.common.structs as ord_structs
from crm.agency_cabinet.gateway.server.src.procedures.ord.campaigns import ListCampaigns
from crm.agency_cabinet.grants.common.structs import AccessLevel

CAMPAIGNS = [
    ord_structs.Campaign(
        id=1,
        campaign_eid='campaign_eid',
        name='name'
    )
]

RESPONSE = ord_structs.CampaignList(
    campaigns=CAMPAIGNS,
    size=len(CAMPAIGNS)
)


@pytest.fixture
def procedure(service_discovery):
    return ListCampaigns(service_discovery)


@pytest.fixture
def input_params():
    return dict(
        agency_id=22,
        report_id=1,
        client_id=1,
        limit=10,
        offset=10
    )


async def test_returns_rows_all_params(
    procedure, input_params, service_discovery
):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.ord.get_campaigns.return_value = RESPONSE

    got = await procedure(yandex_uid=123, **input_params)

    assert got == RESPONSE


async def test_returns_rows_no_optional_params(
    procedure, input_params, service_discovery
):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.ord.get_campaigns.return_value = RESPONSE

    got = await procedure(yandex_uid=123, agency_id=22, report_id=1, client_id=1)

    assert got == RESPONSE
