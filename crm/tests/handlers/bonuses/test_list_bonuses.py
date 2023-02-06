from decimal import Decimal
from unittest.mock import AsyncMock
from datetime import datetime, timezone

import pytest

from crm.agency_cabinet.client_bonuses.common.structs import BonusType, ClientBonus, ClientType
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied

# "/api/agencies/{agency_id}/bonuses"
URL = "/api/agencies/123/bonuses"


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = [
        ClientBonus(
            client_id=1,
            email="alpaca@yandex.ru",
            accrued=Decimal("50.5"),
            spent=Decimal("20"),
            awarded=Decimal("200"),
            active=True,
            currency="RUR",
        ),
        ClientBonus(
            client_id=2,
            email="capibara@yandex.ru",
            accrued=None,
            spent=None,
            awarded=None,
            active=False,
            currency="RUR",
        ),
    ]

    mocker.patch(
        "crm.agency_cabinet.gateway.server.src.procedures.bonuses.ListBonuses",
        return_value=mock,
    )
    return mock


@pytest.mark.parametrize("search_query", [dict(), dict(search_query="something")])
async def test_calls_procedure(search_query, client, procedure):
    await client.get(
        URL,
        params={
            "limit": 50,
            "offset": 10,
            "client_type": "ALL",
            "bonus_type": "ALL",
            "datetime_start": "2021-01-05T14:48:00.000Z",
            "datetime_end": "2021-02-05T14:48:00.000Z",
            **search_query,
        },
    )

    procedure.assert_called_with(
        yandex_uid=42,
        agency_id=123,
        client_type=ClientType.ALL,
        bonus_type=BonusType.ALL,
        limit=50,
        offset=10,
        datetime_start=datetime(2021, 1, 5, 14, 48, 00, tzinfo=timezone.utc),
        datetime_end=datetime(2021, 2, 5, 14, 48, 00, tzinfo=timezone.utc),
        search_query=search_query.get("search_query"),
    )


async def test_returns_clients_bonuses(client):
    got = await client.get(
        URL.format(agency_id=1),
        params={
            "limit": 50,
            "offset": 10,
            "client_type": "ALL",
            "bonus_type": "ALL",
            "datetime_start": "2021-01-05T14:48:00.000Z",
            "datetime_end": "2021-02-05T14:48:00.000Z",
            "search_query": "something",
        },
    )

    assert got == [
        {
            "client_id": "1",
            "email": "alpaca@yandex.ru",
            "accumulated": "50.5",
            "spent": "20",
            "awarded": "200",
            "active": True,
            "currency": "RUR",
        },
        {
            "client_id": "2",
            "email": "capibara@yandex.ru",
            "accumulated": None,
            "spent": None,
            "awarded": None,
            "active": False,
            "currency": "RUR",
        },
    ]


@pytest.mark.parametrize(
    "query_param",
    ["limit", "offset", "client_type", "bonus_type", "datetime_start", "datetime_end"],
)
async def test_returns_403_if_required_param_is_missing(client, query_param):
    params = {
        "limit": 100,
        "offset": 0,
        "client_type": "ALL",
        "bonus_type": "ALL",
        "datetime_start": "2021-01-05T14:48:00.000Z",
        "datetime_end": "2021-05-01T18:00:00.000Z",
        "search_query": "something",
    }
    params.pop(query_param)

    got = await client.get(URL, params=params, expected_status=422)

    assert got == {
        "error": {
            "error_code": "VALIDATION_ERROR",
            "http_code": 422,
            "messages": [
                {
                    "params": {},
                    "text": f"{query_param}: Missing data for required field.",
                }
            ],
        }
    }


@pytest.mark.parametrize(
    ["incorrect_param", "expected_text"],
    [
        # negative limit
        (
            {"limit": -1},
            "limit: Must be greater than or equal to 1 and less than or equal to 100.",
        ),
        # limit is too large
        (
            {"limit": 10000},
            "limit: Must be greater than or equal to 1 and less than or equal to 100.",
        ),
        # negative offset
        ({"offset": -1}, "offset: Must be greater than or equal to 0."),
        # start date must be gt end date
        (
            {
                "datetime_start": "2021-01-05T14:48:00.000Z",
                "datetime_end": "2020-05-01T18:00:00.000Z",
            },
            "_schema: Start datetime must be grater or equal end datetime.",
        ),
        # datetime must be in aware
        (
            {
                "datetime_start": "2021-01-05T14:48:00.000",
                "datetime_end": "2020-05-01T18:00:00.000Z",
            },
            "datetime_start: Not a valid aware datetime.",
        ),
        (
            {"search_query": ""},
            "search_query: Shorter than minimum length 1.",
        ),
    ],
)
async def test_validates_input_data(client, incorrect_param, expected_text):
    params = {
        "client_type": "ALL",
        "bonus_type": "ALL",
        "limit": 100,
        "offset": 0,
        "datetime_start": "2021-01-05T14:48:00.000Z",
        "datetime_end": "2021-05-01T18:00:00.000Z",
        "search_query": "something",
    }
    params.update(incorrect_param)

    got = await client.get(URL, params=params, expected_status=422)

    assert got == {
        "error": {
            "error_code": "VALIDATION_ERROR",
            "http_code": 422,
            "messages": [{"params": {}, "text": expected_text}],
        }
    }


async def test_returns_403_if_access_denied(client, procedure):
    procedure.side_effect = AccessDenied()

    params = {
        "limit": 50,
        "offset": 10,
        "client_type": "ALL",
        "bonus_type": "ALL",
        "datetime_start": "2021-01-05T14:48:00.000Z",
        "datetime_end": "2021-01-05T14:48:00.000Z",
        "search_query": "33353",
    }

    got = await client.get(URL, params=params, expected_status=403)

    assert got == {
        "error": {
            "error_code": "ACCESS_DENIED",
            "http_code": 403,
            "messages": [{"params": {}, "text": "You don't have access"}],
        }
    }
