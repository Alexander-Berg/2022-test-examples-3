from unittest.mock import AsyncMock

import pytest
from crm.agency_cabinet.documents.common.structs import Agreement
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied
from smb.common.testing_utils import dt

URL = "/api/agencies/123/documents/agreements"

params = {
    'invoice_id': 1,
    'contract_id': 1,
    'limit': 10,
    'offset': 10,
    'date_to': '2022-09-22T10:10:10.000Z',
    'date_from': '2022-02-22T10:10:10.000Z',
    'search_query': 'search_query',
}


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = [
        Agreement(
            agreement_id=1,
            name='Соглашение 1',
            got_scan=True,
            got_original=True,
            date=dt('2023-4-1 00:00:00'),
        ),
        Agreement(
            agreement_id=2,
            name='Соглашение 2',
            got_scan=True,
            got_original=True,
            date=dt('2023-4-1 00:00:00'),
        ),
    ]

    mocker.patch(
        "crm.agency_cabinet.gateway.server.src.procedures.documents.agreements.ListAgreements",
        return_value=mock,
    )
    return mock


async def test_calls_procedure(client, procedure):
    await client.get(
        URL,
        params=params
    )

    procedure.assert_called_with(
        yandex_uid=42,
        agency_id=123,
        contract_id=1,
        limit=10,
        offset=10,
        date_to=dt('2022-09-22 10:10:10'),
        date_from=dt('2022-02-22 10:10:10'),
        search_query='search_query',
    )


async def test_returns_agreements(client):
    got = await client.get(
        URL,
        params=params
    )

    assert got == {
        'items': [
            {
                'agreement_id': 1,
                'name': 'Соглашение 1',
                'got_scan': True,
                'got_original': True,
                'date': dt('2023-4-1 00:00:00'),

            },
            {
                'agreement_id': 2,
                'name': 'Соглашение 2',
                'got_scan': True,
                'got_original': True,
                'date': dt('2023-4-1 00:00:00'),
            }
        ]
    }


async def test_returns_403_if_access_denied(client, procedure):
    procedure.side_effect = AccessDenied()

    got = await client.get(
        URL,
        params=params,
        expected_status=403
    )

    assert got == {
        "error": {
            "error_code": "ACCESS_DENIED",
            "http_code": 403,
            "messages": [{"params": {}, "text": "You don't have access"}],
        }
    }
