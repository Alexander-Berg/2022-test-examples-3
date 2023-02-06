from decimal import Decimal
from unittest.mock import AsyncMock

import pytest
from crm.agency_cabinet.documents.common.structs import Payment
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied
from smb.common.testing_utils import dt

URL = "/api/agencies/123/documents/payments"

params = {
    'invoice_id': 1,
    'contract_id': 1,
    'limit': 10,
    'offset': 10,
    'date_to': '2022-09-22T10:10:10.000Z',
    'date_from': '2022-02-22T10:10:10.000Z'
}


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = [
        Payment(
            payment_id=1,
            eid='eid',
            invoice_id=1,
            invoice_eid='1',
            contract_id=1,
            currency='RUR',
            amount=Decimal('66.6'),
            date=dt('2022-3-1 00:00:00'),
        ),
        Payment(
            payment_id=2,
            eid='eid2',
            invoice_id=1,
            invoice_eid='2',
            contract_id=1,
            currency='RUR',
            amount=Decimal('77.7'),
            date=dt('2022-8-1 00:00:00'),
        ),
    ]

    mocker.patch(
        "crm.agency_cabinet.gateway.server.src.procedures.documents.payments.ListPayments",
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
    )


async def test_returns_payments(client):
    got = await client.get(
        URL,
        params=params
    )

    assert got == {
        'items': [
            {
                'payment_id': 1,
                'eid': 'eid',
                'invoice_id': 1,
                'invoice_eid': '1',
                'contract_id': 1,
                'currency': 'RUR',
                'amount': 66.6,
                'date': '2022-03-01T00:00:00+00:00',
            },
            {
                'payment_id': 2,
                'eid': 'eid2',
                'invoice_id': 1,
                'invoice_eid': '2',
                'contract_id': 1,
                'currency': 'RUR',
                'amount': 77.7,
                'date': '2022-08-01T00:00:00+00:00',
            }
        ]
    }


async def test_returns_payments_without_all_params(client):
    got = await client.get(
        URL,
        params={'limit': 5}
    )

    assert got == {
        'items': [
            {
                'payment_id': 1,
                'eid': 'eid',
                'invoice_id': 1,
                'invoice_eid': '1',
                'contract_id': 1,
                'currency': 'RUR',
                'amount': 66.6,
                'date': '2022-03-01T00:00:00+00:00',
            },
            {
                'payment_id': 2,
                'eid': 'eid2',
                'invoice_id': 1,
                'invoice_eid': '2',
                'contract_id': 1,
                'currency': 'RUR',
                'amount': 77.7,
                'date': '2022-08-01T00:00:00+00:00',
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
