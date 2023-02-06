from decimal import Decimal

import pytest
from smb.common.testing_utils import dt

from crm.agency_cabinet.client_bonuses.client import ClientNotFound
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
from crm.agency_cabinet.client_bonuses.proto.errors_pb2 import (
    ClientNotFound as PbClientNotFound,
)
from crm.agency_cabinet.client_bonuses.proto.request_pb2 import RpcRequest

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def pb_output_data():
    return PbFetchClientBonusesGraphOutput(
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


async def test_sends_correct_request(client, rmq_rpc_client, pb_output_data):
    rmq_rpc_client.send_proto_message.return_value = pb_output_data

    await client.fetch_client_graph(agency_id=123, client_id=567)

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name="client-bonuses",
        message=RpcRequest(
            fetch_client_bonuses_graph=PbFetchClientBonusesGraphInput(
                agency_id=123, client_id=567
            )
        ),
        response_message_type=PbFetchClientBonusesGraphOutput,
    )


async def test_returns_data(client, rmq_rpc_client, pb_output_data):
    rmq_rpc_client.send_proto_message.return_value = pb_output_data

    got = await client.fetch_client_graph(agency_id=123, client_id=567)

    assert got == ClientGraph(
        bonuses_available=Decimal("1000"),
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


async def test_raises_if_client_not_found(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = PbFetchClientBonusesGraphOutput(
        client_not_found=PbClientNotFound(client_id=567, agency_id=123)
    )

    with pytest.raises(ClientNotFound) as exc:
        await client.fetch_client_graph(agency_id=123, client_id=567)

    assert exc.value.agency_id == 123
    assert exc.value.client_id == 567
