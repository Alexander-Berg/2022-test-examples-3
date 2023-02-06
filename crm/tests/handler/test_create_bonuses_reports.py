from smb.common.testing_utils import dt
from unittest.mock import AsyncMock

import pytest
from crm.agency_cabinet.client_bonuses.common.structs import (
    ClientType,
    ReportInfo,
    CreateReportInput,
)
from crm.agency_cabinet.client_bonuses.proto.reports_pb2 import (
    ReportInfo as PbReportInfo,
    CreateReportInput as PbCreateReportInput,
    CreateReportOutput as PbCreateReportOutput,
)

from crm.agency_cabinet.client_bonuses.proto.bonuses_pb2 import ClientType as PbClientType
from crm.agency_cabinet.client_bonuses.proto.request_pb2 import RpcRequest
from crm.agency_cabinet.common.consts import ReportsStatuses

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = ReportInfo(
        id=1,
        name='Отчет по бонусам 1',
        status=ReportsStatuses.requested.value,
        created_at=dt('2021-11-11 00:00:00'),
        period_from=dt('2020-11-11 00:00:00'),
        period_to=dt('2021-11-11 00:00:00'),
        client_type=ClientType.ALL
    )

    mocker.patch(
        "crm.agency_cabinet.client_bonuses.server.lib.handler.CreateReport",
        return_value=mock,
    )

    return mock


async def test_calls_procedure(handler, procedure):
    input_pb = RpcRequest(
        create_report=PbCreateReportInput(
            agency_id=22,
            name="Отчет по бонусам 1",
            period_from=dt('2020-11-11 00:00:00', as_proto=True),
            period_to=dt('2021-11-11 00:00:00', as_proto=True),
            client_type=PbClientType.ALL_CLIENTS
        )
    )

    await handler(input_pb.SerializeToString())

    procedure.assert_awaited_with(
        params=CreateReportInput(
            agency_id=22,
            name='Отчет по бонусам 1',
            period_from=dt('2020-11-11 00:00:00'),
            period_to=dt('2021-11-11 00:00:00'),
            client_type=ClientType.ALL,
        )
    )


async def test_returns_serialized_operation_result(handler):
    input_pb = RpcRequest(
        create_report=PbCreateReportInput(
            agency_id=22,
            name="Отчет по бонусам 1",
            period_from=dt('2020-11-11 00:00:00', as_proto=True),
            period_to=dt('2021-11-11 00:00:00', as_proto=True),
            client_type=PbClientType.ALL_CLIENTS
        )
    )

    result = await handler(input_pb.SerializeToString())

    assert PbCreateReportOutput.FromString(result) == PbCreateReportOutput(
        result=PbReportInfo(
            id=1,
            name='Отчет по бонусам 1',
            status=ReportsStatuses.requested.value,
            period_from=dt('2020-11-11 00:00:00', as_proto=True),
            period_to=dt('2021-11-11 00:00:00', as_proto=True),
            created_at=dt('2021-11-11 00:00:00', as_proto=True),
            client_type=PbClientType.ALL_CLIENTS
        )
    )
