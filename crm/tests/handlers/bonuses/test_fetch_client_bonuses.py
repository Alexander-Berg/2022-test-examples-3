from decimal import Decimal
from unittest.mock import AsyncMock
from datetime import datetime, timezone

import mock
import pytest

from crm.agency_cabinet.client_bonuses.common.structs import BonusType, ClientBonus, ClientType
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied

# "/api/agencies/{agency_id}/bonuses/{client_id}"
URL = "/api/agencies/{agency_id}/bonuses/{client_id}"


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
            client_id=123,
            email="alpaca1@yandex.ru",
            accrued=Decimal("50.5"),
            spent=Decimal("20"),
            awarded=Decimal("200"),
            active=True,
            currency="RUR",
        ),
    ]

    mocker.patch(
        "crm.agency_cabinet.gateway.server.src.procedures.bonuses.ListBonuses",
        return_value=mock,
    )
    return mock


async def test_calls_procedure(client, procedure):
    await client.get(
        URL.format(agency_id=123, client_id=1),
    )

    procedure.assert_called_with(
        yandex_uid=42,
        agency_id=123,
        client_type=ClientType.ALL,
        bonus_type=BonusType.ALL,
        limit=0,
        offset=0,
        datetime_start=datetime.min.replace(tzinfo=timezone.utc),
        datetime_end=mock.ANY,
        search_query='1',
    )


async def test_returns_clients_bonuses(client):
    got = await client.get(
        URL.format(agency_id=123, client_id=1),
    )

    assert got == {
        "client_id": "1",
        "email": "alpaca@yandex.ru",
        "accumulated": "50.5",
        "spent": "20",
        "awarded": "200",
        "active": True,
        "currency": "RUR",
    }


async def test_returns_403_if_access_denied(client, procedure):
    procedure.side_effect = AccessDenied()

    got = await client.get(URL.format(agency_id=123, client_id=1), expected_status=403)

    assert got == {
        "error": {
            "error_code": "ACCESS_DENIED",
            "http_code": 403,
            "messages": [{"params": {}, "text": "You don't have access"}],
        }
    }
