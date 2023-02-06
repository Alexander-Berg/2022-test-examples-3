import datetime
import pytest
import typing
from decimal import Decimal
from sqlalchemy.engine.result import RowProxy

from crm.agency_cabinet.documents.common.structs import InvoiceStatus, DetailedInvoice, GetInvoiceInfoInput, Facture
from crm.agency_cabinet.documents.server.src.exceptions import NoSuchInvoiceException, UnsuitableAgencyException
from crm.agency_cabinet.documents.server.src.procedures import GetInvoiceInfo


@pytest.fixture
def procedure():
    return GetInvoiceInfo()


async def test_select_invoice(
    procedure,
    fixture_contracts: typing.List[RowProxy],
    fixture_invoices: typing.List[RowProxy],
    fixture_factures: typing.List[RowProxy]
):
    got = await procedure(GetInvoiceInfoInput(agency_id=123, invoice_id=fixture_invoices[0].id))

    assert got == DetailedInvoice(
        invoice_id=fixture_invoices[0].id,
        eid='invoice_eid',
        contract_id=fixture_invoices[0].contract_id,
        contract_eid=fixture_contracts[0].eid,
        amount=Decimal('50'),
        currency='RUR',
        status=InvoiceStatus.paid,
        date=datetime.datetime(2021, 3, 1, 0, 0, tzinfo=datetime.timezone.utc),
        payment_date=datetime.datetime(2022, 4, 1, 0, 0, tzinfo=datetime.timezone.utc),
        facture=Facture(
            facture_id=fixture_factures[0].id,
            amount=Decimal('50'),
            amount_with_nds=Decimal('60'),
            nds=Decimal('10'),
            date=datetime.datetime(2022, 3, 1, 0, 0, tzinfo=datetime.timezone.utc),
            currency='RUB'
        )
    )


async def test_select_invoice_without_facture(
    procedure,
    fixture_contracts: typing.List[RowProxy],
    fixture_invoices: typing.List[RowProxy]
):
    got = await procedure(GetInvoiceInfoInput(agency_id=123, invoice_id=fixture_invoices[0].id))

    assert got == DetailedInvoice(
        invoice_id=fixture_invoices[0].id,
        eid='invoice_eid',
        contract_id=fixture_invoices[0].contract_id,
        contract_eid=fixture_contracts[0].eid,
        amount=Decimal('50'),
        currency='RUR',
        status=InvoiceStatus.paid,
        date=datetime.datetime(2021, 3, 1, 0, 0, tzinfo=datetime.timezone.utc),
        payment_date=datetime.datetime(2022, 4, 1, 0, 0, tzinfo=datetime.timezone.utc),
        facture=None
    )


async def test_select_no_such_invoice(procedure, fixture_invoices: typing.List[RowProxy]):
    with pytest.raises(NoSuchInvoiceException):
        await procedure(GetInvoiceInfoInput(agency_id=123, invoice_id=-1))


async def test_select_invoice_unsuitable_agency(procedure, fixture_invoices: typing.List[RowProxy]):
    with pytest.raises(UnsuitableAgencyException):
        await procedure(GetInvoiceInfoInput(agency_id=1234, invoice_id=fixture_invoices[0].id))
