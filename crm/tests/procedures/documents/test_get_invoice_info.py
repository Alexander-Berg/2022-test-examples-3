import pytest
from decimal import Decimal

from crm.agency_cabinet.documents.common.structs import DetailedInvoice, InvoiceStatus, Facture
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied
from crm.agency_cabinet.gateway.server.src.procedures.documents.invoices import GetInvoiceInfo
from crm.agency_cabinet.grants.common.structs import AccessLevel
from smb.common.testing_utils import dt


@pytest.fixture
def procedure(service_discovery):
    return GetInvoiceInfo(service_discovery)


@pytest.fixture
def input_params():
    return dict(agency_id=22, invoice_id=1)


async def test_returns_invoice_info_if_access_allowed(
    procedure, input_params, service_discovery
):
    invoice = DetailedInvoice(
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

    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.documents.get_invoice_info.return_value = invoice

    got = await procedure(yandex_uid=123, **input_params)

    assert got == invoice


async def test_calls_other_services_for_info(procedure, input_params, service_discovery):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW

    await procedure(yandex_uid=1, **input_params)

    service_discovery.grants.check_access_level.assert_called_with(
        yandex_uid=1, agency_id=22
    )
    service_discovery.documents.get_invoice_info.assert_called_with(
        **input_params
    )


async def test_raises_if_access_denied(procedure, input_params, service_discovery):
    service_discovery.grants.check_access_level.return_value = AccessLevel.DENY

    with pytest.raises(AccessDenied):
        await procedure(yandex_uid=1, **input_params)


async def test_does_not_call_documents_if_access_denied(
    procedure, input_params, service_discovery
):
    service_discovery.grants.check_access_level.return_value = AccessLevel.DENY

    with pytest.raises(AccessDenied):
        await procedure(yandex_uid=1, **input_params)

    service_discovery.documents.get_invoice_info.assert_not_called()
