import typing

from crm.agency_cabinet.rewards.proto import reports_pb2, request_pb2
from crm.agency_cabinet.rewards.server.src.db import models
from crm.agency_cabinet.rewards.server.src.handler import Handler


async def test_delete_report(fixture_contracts: typing.List[models.Contract], fixture_reports: typing.List[models.ReportMetaInfo]):
    request_pb = request_pb2.RpcRequest(
        delete_report=reports_pb2.DeleteReport(
            agency_id=fixture_contracts[0].agency_id,
            report_id=fixture_reports[0].id
        )
    )

    output = await Handler()(request_pb.SerializeToString())

    result = reports_pb2.DeleteReportOutput.FromString(output).result.is_deleted

    assert result is True

    deleted_report = await models.ReportMetaInfo.query.where(models.ReportMetaInfo.id == fixture_reports[0].id).gino.first()
    assert deleted_report is None
