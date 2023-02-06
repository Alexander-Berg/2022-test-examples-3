from unittest.mock import AsyncMock

import pytest
from crm.agency_cabinet.client_bonuses.common.structs import (
    ClientType,
    ReportInfo,
    ListBonusesReportsInfoInput,
)
from crm.agency_cabinet.client_bonuses.proto.reports_pb2 import (
    ReportInfo as PbReportInfo,
    BonusesReportsInfoList as PbBonusesReportsInfoList,
    ListBonusesReportsInfoInput as PbListBonusesReportsInfoInput,
    ListBonusesReportsInfoOutput as PbListBonusesReportsInfoOutput,
)

from crm.agency_cabinet.client_bonuses.proto.bonuses_pb2 import ClientType as PbClientType

from crm.agency_cabinet.client_bonuses.proto.request_pb2 import RpcRequest
from crm.agency_cabinet.common.consts import ReportsStatuses
from smb.common.testing_utils import dt

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = [
        ReportInfo(
            id=1,
            name='Отчет по бонусам 1',
            status=ReportsStatuses.ready.value,
            period_from=dt('2021-03-01 00:00:00'),
            period_to=dt('2021-06-01 00:00:00'),
            created_at=dt('2021-07-01 00:00:00'),
            client_type=ClientType.EXCLUDED
        ),
        ReportInfo(
            id=2,
            name='Отчет по бонусам 2',
            status=ReportsStatuses.in_progress.value,
            period_from=dt('2021-03-01 00:00:00'),
            period_to=dt('2021-06-01 00:00:00'),
            created_at=dt('2021-07-01 00:00:00'),
            client_type=ClientType.EXCLUDED
        ),
    ]

    mocker.patch(
        "crm.agency_cabinet.client_bonuses.server.lib.handler.ListBonusesReportsInfo",
        return_value=mock,
    )

    return mock


async def test_calls_procedure(handler, procedure):
    input_pb = RpcRequest(
        list_bonuses_reports=PbListBonusesReportsInfoInput(
            agency_id=22
        )
    )

    await handler(input_pb.SerializeToString())

    procedure.assert_awaited_with(
        params=ListBonusesReportsInfoInput(
            agency_id=22
        )
    )


async def test_returns_serialized_operation_result(handler):
    input_pb = RpcRequest(
        list_bonuses_reports=PbListBonusesReportsInfoInput(
            agency_id=22
        )
    )

    result = await handler(input_pb.SerializeToString())

    assert PbListBonusesReportsInfoOutput.FromString(result) == PbListBonusesReportsInfoOutput(
        reports=PbBonusesReportsInfoList(
            reports=[
                PbReportInfo(
                    id=1,
                    name='Отчет по бонусам 1',
                    status=ReportsStatuses.ready.value,
                    period_from=dt('2021-03-01 00:00:00', as_proto=True),
                    period_to=dt('2021-06-01 00:00:00', as_proto=True),
                    created_at=dt('2021-07-01 00:00:00', as_proto=True),
                    client_type=PbClientType.EXCLUDED
                ),
                PbReportInfo(
                    id=2,
                    name='Отчет по бонусам 2',
                    status=ReportsStatuses.in_progress.value,
                    period_from=dt('2021-03-01 00:00:00', as_proto=True),
                    period_to=dt('2021-06-01 00:00:00', as_proto=True),
                    created_at=dt('2021-07-01 00:00:00', as_proto=True),
                    client_type=PbClientType.EXCLUDED
                ),
            ]
        )
    )
