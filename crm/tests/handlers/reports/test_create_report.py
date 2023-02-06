import typing
from datetime import datetime

from google.protobuf.timestamp_pb2 import Timestamp

from crm.agency_cabinet.common.consts import ReportsTypes, Services
from crm.agency_cabinet.rewards.proto import common_pb2, reports_pb2, request_pb2
from crm.agency_cabinet.rewards.server.src.db import models
from crm.agency_cabinet.rewards.server.src.handler import Handler


async def test_create_report(fixture_contracts: typing.List[models.Contract], fixture_reports: typing.List[models.ReportMetaInfo], mocker):
    mocker.patch('crm.agency_cabinet.rewards.server.src.procedures.reports.check_params', return_value=([]))
    period_from = Timestamp()
    period_to = Timestamp()
    period_from.FromDatetime(datetime(2021, 1, 2))
    period_to.FromDatetime(datetime(2021, 3, 4))
    request_pb = request_pb2.RpcRequest(
        create_report=reports_pb2.CreateReport(
            agency_id=fixture_contracts[0].agency_id,
            contract_id=fixture_contracts[0].id,
            name='Новый отчет',
            type=ReportsTypes.custom.value,
            service=Services.direct.value,
            period_from=period_from,
            period_to=period_to,
            clients_ids=[]
        )
    )
    existed_ids = [report.id for report in await models.ReportMetaInfo.query.gino.all()]

    output = await Handler()(request_pb.SerializeToString())

    result = reports_pb2.CreateReportOutput.FromString(output).result

    created_report = await models.ReportMetaInfo.query.where(models.ReportMetaInfo.id.notin_(existed_ids)).gino.first()
    assert created_report is not None
    created_at = Timestamp()
    created_at.FromDatetime(created_report.created_at)
    created_proto = reports_pb2.DetailedReportInfo(id=created_report.id,
                                                   contract_id=created_report.contract_id,
                                                   name=created_report.name,
                                                   type=created_report.type,
                                                   service=created_report.service,
                                                   status=created_report.status,
                                                   period_from=period_from,
                                                   period_to=period_to,
                                                   created_at=created_at,
                                                   clients_ids=created_report.clients_ids
                                                   )
    assert created_report is not None
    assert result == created_proto


async def test_create_report_unsuitable_agency(fixture_contracts: typing.List[models.Contract], fixture_reports: typing.List[models.ReportMetaInfo]):
    period_from = Timestamp()
    period_to = Timestamp()
    period_from.FromDatetime(datetime(2021, 1, 2))
    period_to.FromDatetime(datetime(2021, 3, 4))
    request_pb = request_pb2.RpcRequest(
        create_report=reports_pb2.CreateReport(
            agency_id=fixture_contracts[0].agency_id,
            contract_id=fixture_contracts[1].id,
            name='Новый отчет',
            type=ReportsTypes.custom.value,
            service=Services.direct.value,
            period_from=period_from,
            period_to=period_to,
            clients_ids=[]
        )
    )

    output = await Handler()(request_pb.SerializeToString())

    proto = reports_pb2.CreateReportOutput.FromString(output)
    result = proto.WhichOneof('response')
    assert result == 'unsuitable_agency'
    assert proto.unsuitable_agency == common_pb2.Empty()


async def test_create_report_unsupported_parameters(fixture_contracts: typing.List[models.Contract], fixture_reports: typing.List[models.ReportMetaInfo]):
    period_from = Timestamp()
    period_to = Timestamp()
    period_from.FromDatetime(datetime(2019, 1, 2))
    period_to.FromDatetime(datetime(2019, 3, 4))
    request_pb = request_pb2.RpcRequest(
        create_report=reports_pb2.CreateReport(
            agency_id=fixture_contracts[0].agency_id,
            contract_id=fixture_contracts[0].id,
            name='Новый отчет',
            type=ReportsTypes.custom.value,
            service=Services.direct.value,
            period_from=period_from,
            period_to=period_to,
            clients_ids=[]
        )
    )

    output = await Handler()(request_pb.SerializeToString())

    proto = reports_pb2.CreateReportOutput.FromString(output)
    result = proto.WhichOneof('response')
    assert result == 'unsupported_parameters'
    assert proto.unsuitable_agency == common_pb2.Empty()
