import pytest
from unittest.mock import AsyncMock

from crm.agency_cabinet.client_bonuses.common.structs import GetDetailedReportInfoResponse, ReportInfo, ClientType
from crm.agency_cabinet.client_bonuses.proto import reports_pb2, request_pb2
from crm.agency_cabinet.common.consts.report import ReportsStatuses
from smb.common.testing_utils import dt
from crm.agency_cabinet.client_bonuses.proto.bonuses_pb2 import ClientType as PbClientType

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = GetDetailedReportInfoResponse(
        ReportInfo(
            id=1,
            name='test',
            created_at=dt('2021-11-11 00:00:00'),
            period_from=dt('2020-11-1 00:00:00'),
            period_to=dt('2020-11-11 00:00:00'),
            status=ReportsStatuses.requested.value,
            client_type=ClientType.ALL
        )
    )

    mocker.patch(
        "crm.agency_cabinet.client_bonuses.server.lib.handler.GetDetailedReportInfo",
        return_value=mock,
    )

    return mock


async def test_returns_serialized_operation_result(handler):
    input_pb = request_pb2.RpcRequest(
        get_detailed_report_info=reports_pb2.GetDetailedReportInfo(
            agency_id=22, report_id=42
        )
    )
    result = await handler(input_pb.SerializeToString())

    assert reports_pb2.GetDetailedReportInfoOutput.FromString(result) == reports_pb2.GetDetailedReportInfoOutput(
        result=reports_pb2.ReportInfo(
            id=1,
            name='test',
            created_at=dt('2021-11-11 00:00:00', as_proto=True),
            period_from=dt('2020-11-1 00:00:00', as_proto=True),
            period_to=dt('2020-11-11 00:00:00', as_proto=True),
            status=ReportsStatuses.requested.value,
            client_type=PbClientType.ALL_CLIENTS
        )
    )
