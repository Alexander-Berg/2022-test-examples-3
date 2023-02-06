import pytest
from crm.agency_cabinet.ord.proto import request_pb2, reports_pb2, common_pb2


pytestmark = [pytest.mark.asyncio]


async def test_send_report_sends_correct_request(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = reports_pb2.SendReportOutput(
        result=common_pb2.Empty()
    )

    got = await client.send_report(agency_id=1, report_id=1)

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name='ord',
        message=request_pb2.RpcRequest(
            send_report=reports_pb2.SendReportInput(
                agency_id=1,
                report_id=1
            )
        ),
        response_message_type=reports_pb2.SendReportOutput,
    )

    assert got is None
