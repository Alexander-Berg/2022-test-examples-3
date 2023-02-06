import pytest

from smb.common.testing_utils import dt

from crm.agency_cabinet.ord.server.src.db import models


@pytest.fixture()
async def fixture_cr_reports(fixture_report_settings):
    rows = [
        {
            'agency_id': 1,
            'settings_id': fixture_report_settings[1].id,
            'reporter_type': 'partner',
            'period_from': dt('2022-6-1 00:00:00'),
            'status': 'draft',
            'is_deleted': False,
        },
    ]
    yield await models.Report.bulk_insert(rows)
    await models.Report.delete.gino.status()


@pytest.fixture()
async def fixture_cr_clients(fixture_cr_reports):
    rows = [
        {
            'report_id': fixture_cr_reports[0].id,
            'client_id': '111',
            'name': 'test_client_name_1',
            'login': 'test_client_login_1',
        },
        {
            'report_id': fixture_cr_reports[0].id,
            'client_id': '222',
            'name': 'test_client_name_2',
            'login': 'test_client_login_2',
        }
    ]

    yield await models.Client.bulk_insert(rows)
    await models.Client.delete.gino.status()


@pytest.fixture()
async def fixture_cr_organizations():
    rows = [
        {
            'partner_id': 1,
            'type': 'ul',
            'inn': 'inn-123456',
            'name': 'OOO Vesna',
            'is_rr': False,
            'is_ors': False,
            'mobile_phone': '987654321',
            'epay_number': '234',
            'reg_number': '345',
            'alter_inn': '456',
            'oksm_number': '567',
            'rs_url': 'www.test.ru'
        },
        {
            'partner_id': 1,
            'type': 'fl',
            'inn': 'inn-234567',
            'name': None,
            'is_rr': None,
            'is_ors': None,
            'mobile_phone': None,
            'epay_number': None,
            'reg_number': None,
            'alter_inn': None,
            'oksm_number': None,
            'rs_url': None,
        },
        {
            'partner_id': 1,
            'type': 'ffl',
            'inn': None,
            'name': None,
            'is_rr': None,
            'is_ors': None,
            'mobile_phone': '0987654321',
            'epay_number': None,
            'reg_number': None,
            'alter_inn': None,
            'oksm_number': None,
            'rs_url': None,
        },
    ]

    yield await models.Organization.bulk_insert(rows)
    await models.Organization.delete.gino.status()


@pytest.fixture()
async def fixture_cr_contracts(fixture_cr_organizations):
    rows = [
        {
            'partner_id': 1,
            'contractor_id': fixture_cr_organizations[0].id,
            'client_id': fixture_cr_organizations[1].id,
            'contract_eid': 'c-1',
            'is_reg_report': False,
            'type': 'contract',
            'action_type': 'other',
            'subject_type': 'distribution',
            'date': dt('2022-3-1 00:00:00'),
            'amount': 100000,
            'is_vat': True,
        },
        {
            'partner_id': 1,
            'contractor_id': fixture_cr_organizations[1].id,
            'client_id': fixture_cr_organizations[2].id,
            'contract_eid': 'c-2',
            'is_reg_report': None,
            'type': None,
            'action_type': None,
            'subject_type': None,
            'date': None,
            'amount': None,
            'is_vat': None,
        },
        {
            'partner_id': 1,
            'contractor_id': fixture_cr_organizations[0].id,
            'client_id': fixture_cr_organizations[2].id,
            'contract_eid': 'c-3',
            'is_reg_report': None,
            'type': None,
            'action_type': None,
            'subject_type': None,
            'date': None,
            'amount': None,
            'is_vat': None,
        },
        {
            'partner_id': 1,
            'client_id': None,
            'contractor_id': None,
            'contract_eid': 'c-4',
            'is_reg_report': None,
            'type': None,
            'action_type': None,
            'subject_type': None,
            'date': None,
            'amount': None,
            'is_vat': None,
        }
    ]

    yield await models.Contract.bulk_insert(rows)
    await models.Contract.delete.gino.status()


@pytest.fixture()
async def fixture_cr_acts(fixture_cr_reports):
    rows = [
        {
            'report_id': fixture_cr_reports[0].id,
            'act_eid': 'a-1',
            'amount': 20000,
            'is_vat': True,
        },
        {
            'report_id': fixture_cr_reports[0].id,
            'act_eid': 'a-2',
            'amount': None,
            'is_vat': None,
        },
    ]

    yield await models.Act.bulk_insert(rows)
    await models.Act.delete.gino.status()


@pytest.fixture()
async def fixture_cr_campaigns(fixture_cr_reports, fixture_cr_clients):
    rows = [
        {
            'report_id': fixture_cr_reports[0].id,
            'client_id': fixture_cr_clients[0].id,
            'campaign_eid': 'campaign-1',
            'name': 'campaign_name_1',
        },
        {
            'report_id': fixture_cr_reports[0].id,
            'client_id': fixture_cr_clients[0].id,
            'campaign_eid': 'campaign-2',
            'name': 'campaign_name_2',
        },
        {
            'report_id': fixture_cr_reports[0].id,
            'client_id': fixture_cr_clients[0].id,
            'campaign_eid': 'campaign-3',
            'name': 'campaign_name_3',
        },
    ]

    yield await models.Campaign.bulk_insert(rows)
    await models.Campaign.delete.gino.status()


@pytest.fixture()
async def fixture_cr_client_rows(fixture_cr_clients, fixture_cr_contracts, fixture_cr_acts, fixture_cr_campaigns):
    rows = [
        {
            'client_id': fixture_cr_clients[0].id,
            'campaign_id': fixture_cr_campaigns[0].id,
            'ad_distributor_contract_id': fixture_cr_contracts[0].id,
            'ad_distributor_act_id': fixture_cr_acts[0].id,
            'partner_contract_id': fixture_cr_contracts[1].id,
            'partner_act_id': fixture_cr_acts[1].id,
            'advertiser_contract_id': fixture_cr_contracts[1].id,
            'suggested_amount': 10000,
            'updated_at': dt('2022-7-1 00:05:00'),
        },
        {
            'client_id': fixture_cr_clients[0].id,
            'campaign_id': fixture_cr_campaigns[1].id,
            'ad_distributor_contract_id': fixture_cr_contracts[3].id,
            'ad_distributor_act_id': None,
            'partner_contract_id': None,
            'partner_act_id': None,
            'advertiser_contract_id': fixture_cr_contracts[2].id,
            'suggested_amount': 10000,
            'updated_at': dt('2022-7-1 00:04:00'),
        },
        {
            'client_id': fixture_cr_clients[0].id,
            'campaign_id': fixture_cr_campaigns[2].id,
            'ad_distributor_contract_id': None,
            'ad_distributor_act_id': None,
            'partner_contract_id': None,
            'partner_act_id': None,
            'advertiser_contract_id': None,
            'suggested_amount': None,
            'updated_at': dt('2022-7-1 00:03:00'),
        },
    ]

    yield await models.ClientRow.bulk_insert(rows)
    await models.ClientRow.delete.gino.status()
