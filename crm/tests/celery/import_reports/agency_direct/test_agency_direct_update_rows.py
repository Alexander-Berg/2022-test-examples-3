import pytest
from datetime import datetime, timezone
from crm.agency_cabinet.ord.server.src.db import models

from crm.agency_cabinet.ord.server.tests.celery.import_reports.agency_direct.base import assert_report_data


@pytest.fixture
def fixture_update_rows():
    row_client_1 = [
        # Логин/Кампания
        'client_id_1', 'client_login_1', 'campaign_name_1', 3000, 'campaign_eid_1',
        # Площадка
        'ad_distributor_act_eid_1', 3000, True,
        # Я-Агентство
        '123456789', 'ul', 'agency_name_1', None, None, None, None, None,
        # Мой контрагент
        '234567891', 'ul', 'edit_contragent_name_1', None, None, None, None, None,
        # Договор между агентством и его контрагентом
        'partner_contract_eid_1', 'contract', 'distribution', 'distribution', datetime(2022, 6, 1), 3000, 'да',
        'act1', 10000, 'да', False,
        # Контрагент конечного рекламодателя'
        '12121212', 'ul', 'edit_advertiser_contragent_name_1', None, None, None, None, None,
        # Конечный рекламодатель
        '111222333', 'ul', 'edit_advertiser_name_1', None, None, None, None, None,
        # Договор между конечным рекламодателем и его исполнителем
        'advertiser_contract_eid_1', 'contract', 'distribution', 'distribution', datetime(2022, 6, 1), 3000, 'да'
    ]

    row_client_1_2 = [
        # Логин/Кампания
        'client_id_1', 'client_login_1', 'campaign_name_1', 3000, 'campaign_eid_1',
        # Площадка
        'ad_distributor_act_eid_1', 3000, True,
        # Я-Агентство
        '123456789', 'ul', 'agency_name_1', None, None, None, None, None,
        # Мой контрагент
        '234567891', 'ul', 'edit_contragent_name_1', None, None, None, None, None,
        # Договор между агентством и его контрагентом
        'partner_contract_eid_1', 'contract', 'distribution', 'distribution', datetime(2022, 6, 1), 3000, 'да',
        'act2', 20000, 'да', False,
        # Контрагент конечного рекламодателя'
        '12121212', 'ul', 'edit_advertiser_contragent_name_1', None, None, None, None, None,
        # Конечный рекламодатель
        '111222333', 'ul', 'edit_advertiser_name_1', None, None, None, None, None,
        # Договор между конечным рекламодателем и его исполнителем
        'advertiser_contract_eid_1', 'contract', 'distribution', 'distribution', datetime(2022, 6, 1), 3000, 'да'
    ]

    row_client_2 = [
        # Логин/Кампания
        'client_id_2', 'client_login_2', 'campaign_name_2', 3000, 'campaign_eid_2',
        # Площадка
        'ad_distributor_act_eid_2', 3000, True,
        # Я-Агентство
        '123456789', 'ul', 'agency_name_1', None, None, None, None, None,
        # Мой контрагент
        '345678912', 'ul', 'edit_contragent_name_2', None, None, None, None, None,
        # Договор между агентством и его контрагентом
        'partner_contract_eid_2', 'contract', 'distribution', 'distribution', datetime(2022, 6, 1), 3000, 'да',
        'act3', 30000, 'да', False,
        # Контрагент конечного рекламодателя'
        '21212121', 'ul', 'edit_advertiser_contragent_name_2', None, None, None, None, None,
        # Конечный рекламодатель
        '222333111', 'ul', 'edit_advertiser_name_2', None, None, None, None, None,
        # Договор между конечным рекламодателем и его исполнителем
        'edit_advertiser_contract_eid_2', 'contract', 'distribution', 'distribution', datetime(2022, 6, 1), 3000, 'да'
    ]

    return [
        row_client_1,
        row_client_1_2,
        row_client_2
    ]


async def test_import_update_rows(
    fixture_celery_report_import,
    fixture_celery_client_rows,
    agency_direct_report_importer,
    fixture_update_rows,
):
    await agency_direct_report_importer.init()
    for row in fixture_update_rows:
        await agency_direct_report_importer._load_row(row)
    await agency_direct_report_importer.after_load()

    client_rows = await models.ClientRow.query.order_by(models.ClientRow.id).gino.all()

    expected_partner_acts = [
        ('act1', 10000, True),
        ('act2', 20000, True),
        ('act3', 30000, True),
    ]

    expected_partner_contracts = [
        (
            'partner_contract_eid_1', 'contract', 'distribution', 'distribution',
            datetime(2022, 6, 1, tzinfo=timezone.utc), 3000, True
        ),
        (
            'partner_contract_eid_1', 'contract', 'distribution', 'distribution',
            datetime(2022, 6, 1, tzinfo=timezone.utc), 3000, True
        ),
        (
            'partner_contract_eid_2', 'contract', 'distribution', 'distribution',
            datetime(2022, 6, 1, tzinfo=timezone.utc), 3000, True
        )
    ]

    expected_partner_client_orgs = [
        ('234567891', 'ul', 'edit_contragent_name_1'),
        ('234567891', 'ul', 'edit_contragent_name_1'),
        ('345678912', 'ul', 'edit_contragent_name_2'),
    ]

    expected_advertiser_contracts = [
        (
            'advertiser_contract_eid_1', 'contract', 'distribution', 'distribution',
            datetime(2022, 6, 1, tzinfo=timezone.utc), 3000, True
        ),
        (
            'advertiser_contract_eid_1', 'contract', 'distribution', 'distribution',
            datetime(2022, 6, 1, tzinfo=timezone.utc), 3000, True
        ),
        (
            'edit_advertiser_contract_eid_2', 'contract', 'distribution', 'distribution',
            datetime(2022, 6, 1, tzinfo=timezone.utc), 3000, True
        )
    ]

    expected_advertiser_orgs = [
        ('111222333', 'ul', 'edit_advertiser_name_1'),
        ('111222333', 'ul', 'edit_advertiser_name_1'),
        ('222333111', 'ul', 'edit_advertiser_name_2'),
    ]

    expected_advertiser_contractor_orgs = [
        ('12121212', 'ul', 'edit_advertiser_contragent_name_1'),
        ('12121212', 'ul', 'edit_advertiser_contragent_name_1'),
        ('21212121', 'ul', 'edit_advertiser_contragent_name_2'),
    ]

    await assert_report_data(
        client_rows,
        expected_partner_acts,
        expected_partner_contracts,
        expected_partner_client_orgs,
        expected_advertiser_contracts,
        expected_advertiser_orgs,
        expected_advertiser_contractor_orgs
    )


@pytest.fixture
def fixture_celery_agency_direct_remove_rows():
    row_client_1 = [
        # Логин/Кампания
        'client_id_1', 'client_login_1', 'campaign_name_1', 3000, 'campaign_eid_1',
        # Площадка
        'ad_distributor_act_eid_1', 3000, 'да',
        # Я-Агентство
        '123456789', 'ul', 'agency_name_1', None, None, None, None, None,
        # Мой контрагент
        '234567891', 'ul', 'edit_contragent_name_1', None, None, None, None, None,
        # Договор между агентством и его контрагентом
        'partner_contract_eid_1', 'contract', 'distribution', 'distribution', datetime(2022, 6, 1), 3000, 'да',
        'act1', 10000, 'да', False,
        # Контрагент конечного рекламодателя'
        '12121212', 'ul', 'edit_advertiser_contragent_name_1', None, None, None, None, None,
        # Конечный рекламодатель
        '111222333', 'ul', 'edit_advertiser_name_1', None, None, None, None, None,
        # Договор между конечным рекламодателем и его исполнителем
        'advertiser_contract_eid_1', 'contract', 'distribution', 'distribution', datetime(2022, 6, 1), 3000, 'да'
    ]

    row_client_2 = [
        # Логин/Кампания
        'client_id_2', 'client_login_2', 'campaign_name_2', 3000, 'campaign_eid_2',
        # Площадка
        'ad_distributor_act_eid_2', 3000, 'да',
        # Я-Агентство
        '123456789', 'ul', 'agency_name_1', None, None, None, None, None,
        # Мой контрагент
        '345678912', 'ul', 'edit_contragent_name_2', None, None, None, None, None,
        # Договор между агентством и его контрагентом
        'partner_contract_eid_2', 'contract', 'distribution', 'distribution', datetime(2022, 6, 1), 3000, 'да',
        'act3', 30000, 'да', False,
        # Контрагент конечного рекламодателя'
        '21212121', 'ul', 'edit_advertiser_contragent_name_2', None, None, None, None, None,
        # Конечный рекламодатель
        '222333111', 'ul', 'edit_advertiser_name_2', None, None, None, None, None,
        # Договор между конечным рекламодателем и его исполнителем
        'edit_advertiser_contract_eid_2', 'contract', 'distribution', 'distribution', datetime(2022, 6, 1), 3000, 'да'
    ]

    return [
        row_client_1,
        row_client_2
    ]


async def test_load_remove_rows(
    fixture_celery_report_import,
    fixture_celery_client_rows,
    agency_direct_report_importer,
    fixture_celery_agency_direct_remove_rows,
):
    await agency_direct_report_importer.init()
    for row in fixture_celery_agency_direct_remove_rows:
        await agency_direct_report_importer._load_row(row)
    await agency_direct_report_importer.after_load()

    client_rows = await models.ClientRow.query.order_by(models.ClientRow.id).gino.all()

    expected_partner_acts = [
        ('act1', 10000, True),
        ('act3', 30000, True),
    ]

    expected_partner_contracts = [
        (
            'partner_contract_eid_1', 'contract', 'distribution', 'distribution',
            datetime(2022, 6, 1, tzinfo=timezone.utc), 3000, True
        ),
        (
            'partner_contract_eid_2', 'contract', 'distribution', 'distribution',
            datetime(2022, 6, 1, tzinfo=timezone.utc), 3000, True
        )
    ]

    expected_partner_client_orgs = [
        ('234567891', 'ul', 'edit_contragent_name_1'),
        ('345678912', 'ul', 'edit_contragent_name_2'),
    ]

    expected_advertiser_contracts = [
        (
            'advertiser_contract_eid_1', 'contract', 'distribution', 'distribution',
            datetime(2022, 6, 1, tzinfo=timezone.utc), 3000, True
        ),
        (
            'edit_advertiser_contract_eid_2', 'contract', 'distribution', 'distribution',
            datetime(2022, 6, 1, tzinfo=timezone.utc), 3000, True
        )
    ]

    expected_advertiser_orgs = [
        ('111222333', 'ul', 'edit_advertiser_name_1'),
        ('222333111', 'ul', 'edit_advertiser_name_2'),
    ]

    expected_advertiser_contractor_orgs = [
        ('12121212', 'ul', 'edit_advertiser_contragent_name_1'),
        ('21212121', 'ul', 'edit_advertiser_contragent_name_2'),
    ]

    await assert_report_data(
        client_rows,
        expected_partner_acts,
        expected_partner_contracts,
        expected_partner_client_orgs,
        expected_advertiser_contracts,
        expected_advertiser_orgs,
        expected_advertiser_contractor_orgs
    )
