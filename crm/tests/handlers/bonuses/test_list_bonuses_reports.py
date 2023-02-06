from crm.agency_cabinet.common.consts import ReportsStatuses
from unittest.mock import AsyncMock

import pytest

from crm.agency_cabinet.client_bonuses.common.structs import ReportInfo, ClientType
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied
from smb.common.testing_utils import dt

URL = "/api/agencies/123/bonuses/reports"


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = [
        ReportInfo(
            id=1,
            name='Отчет по бонусам 1',
            status=ReportsStatuses.ready.value,
            period_from=dt('2021-3-1 00:00:00'),
            period_to=dt('2021-6-1 00:00:00'),
            created_at=dt('2021-7-1 00:00:00'),
            client_type=ClientType.ACTIVE,
        ),
        ReportInfo(
            id=2,
            name='Отчет по бонусам 2',
            status=ReportsStatuses.in_progress.value,
            period_from=dt('2021-3-1 00:00:00'),
            period_to=dt('2021-6-1 00:00:00'),
            created_at=dt('2021-7-1 00:00:00'),
            client_type=ClientType.ACTIVE,
        ),
    ]

    mocker.patch(
        "crm.agency_cabinet.gateway.server.src.procedures.bonuses.ListBonusesReports",
        return_value=mock,
    )
    return mock


async def test_calls_procedure(client, procedure):
    await client.get(URL)

    procedure.assert_called_with(
        yandex_uid=42,
        agency_id=123
    )


async def test_returns_clients_bonuses(client):
    got = await client.get(URL)

    assert got == [
        {
            "id": 1,
            "name": "Отчет по бонусам 1",
            "status": "ready",
            "period_from": '2021-03-01T00:00:00+00:00',
            "period_to": '2021-06-01T00:00:00+00:00',
            "created_at": '2021-07-01T00:00:00+00:00',
            "client_type": "ACTIVE",
        },
        {
            "id": 2,
            "name": "Отчет по бонусам 2",
            "status": "in_progress",
            "period_from": '2021-03-01T00:00:00+00:00',
            "period_to": '2021-06-01T00:00:00+00:00',
            "created_at": '2021-07-01T00:00:00+00:00',
            "client_type": "ACTIVE",
        },
    ]


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
