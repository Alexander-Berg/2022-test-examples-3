from unittest.mock import AsyncMock

import pytest
from crm.agency_cabinet.ord.common import structs
from crm.agency_cabinet.ord.proto import request_pb2, campaigns_pb2


@pytest.fixture(autouse=True)
def procedure(mocker, fixture_campaign):
    mock = AsyncMock()
    mock.return_value = structs.CampaignList(
        campaigns=[
            structs.Campaign(
                id=fixture_campaign[0].id,
                campaign_eid=fixture_campaign[0].campaign_eid,
                name=fixture_campaign[0].name
            )
        ],
        size=1
    )

    mocker.patch(
        "crm.agency_cabinet.ord.server.src.procedures.GetCampaigns",
        return_value=mock,
    )

    return mock


async def test_calls_procedure(handler, procedure):
    input_pb = request_pb2.RpcRequest(
        get_campaigns=campaigns_pb2.GetCampaignsInput(
            agency_id=1,
            report_id=1,
            client_id=1
        )
    )

    await handler(input_pb.SerializeToString())

    procedure.assert_awaited_with(request=structs.GetCampaignsInput(agency_id=1, report_id=1, client_id=1))


@pytest.mark.parametrize(
    'input',
    [
        campaigns_pb2.GetCampaignsInput(
            agency_id=1,
            report_id=1,
            client_id=1
        )
    ]
)
async def test_returns_serialized_operation_result(handler, procedure, fixture_campaign, input):
    input_pb = request_pb2.RpcRequest(
        get_campaigns=input
    )

    result = await handler(input_pb.SerializeToString())

    assert campaigns_pb2.GetCampaignsOutput.FromString(result) == campaigns_pb2.GetCampaignsOutput(
        result=campaigns_pb2.CampaignList(
            campaigns=[
                campaigns_pb2.Campaign(
                    id=fixture_campaign[0].id,
                    campaign_eid=fixture_campaign[0].campaign_eid,
                    name=fixture_campaign[0].name
                )
            ],
            size=1
        )
    )
