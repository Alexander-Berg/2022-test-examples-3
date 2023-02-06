from datetime import datetime, timezone
from decimal import Decimal
from unittest.mock import AsyncMock

import pytest

from crm.agency_cabinet.client_bonuses.common.structs import (
    BonusAmount,
    BonusDetails,
    BonusStatusType,
)
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied, NotFound

# /api/agencies/{agency_id}/bonuses/{client_id}/history_log
URL = "/api/agencies/62342/bonuses/11111/history_log"


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = (
        [
            BonusDetails(
                type=BonusStatusType.spent,
                amounts=[],
                total=Decimal("500"),
                date=datetime(2021, 3, 1, 00, 00, 00, tzinfo=timezone.utc),
            ),
            BonusDetails(
                type=BonusStatusType.accrued,
                amounts=[
                    BonusAmount(program_id=1, amount=Decimal("100.20")),
                    BonusAmount(program_id=2, amount=Decimal("10.20")),
                ],
                total=Decimal(2_000_000_000),
                date=datetime(2021, 2, 1, 00, 00, 00, tzinfo=timezone.utc),
            ),
        ],
    )

    mocker.patch(
        "crm.agency_cabinet.gateway.server.src.procedures.bonuses.FetchClientBonusesDetails",
        return_value=mock,
    )

    return mock


@pytest.mark.parametrize(
    "pagination_params",
    [
        {"limit": 100, "offset": 0},
        {"limit": 1, "offset": 50},
    ],
)
@pytest.mark.parametrize(
    "history_log, expected_history_log",
    [
        ([], []),
        (
            [
                BonusDetails(
                    type=BonusStatusType.spent,
                    amounts=[],
                    total=Decimal("500"),
                    date=datetime(2021, 3, 1, 00, 00, 00, tzinfo=timezone.utc),
                ),
                BonusDetails(
                    type=BonusStatusType.accrued,
                    amounts=[
                        BonusAmount(program_id=1, amount=Decimal("200")),
                        BonusAmount(program_id=2, amount=Decimal("400")),
                    ],
                    total=Decimal(2_000_000_000),
                    date=datetime(2021, 1, 1, 00, 00, 00, tzinfo=timezone.utc),
                ),
            ],
            [
                {
                    "type": "spent",
                    "amounts": [],
                    "total": "500",
                    "date": "2021-03-01T00:00:00+00:00",
                },
                {
                    "type": "accrued",
                    "amounts": [
                        {"program_id": 1, "amount": "200"},
                        {"program_id": 2, "amount": "400"},
                    ],
                    "total": "2000000000",
                    "date": "2021-01-01T00:00:00+00:00",
                },
            ],
        ),
    ],
)
async def test_returns_history_log(
    client, procedure, pagination_params, history_log, expected_history_log
):
    procedure.return_value = history_log

    query_prams = {
        "datetime_start": "2021-01-05T14:48:00.000Z",
        "datetime_end": "2021-05-01T18:00:00.000Z",
        **pagination_params,
    }
    got = await client.get(URL, params=query_prams, expected_status=200)

    assert got == expected_history_log


async def test_calls_procedure(client, procedure):
    query_prams = {
        "datetime_start": "2021-01-05T14:48:00.000Z",
        "datetime_end": "2021-05-01T18:00:00.000Z",
    }
    await client.get(URL, params=query_prams, expected_status=200)

    procedure.assert_called_with(
        yandex_uid=42,
        agency_id=62342,
        client_id=11111,
        datetime_start=datetime(2021, 1, 5, 14, 48, 00, tzinfo=timezone.utc),
        datetime_end=datetime(2021, 5, 1, 18, 00, 00, tzinfo=timezone.utc),
    )


async def test_returns_404_if_data_not_found(client, procedure):
    procedure.side_effect = NotFound()

    query_prams = {
        "datetime_start": "2021-01-05T14:48:00.000Z",
        "datetime_end": "2021-05-01T18:00:00.000Z",
    }

    got = await client.get(URL, params=query_prams, expected_status=404)

    assert got == {
        "error": {
            "error_code": "NOT_FOUND",
            "http_code": 404,
            "messages": [{"params": {}, "text": "Object is not found"}],
        }
    }


async def test_returns_403_if_access_denied(client, procedure):
    procedure.side_effect = AccessDenied()

    query_prams = {
        "datetime_start": "2021-01-05T14:48:00.000Z",
        "datetime_end": "2021-05-01T18:00:00.000Z",
    }

    got = await client.get(URL, params=query_prams, expected_status=403)

    assert got == {
        "error": {
            "error_code": "ACCESS_DENIED",
            "http_code": 403,
            "messages": [{"params": {}, "text": "You don't have access"}],
        }
    }


@pytest.mark.parametrize(
    "query_prams, expected_text",
    [
        # start date must be gt end date
        (
            {
                "datetime_start": "2021-01-05T14:48:00.000Z",
                "datetime_end": "2020-05-01T18:00:00.000Z",
            },
            "_schema: Start datetime must be grater or equal end datetime.",
        ),
        # datetime must be aware
        (
            {
                "datetime_start": "2021-01-05T14:48:00.000",
                "datetime_end": "2020-05-01T18:00:00.000Z",
            },
            "datetime_start: Not a valid aware datetime.",
        ),
        (
            {
                "datetime_start": "2021-01-05T14:48:00.000Z",
                "datetime_end": "2020-05-01T18:00:00.000",
            },
            "datetime_end: Not a valid aware datetime.",
        ),
    ],
)
async def test_validates_input_data(client, query_prams, expected_text):
    got = await client.get(URL, params=query_prams, expected_status=422)

    assert got == {
        "error": {
            "error_code": "VALIDATION_ERROR",
            "http_code": 422,
            "messages": [{"params": {}, "text": expected_text}],
        }
    }
