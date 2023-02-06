import typing
from decimal import Decimal

import pytest
from crm.agency_cabinet.documents.common.structs import (
    Payment,
    ListPaymentsInput
)
from crm.agency_cabinet.documents.server.src.procedures import ListPayments
from smb.common.testing_utils import dt
from sqlalchemy.engine.result import RowProxy


@pytest.fixture
def procedure():
    return ListPayments()


async def test_select_payment_filter_all(
    procedure,
    fixture_contracts: typing.List[RowProxy],
    fixture_payments: typing.List[RowProxy],
    fixture_invoices: typing.List[RowProxy]
):
    got = await procedure(ListPaymentsInput(
        agency_id=123,
        invoice_id=fixture_invoices[0].id,
        contract_id=fixture_contracts[0].id,
        limit=10,
        offset=0,
        date_to=dt('2022-05-22 10:10:10'),
        date_from=dt('2020-01-22 10:10:10'),
    ))
    assert got == [
        Payment(
            payment_id=fixture_payments[0].id,
            eid=fixture_payments[0].eid,
            invoice_id=fixture_invoices[0].id,
            invoice_eid='invoice_eid',
            contract_id=fixture_contracts[0].id,
            currency='RUR',
            amount=Decimal('50'),
            date=dt('2021-4-1 00:00:00'),
        ),
    ]


async def test_select_payment_filter_required_only(
    procedure,
    fixture_contracts: typing.List[RowProxy],
    fixture_payments: typing.List[RowProxy],
    fixture_invoices: typing.List[RowProxy]
):
    got = await procedure(ListPaymentsInput(
        agency_id=123))
    assert got == [
        Payment(
            payment_id=fixture_payments[1].id,
            eid=fixture_payments[1].eid,
            invoice_id=fixture_invoices[0].id,
            invoice_eid='invoice_eid',
            contract_id=fixture_contracts[0].id,
            currency='RUR',
            amount=Decimal('50'),
            date=dt('2023-4-1 00:00:00'),
        ),
        Payment(
            payment_id=fixture_payments[0].id,
            eid=fixture_payments[0].eid,
            invoice_id=fixture_invoices[0].id,
            invoice_eid='invoice_eid',
            contract_id=fixture_contracts[0].id,
            currency='RUR',
            amount=Decimal('50'),
            date=dt('2021-4-1 00:00:00'),
        ),
    ]


async def test_select_no_payments(
    procedure,
    fixture_contracts: typing.List[RowProxy],
    fixture_payments: typing.List[RowProxy],
    fixture_invoices: typing.List[RowProxy]
):
    got = await procedure(ListPaymentsInput(
        agency_id=123,
        invoice_id=fixture_invoices[0].id,
        contract_id=fixture_contracts[0].id,
        limit=10,
        offset=0,
        date_to=dt('2029-02-22 10:10:10'),
        date_from=dt('2029-01-22 10:10:10'),
    ))
    assert got == []
