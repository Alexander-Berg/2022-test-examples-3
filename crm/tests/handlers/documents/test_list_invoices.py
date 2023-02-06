import pytest
from decimal import Decimal
from unittest.mock import AsyncMock

from smb.common.testing_utils import dt

from crm.agency_cabinet.documents.common.structs import Invoice, InvoiceStatus
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied

URL = '/api/agencies/123/documents/invoices'


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = [
        Invoice(
            invoice_id=1,
            eid='test1',
            contract_id=1234,
            contract_eid='test_contract_eid',
            amount=Decimal(12),
            currency='RUB',
            status=InvoiceStatus.paid,
            date=dt('2021-4-1 00:00:00'),
            payment_date=dt('2021-4-10 00:00:00'),
            has_facture=False
        ),

        Invoice(
            invoice_id=2,
            eid='test2',
            contract_id=1234,
            contract_eid='test_contract_eid',
            amount=Decimal(13),
            currency='RUB',
            status=InvoiceStatus.not_paid,
            date=dt('2021-5-1 00:00:00'),
            payment_date=None,
            has_facture=False
        )
    ]

    mocker.patch(
        'crm.agency_cabinet.gateway.server.src.procedures.documents.invoices.ListInvoices',
        return_value=mock,
    )
    return mock


async def test_calls_procedure(client, procedure):
    await client.get(URL)

    procedure.assert_called_with(
        yandex_uid=42,
        agency_id=123,
    )


async def test_returns_invoices(client):
    got = await client.get(URL)

    assert got == {
        'items': [
            {
                'invoice_id': 1,
                'eid': 'test1',
                'contract_id': 1234,
                'contract_eid': 'test_contract_eid',
                'amount': 12.0,
                'currency': 'RUB',
                'status': 'paid',
                'date': '2021-04-01T00:00:00+00:00',
                'payment_date': '2021-04-10T00:00:00+00:00',
                'has_facture': False,
                'has_factura': False
            },
            {
                'invoice_id': 2,
                'eid': 'test2',
                'contract_id': 1234,
                'contract_eid': 'test_contract_eid',
                'amount': 13.0,
                'currency': 'RUB',
                'status': 'not_paid',
                'date': '2021-05-01T00:00:00+00:00',
                'payment_date': None,
                'has_facture': False,
                'has_factura': False
            },
        ]}


async def test_returns_403_if_access_denied(client, procedure):
    procedure.side_effect = AccessDenied()

    got = await client.get(URL, expected_status=403)

    assert got == {
        'error': {
            'error_code': 'ACCESS_DENIED',
            'http_code': 403,
            'messages': [{'params': {}, 'text': "You don't have access"}],
        }
    }
