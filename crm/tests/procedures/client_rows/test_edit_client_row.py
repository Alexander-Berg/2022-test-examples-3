import pytest

from crm.agency_cabinet.ord.common import structs
from crm.agency_cabinet.ord.server.src import procedures
from crm.agency_cabinet.ord.common.exceptions import UnsuitableReportException, UnsuitableAgencyException, \
    NoSuchClientRowException, UnsuitableClientException, OrdException
from crm.agency_cabinet.ord.server.src.db import models


@pytest.fixture
def procedure():
    return procedures.EditClientRow()


@pytest.fixture()
async def fixture_client_rows_edit(fixture_clients2, fixture_campaign2):
    rows = [
        {
            'client_id': fixture_clients2[7].id,
            'campaign_id': None,
            'client_contract_id': None,
            'suggested_amount': 1000
        },
        {
            'client_id': fixture_clients2[7].id,
            'campaign_id': None,
            'client_contract_id': None,
            'suggested_amount': 1000
        },
    ]

    yield await models.ClientRow.bulk_insert(rows)
    await models.ClientRow.delete.gino.status()


# todo: migrate to fixtures from conftest
@pytest.mark.skip('Need to use new models in edit_client_row')
async def test_edit_client_row_client_act_id(
    procedure, fixture_clients2, fixture_client_rows_edit, fixture_campaign2
):
    await procedure(structs.EditClientRowInput(
        agency_id=1,
        report_id=0,
        row_id=fixture_client_rows_edit[0].id,
        client_id=fixture_client_rows_edit[0].client_id,
    ))

    row = await models.ClientRow.query.where(models.ClientRow.id == fixture_client_rows_edit[0].id).gino.first()
    assert row.client_act_id == 0


@pytest.mark.skip('Need to use new models in edit_client_row')
async def test_edit_client_row_ad_distributor_act_ids(
    procedure, fixture_clients2, fixture_client_rows_edit, fixture_campaign2
):
    await procedure(structs.EditClientRowInput(
        agency_id=1,
        report_id=0,
        row_id=fixture_client_rows_edit[0].id,
        client_id=fixture_client_rows_edit[0].client_id
    ))

    row = await models.ClientRow.query.where(models.ClientRow.id == fixture_client_rows_edit[0].id).gino.first()
    assert row.ad_distributor_act_id == 0


@pytest.mark.skip('Need to use new models in edit_client_row')
async def test_edit_client_row_contract_id(
    procedure, fixture_clients2, fixture_client_rows_edit, fixture_campaign2
):
    await procedure(structs.EditClientRowInput(
        agency_id=1,
        report_id=0,
        row_id=fixture_client_rows_edit[0].id,
        client_id=fixture_client_rows_edit[0].client_id,
    ))

    _ = await models.ClientRow.query.where(models.ClientRow.id == fixture_client_rows_edit[0].id).gino.first()
    await models.ClientRow.delete.gino.status()
    await models.Campaign.delete.gino.status()
    await models.Client.delete.gino.status()
    await models.Report.delete.gino.status()


@pytest.mark.skip('Need to use new models in edit_client_row')
async def test_edit_client_row_campaign(
    procedure, fixture_clients2, fixture_client_rows_edit, fixture_campaign2
):
    await procedure(structs.EditClientRowInput(
        agency_id=1,
        report_id=0,
        row_id=fixture_client_rows_edit[0].id,
        client_id=fixture_client_rows_edit[0].client_id,
        campaign_eid='new campaign',
        campaign_name='new'
    ))

    campaigns = await models.Campaign.query.where(models.Campaign.client_id == fixture_client_rows_edit[0].client_id).gino.all()
    assert len(campaigns) == 1

    row = await models.ClientRow.query.where(models.ClientRow.id == fixture_client_rows_edit[0].id).gino.first()
    assert row.campaign_id == campaigns[0].id

    await models.ClientRow.delete.gino.status()
    await models.Campaign.delete.gino.status()
    await models.Client.delete.gino.status()
    await models.Report.delete.gino.status()


@pytest.mark.skip('Need to use new models in edit_client_row')
async def test_edit_client_row_unsuitable_agency(
    procedure, fixture_clients2, fixture_client_rows_edit, fixture_campaign2
):
    with pytest.raises(UnsuitableAgencyException):
        await procedure(structs.EditClientRowInput(
            agency_id=404,
            report_id=0,
            row_id=fixture_client_rows_edit[0].id,
            client_id=fixture_client_rows_edit[0].client_id
        ))


@pytest.mark.skip('Need to use new models in edit_client_row')
async def test_edit_report_no_such_report(
    procedure, fixture_clients2, fixture_client_rows_edit, fixture_campaign2
):
    with pytest.raises(UnsuitableReportException):
        await procedure(structs.EditClientRowInput(
            agency_id=1,
            report_id=404,
            row_id=fixture_client_rows_edit[0].id,
            client_id=fixture_client_rows_edit[0].client_id
        ))


@pytest.mark.skip('Need to use new models in edit_client_row')
async def test_edit_report_no_such_client_row(
    procedure, fixture_clients2, fixture_client_rows_edit, fixture_campaign2
):
    with pytest.raises(NoSuchClientRowException):
        await procedure(structs.EditClientRowInput(
            agency_id=1,
            report_id=0,
            row_id=404,
            client_id=fixture_client_rows_edit[0].client_id
        ))


@pytest.mark.skip('Need to use new models in edit_client_row')
async def test_edit_report_unsuitable_client(
    procedure, fixture_clients2, fixture_client_rows_edit, fixture_campaign2
):
    with pytest.raises(UnsuitableClientException):
        await procedure(structs.EditClientRowInput(
            agency_id=1,
            report_id=0,
            row_id=fixture_client_rows_edit[0].id,
            client_id=404,
        ))


@pytest.mark.skip('Need to use new models in edit_client_row')
async def test_edit_client_row_exception(
    procedure, fixture_clients2, fixture_client_rows_edit, fixture_campaign2
):

    with pytest.raises(OrdException):
        await procedure(structs.EditClientRowInput(
            agency_id=1,
            report_id=0,
            row_id=fixture_client_rows_edit[0].id,
            client_id=fixture_client_rows_edit[0].client_id,
            ad_distributor_act_id=1000000000000,
        ))

    with pytest.raises(OrdException):
        await procedure(structs.EditClientRowInput(
            agency_id=1,
            report_id=0,
            row_id=fixture_client_rows_edit[0].id,
            client_id=fixture_client_rows_edit[0].client_id,
            client_act_id=1000000000000,
        ))

    with pytest.raises(OrdException):
        await procedure(structs.EditClientRowInput(
            agency_id=1,
            report_id=0,
            row_id=fixture_client_rows_edit[0].id,
            client_id=fixture_client_rows_edit[0].client_id,
            client_contract_id=1000000000000,
        ))
