import pytest

from crm.agency_cabinet.ord.client.exceptions import NoSuchReportException, UnsuitableAgencyException
from crm.agency_cabinet.ord.proto import request_pb2, reports_pb2, common_pb2


pytestmark = [pytest.mark.asyncio]


async def test_sends_correct_request(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = reports_pb2.DeleteReportOutput(
        result=common_pb2.Empty()
    )

    await client.delete_report(agency_id=1, report_id=1)

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name='ord',
        message=request_pb2.RpcRequest(
            delete_report=reports_pb2.DeleteReportRowInput(
                agency_id=1,
                report_id=1,
            )
        ),
        response_message_type=reports_pb2.DeleteReportOutput,
    )


async def test_returns_data(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = reports_pb2.DeleteReportOutput(
        result=common_pb2.Empty()
    )

    await client.delete_report(agency_id=1, report_id=1)


async def test_unsuitable_agency(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = reports_pb2.DeleteReportOutput(
        unsuitable_agency=common_pb2.ErrorMessageResponse()
    )

    with pytest.raises(UnsuitableAgencyException):
        await client.delete_report(agency_id=1, report_id=1)


async def test_no_such_report(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = reports_pb2.DeleteReportOutput(
        no_such_report=common_pb2.ErrorMessageResponse()
    )

    with pytest.raises(NoSuchReportException):
        await client.delete_report(agency_id=1, report_id=1)
