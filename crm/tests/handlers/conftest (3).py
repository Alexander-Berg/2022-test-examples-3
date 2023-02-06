import pytest
import typing
import datetime
from crm.agency_cabinet.ord.server.src.db import models

AGENCY_ID = 1


@pytest.fixture()
async def fixture_reports(fixture_report_settings: typing.List[models.ReportSettings]):
    rows = [
        {
            'agency_id': AGENCY_ID,
            'settings_id': fixture_report_settings[1].id,
            'reporter_type': 'partner',
            'period_from': datetime.datetime(2021, 1, 1, tzinfo=datetime.timezone.utc),
            'status': 'sent',
            'is_deleted': False,
            'sending_date': datetime.datetime(2021, 2, 1, tzinfo=datetime.timezone.utc),
        },
        {
            'agency_id': AGENCY_ID,
            'settings_id': fixture_report_settings[1].id,
            'reporter_type': 'partner',
            'period_from': datetime.datetime(2021, 2, 1, tzinfo=datetime.timezone.utc),
            'status': 'draft',
            'is_deleted': False,
            'sending_date': None,
        }
    ]
    yield await models.Report.bulk_insert(rows)
    await models.Report.delete.gino.status()


@pytest.fixture()
async def fixture_clients(fixture_reports):
    rows = []
    for report in fixture_reports:
        rows.extend([
            {
                'name': 'test',
                'report_id': report.id,
                'login': 'test',
                'client_id': '1',
                'is_yt': True
            },
            {
                'name': 'test2',
                'report_id': report.id,
                'login': 'test2',
                'client_id': '2',
                'is_yt': True
            }
        ])
    yield await models.Client.bulk_insert(rows)
    await models.Client.delete.gino.status()


@pytest.fixture()
async def fixture_campaign(fixture_reports, fixture_clients):
    rows = [
        {
            'report_id': fixture_reports[0].id,
            'client_id': fixture_clients[0].id,
            'campaign_eid': 'campaign eid',
            'name': 'campaign name'
        }
    ]

    yield await models.Campaign.bulk_insert(rows)
    await models.Campaign.delete.gino.status()


@pytest.fixture()
async def fixture_client_rows(
    fixture_clients,
    fixture_campaign,
    fixture_contract_handler,
    fixture_act_handler
):
    rows = [
        {
            'client_id': fixture_clients[0].id,
            'campaign_id': fixture_campaign[0].id,
            'suggested_amount': 1000,
            'ad_distributor_contract_id': fixture_contract_handler[0].id,
            'partner_contract_id': fixture_contract_handler[0].id,
            'advertiser_contract_id': fixture_contract_handler[0].id,
            'partner_act_id': fixture_act_handler[0].id,
            'ad_distributor_act_id': fixture_act_handler[0].id,
        },
        {
            'client_id': fixture_clients[0].id,
            'campaign_id': fixture_campaign[0].id,
            'suggested_amount': 2000,
            'ad_distributor_contract_id': fixture_contract_handler[0].id,
            'partner_contract_id': fixture_contract_handler[0].id,
            'advertiser_contract_id': fixture_contract_handler[0].id,
            'partner_act_id': fixture_act_handler[0].id,
            'ad_distributor_act_id': fixture_act_handler[0].id,
        },
        {
            'client_id': fixture_clients[0].id,
            'campaign_id': fixture_campaign[0].id,
            'suggested_amount': 3000,
            'ad_distributor_contract_id': fixture_contract_handler[0].id,
            'partner_contract_id': fixture_contract_handler[0].id,
            'advertiser_contract_id': fixture_contract_handler[0].id,
            'partner_act_id': fixture_act_handler[0].id,
            'ad_distributor_act_id': fixture_act_handler[0].id,
        }
    ]

    yield await models.ClientRow.bulk_insert(rows)
    await models.ClientRow.delete.gino.status()


@pytest.fixture()
async def fixture_reports3_handler(fixture_report_settings: typing.List[models.ReportSettings]):
    rows = [
        {
            'agency_id': AGENCY_ID,
            'settings_id': fixture_report_settings[1].id,
            'reporter_type': 'partner',
            'period_from': datetime.datetime(2021, 1, 1, tzinfo=datetime.timezone.utc),
            'status': 'draft',
            'is_deleted': False,
        },
        {
            'agency_id': AGENCY_ID,
            'settings_id': fixture_report_settings[0].id,
            'reporter_type': 'partner',
            'period_from': datetime.datetime(2021, 1, 1, tzinfo=datetime.timezone.utc),
            'status': 'draft',
            'is_deleted': False,
        },
    ]
    yield await models.Report.bulk_insert(rows)
    await models.Report.delete.gino.status()


@pytest.fixture()
async def fixture_clients3(fixture_reports3_handler):
    rows = [
        {
            'report_id': fixture_reports3_handler[0].id,
            'name': 'test_client_name_1',
            'login': 'test_client_login_1',
            'client_id': '111',
            'is_yt': False
        }
    ]

    yield await models.Client.bulk_insert(rows)
    await models.Client.delete.gino.status()


@pytest.fixture()
async def fixture_client_rows3(fixture_clients3):
    rows = [
        {
            'client_id': fixture_clients3[0].id,
            'campaign_id': None,
            'suggested_amount': None,
            'ad_distributor_contract_id': None,
            'partner_contract_id': None,
            'advertiser_contract_id': None,
            'partner_act_id': None,
            'ad_distributor_act_id': None,
        }
    ]
    yield await models.ClientRow.bulk_insert(rows)
    await models.ClientRow.delete.gino.status()


@pytest.fixture()
async def fixture_organization():
    rows = [
        {
            'type': 'ip',
        }
    ]
    yield await models.Organization.bulk_insert(rows)
    await models.Organization.delete.gino.status()


@pytest.fixture()
async def fixture_contract_handler(fixture_organization):
    rows = [
        {
            'client_id': fixture_organization[0].id,
        }
    ]
    yield await models.Contract.bulk_insert(rows)
    await models.Contract.delete.gino.status()


@pytest.fixture()
async def fixture_act_handler(fixture_reports):
    rows = [
        {
            'report_id': fixture_reports[0].id,
            'act_eid': 'some eid'
        },
        {
            'report_id': fixture_reports[0].id,
            'act_eid': 'act1',
            'amount': 1,
            'is_vat': False,
        },
        {
            'report_id': fixture_reports[1].id,
            'act_eid': 'act2',
            'amount': 2,
            'is_vat': False,
        },
        {
            'report_id': fixture_reports[0].id,
            'act_eid': 'act3',
            'amount': None,
            'is_vat': True,
        },
        {
            'report_id': fixture_reports[0].id,
            'act_eid': 'act4',
            'amount': 3,
            'is_vat': False,
        },
        {
            'report_id': fixture_reports[1].id,
            'act_eid': 'act5',
            'amount': 5,
            'is_vat': True,
        },
    ]
    yield await models.Act.bulk_insert(rows)
    await models.Act.delete.gino.status()
