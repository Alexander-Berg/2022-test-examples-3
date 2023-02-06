import pytest
from decimal import Decimal

from crm.agency_cabinet.ord.proto import request_pb2, acts_pb2, common_pb2

pytestmark = [pytest.mark.asyncio]


async def test_edit_act(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = acts_pb2.EditActOutput(
        result=common_pb2.Empty()
    )

    await client.edit_act(
        agency_id=1,
        act_id=1,
        report_id=1,
        act_eid='eid',
        amount=Decimal('10'),
        is_vat=True,
    )

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name='ord',
        message=request_pb2.RpcRequest(
            edit_act=acts_pb2.EditActInput(
                agency_id=1,
                act_id=1,
                report_id=1,
                act_eid='eid',
                amount='10',
                is_vat=True,
            )
        ),
        response_message_type=acts_pb2.EditActOutput,
    )
