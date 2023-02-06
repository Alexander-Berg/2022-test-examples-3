from decimal import Decimal
from unittest.mock import AsyncMock

import pytest
from crm.agency_cabinet.documents.common.structs import Act
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied
from smb.common.testing_utils import dt

URL = "/api/agencies/123/documents/acts"

params = {
    'invoice_id': 1,
    'contract_id': 1,
    'limit': 10,
    'offset': 10,
    'date_to': '2022-09-22T10:10:10.000Z',
    'date_from': '2022-02-22T10:10:10.000Z',
    'search_query': 'search_query',
}

bad_params = {
    'invoice_id': 1,
    'contract_id': 1,
    'limit': 10,
    'offset': 10,
    'date_to': '2022-02-22T10:10:10.000Z',
    'date_from': '2022-09-22T10:10:10.000Z',
    'search_query': 'search_query',
}


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = [
        Act(
            act_id=1,
            invoice_id=1,
            contract_id=1,
            currency='RUR',
            amount=Decimal('66.6'),
            date=dt('2022-3-1 00:00:00'),
            eid='test_eid',
            contract_eid='test_contract_eid',
            invoice_eid='test_invoice_eid',
        ),
        Act(
            act_id=2,
            invoice_id=1,
            contract_id=1,
            currency='RUR',
            amount=Decimal('77.7'),
            date=dt('2022-8-1 00:00:00'),
            eid='test_eid',
            contract_eid='test_contract_eid',
            invoice_eid='test_invoice_eid',
        ),
    ]

    mocker.patch(
        "crm.agency_cabinet.gateway.server.src.procedures.documents.acts.ListActs",
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
        invoice_id=1,
        contract_id=1,
        limit=10,
        offset=10,
        date_to=dt('2022-09-22 10:10:10'),
        date_from=dt('2022-02-22 10:10:10'),
        search_query='search_query',
    )


async def test_returns_acts(client):
    got = await client.get(
        URL,
        params=params
    )

    assert got == {
        'items': [
            {
                'act_id': 1,
                'invoice_id': 1,
                'contract_id': 1,
                'currency': 'RUR',
                'amount': 66.6,
                'date': '2022-03-01T00:00:00+00:00',
                'eid': 'test_eid',
                'contract_eid': 'test_contract_eid',
                'invoice_eid': 'test_invoice_eid',
            },
            {
                'act_id': 2,
                'invoice_id': 1,
                'contract_id': 1,
                'currency': 'RUR',
                'amount': 77.7,
                'date': '2022-08-01T00:00:00+00:00',
                'eid': 'test_eid',
                'contract_eid': 'test_contract_eid',
                'invoice_eid': 'test_invoice_eid',
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


async def test_validation_error(client):
    got = await client.get(
        URL,
        params=bad_params
    )

    assert got == {'error': {'error_code': 'VALIDATION_ERROR',
                             'http_code': 422,
                             'messages': [{'params': {},
                                           'text': '_schema: Date to must be grater or equal '
                                           'date from.'}]}}
