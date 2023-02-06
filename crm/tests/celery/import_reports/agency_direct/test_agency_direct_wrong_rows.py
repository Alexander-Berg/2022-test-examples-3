import pytest
from datetime import datetime


@pytest.fixture
def fixture_missed_rows():
    row_client_2 = [
        # Логин/Кампания
        'client_id_2', 'client_login_2', 'campaign_name_2', 3000, 'campaign_eid_2',
        # Площадка
        'ad_distributor_act_eid_2', 3000, 'да',
        # Я-Агентство
        '123456789', 'ul', 'agency_name_1', None, None, None, None, None,
        # Мой контрагент
        'new_345678912', 'ul', 'contragent_name_2', None, None, None, None, None,
        # Договор между агентством и его контрагентом
        'new_partner_contract_eid_2', 'contract', 'distribution', 'distribution', datetime(2022, 6, 1), 3000, 'да',
        'new_act3', 3000, 'да', False,
        # Контрагент конечного рекламодателя'
        'new_21212121', 'ul', 'advertiser_contragent_name_2', None, None, None, None, None,
        # Конечный рекламодатель
        'new_222333111', 'ul', 'advertiser_name_2', None, None, None, None, None,
        # Договор между конечным рекламодателем и его исполнителем
        'new_advertiser_contract_eid_2', 'contract', 'distribution', 'distribution', datetime(2022, 6, 1), 3000, 'да'
    ]

    return [
        row_client_2
    ]


def fixture_mismatch_rows():
    row_client_2_1 = [
        # Логин/Кампания
        'client_id_2', 'client_login_2', 'campaign_name_2', 3000, 'campaign_eid_2',
        # Площадка
        'ad_distributor_act_eid_2', 3000, 'да',
        # Я-Агентство
        '123456789', 'ul', 'agency_name_1', None, None, None, None, None,
        # Мой контрагент
        'new_345678912', 'ul', 'contragent_name_2', None, None, None, None, None,
        # Договор между агентством и его контрагентом
        'new_partner_contract_eid_2', 'contract', 'distribution', 'distribution', datetime(2022, 6, 1), 3000, 'да',
        'new_act3', 3000, 'да', False,
        # Контрагент конечного рекламодателя'
        'new_21212121', 'ul', 'advertiser_contragent_name_2', None, None, None, None, None,
        # Конечный рекламодатель
        'new_222333111', 'ul', 'advertiser_name_2', None, None, None, None, None,
        # Договор между конечным рекламодателем и его исполнителем
        'new_advertiser_contract_eid_2', 'contract', 'distribution', 'distribution', datetime(2022, 6, 1), 3000, 'да'
    ]

    row_client_2_2 = [
        # Логин/Кампания
        'client_id_2', 'client_login_2', 'campaign_name_2', 3000, 'campaign_eid_2',
        # Площадка
        'ad_distributor_act_eid_1', 3000, 'да',
        # Я-Агентство
        '123456789', 'ul', 'agency_name_1', None, None, None, None, None,
        # Мой контрагент
        'new_345678912', 'ul', 'contragent_name_2', None, None, None, None, None,
        # Договор между агентством и его контрагентом
        'new_partner_contract_eid_2', 'contract', 'distribution', 'distribution', datetime(2022, 6, 1), 3000, 'да',
        'new_act3', 3000, 'да', False,
        # Контрагент конечного рекламодателя'
        'new_21212121', 'ul', 'advertiser_contragent_name_2', None, None, None, None, None,
        # Конечный рекламодатель
        'new_222333111', 'ul', 'advertiser_name_2', None, None, None, None, None,
        # Договор между конечным рекламодателем и его исполнителем
        'new_advertiser_contract_eid_2', 'contract', 'distribution', 'distribution', datetime(2022, 6, 1), 3000, 'да'
    ]

    return [
        row_client_2_1,
        row_client_2_2,
    ]


@pytest.mark.parametrize(
    "fixture_agency_direct_rows",
    [
        fixture_missed_rows,
        fixture_mismatch_rows,
    ],
)
async def test_load_wrong_rows(
    agency_direct_report_importer,
    fixture_celery_report_import,
    fixture_celery_client_rows,
    fixture_agency_direct_rows,
):
    raise_exception = False
    try:
        await agency_direct_report_importer.init()
        for row in fixture_mismatch_rows:
            await agency_direct_report_importer._load_row(row)

        await agency_direct_report_importer.after_load()

    except Exception:
        raise_exception = True

    assert raise_exception is True
