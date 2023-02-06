import pytest
from unittest.mock import AsyncMock

from crm.agency_cabinet.client_bonuses.common.structs import GetReportUrlResponse

from crm.agency_cabinet.client_bonuses.proto import reports_pb2, request_pb2

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = GetReportUrlResponse(
        report_url='http://mock.report.url'
    )

    mocker.patch(
        "crm.agency_cabinet.client_bonuses.server.lib.handler.GetReportUrl",
        return_value=mock,
    )

    return mock


async def test_returns_serialized_operation_result(handler):
    input_pb = request_pb2.RpcRequest(
        get_report_url=reports_pb2.GetReportUrl(
            agency_id=22, report_id=42
        )
    )
    await handler.setup()
    result = await handler(input_pb.SerializeToString())

    assert reports_pb2.GetReportUrlOutput.FromString(result) == reports_pb2.GetReportUrlOutput(
        result=reports_pb2.ReportUrl(url='http://mock.report.url')
    )
