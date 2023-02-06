from crm.agency_cabinet.common.consts import ReportsStatuses
from unittest.mock import AsyncMock

import pytest

from crm.agency_cabinet.client_bonuses.common.structs import ReportInfo, ClientType
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied
from smb.common.testing_utils import dt


URL = "/api/agencies/123/bonuses/reports"

request_json = {
    'name': 'Отчет по бонусам 1',
    'period_from': '2021-03-01T00:00:00.000Z',
    'period_to': '2021-06-01T00:00:00.000Z',
    'client_type': 'ALL',
}


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = ReportInfo(
        id=1,
        name='Отчет по бонусам 1',
        status=ReportsStatuses.requested.value,
        period_from=dt('2021-3-1 00:00:00'),
        period_to=dt('2021-6-1 00:00:00'),
        created_at=dt('2021-7-1 00:00:00'),
        client_type=ClientType.ALL,
    )

    mocker.patch(
        "crm.agency_cabinet.gateway.server.src.procedures.bonuses.CreateReport",
        return_value=mock,
    )
    return mock


async def test_calls_procedure(client, procedure):
    await client.post(URL, json=request_json)

    procedure.assert_called_with(
        yandex_uid=42,
        agency_id=123,
        report_json=dict(
            name='Отчет по бонусам 1',
            period_from=dt('2021-3-1 00:00:00'),
            period_to=dt('2021-6-1 00:00:00'),
            client_type=ClientType.ALL,
        )
    )


async def test_creates_bonuses_report(client):
    got = await client.post(URL, json=request_json)

    assert got == {
        "id": 1,
        "name": "Отчет по бонусам 1",
        "status": "requested",
        "period_from": '2021-03-01T00:00:00+00:00',
        "period_to": '2021-06-01T00:00:00+00:00',
        "created_at": '2021-07-01T00:00:00+00:00',
        "client_type": "ALL",
    }


async def test_returns_403_if_access_denied(client, procedure):
    procedure.side_effect = AccessDenied()

    got = await client.post(URL, json=request_json)

    assert got == {
        "error": {
            "error_code": "ACCESS_DENIED",
            "http_code": 403,
            "messages": [{"params": {}, "text": "You don't have access"}],
        }
    }
