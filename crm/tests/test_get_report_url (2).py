import pytest

from crm.agency_cabinet.ord.proto import reports_pb2, request_pb2, common_pb2
from crm.agency_cabinet.ord.client.exceptions import UnsuitableAgencyException, NoSuchReportException, \
    ReportNotReadyException, FileNotFoundException


pytestmark = [pytest.mark.asyncio]


async def test_sends_correct_request(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = reports_pb2.GetReportUrlOutput(
        result=reports_pb2.ReportUrl(url='url')
    )

    await client.get_report_url(agency_id=1, report_id=1, report_export_id=1)

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name='ord',
        message=request_pb2.RpcRequest(
            get_report_url=reports_pb2.GetReportUrlInput(
                agency_id=1,
                report_id=1,
                report_export_id=1
            )
        ),
        response_message_type=reports_pb2.GetReportUrlOutput,
    )


async def test_returns_data(client, rmq_rpc_client):
    report_url = 'test.url'
    rmq_rpc_client.send_proto_message.return_value = reports_pb2.GetReportUrlOutput(
        result=reports_pb2.ReportUrl(url=report_url)
    )

    got = await client.get_report_url(agency_id=22, report_id=1, report_export_id=1)

    assert got == report_url


async def test_unsuitable_agency(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = reports_pb2.GetReportUrlOutput(
        unsuitable_agency=common_pb2.ErrorMessageResponse()
    )

    with pytest.raises(UnsuitableAgencyException):
        await client.get_report_url(agency_id=22, report_id=1, report_export_id=1)


async def test_no_such_report(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = reports_pb2.GetReportUrlOutput(
        no_such_report=common_pb2.ErrorMessageResponse()
    )

    with pytest.raises(NoSuchReportException):
        await client.get_report_url(agency_id=22, report_id=1, report_export_id=1)


async def test_file_not_found(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = reports_pb2.GetReportUrlOutput(
        file_not_found=common_pb2.ErrorMessageResponse()
    )

    with pytest.raises(FileNotFoundException):
        await client.get_report_url(agency_id=22, report_id=1, report_export_id=1)


async def test_report_not_ready(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = reports_pb2.GetReportUrlOutput(
        report_not_ready=common_pb2.ErrorMessageResponse()
    )

    with pytest.raises(ReportNotReadyException):
        await client.get_report_url(agency_id=22, report_id=1, report_export_id=1)
