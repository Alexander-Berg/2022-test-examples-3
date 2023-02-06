from unittest.mock import AsyncMock

import pytest

from crm.agency_cabinet.common.server.common.structs import TaskStatuses
from crm.agency_cabinet.ord.common import structs
from crm.agency_cabinet.ord.proto import reports_pb2, request_pb2, common_pb2
from crm.agency_cabinet.ord.common.exceptions import UnsuitableAgencyException, NoSuchReportException


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = structs.ReportExportResponse(report_export_id=1, status=TaskStatuses.requested)

    mocker.patch(
        "crm.agency_cabinet.ord.server.src.procedures.GetReportExportInfo",
        return_value=mock,
    )

    return mock


async def test_get_report_export_info(handler, procedure):
    expected = reports_pb2.ReportExportOutput(
        result=reports_pb2.ReportExport(
            report_export_id=1,
            status=2
        )
    )

    request_pb = request_pb2.RpcRequest(
        get_report_export_info=reports_pb2.ReportExportInfoInput(
            agency_id=1,
            report_id=1,
            report_export_id=1
        )
    )

    data = await handler(request_pb.SerializeToString())
    res = reports_pb2.ReportExportOutput.FromString(data)

    assert res == expected


@pytest.mark.parametrize(
    ('side_effect', 'expected_message'),
    [
        (
            UnsuitableAgencyException,
            reports_pb2.ReportExportOutput(unsuitable_agency=common_pb2.ErrorMessageResponse(message=''))
        ),
        (
            NoSuchReportException,
            reports_pb2.ReportExportOutput(no_such_report=common_pb2.ErrorMessageResponse(message=''))
        ),
    ]
)
async def test_returns_error(handler, fixture_reports, mocker, side_effect, expected_message):
    mock = AsyncMock()
    mock.return_value = None
    mock.side_effect = [side_effect]

    with mocker.patch(
        "crm.agency_cabinet.ord.server.src.procedures.GetReportExportInfo",
        return_value=mock,
    ):
        input_pb = request_pb2.RpcRequest(
            get_report_export_info=reports_pb2.ReportExportInfoInput(
                agency_id=1,
                report_id=1,
                report_export_id=1
            )
        )

        result = await handler(input_pb.SerializeToString())

        assert reports_pb2.ReportExportOutput.FromString(result) == expected_message
