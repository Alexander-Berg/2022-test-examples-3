import typing
from unittest.mock import AsyncMock

from crm.agency_cabinet.rewards.proto import reports_pb2, request_pb2, common_pb2
from crm.agency_cabinet.rewards.server.src.db import models
from crm.agency_cabinet.rewards.server.src.handler import Handler


async def test_get_report_url(fixture_contracts: typing.List[models.Contract], fixture_reports: typing.List[models.ReportMetaInfo], mocker):
    url = 'test.xslx'
    mocker.patch(
        'crm.agency_cabinet.rewards.server.src.procedures.reports.create_presigned_url_async',
        mock=AsyncMock,
        return_value=url)
    request_pb = request_pb2.RpcRequest(
        get_report_url=reports_pb2.GetReportUrl(
            agency_id=fixture_contracts[0].agency_id,
            report_id=fixture_reports[0].id
        )
    )
    output = await Handler()(request_pb.SerializeToString())

    result = reports_pb2.GetReportUrlOutput.FromString(output).result.url

    assert result == url


async def test_report_file_not_ready(fixture_contracts: typing.List[models.Contract], fixture_reports: typing.List[models.ReportMetaInfo]):
    request_pb = request_pb2.RpcRequest(
        get_report_url=reports_pb2.GetReportUrl(
            agency_id=fixture_contracts[1].agency_id,
            report_id=fixture_reports[1].id
        )
    )
    output = await Handler()(request_pb.SerializeToString())

    proto = reports_pb2.GetReportUrlOutput.FromString(output)
    result = proto.WhichOneof('response')
    assert result == 'report_not_ready'
    assert proto.report_not_ready == common_pb2.Empty()


async def test_no_such_report(fixture_contracts: typing.List[models.Contract], fixture_reports: typing.List[models.ReportMetaInfo]):
    request_pb = request_pb2.RpcRequest(
        get_report_url=reports_pb2.GetReportUrl(
            agency_id=fixture_contracts[0].agency_id,
            report_id=fixture_reports[1].id + 1000
        )
    )
    output = await Handler()(request_pb.SerializeToString())

    proto = reports_pb2.GetReportUrlOutput.FromString(output)
    result = proto.WhichOneof('response')
    assert result == 'no_such_report'
    assert proto.no_such_report == common_pb2.Empty()


async def test_unsuitable_agency(fixture_contracts: typing.List[models.Contract], fixture_reports: typing.List[models.ReportMetaInfo]):
    request_pb = request_pb2.RpcRequest(
        get_report_url=reports_pb2.GetReportUrl(
            agency_id=fixture_contracts[0].agency_id,
            report_id=fixture_reports[2].id
        )
    )
    output = await Handler()(request_pb.SerializeToString())

    proto = reports_pb2.GetReportUrlOutput.FromString(output)
    result = proto.WhichOneof('response')
    assert result == 'unsuitable_agency'
    assert proto.unsuitable_agency == common_pb2.Empty()


async def test_file_not_found(fixture_contracts: typing.List[models.Contract], fixture_reports: typing.List[models.ReportMetaInfo]):
    request_pb = request_pb2.RpcRequest(
        get_report_url=reports_pb2.GetReportUrl(
            agency_id=fixture_contracts[2].agency_id,
            report_id=fixture_reports[2].id
        )
    )
    output = await Handler()(request_pb.SerializeToString())

    proto = reports_pb2.GetReportUrlOutput.FromString(output)
    result = proto.WhichOneof('response')
    assert result == 'file_not_found'
    assert proto.file_not_found == common_pb2.Empty()
