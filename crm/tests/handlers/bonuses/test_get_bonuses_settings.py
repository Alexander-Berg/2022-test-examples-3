from unittest.mock import AsyncMock

import pytest

from crm.agency_cabinet.client_bonuses.common.structs import ClientBonusSettings
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied

from smb.common.testing_utils import dt

URL = "/api/agencies/123/bonuses/settings"


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = ClientBonusSettings(
        first_date=dt("2020-11-11 00:00:00"),
        last_date=dt("2021-11-11 00:00:00")
    )

    mocker.patch(
        "crm.agency_cabinet.gateway.server.src.procedures.bonuses.GetBonusesSettings",
        return_value=mock,
    )
    return mock


async def test_calls_procedure(client, procedure):
    await client.get(
        URL
    )

    procedure.assert_called_with(
        yandex_uid=42,
        agency_id=123
    )


async def test_returns_clients_bonus_settings(client):
    got = await client.get(URL)

    assert got == {
        "first_date": "2020-11-11T00:00:00+00:00",
        "last_date": "2021-11-11T00:00:00+00:00",
    }


async def test_returns_403_if_access_denied(client, procedure):
    procedure.side_effect = AccessDenied()

    params = {}

    got = await client.get(URL, params=params, expected_status=403)

    assert got == {
        "error": {
            "error_code": "ACCESS_DENIED",
            "http_code": 403,
            "messages": [{"params": {}, "text": "You don't have access"}],
        }
    }
