import pytest
from decimal import Decimal

from crm.agency_cabinet.documents.common.structs.invoices import Invoice, InvoiceStatus
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied
from crm.agency_cabinet.gateway.server.src.procedures.documents.invoices import ListInvoices
from crm.agency_cabinet.grants.common.structs import AccessLevel
from smb.common.testing_utils import dt


@pytest.fixture
def procedure(service_discovery):
    return ListInvoices(service_discovery)


@pytest.fixture
def input_params():
    return dict(
        agency_id=22,
        contract_id=None,
        date_from=None,
        date_to=None,
        status=None,
        limit=None,
        offset=None,
        search_query=None,
    )


async def test_returns_invoice_list_if_access_allowed(
    procedure, input_params, service_discovery
):
    invoices = [
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
            payment_date=dt('2021-5-10 00:00:00'),
            has_facture=False
        )
    ]

    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.documents.list_invoices.return_value = invoices

    got = await procedure(yandex_uid=123, **input_params)

    assert got == invoices


async def test_calls_other_services_for_info(procedure, input_params, service_discovery):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW

    await procedure(yandex_uid=1, **input_params)

    service_discovery.grants.check_access_level.assert_called_with(
        yandex_uid=1, agency_id=22
    )
    service_discovery.documents.list_invoices.assert_called_with(
        **input_params
    )


async def test_raises_if_access_denied(procedure, input_params, service_discovery):
    service_discovery.grants.check_access_level.return_value = AccessLevel.DENY

    with pytest.raises(AccessDenied):
        await procedure(yandex_uid=1, **input_params)


async def test_does_not_call_procedure_if_access_denied(
    procedure, input_params, service_discovery
):
    service_discovery.grants.check_access_level.return_value = AccessLevel.DENY

    with pytest.raises(AccessDenied):
        await procedure(yandex_uid=1, **input_params)

    service_discovery.documents.list_invoices.assert_not_called()
