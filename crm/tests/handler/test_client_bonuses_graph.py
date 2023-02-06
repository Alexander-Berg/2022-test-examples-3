from decimal import Decimal
from unittest.mock import AsyncMock

import pytest
from smb.common.testing_utils import dt

from crm.agency_cabinet.client_bonuses.common.structs import (
    ClientGraph,
    GraphPoint,
    ProgramBonusesGraph,
)
from crm.agency_cabinet.client_bonuses.proto.bonuses_pb2 import (
    ClientGraph as PbClientGraph,
    FetchClientBonusesGraphInput as PbFetchClientBonusesGraphInput,
    FetchClientBonusesGraphOutput as PbFetchClientBonusesGraphOutput,
    GraphPoint as PbGraphPoint,
    ProgramBonusesGraph as PbProgramBonusesGraph,
)
from crm.agency_cabinet.client_bonuses.proto.request_pb2 import RpcRequest

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = ClientGraph(
        bonuses_available=Decimal(1000),
        overall_spent=[
            GraphPoint(point=dt("2021-10-01 00:00:00"), value=Decimal("100.500")),
            GraphPoint(point=dt("2021-11-01 00:00:00"), value=Decimal("200")),
        ],
        overall_accrued=[
            GraphPoint(point=dt("2021-10-01 00:00:00"), value=Decimal("100.500")),
            GraphPoint(point=dt("2021-11-01 00:00:00"), value=Decimal("200")),
        ],
        programs=[
            ProgramBonusesGraph(
                program_id=1,
                historical_monthly_data=[
                    GraphPoint(
                        point=dt("2021-10-01 00:00:00"), value=Decimal("100.500")
                    ),
                    GraphPoint(point=dt("2021-11-01 00:00:00"), value=Decimal("200")),
                ],
            )
        ],
    )

    mocker.patch(
        "crm.agency_cabinet.client_bonuses.server.lib.handler.FetchClientBonusesGraph",
        return_value=mock,
    )

    return mock


async def test_calls_procedure(handler, procedure):
    input_pb = RpcRequest(
        fetch_client_bonuses_graph=PbFetchClientBonusesGraphInput(
            agency_id=22, client_id=42
        )
    )

    await handler(input_pb.SerializeToString())

    procedure.assert_awaited_with(agency_id=22, client_id=42)


async def test_returns_serialized_operation_result(handler):
    input_pb = RpcRequest(
        fetch_client_bonuses_graph=PbFetchClientBonusesGraphInput(
            agency_id=22, client_id=42
        )
    )

    result = await handler(input_pb.SerializeToString())

    assert PbFetchClientBonusesGraphOutput.FromString(
        result
    ) == PbFetchClientBonusesGraphOutput(
        details=PbClientGraph(
            bonuses_available="1000",
            overall_spent=[
                PbGraphPoint(
                    point=dt("2021-10-01 00:00:00", as_proto=True), value="100.5"
                ),
                PbGraphPoint(
                    point=dt("2021-11-01 00:00:00", as_proto=True), value="200"
                ),
            ],
            overall_accrued=[
                PbGraphPoint(
                    point=dt("2021-10-01 00:00:00", as_proto=True), value="100.5"
                ),
                PbGraphPoint(
                    point=dt("2021-11-01 00:00:00", as_proto=True), value="200"
                ),
            ],
            programs=[
                PbProgramBonusesGraph(
                    program_id=1,
                    historical_monthly_data=[
                        PbGraphPoint(
                            point=dt("2021-10-01 00:00:00", as_proto=True),
                            value="100.5",
                        ),
                        PbGraphPoint(
                            point=dt("2021-11-01 00:00:00", as_proto=True), value="200"
                        ),
                    ],
                ),
            ],
        )
    )
