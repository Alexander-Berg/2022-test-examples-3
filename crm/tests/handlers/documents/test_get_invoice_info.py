import pytest
from decimal import Decimal
from unittest.mock import AsyncMock

from smb.common.testing_utils import dt

from crm.agency_cabinet.documents.common.structs import DetailedInvoice, InvoiceStatus, Facture
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied


URL = "/api/agencies/123/documents/invoices/1"


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = DetailedInvoice(
        invoice_id=1,
        eid='test1',
        contract_id=1234,
        contract_eid='test_contract_eid',
        amount=Decimal(12),
        currency='RUB',
        status=InvoiceStatus.paid,
        date=dt('2021-4-1 00:00:00'),
        payment_date=dt('2021-4-10 00:00:00'),
        facture=Facture(
            facture_id=1,
            amount=Decimal(12),
            amount_with_nds=Decimal(13),
            nds=Decimal(1),
            date=dt('2021-4-1 00:00:00'),
            currency='RUB'
        )
    )

    mocker.patch(
        "crm.agency_cabinet.gateway.server.src.procedures.documents.invoices.GetInvoiceInfo",
        return_value=mock,
    )
    return mock


async def test_calls_procedure(client, procedure):
    await client.get(URL)

    procedure.assert_called_with(
        yandex_uid=42,
        agency_id=123,
        invoice_id=1,
    )


async def test_returns_invoices(client):
    got = await client.get(URL)

    assert got == {
        'invoice_id': 1,
        'eid': 'test1',
        'contract_id': 1234,
        'contract_eid': 'test_contract_eid',
        'amount': 12.0,
        'currency': 'RUB',
        'status': InvoiceStatus.paid.value,
        'date': '2021-04-01T00:00:00+00:00',
        'payment_date': '2021-04-10T00:00:00+00:00',
        'facture': {
            'facture_id': 1,
            'factura_id': 1,
            'amount': 12.0,
            'amount_with_nds': 13.0,
            'nds': 1.0,
            'date': '2021-04-01T00:00:00+00:00',
            'currency': 'RUB'
        }
    }


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
