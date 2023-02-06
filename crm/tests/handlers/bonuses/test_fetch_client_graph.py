from decimal import Decimal
from unittest.mock import AsyncMock

import pytest
from smb.common.testing_utils import dt

from crm.agency_cabinet.client_bonuses.common.structs import (
    ClientGraph,
    GraphPoint,
    ProgramBonusesGraph,
)
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied, NotFound


from crm.agency_cabinet.common.testing import BaseTestClient

URL = "/api/agencies/22/bonuses/42/graph"


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = ClientGraph(
        bonuses_available=Decimal("1000.000000"),
        overall_spent=[
            GraphPoint(point=dt("2021-10-01 00:00:00"), value=Decimal("200.000000")),
            GraphPoint(point=dt("2021-11-01 00:00:00"), value=Decimal("400.000000")),
        ],
        overall_accrued=[
            GraphPoint(point=dt("2021-10-01 00:00:00"), value=Decimal("200.000000")),
            GraphPoint(point=dt("2021-11-01 00:00:00"), value=Decimal("600.000000")),
        ],
        programs=[
            ProgramBonusesGraph(
                program_id=1,
                historical_monthly_data=[
                    GraphPoint(
                        point=dt("2021-10-01 00:00:00"), value=Decimal("200.000000")
                    ),
                    GraphPoint(
                        point=dt("2021-11-01 00:00:00"), value=Decimal("400.000000")
                    ),
                ],
            ),
            ProgramBonusesGraph(
                program_id=2,
                historical_monthly_data=[
                    GraphPoint(
                        point=dt("2021-11-01 00:00:00"), value=Decimal("200.000000")
                    )
                ],
            ),
        ],
    )

    mocker.patch(
        "crm.agency_cabinet.gateway.server.src.procedures.bonuses.FetchClientBonusesGraph",
        return_value=mock,
    )
    return mock


async def test_calls_procedure(client: BaseTestClient, procedure):
    await client.get(URL, expected_status=200)

    procedure.assert_called_with(yandex_uid=42, agency_id=22, client_id=42)


async def test_returns_client_graph(client: BaseTestClient):
    got = await client.get(URL, expected_status=200)

    assert got == {
        "bonuses_available": "1000.000000",
        "overall_accrued": {
            "2021-10-01T00:00:00+00:00": "200.000000",
            "2021-11-01T00:00:00+00:00": "600.000000",
        },
        "overall_spent": {
            "2021-10-01T00:00:00+00:00": "200.000000",
            "2021-11-01T00:00:00+00:00": "400.000000",
        },
        "programs": [
            {
                "historical_monthly_data": {
                    "2021-10-01T00:00:00+00:00": "200.000000",
                    "2021-11-01T00:00:00+00:00": "400.000000",
                },
                "program_id": 1,
            },
            {
                "historical_monthly_data": {"2021-11-01T00:00:00+00:00": "200.000000"},
                "program_id": 2,
            },
        ],
    }


async def test_returns_403_if_access_denied(client, procedure):
    procedure.side_effect = AccessDenied()

    got = await client.get(URL, expected_status=403)

    assert got == {
        "error": {
            "error_code": "ACCESS_DENIED",
            "http_code": 403,
            "messages": [{"params": {}, "text": "You don't have access"}],
        }
    }


async def test_returns_404_if_data_not_found(client, procedure):
    procedure.side_effect = NotFound()

    got = await client.get(URL, expected_status=404)

    assert got == {
        "error": {
            "error_code": "NOT_FOUND",
            "http_code": 404,
            "messages": [{"params": {}, "text": "Object is not found"}],
        }
    }
