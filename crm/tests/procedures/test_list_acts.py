import typing
from decimal import Decimal

import pytest
from crm.agency_cabinet.documents.common.structs import (
    Act,
    ListActsInput
)
from crm.agency_cabinet.documents.server.src.procedures import ListActs
from smb.common.testing_utils import dt
from sqlalchemy.engine.result import RowProxy


@pytest.fixture
def procedure():
    return ListActs()


async def test_select_act_all_filters(
    procedure,
    fixture_contracts: typing.List[RowProxy],
    fixture_acts: typing.List[RowProxy],
    fixture_invoices: typing.List[RowProxy]
):
    got = await procedure(ListActsInput(
        agency_id=123,
        invoice_id=fixture_invoices[1].id,
        contract_id=fixture_contracts[0].id,
        limit=10,
        offset=0,
        date_to=dt('2022-05-22 10:10:10'),
        date_from=dt('2020-01-22 10:10:10'),
        search_query=None,
    ))
    assert got == [
        Act(
            act_id=fixture_acts[0].id,
            invoice_id=fixture_invoices[1].id,
            contract_id=fixture_contracts[0].id,
            currency='KZT',
            amount=Decimal('150'),
            date=dt('2021-4-1 00:00:00'),
            eid=fixture_acts[0].eid,
            contract_eid=fixture_contracts[0].eid,
            invoice_eid=fixture_invoices[1].eid,
        ),
    ]


async def test_select_act_only_required_filter(
    procedure,
    fixture_contracts: typing.List[RowProxy],
    fixture_acts: typing.List[RowProxy],
    fixture_invoices: typing.List[RowProxy]
):
    got = await procedure(ListActsInput(
        agency_id=123,
    ))
    assert got == [
        Act(
            act_id=fixture_acts[1].id,
            invoice_id=fixture_invoices[1].id,
            contract_id=fixture_contracts[0].id,
            currency='KZT',
            amount=Decimal('150'),
            date=dt('2023-4-1 00:00:00'),
            eid=fixture_acts[1].eid,
            contract_eid=fixture_contracts[0].eid,
            invoice_eid=fixture_invoices[1].eid,
        ),
        Act(
            act_id=fixture_acts[0].id,
            invoice_id=fixture_invoices[1].id,
            contract_id=fixture_contracts[0].id,
            currency='KZT',
            amount=Decimal('150'),
            date=dt('2021-4-1 00:00:00'),
            eid=fixture_acts[0].eid,
            contract_eid=fixture_contracts[0].eid,
            invoice_eid=fixture_invoices[1].eid,
        ),
    ]


async def test_select_act_search_query_act_eid(
    procedure,
    fixture_contracts: typing.List[RowProxy],
    fixture_acts: typing.List[RowProxy],
    fixture_invoices: typing.List[RowProxy]
):
    got = await procedure(ListActsInput(
        agency_id=123,
        search_query=fixture_invoices[1].eid,
    ))
    assert got == [
        Act(
            act_id=fixture_acts[1].id,
            invoice_id=fixture_invoices[1].id,
            contract_id=fixture_contracts[0].id,
            currency='KZT',
            amount=Decimal('150'),
            date=dt('2023-4-1 00:00:00'),
            eid=fixture_acts[1].eid,
            contract_eid=fixture_contracts[0].eid,
            invoice_eid=fixture_invoices[1].eid,
        ),
        Act(
            act_id=fixture_acts[0].id,
            invoice_id=fixture_invoices[1].id,
            contract_id=fixture_contracts[0].id,
            currency='KZT',
            amount=Decimal('150'),
            date=dt('2021-4-1 00:00:00'),
            eid=fixture_acts[0].eid,
            contract_eid=fixture_contracts[0].eid,
            invoice_eid=fixture_invoices[1].eid,
        ),
    ]


async def test_select_act_partial_search_query(
    procedure,
    fixture_contracts: typing.List[RowProxy],
    fixture_acts: typing.List[RowProxy],
    fixture_invoices: typing.List[RowProxy]
):
    got = await procedure(ListActsInput(
        agency_id=123,
        search_query=fixture_invoices[1].eid[3].upper(),
    ))
    assert got == [
        Act(
            act_id=fixture_acts[1].id,
            invoice_id=fixture_invoices[1].id,
            contract_id=fixture_contracts[0].id,
            currency='KZT',
            amount=Decimal('150'),
            date=dt('2023-4-1 00:00:00'),
            eid=fixture_acts[1].eid,
            contract_eid=fixture_contracts[0].eid,
            invoice_eid=fixture_invoices[1].eid,
        ),
        Act(
            act_id=fixture_acts[0].id,
            invoice_id=fixture_invoices[1].id,
            contract_id=fixture_contracts[0].id,
            currency='KZT',
            amount=Decimal('150'),
            date=dt('2021-4-1 00:00:00'),
            eid=fixture_acts[0].eid,
            contract_eid=fixture_contracts[0].eid,
            invoice_eid=fixture_invoices[1].eid,
        ),
    ]


async def test_select_no_acts(
    procedure,
    fixture_contracts: typing.List[RowProxy],
    fixture_acts: typing.List[RowProxy],
    fixture_invoices: typing.List[RowProxy]
):
    got = await procedure(ListActsInput(
        agency_id=123,
        invoice_id=fixture_invoices[0].id,
        contract_id=fixture_contracts[0].id,
        limit=10,
        offset=0,
        date_to=dt('2029-02-22 10:10:10'),
        date_from=dt('2029-01-22 10:10:10'),
    ))
    assert got == []
