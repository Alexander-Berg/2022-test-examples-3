from unittest.mock import AsyncMock

import pytest

from smb.common.testing_utils import dt

from crm.agency_cabinet.ord.common import structs, consts
from crm.agency_cabinet.ord.proto import request_pb2, reports_pb2


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = structs.ReportInfo(
        report_id=1,
        period_from=dt('2020-09-22 10:10:10'),
        status=consts.ReportStatuses.draft,
        reporter_type=consts.ReporterType.partner,
        clients_count=0,
        campaigns_count=0,
        sending_date=dt('2020-09-22 10:10:10'),
        settings=structs.ReportSettings(
            name='other',
            display_name='Другое',
            allow_create_ad_distributor_acts=True,
            allow_create_clients=True,
            allow_create_campaigns=True,
            allow_edit_report=True,
        )
    )

    mocker.patch(
        "crm.agency_cabinet.ord.server.src.procedures.CreateReport",
        return_value=mock,
    )

    return mock


async def test_calls_procedure(handler, procedure):
    input_pb = request_pb2.RpcRequest(
        create_report=reports_pb2.CreateReportInput(
            agency_id=22,
            period_from=dt('2020-09-22 10:10:10', as_proto=True),
            reporter_type=0,
        )
    )

    await handler(input_pb.SerializeToString())

    procedure.assert_awaited_with(
        request=structs.CreateReportRequest(
            agency_id=22,
            period_from=dt('2020-09-22 10:10:10'),
            reporter_type=consts.ReporterType.partner,
        )
    )


async def test_returns_serialized_operation_result(handler, procedure):
    input_pb = request_pb2.RpcRequest(
        create_report=reports_pb2.CreateReportInput(
            agency_id=22,
            period_from=dt('2020-09-22 10:10:10', as_proto=True),
            reporter_type=0,
        )
    )

    result = await handler(input_pb.SerializeToString())

    assert reports_pb2.CreateReportOutput.FromString(result) == reports_pb2.CreateReportOutput(
        result=reports_pb2.ReportInfo(
            report_id=1,
            period_from=dt('2020-09-22 10:10:10', as_proto=True),
            status=1,
            reporter_type=0,
            clients_count=0,
            campaigns_count=0,
            settings=reports_pb2.ReportSettings(
                name='other',
                display_name='Другое',
                allow_create_ad_distributor_acts=True,
                allow_create_clients=True,
                allow_create_campaigns=True,
                allow_edit_report=True,
            ),
            sending_date=dt('2020-09-22 10:10:10', as_proto=True)
        )
    )
