import typing

from google.protobuf.timestamp_pb2 import Timestamp

from crm.agency_cabinet.rewards.proto import reports_pb2, request_pb2
from crm.agency_cabinet.rewards.server.src.db import models
from crm.agency_cabinet.rewards.server.src.handler import Handler


async def test_get_reports_info(fixture_contracts: typing.List[models.Contract], fixture_reports: typing.List[models.ReportMetaInfo]):
    request_pb = request_pb2.RpcRequest(
        get_reports_info=reports_pb2.GetReportsInfo(
            agency_id=fixture_contracts[0].agency_id
        )
    )
    # TODO: check filters
    # TODO: test get_report_info
    expected_model = fixture_reports[0]
    period_from = Timestamp()
    period_from.FromDatetime(expected_model.period_from)
    period_to = Timestamp()
    period_to.FromDatetime(expected_model.period_to)
    created_at = Timestamp()
    created_at.FromDatetime(expected_model.created_at)

    expected_output = [reports_pb2.ReportInfo(id=expected_model.id,
                                              contract_id=expected_model.contract_id,
                                              name=expected_model.name,
                                              type=expected_model.type,
                                              service=expected_model.service,
                                              status=expected_model.status,
                                              period_from=period_from,
                                              period_to=period_to,
                                              created_at=created_at,
                                              clients_ids=expected_model.clients_ids
                                              )
                       ]

    output = await Handler()(request_pb.SerializeToString())

    reports = reports_pb2.GetReportsInfoOutput.FromString(output).result.reports

    assert len(reports) == 1

    for expected, result in zip(expected_output, reports):
        assert expected == result
