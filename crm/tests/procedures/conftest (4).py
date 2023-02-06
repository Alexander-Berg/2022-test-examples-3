import pytest

from smb.common.testing_utils import dt

from crm.agency_cabinet.ord.server.src.db import models


AGENCY_ID = 1


@pytest.fixture()
async def fixture_reports2(fixture_report_settings):
    rows = [
        {
            'agency_id': AGENCY_ID,
            'settings_id': fixture_report_settings[1].id,
            'reporter_type': 'partner',
            'period_from': dt('2021-3-1 00:00:00'),
            'status': 'draft',
            'is_deleted': False,
        },
        {
            'agency_id': AGENCY_ID,
            'settings_id': fixture_report_settings[1].id,
            'reporter_type': 'partner',
            'period_from': dt('2021-4-1 00:00:00'),
            'status': 'draft',
            'is_deleted': False,
        },
        {
            'agency_id': 2,
            'settings_id': fixture_report_settings[1].id,
            'reporter_type': 'partner',
            'period_from': dt('2021-4-1 00:00:00'),
            'status': 'draft',
            'is_deleted': False,
        },
        {
            'agency_id': AGENCY_ID,
            'settings_id': fixture_report_settings[0].id,
            'reporter_type': 'partner',
            'period_from': dt('2021-4-1 00:00:00'),
            'status': 'draft',
            'is_deleted': False,
        },

    ]
    yield await models.Report.bulk_insert(rows)

    await models.Report.delete.gino.status()


@pytest.fixture
async def fixture_report_export(fixture_reports2, fixture_s3_mds_file):
    rows = [
        {
            'report_id': fixture_reports2[0].id,
            'file_id': None,
            'status': 'requested',
        },
        {
            'report_id': fixture_reports2[1].id,
            'file_id': fixture_s3_mds_file[1].id,
            'status': 'ready',
        },
        {
            'report_id': fixture_reports2[1].id,
            'file_id': None,
            'status': 'in_progress',
        },
        {
            'report_id': fixture_reports2[1].id,
            'file_id': None,
            'status': 'ready',
        },
    ]

    yield await models.ReportExportInfo.bulk_insert(rows)
    await models.ReportExportInfo.delete.gino.status()


@pytest.fixture()
async def fixture_s3_mds_file():
    rows = [
        {
            'bucket': 'bucket',
            'name': 'name',
            'display_name': 'display_name',
        },
        {
            'bucket': 'bucket2',
            'name': 'name2',
            'display_name': 'display_name2',
        },
    ]

    yield await models.S3MdsFile.bulk_insert(rows)
    await models.S3MdsFile.delete.gino.status()


@pytest.fixture()
async def fixture_clients2(fixture_reports2):
    rows = []
    for report in fixture_reports2:
        rows.extend([
            {
                'report_id': report.id,
                'name': 'test_client_name_1',
                'login': 'test_client_login_1',
                'client_id': '111',
                'is_yt': False
            },
            {
                'report_id': report.id,
                'name': 'test_client_name_2',
                'login': 'test_client_login_2',
                'client_id': '222',
                'is_yt': True
            }
        ])

    yield await models.Client.bulk_insert(rows)
    await models.Client.delete.gino.status()


@pytest.fixture()
async def fixture_campaign2(fixture_reports2, fixture_clients2):
    rows = [
        {
            'report_id': fixture_reports2[0].id,
            'client_id': fixture_clients2[0].id,
            'campaign_eid': 'campaign eid',
            'name': 'campaign name'
        }
    ]

    yield await models.Campaign.bulk_insert(rows)
    await models.Campaign.delete.gino.status()


@pytest.fixture()
async def fixture_client_rows2(
    fixture_clients2,
    fixture_campaign2,
    fixture_contract_procedure,
    fixture_act_procedure,
):
    rows = [
        {
            'client_id': fixture_clients2[0].id,
            'campaign_id': fixture_campaign2[0].id,
            'suggested_amount': 1000,
            'ad_distributor_contract_id': fixture_contract_procedure[0].id,
            'partner_contract_id': fixture_contract_procedure[0].id,
            'advertiser_contract_id': fixture_contract_procedure[0].id,
            'partner_act_id': fixture_act_procedure[0].id,
            'ad_distributor_act_id': fixture_act_procedure[0].id,
        },
        {
            'client_id': fixture_clients2[0].id,
            'campaign_id': fixture_campaign2[0].id,
            'suggested_amount': 2000,
            'ad_distributor_contract_id': fixture_contract_procedure[0].id,
            'partner_contract_id': fixture_contract_procedure[0].id,
            'advertiser_contract_id': fixture_contract_procedure[0].id,
            'partner_act_id': fixture_act_procedure[0].id,
            'ad_distributor_act_id': fixture_act_procedure[0].id,
        },
        {
            'client_id': fixture_clients2[0].id,
            'campaign_id': fixture_campaign2[0].id,
            'suggested_amount': 3000,
            'ad_distributor_contract_id': fixture_contract_procedure[0].id,
            'partner_contract_id': fixture_contract_procedure[0].id,
            'advertiser_contract_id': fixture_contract_procedure[0].id,
            'partner_act_id': fixture_act_procedure[0].id,
            'ad_distributor_act_id': fixture_act_procedure[0].id,
        },
        {
            'client_id': fixture_clients2[0].id,
            'campaign_id': fixture_campaign2[0].id,
            'suggested_amount': 4000,
            'ad_distributor_contract_id': fixture_contract_procedure[0].id,
            'partner_contract_id': fixture_contract_procedure[0].id,
            'advertiser_contract_id': fixture_contract_procedure[0].id,
            'partner_act_id': fixture_act_procedure[0].id,
            'ad_distributor_act_id': fixture_act_procedure[0].id,
        }
    ]

    yield await models.ClientRow.bulk_insert(rows)
    await models.ClientRow.delete.gino.status()


@pytest.fixture()
async def fixture_reports3(fixture_report_settings):
    rows = [
        {
            'agency_id': AGENCY_ID,
            'settings_id': fixture_report_settings[1].id,
            'reporter_type': 'partner',
            'period_from': dt('2021-3-1 00:00:00'),
            'status': 'draft',
            'is_deleted': False,
        },
    ]
    yield await models.Report.bulk_insert(rows)
    await models.Report.delete.gino.status()


@pytest.fixture()
async def fixture_clients3(fixture_reports3):
    rows = [
        {
            'report_id': fixture_reports3[0].id,
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
            'client_act_id': None,
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
async def fixture_organizations():
    rows = [
        {
            'partner_id': 1,
            'type': 'ip',
            'inn': 'some inn',
            'mobile_phone': None,
            'is_valid': True,
        },
        {
            'partner_id': 1,
            'type': 'ip',
            'inn': 'contractor inn',
            'mobile_phone': None,
            'is_valid': True,
        },
        {
            'partner_id': 1,
            'type': 'fl',
            'inn': None,
            'mobile_phone': '1234567890',
        }
    ]
    yield await models.Organization.bulk_insert(rows)
    await models.Organization.delete.gino.status()


@pytest.fixture()
async def fixture_contract_procedure(fixture_organizations):
    rows = [
        {
            'partner_id': 1,
            'client_id': fixture_organizations[0].id,
            'contractor_id': fixture_organizations[1].id,
            'contract_eid': 'aaaaaa',
            'is_valid': True,
        },
        {
            'partner_id': 1,
            'client_id': fixture_organizations[0].id,
            'contractor_id': fixture_organizations[1].id,
            'contract_eid': None,
            'is_valid': False,
        }
    ]
    yield await models.Contract.bulk_insert(rows)
    await models.Contract.delete.gino.status()


@pytest.fixture()
async def fixture_act_procedure(fixture_reports2):
    rows = [
        {
            'report_id': fixture_reports2[0].id,
            'act_eid': 'some eid',
            'is_valid': True,
        },
    ]
    yield await models.Act.bulk_insert(rows)
    await models.Act.delete.gino.status()


@pytest.fixture()
async def fixture_client_rows_has_errors(
    fixture_clients2,
    fixture_campaign2,
    fixture_contract_procedure,
    fixture_act_procedure,
):
    rows = [
        {
            'client_id': fixture_clients2[0].id,
            'campaign_id': fixture_campaign2[0].id,
            'suggested_amount': 1000,
            'ad_distributor_contract_id': fixture_contract_procedure[0].id,
            'partner_contract_id': fixture_contract_procedure[0].id,
            'advertiser_contract_id': fixture_contract_procedure[1].id,
            'partner_act_id': fixture_act_procedure[0].id,
            'ad_distributor_act_id': fixture_act_procedure[0].id,
        },
        {
            'client_id': fixture_clients2[1].id,
            'campaign_id': fixture_campaign2[0].id,
            'suggested_amount': 2000,
            'ad_distributor_contract_id': fixture_contract_procedure[0].id,
            'partner_contract_id': fixture_contract_procedure[1].id,
            'advertiser_contract_id': fixture_contract_procedure[0].id,
            'partner_act_id': fixture_act_procedure[0].id,
            'ad_distributor_act_id': fixture_act_procedure[0].id,
        },
        {
            'client_id': fixture_clients2[2].id,
            'campaign_id': fixture_campaign2[0].id,
            'suggested_amount': 3000,
            'ad_distributor_contract_id': fixture_contract_procedure[0].id,
            'partner_contract_id': fixture_contract_procedure[0].id,
            'advertiser_contract_id': fixture_contract_procedure[0].id,
            'partner_act_id': None,
            'ad_distributor_act_id': fixture_act_procedure[0].id,
        },
        {
            'client_id': fixture_clients2[3].id,
            'campaign_id': fixture_campaign2[0].id,
            'suggested_amount': 4000,
            'ad_distributor_contract_id': fixture_contract_procedure[0].id,
            'partner_contract_id': fixture_contract_procedure[0].id,
            'advertiser_contract_id': fixture_contract_procedure[0].id,
            'partner_act_id': fixture_act_procedure[0].id,
            'ad_distributor_act_id': fixture_act_procedure[0].id,
        }
    ]

    yield await models.ClientRow.bulk_insert(rows)
    await models.ClientRow.delete.gino.status()


@pytest.fixture()
async def fixture_acts_procedures(fixture_reports2):
    rows = [
        {
            'report_id': fixture_reports2[0].id,
            'act_eid': 'act1',
            'amount': 1,
            'is_vat': False,
        },
        {
            'report_id': fixture_reports2[0].id,
            'act_eid': 'act2',
            'amount': 2,
            'is_vat': False,
        },
        {
            'report_id': fixture_reports2[0].id,
            'act_eid': 'act3',
            'amount': None,
            'is_vat': None,
        },
        {
            'report_id': fixture_reports2[0].id,
            'act_eid': 'act4',
            'amount': 3,
            'is_vat': False,
        },
        {
            'report_id': fixture_reports2[3].id,
            'act_eid': 'act5',
            'amount': 5,
            'is_vat': True,
        },
    ]
    yield await models.Act.bulk_insert(rows)
    await models.Act.delete.gino.status()


@pytest.fixture()
async def fixture_invites_procedures():
    rows = [
        {
            'partner_id': AGENCY_ID,
            'requestor_uid': 123,
            'email': 'invite1@yandex.ru',
            'invited_partner_id': 10,
            'status': 'sent',
        },
        {
            'partner_id': AGENCY_ID,
            'requestor_uid': 123,
            'email': 'invite2@yandex.ru',
            'invited_partner_id': 11,
            'status': 'accepted',
        },
        {
            'partner_id': AGENCY_ID,
            'requestor_uid': 123,
            'email': 'invite3@yandex.ru',
            'invited_partner_id': 12,
            'status': 'sent',
        },
        {
            'partner_id': AGENCY_ID,
            'requestor_uid': 123,
            'email': 'invite4@yandex.ru',
            'invited_partner_id': 13,
            'status': 'revoked',
        },
        {
            'partner_id': 4,
            'requestor_uid': 234,
            'email': 'invite5@yandex.ru',
            'invited_partner_id': 14,
            'status': 'sent',
        },
    ]
    yield await models.Invitation.bulk_insert(rows)
    await models.Invitation.delete.gino.status()
