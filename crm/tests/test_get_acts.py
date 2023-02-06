import pytest

from decimal import Decimal

from crm.agency_cabinet.ord.common.structs import ActList, Act
from crm.agency_cabinet.ord.proto import request_pb2, acts_pb2

pytestmark = [pytest.mark.asyncio]


async def test_ord_get_acts(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = acts_pb2.GetActsOutput(
        result=acts_pb2.ActList(
            acts=[
                acts_pb2.Act(
                    act_id=1,
                    act_eid='act1',
                    amount='10',
                    is_vat=False,
                ),
                acts_pb2.Act(
                    act_id=2,
                    act_eid='act2',
                    amount=None,
                    is_vat=False,
                )
            ],
            size=2
        ))

    got = await client.get_acts(
        search_query='search_query',
        limit=2,
        offset=0,
        report_id=1,
        agency_id=1,
    )

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name='ord',
        message=request_pb2.RpcRequest(
            get_acts=acts_pb2.GetActsInput(
                search_query='search_query',
                limit=2,
                offset=0,
                report_id=1,
                agency_id=1,
            )
        ),
        response_message_type=acts_pb2.GetActsOutput,
    )

    assert got == ActList(
        size=2,
        acts=[
            Act(
                act_id=1,
                act_eid='act1',
                amount=Decimal('10'),
                is_vat=False,
            ),
            Act(
                act_id=2,
                act_eid='act2',
                amount=None,
                is_vat=False,
            ),
        ]
    )
