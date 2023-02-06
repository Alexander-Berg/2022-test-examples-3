from unittest.mock import AsyncMock

import pytest
from crm.agency_cabinet.client_bonuses.common.structs import DeleteReportOutput
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied
from crm.agency_cabinet.gateway.server.src.exceptions import InternalServerError

URL = "/api/agencies/123/bonuses/reports/1/delete"


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = DeleteReportOutput(is_deleted=True)

    mocker.patch(
        "crm.agency_cabinet.gateway.server.src.procedures.bonuses.DeleteReport",
        return_value=mock,
    )
    return mock


async def test_calls_procedure(client, procedure):
    await client.post(URL, json={})

    procedure.assert_called_with(
        yandex_uid=42,
        agency_id=123,
        report_id=1
    )


async def test_deletes_bonuses_report(client):
    got = await client.post(URL, json={})

    assert got == {'status': 'ok'}


async def test_returns_403_if_access_denied(client, procedure):
    procedure.side_effect = AccessDenied()

    got = await client.post(URL, json={})

    assert got == {
        "error": {
            "error_code": "ACCESS_DENIED",
            "http_code": 403,
            "messages": [{"params": {}, "text": "You don't have access"}],
        }
    }


async def test_returns_internal_error_if_report_not_found(client, procedure):
    procedure.side_effect = InternalServerError('Unsuccessful report delete operation')

    got = await client.post(URL, json={})

    assert got == {
        'error': {
            'error_code': 'INTERNAL_SERVER_ERROR',
            'http_code': 500,
            'messages': [{'params': {}, 'text': 'Unsuccessful report delete operation'}]
        }
    }
