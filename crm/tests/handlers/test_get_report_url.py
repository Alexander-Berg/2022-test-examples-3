from unittest.mock import AsyncMock

import pytest

from crm.agency_cabinet.ord.common import structs
from crm.agency_cabinet.ord.proto import request_pb2, reports_pb2, common_pb2
from crm.agency_cabinet.ord.common.exceptions import UnsuitableAgencyException, NoSuchReportException, \
    FileNotFoundException, ReportNotReadyException


pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = structs.GetReportUrlResponse(report_url='http://report.ru')

    mocker.patch(
        "crm.agency_cabinet.ord.server.src.procedures.GetReportUrl",
        return_value=mock,
    )

    return mock


async def test_returns_serialized_operation_result(handler, procedure):
    input_pb = request_pb2.RpcRequest(
        get_report_url=reports_pb2.GetReportUrlInput(
            agency_id=22,
            report_id=1,
            report_export_id=1
        )
    )

    result = await handler(input_pb.SerializeToString())

    assert reports_pb2.GetReportUrlOutput.FromString(result) == reports_pb2.GetReportUrlOutput(
        result=reports_pb2.ReportUrl(url='http://report.ru')
    )


@pytest.mark.parametrize(
    ('side_effect', 'expected_message'),
    [
        (
            UnsuitableAgencyException,
            reports_pb2.GetReportUrlOutput(unsuitable_agency=common_pb2.ErrorMessageResponse(message=''))
        ),
        (
            NoSuchReportException,
            reports_pb2.GetReportUrlOutput(no_such_report=common_pb2.ErrorMessageResponse(message=''))
        ),
        (
            ReportNotReadyException,
            reports_pb2.GetReportUrlOutput(report_not_ready=common_pb2.ErrorMessageResponse(message=''))
        ),
        (
            FileNotFoundException,
            reports_pb2.GetReportUrlOutput(file_not_found=common_pb2.ErrorMessageResponse(message=''))
        ),
    ]
)
async def test_returns_error(handler, fixture_reports, mocker, side_effect, expected_message):
    mock = AsyncMock()
    mock.return_value = None
    mock.side_effect = [side_effect]

    with mocker.patch(
        "crm.agency_cabinet.ord.server.src.procedures.GetReportUrl",
        return_value=mock,
    ):
        input_pb = request_pb2.RpcRequest(
            get_report_url=reports_pb2.GetReportUrlInput(
                agency_id=22,
                report_id=1,
                report_export_id=1
            )
        )

        result = await handler(input_pb.SerializeToString())

        assert reports_pb2.GetReportUrlOutput.FromString(result) == expected_message
