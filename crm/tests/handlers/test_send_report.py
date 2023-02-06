import typing
from datetime import datetime, timezone
from crm.agency_cabinet.ord.proto import reports_pb2, request_pb2
from crm.agency_cabinet.ord.server.src.db import models
from crm.agency_cabinet.ord.common import structs


async def test_send_ord_report(
    handler,
    fixture_reports: typing.List[models.Report],
):
    request_pb = request_pb2.RpcRequest(
        send_report=reports_pb2.SendReportInput(
            agency_id=fixture_reports[0].agency_id,
            report_id=fixture_reports[1].id
        )
    )
    now = datetime.now(tz=timezone.utc)

    assert fixture_reports[1].status == 'draft'

    data = await handler(request_pb.SerializeToString())

    message = reports_pb2.SendReportOutput.FromString(data)
    assert message.HasField('result')

    report = await models.Report.query.where(models.Report.id == fixture_reports[1].id).gino.first()
    assert report.status == 'sent'
    assert report.sending_date.replace(second=0, microsecond=0) == now.replace(second=0, microsecond=0)


async def test_send_ord_report_already_sent(
    handler,
    fixture_reports: typing.List[models.Report],
):
    request_pb = request_pb2.RpcRequest(
        send_report=reports_pb2.SendReportInput(
            agency_id=fixture_reports[0].agency_id,
            report_id=fixture_reports[0].id
        )
    )

    data = await handler(request_pb.SerializeToString())

    message = reports_pb2.SendReportOutput.FromString(data)
    expected = structs.ErrorMessageResponse(
        message=f'Report with id {fixture_reports[0].id} was already sent'
    )
    assert message.already_sent is not None
    assert structs.ErrorMessageResponse.from_proto(message.already_sent) == expected


async def test_send_ord_report_no_such_report(
    handler,
    fixture_reports: typing.List[models.Report],
):
    request_pb = request_pb2.RpcRequest(
        send_report=reports_pb2.SendReportInput(
            agency_id=1,
            report_id=9999
        )
    )

    data = await handler(request_pb.SerializeToString())

    message = reports_pb2.SendReportOutput.FromString(data)
    expected = structs.ErrorMessageResponse(
        message='Can\'t find report with id 9999'
    )
    assert message.no_such_report is not None
    assert structs.ErrorMessageResponse.from_proto(message.no_such_report) == expected


async def test_ord_get_detailed_report_info_unsuitable_agency(
    handler,
    fixture_reports: typing.List[models.Report],
):
    request_pb = request_pb2.RpcRequest(
        send_report=reports_pb2.SendReportInput(
            agency_id=9999,
            report_id=fixture_reports[0].id
        )
    )

    data = await handler(request_pb.SerializeToString())

    message = reports_pb2.SendReportOutput.FromString(data)
    expected = structs.ErrorMessageResponse(
        message=f'Unsuitable agency for report with id {fixture_reports[0].id}'
    )
    assert message.unsuitable_agency is not None
    assert structs.ErrorMessageResponse.from_proto(message.unsuitable_agency) == expected
