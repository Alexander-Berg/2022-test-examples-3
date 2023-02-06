import pytest

from crm.agency_cabinet.common.server.common.structs import TaskStatuses
from crm.agency_cabinet.ord.common import structs
from crm.agency_cabinet.ord.proto import reports_pb2, request_pb2, common_pb2
from crm.agency_cabinet.ord.client.exceptions import UnsuitableAgencyException, NoSuchReportException

pytestmark = [pytest.mark.asyncio]


async def test_export_report_sends_correct_request(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = reports_pb2.ReportExportOutput(
        result=reports_pb2.ReportExport(
            report_export_id=1, status=1
        )
    )

    await client.report_export(agency_id=1, report_id=1)

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name='ord',
        message=request_pb2.RpcRequest(
            report_export=reports_pb2.ReportExportInput(
                agency_id=1,
                report_id=1,
            )
        ),
        response_message_type=reports_pb2.ReportExportOutput,
    )


async def test_export_report_returns_data(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = reports_pb2.ReportExportOutput(
        result=reports_pb2.ReportExport(
            report_export_id=1, status=2
        )
    )

    got = await client.report_export(agency_id=1, report_id=1)

    assert got == structs.ReportExportResponse(
        report_export_id=1,
        status=TaskStatuses.requested
    )


async def test_get_report_export_info_sends_correct_request(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = reports_pb2.ReportExportOutput(
        result=reports_pb2.ReportExport(
            report_export_id=1, status=2
        )
    )

    await client.get_report_export_info(agency_id=1, report_id=1, report_export_id=1)

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name='ord',
        message=request_pb2.RpcRequest(
            get_report_export_info=reports_pb2.ReportExportInfoInput(
                agency_id=1,
                report_id=1,
                report_export_id=1
            )
        ),
        response_message_type=reports_pb2.ReportExportOutput,
    )


async def test_get_report_export_info_returns_data(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = reports_pb2.ReportExportOutput(
        result=reports_pb2.ReportExport(
            report_export_id=1, status=2
        )
    )

    got = await client.get_report_export_info(agency_id=1, report_id=1, report_export_id=1)

    assert got == structs.ReportExportResponse(
        report_export_id=1,
        status=TaskStatuses.requested
    )


async def test_export_unsuitable_agency(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = reports_pb2.ReportExportOutput(
        unsuitable_agency=common_pb2.ErrorMessageResponse()
    )

    with pytest.raises(UnsuitableAgencyException):
        await client.report_export(agency_id=1, report_id=1)


async def test_get_export_report_unsuitable_agency(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = reports_pb2.ReportExportOutput(
        unsuitable_agency=common_pb2.ErrorMessageResponse()
    )

    with pytest.raises(UnsuitableAgencyException):
        await client.get_report_export_info(agency_id=1, report_id=1, report_export_id=1)


async def test_get_export_report_no_such_report(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = reports_pb2.ReportExportOutput(
        no_such_report=common_pb2.ErrorMessageResponse()
    )

    with pytest.raises(NoSuchReportException):
        await client.get_report_export_info(agency_id=1, report_id=1, report_export_id=1)
