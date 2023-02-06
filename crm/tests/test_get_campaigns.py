import pytest
from crm.agency_cabinet.ord.common.structs import Campaign, CampaignList
from crm.agency_cabinet.ord.proto import request_pb2, campaigns_pb2

pytestmark = [pytest.mark.asyncio]


async def test_get_campaigns_sends_correct_request(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = campaigns_pb2.GetCampaignsOutput(
        result=campaigns_pb2.CampaignList(
            campaigns=[campaigns_pb2.Campaign(
                id=1,
                campaign_eid='123',
                name='campaign'
                )
            ],
            size=1
        ))

    got = await client.get_campaigns(
        agency_id=1,
        report_id=1,
        client_id=1
    )

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name='ord',
        message=request_pb2.RpcRequest(
            get_campaigns=campaigns_pb2.GetCampaignsInput(
                agency_id=1,
                report_id=1,
                client_id=1
            )
        ),
        response_message_type=campaigns_pb2.GetCampaignsOutput,
    )

    assert got == CampaignList(
        campaigns=[
            Campaign(
                id=1,
                campaign_eid='123',
                name='campaign'
            )
        ],
        size=1
    )
