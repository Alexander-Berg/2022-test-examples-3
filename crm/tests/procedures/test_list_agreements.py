import typing

import pytest
from crm.agency_cabinet.documents.common.structs import (
    Agreement,
    ListAgreementsInput
)
from crm.agency_cabinet.documents.server.src.procedures import ListAgreements
from smb.common.testing_utils import dt
from sqlalchemy.engine.result import RowProxy


@pytest.fixture
def procedure():
    return ListAgreements()


async def test_select_agreement_all_filters(
    procedure,
    fixture_contracts: typing.List[RowProxy],
    fixture_agreements: typing.List[RowProxy],
    fixture_invoices: typing.List[RowProxy]
):
    got = await procedure(ListAgreementsInput(
        agency_id=321,
        contract_id=fixture_contracts[2].id,
        limit=10,
        offset=0,
        date_to=dt('2023-05-22 10:10:10'),
        date_from=dt('2020-01-22 10:10:10'),
        search_query=None,
    ))
    assert got == [
        Agreement(
            agreement_id=fixture_agreements[1].id,
            name=fixture_agreements[1].name,
            got_scan=fixture_agreements[1].got_scan,
            got_original=fixture_agreements[1].got_original,
            date=fixture_agreements[1].date,
        )
    ]


async def test_select_agreement_only_required_filter(
    procedure,
    fixture_contracts: typing.List[RowProxy],
    fixture_agreements: typing.List[RowProxy],
):
    got = await procedure(ListAgreementsInput(
        agency_id=321,
        contract_id=fixture_contracts[2].id
    ))
    assert got == [
        Agreement(
            agreement_id=fixture_agreements[0].id,
            name=fixture_agreements[0].name,
            got_scan=fixture_agreements[0].got_scan,
            got_original=fixture_agreements[0].got_original,
            date=fixture_agreements[0].date,
        ),
        Agreement(
            agreement_id=fixture_agreements[1].id,
            name=fixture_agreements[1].name,
            got_scan=fixture_agreements[1].got_scan,
            got_original=fixture_agreements[1].got_original,
            date=fixture_agreements[1].date,
        )
    ]


async def test_select_agreement_search_query_contract_eid(
    procedure,
    fixture_contracts: typing.List[RowProxy],
    fixture_agreements: typing.List[RowProxy],
):
    got = await procedure(ListAgreementsInput(
        agency_id=321,
        contract_id=fixture_contracts[2].id,
        search_query=fixture_contracts[2].eid,
    ))
    assert got == [
        Agreement(
            agreement_id=fixture_agreements[0].id,
            name=fixture_agreements[0].name,
            got_scan=fixture_agreements[0].got_scan,
            got_original=fixture_agreements[0].got_original,
            date=fixture_agreements[0].date,
        ),
        Agreement(
            agreement_id=fixture_agreements[1].id,
            name=fixture_agreements[1].name,
            got_scan=fixture_agreements[1].got_scan,
            got_original=fixture_agreements[1].got_original,
            date=fixture_agreements[1].date,
        )
    ]


async def test_select_agreement_partial_search_query(
    procedure,
    fixture_contracts: typing.List[RowProxy],
    fixture_agreements: typing.List[RowProxy],
    fixture_invoices: typing.List[RowProxy]
):
    got = await procedure(ListAgreementsInput(
        agency_id=321,
        contract_id=fixture_contracts[2].id,
        search_query=fixture_contracts[2].eid[3].upper(),
    ))
    assert got == [
        Agreement(
            agreement_id=fixture_agreements[0].id,
            name=fixture_agreements[0].name,
            got_scan=fixture_agreements[0].got_scan,
            got_original=fixture_agreements[0].got_original,
            date=fixture_agreements[0].date,
        ),
        Agreement(
            agreement_id=fixture_agreements[1].id,
            name=fixture_agreements[1].name,
            got_scan=fixture_agreements[1].got_scan,
            got_original=fixture_agreements[1].got_original,
            date=fixture_agreements[1].date,
        )
    ]


async def test_select_no_agreements(
    procedure,
    fixture_contracts: typing.List[RowProxy],
):
    got = await procedure(ListAgreementsInput(
        agency_id=321,
        contract_id=fixture_contracts[2].id,
        limit=10,
        offset=0,
        date_to=dt('2029-02-22 10:10:10'),
        date_from=dt('2029-01-22 10:10:10'),
    ))
    assert got == []
