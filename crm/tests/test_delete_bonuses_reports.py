import pytest
from crm.agency_cabinet.client_bonuses.proto.common_pb2 import Empty as PbEmpty
from crm.agency_cabinet.client_bonuses.proto.reports_pb2 import (
    DeleteResponse as PbDeleteResponse,
    DeleteReportInput as PbDeleteReportInput,
    DeleteReportOutput as PbDeleteReportOutput,
)
from crm.agency_cabinet.client_bonuses.proto.request_pb2 import (
    RpcRequest as PbRpcRequest,
)
from crm.agency_cabinet.client_bonuses.client import NoSuchReportException, UnsuitableAgencyException

pytestmark = [pytest.mark.asyncio]


async def test_sends_correct_request(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = PbDeleteReportOutput(
        result=PbDeleteResponse(is_deleted=True)
    )

    await client.delete_report(
        agency_id=22,
        report_id=1
    )

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name="client-bonuses",
        message=PbRpcRequest(
            delete_report=PbDeleteReportInput(
                agency_id=22,
                report_id=1
            )
        ),
        response_message_type=PbDeleteReportOutput,
    )


async def test_returns_data(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = PbDeleteReportOutput(
        result=PbDeleteResponse(is_deleted=True)
    )

    got = await client.delete_report(
        agency_id=22,
        report_id=1
    )

    assert got is True and type(got) == bool


async def test_no_such_report(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = PbDeleteReportOutput(
        no_such_report=PbEmpty()
    )

    with pytest.raises(NoSuchReportException):
        await client.delete_report(
            agency_id=22,
            report_id=1
        )


async def test_not_suitable_agency(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = PbDeleteReportOutput(
        unsuitable_agency=PbEmpty()
    )

    with pytest.raises(UnsuitableAgencyException):
        await client.delete_report(
            agency_id=22,
            report_id=1
        )
