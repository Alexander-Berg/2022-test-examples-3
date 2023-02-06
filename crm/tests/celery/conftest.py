import pytest
import datetime
import typing

from crm.agency_cabinet.common.server.common.config import MdsConfig
from crm.agency_cabinet.ord.server.src.db import models

AGENCY_ID = 1

pytest_plugins = [
    'crm.agency_cabinet.common.service_discovery.pytest.plugin',
]


@pytest.fixture()
def fixture_agency_id():
    return AGENCY_ID


@pytest.fixture()
def mds_cfg():
    environ_dict = {
        'MDS_ENDPOINT_URL': None,
        'MDS_ACCESS_KEY_ID': None,
        'MDS_SECRET_ACCESS_KEY': None,
        'MDS_BUCKET': 'agency-cabinet-common'
    }
    return MdsConfig.from_environ(environ_dict)


@pytest.fixture()
async def fixture_celery_reports(fixture_report_settings: typing.List[models.ReportSettings]):
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
    ]
    yield await models.Report.bulk_insert(rows)
    await models.Report.delete.gino.status()


@pytest.fixture()
async def fixture_report_export(fixture_celery_reports):
    rows = [
        {
            'report_id': fixture_celery_reports[0].id,
            'file_id': None,
            'status': 'requested',
        },
    ]

    yield await models.ReportExportInfo.bulk_insert(rows)
    await models.ReportExportInfo.delete.gino.status()


@pytest.fixture()
async def fixture_clients_celery(fixture_celery_reports: typing.List[models.Report]):
    rows = [
        {
            'name': 'client_name_1',
            'report_id': fixture_celery_reports[0].id,
            'login': 'client_login_1',
            'client_id': 'client_id_1',
        },
        {
            'name': 'client_name2',
            'report_id': fixture_celery_reports[0].id,
            'login': 'client_login_2',
            'client_id': 'client_id_2',
        },
    ]
    yield await models.Client.bulk_insert(rows)
    await models.Client.delete.gino.status()


@pytest.fixture()
async def fixture_partner_acts_celery(fixture_celery_reports):
    rows = [
        {
            'report_id': fixture_celery_reports[0].id,
            'act_eid': 'act1',
            'amount': 1000,
            'is_vat': True
        },
        {
            'report_id': fixture_celery_reports[0].id,
            'act_eid': 'act2',
            'amount': 2000,
            'is_vat': True
        },
        {
            'report_id': fixture_celery_reports[0].id,
            'act_eid': 'act3',
            'amount': 3000,
            'is_vat': True
        }
    ]
    yield await models.Act.bulk_insert(rows)
    await models.Act.delete.gino.status()


@pytest.fixture()
async def fixture_ad_distributor_acts(fixture_celery_reports):
    rows = [
        {
            'act_eid': 'ad_distributor_act_eid_1',
            'amount': 3000,
            'report_id': fixture_celery_reports[0].id,
            'is_vat': True
        },
        {
            'act_eid': 'ad_distributor_act_eid_2',
            'amount': 3000,
            'report_id': fixture_celery_reports[0].id,
            'is_vat': True
        },
    ]
    yield await models.Act.bulk_insert(rows)
    await models.Act.delete.gino.status()


@pytest.fixture()
async def fixture_campaign_celery(fixture_celery_reports: typing.List[models.Report], fixture_clients_celery):
    rows = [
        {
            'report_id': fixture_celery_reports[0].id,
            'campaign_eid': 'campaign_eid_1',
            'client_id': fixture_clients_celery[0].id,
            'name': 'campaign_name_1'
        },
        {
            'report_id': fixture_celery_reports[0].id,
            'campaign_eid': 'campaign_eid_2',
            'client_id': fixture_clients_celery[1].id,
            'name': 'campaign_name_2'
        }

    ]

    yield await models.Campaign.bulk_insert(rows)
    await models.Campaign.delete.gino.status()


@pytest.fixture
async def fixture_advertiser_organization_celery():
    rows = [
        {
            'partner_id': AGENCY_ID,
            'inn': '111222333',
            'name': 'advertiser_name_1',
            'type' : 'ul',
        },
        {
            'partner_id': AGENCY_ID,
            'inn': '222333111',
            'name': 'advertiser_name_2',
            'type' : 'ul',
        },
    ]

    yield await models.Organization.bulk_insert(rows)
    await models.Organization.delete.gino.status()


@pytest.fixture
async def fixture_advertiser_contragent_organization_celery():
    rows = [
        {
            'partner_id': AGENCY_ID,
            'inn': '12121212',
            'name': 'advertiser_contragent_name_1',
            'type' : 'ul',
        },
        {
            'partner_id': AGENCY_ID,
            'inn': '21212121',
            'name': 'advertiser_contragent_name_2',
            'type' : 'ul',
        },
    ]

    yield await models.Organization.bulk_insert(rows)
    await models.Organization.delete.gino.status()


@pytest.fixture
async def fixture_advertiser_contracts_celery(fixture_advertiser_organization_celery: typing.List[models.Organization],
                                              fixture_advertiser_contragent_organization_celery: typing.List[
                                              models.Organization]):
    rows = [
        {
            'partner_id': AGENCY_ID,
            'contractor_id': fixture_advertiser_contragent_organization_celery[0].id,
            'client_id': fixture_advertiser_organization_celery[0].id,
            'contract_eid': 'advertiser_contract_eid_1',
            'is_reg_report': True,
            'type': 'contract',
            'action_type': 'distribution',
            'subject_type': 'distribution',
            'date' : datetime.datetime(2022, 5, 1, tzinfo=datetime.timezone.utc),
            'amount': 3000,
            'is_vat': True
        },
        {
            'partner_id': AGENCY_ID,
            'contractor_id': fixture_advertiser_contragent_organization_celery[1].id,
            'client_id': fixture_advertiser_organization_celery[1].id,
            'contract_eid': 'advertiser_contract_eid_2',
            'is_reg_report': True,
            'type': 'contract',
            'action_type': 'distribution',
            'subject_type': 'distribution',
            'date' : datetime.datetime(2022, 5, 1, tzinfo=datetime.timezone.utc),
            'amount': 3000,
            'is_vat': True
        },
    ]

    yield await models.Contract.bulk_insert(rows)
    await models.Contract.delete.gino.status()


@pytest.fixture
async def fixture_ad_distributor_partner_organization_celery():
    rows = [
        {
            'partner_id': AGENCY_ID,
            'inn': '123456789',
            'name': 'agency_name_1',
            'type' : 'ul',
        },
    ]

    yield await models.Organization.bulk_insert(rows)
    await models.Organization.delete.gino.status()


@pytest.fixture
async def fixture_partner_contragent_organization_celery():
    rows = [
        {
            'partner_id': AGENCY_ID,
            'inn': '234567891',
            'name': 'contragent_name_1',
            'type' : 'ul',
        },
        {
            'partner_id': AGENCY_ID,
            'inn': '345678912',
            'name': 'contragent_name_2',
            'type' : 'ul',
        },
    ]

    yield await models.Organization.bulk_insert(rows)
    await models.Organization.delete.gino.status()


@pytest.fixture
async def fixture_partner_contracts_celery(fixture_ad_distributor_partner_organization_celery: typing.List[models.Organization],
                                           fixture_partner_contragent_organization_celery: typing.List[models.Organization]):
    rows = [
        {
            'partner_id': AGENCY_ID,
            'contractor_id': fixture_ad_distributor_partner_organization_celery[0].id,
            'client_id': fixture_partner_contragent_organization_celery[0].id,
            'contract_eid': 'partner_contract_eid_1',
            'is_reg_report': True,
            'type': 'contract',
            'action_type': 'distribution',
            'subject_type': 'distribution',
            'date' : datetime.datetime(2022, 5, 1, tzinfo=datetime.timezone.utc),
            'amount': 3000,
            'is_vat': True
        },
        {
            'partner_id': AGENCY_ID,
            'contractor_id': fixture_ad_distributor_partner_organization_celery[0].id,
            'client_id': fixture_partner_contragent_organization_celery[1].id,
            'contract_eid': 'partner_contract_eid_2',
            'is_reg_report': True,
            'type': 'contract',
            'action_type': 'distribution',
            'subject_type': 'distribution',
            'date' : datetime.datetime(2022, 5, 1, tzinfo=datetime.timezone.utc),
            'amount': 3000,
            'is_vat': True
        },
    ]

    yield await models.Contract.bulk_insert(rows)
    await models.Contract.delete.gino.status()


@pytest.fixture
async def fixture_ad_distributor_contracts_celery(fixture_ad_distributor_partner_organization_celery: typing.List[models.Organization]):
    rows = [
        {
            'partner_id': AGENCY_ID,
            'client_id': fixture_ad_distributor_partner_organization_celery[0].id,
            'contract_eid': 'ad_distributor_contract_eid_1',
            'is_reg_report': True,
            'type': 'contract',
            'action_type': 'distribution',
            'subject_type': 'distribution',
            'date' : datetime.datetime(2022, 5, 1, tzinfo=datetime.timezone.utc),
            'amount': 3000,
            'is_vat': True
        },
    ]

    yield await models.Contract.bulk_insert(rows)
    await models.Contract.delete.gino.status()


@pytest.fixture()
async def fixture_celery_client_rows(fixture_clients_celery: typing.List[models.Client],
                                     fixture_partner_acts_celery: typing.List[models.Act],
                                     fixture_campaign_celery: typing.List[models.Campaign],
                                     fixture_ad_distributor_acts: typing.List[models.Act],
                                     fixture_ad_distributor_contracts_celery: typing.List[models.Contract],
                                     fixture_advertiser_contracts_celery: typing.List[models.Contract],
                                     fixture_partner_contracts_celery: typing.List[models.Contract]):
    rows = [
        {
            'client_id': fixture_clients_celery[0].id,
            'partner_act_id': fixture_partner_acts_celery[0].id,
            'campaign_id': fixture_campaign_celery[0].id,
            'suggested_amount': 3000,
            'ad_distributor_act_id': fixture_ad_distributor_acts[0].id,
            'advertiser_contract_id': fixture_advertiser_contracts_celery[0].id,
            'partner_contract_id': fixture_partner_contracts_celery[0].id,
            'ad_distributor_contract_id': fixture_ad_distributor_contracts_celery[0].id
        },
        {
            'client_id': fixture_clients_celery[0].id,
            'partner_act_id': fixture_partner_acts_celery[1].id,
            'campaign_id': fixture_campaign_celery[0].id,
            'suggested_amount': 3000,
            'ad_distributor_act_id': fixture_ad_distributor_acts[0].id,
            'advertiser_contract_id': fixture_advertiser_contracts_celery[0].id,
            'partner_contract_id': fixture_partner_contracts_celery[0].id,
            'ad_distributor_contract_id': fixture_ad_distributor_contracts_celery[0].id
        },
        {
            'client_id': fixture_clients_celery[1].id,
            'partner_act_id': fixture_partner_acts_celery[2].id,
            'campaign_id': fixture_campaign_celery[1].id,
            'suggested_amount': 3000,
            'ad_distributor_act_id': fixture_ad_distributor_acts[1].id,
            'advertiser_contract_id': fixture_advertiser_contracts_celery[1].id,
            'partner_contract_id': fixture_partner_contracts_celery[1].id,
            'ad_distributor_contract_id': fixture_ad_distributor_contracts_celery[0].id
        }
    ]

    yield await models.ClientRow.bulk_insert(rows)
    await models.ClientRow.delete.gino.status()


@pytest.fixture()
async def fixture_celery_report_import(fixture_celery_reports):
    rows = [
        {
            'report_id': fixture_celery_reports[0].id,
            'file_id': None,
            'status': 'requested',
        },
    ]

    yield await models.ReportImportInfo.bulk_insert(rows)
    await models.ReportImportInfo.delete.gino.status()


@pytest.fixture()
async def fixture_celery_report_export(fixture_celery_reports):
    rows = [
        {
            'report_id': fixture_celery_reports[0].id,
            'file_id': None,
            'status': 'requested',
        },
    ]

    yield await models.ReportExportInfo.bulk_insert(rows)
    await models.ReportExportInfo.delete.gino.status()
