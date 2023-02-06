import datetime
import typing
from decimal import Decimal

import pytest
from crm.agency_cabinet.documents.common.structs import invoices
from crm.agency_cabinet.documents.server.src.db import models
from crm.agency_cabinet.documents.server.src.procedures import ListInvoices
from smb.common.testing_utils import dt
from sqlalchemy.engine.result import RowProxy


@pytest.fixture
def procedure():
    return ListInvoices()


@pytest.fixture
async def fixture_contracts():
    rows = [
        {
            'eid': 'test9',
            'inn': 'inn',
            'status': 'valid',
            'agency_id': 123,
            'payment_type': 'prepayment',
            'services': ['zen', 'direct'],
            'signing_date': dt('2021-3-1 00:00:00'),
            'finish_date': dt('2022-3-1 00:00:00'),
            'credit_limit': 66.6
        },
        {
            'eid': 'test10',
            'inn': 'inn',
            'status': 'valid',
            'agency_id': 123,
            'payment_type': 'prepayment',
            'services': ['zen', 'direct'],
            'signing_date': dt('2021-3-1 00:00:00'),
            'finish_date': dt('2022-3-1 00:00:00'),
            'credit_limit': 66.6
        },
    ]
    yield await models.Contract.bulk_insert(rows)

    await models.Contract.delete.gino.status()


@pytest.fixture
async def fixture_invoices(fixture_contracts):
    rows = [
        {
            'eid': 'test1',
            'contract_id': fixture_contracts[0].id,
            'amount': 12,
            'currency': 'RUB',
            'status': invoices.InvoiceStatus.paid.value,
            'date': dt('2021-4-1 00:00:00'),
            'payment_date': dt('2021-4-10 00:00:00')
        },
        {
            'eid': 'test2',
            'contract_id': fixture_contracts[1].id,
            'amount': 13,
            'currency': 'RUB',
            'status': invoices.InvoiceStatus.not_paid.value,
            'date': dt('2021-5-1 00:00:00'),
            'payment_date': None
        }
    ]
    yield await models.Invoice.bulk_insert(rows)

    await models.Invoice.delete.gino.status()


@pytest.fixture
async def fixture_facture(fixture_invoices, fixture_acts):
    rows = [
        {
            'invoice_id': fixture_invoices[0].id,
            'amount': 12,
            'amount_with_nds': 13,
            'nds': 1,
            'currency': 'RUB',
            'date': dt('2021-4-1 00:00:00'),
            'act_id': fixture_acts[0].id,
        },
    ]
    yield await models.Facture.bulk_insert(rows)

    await models.Facture.delete.gino.status()


@pytest.fixture
def fixture_structs_invoices(fixture_invoices, fixture_contracts):
    return [
        invoices.Invoice(
            invoice_id=fixture_invoices[0].id,
            eid='test1',
            contract_id=fixture_invoices[0].contract_id,
            contract_eid=fixture_contracts[0].eid,
            amount=Decimal(12),
            currency='RUB',
            status=invoices.InvoiceStatus.paid,
            date=dt('2021-4-1 00:00:00'),
            payment_date=dt('2021-4-10 00:00:00'),
            has_facture=True
        ),

        invoices.Invoice(
            invoice_id=fixture_invoices[1].id,
            eid='test2',
            contract_id=fixture_invoices[1].contract_id,
            contract_eid=fixture_contracts[1].eid,
            amount=Decimal(13),
            currency='RUB',
            status=invoices.InvoiceStatus.not_paid,
            date=dt('2021-5-1 00:00:00'),
            payment_date=None,
            has_facture=False
        ),
    ]


async def test_select_invoices(
    procedure,
    fixture_contracts: typing.List[RowProxy],
    fixture_structs_invoices: typing.List[RowProxy],
    fixture_facture: typing.List[RowProxy]
):
    got = await procedure(invoices.ListInvoicesInput(agency_id=fixture_contracts[0].agency_id))
    assert got == [fixture_structs_invoices[1], fixture_structs_invoices[0]]


async def test_select_invoices_filter_contract(
    procedure,
    fixture_contracts: typing.List[RowProxy],
    fixture_structs_invoices: typing.List[RowProxy],
    fixture_facture: typing.List[RowProxy]
):
    got = await procedure(
        invoices.ListInvoicesInput(
            agency_id=fixture_contracts[0].agency_id,
            contract_id=fixture_contracts[0].id
        )
    )
    assert got == [fixture_structs_invoices[0]]


async def test_select_invoices_filter_date_from(
    procedure,
    fixture_contracts: typing.List[RowProxy],
    fixture_structs_invoices: typing.List[RowProxy],
    fixture_facture: typing.List[RowProxy]
):
    got = await procedure(
        invoices.ListInvoicesInput(
            agency_id=fixture_contracts[0].agency_id,
            date_from=datetime.datetime(2021, 5, 1)
        )
    )
    assert got == [fixture_structs_invoices[1]]


async def test_select_invoices_filter_date_to(
    procedure,
    fixture_contracts: typing.List[RowProxy],
    fixture_structs_invoices: typing.List[RowProxy],
    fixture_facture: typing.List[RowProxy]
):
    got = await procedure(
        invoices.ListInvoicesInput(
            agency_id=fixture_contracts[0].agency_id,
            date_to=datetime.datetime(2021, 5, 1)
        )
    )
    assert got == [fixture_structs_invoices[0]]


async def test_select_invoices_filter_payment_status(
    procedure,
    fixture_contracts: typing.List[RowProxy],
    fixture_structs_invoices: typing.List[RowProxy],
    fixture_facture: typing.List[RowProxy]
):
    got = await procedure(
        invoices.ListInvoicesInput(
            agency_id=fixture_contracts[0].agency_id,
            status=invoices.InvoiceStatus.paid
        )
    )
    assert got == [fixture_structs_invoices[0]]


async def test_select_invoices_limit_offset(
    procedure,
    fixture_contracts: typing.List[RowProxy],
    fixture_structs_invoices: typing.List[RowProxy],
    fixture_facture: typing.List[RowProxy]
):
    got = await procedure(
        invoices.ListInvoicesInput(
            agency_id=fixture_contracts[0].agency_id,
            limit=1,
            offset=1
        )
    )
    assert got == [fixture_structs_invoices[0]]


async def test_select_invoices_search_query(
    procedure,
    fixture_contracts: typing.List[RowProxy],
    fixture_structs_invoices: typing.List[RowProxy],
    fixture_facture: typing.List[RowProxy]
):
    got = await procedure(
        invoices.ListInvoicesInput(
            agency_id=fixture_contracts[0].agency_id,
            search_query=fixture_structs_invoices[0].eid
        )
    )
    assert got == [fixture_structs_invoices[0]]


async def test_select_invoices_partial_search_query(
    procedure,
    fixture_contracts: typing.List[RowProxy],
    fixture_structs_invoices: typing.List[RowProxy],
    fixture_facture: typing.List[RowProxy]
):
    got = await procedure(
        invoices.ListInvoicesInput(
            agency_id=fixture_contracts[0].agency_id,
            search_query=fixture_structs_invoices[0].eid[3].upper()
        )
    )
    assert got == [fixture_structs_invoices[1], fixture_structs_invoices[0]]


async def test_select_no_invoices(
    procedure,
    fixture_contracts: typing.List[RowProxy]
):
    got = await procedure(invoices.ListInvoicesInput(agency_id=1234))
    assert got == []
