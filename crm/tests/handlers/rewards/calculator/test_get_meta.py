from datetime import datetime
from crm.agency_cabinet.common.consts import ContractType
from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.common import structs as grants_structs
from crm.agency_cabinet.rewards.common import structs as rewards_structs


URL = '/api/agencies/{agency_id}/calculator/meta?contract_id={contract_id}&service={service}'


async def test_access_deny(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.DENY
    await client.get(URL.format(agency_id=1, contract_id=123456, service='direct'), expected_status=403)


async def test_validation_query_parameters(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.ALLOW
    await client.get(
        '/api/agencies/{agency_id}/calculator/meta?contract_id={contract_id}'.format(agency_id=1, contract_id=123456),
        expected_status=422
    )

    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.ALLOW
    await client.get(
        '/api/agencies/{agency_id}/calculator/meta?service={service}'.format(agency_id=1, service=123456),
        expected_status=422
    )

    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.ALLOW
    await client.get(
        '/api/agencies/{agency_id}/calculator/meta'.format(agency_id=1),
        expected_status=422
    )


async def test_get_direct_meta(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.ALLOW

    service_discovery.rewards.get_contracts_info.return_value = [
        rewards_structs.ContractInfo(
            contract_id=123456,
            eid='1111/1',
            inn='123456',
            services=['direct', 'media'],
            finish_date=datetime(2022, 3, 1),
            payment_type='postpayment',
            type=ContractType.prof,
            is_crisis=True,
        )
    ]

    service_discovery.rewards.get_calculator_meta.return_value = rewards_structs.GetCalculatorMetaResponse(
        result='{"month": {"grades": [{"grade_id": "D", "threshold_start": 0, "threshold_end": 150000, '
               '"reward_percent": 5, "reward_fix": 0}, {"grade_id": "C", "threshold_start": 150000, '
               '"threshold_end": 800000, "reward_percent": 4, "reward_fix": 7500}, '
               '{"grade_id": "B", "threshold_start": 800000, "threshold_end": 5000000, "reward_percent": 3, '
               '"reward_fix": 33500}, {"grade_id": "A", "threshold_start": 5000000, "reward_percent": 2, '
               '"reward_fix": 159500}], "indexes": [{"index_id": "rsya", "reward_percent": 5}, '
               '{"index_id": "early_payment", "reward_percent": 2}]}, '
               '"quarter": {"indexes": [{"index_id": "metrica", "reward_percent": 2}, '
               '{"index_id": "conversion_autostrategy", "reward_percent": 6}, '
               '{"index_id": "key_goals", "reward_percent": 3}, '
               '{"index_id": "video_cpc", "reward_percent": 15}, '
               '{"index_id": "product_gallery", "reward_percent": 6}, {"index_id": "k50", "reward_percent": 3}, '
               '{"index_id": "retargeting", "reward_percent": 6},'
               ' {"index_id": "search_autotargeting", "reward_percent": 6}, '
               '{"index_id": "rmp", "reward_percent": 12}]}}'
    )

    got = await client.get(URL.format(agency_id=1, contract_id=123456, service='direct'), expected_status=200)

    assert 'month' in got
    assert 'quarter' in got

    service_discovery.rewards.get_calculator_meta.return_value = rewards_structs.GetCalculatorMetaResponse(
        result='{"month":{"grades":[{"grade_id":"D","threshold_start":0,"threshold_end":150000,"reward_percent":5, '
        '"reward_fix":0},{"grade_id":"C","threshold_start":150000,"threshold_end":1000000,"reward_percent":4, '
        '"reward_fix":7500},{"grade_id":"B","threshold_start":1000000,"threshold_end":5000000,"reward_percent":3,'
        '"reward_fix":41500},{"grade_id":"A","threshold_start":5000000,"threshold_end":null,"reward_percent":2,'
        '"reward_fix":161500}],"indexes":[{"index_id":"rsya","reward_percent":5}]},'
        '"quarters":[{"indexes":[{"index_id":"rmp","reward_percent":5},{"index_id":"conversion_autostrategy",'
        '"reward_percent":5},{"index_id":"metrica","reward_percent":3},{"index_id":"key_goals","reward_percent":3},'
        '{"index_id":"video_cpc","reward_percent":5},{"index_id":"smart_banners","reward_percent":3},'
        '{"index_id":"k50","reward_percent":2}]},{"indexes":[{"index_id":"conversion_autostrategy","reward_percent":5},'
        '{"index_id":"metrica","reward_percent":3},{"index_id":"key_goals","reward_percent":3},'
        '{"index_id":"video_cpc","reward_percent":5},{"index_id":"smart_banners","reward_percent":3},'
        '{"index_id":"k50","reward_percent":2}],"indexes_with_grade":[{"index_id":"rmp","grades":[{"threshold_start":0,'
        '"threshold_end":1000000,"reward_percent":5},{"threshold_start":1000000,"threshold_end":15000000,'
        '"reward_percent":7},{"threshold_start":15000000,"threshold_end":30000000,"reward_percent":9},'
        '{"threshold_start":30000000,"threshold_end":50000000,"reward_percent":11},{"threshold_start":50000000,'
        '"threshold_end":null,"reward_percent":13}]}]},{"indexes":[{"index_id":"conversion_autostrategy",'
        '"reward_percent":6},{"index_id":"metrica","reward_percent":2},{"index_id":"key_goals","reward_percent":4},'
        '{"index_id":"video_cpc","reward_percent":10},{"index_id":"smart_banners","reward_percent":3},'
        '{"index_id":"k50",''"reward_percent":2},{"index_id":"retargeting","reward_percent":5}],'
        '"indexes_with_grade":[{"index_id":"rmp","grades":[{"threshold_start":0,"threshold_end":1000000,'
        '"reward_percent":5},{"threshold_start":1000000,"threshold_end":15000000,"reward_percent":7},'
        '{"threshold_start":15000000,"threshold_end":30000000,"reward_percent":9},'
        '{"threshold_start":30000000,"threshold_end":50000000,"reward_percent":11},'
        '{"threshold_start":50000000,"threshold_end":null,"reward_percent":13}]}]},'
        '{"indexes":[{"index_id":"conversion_autostrategy","reward_percent":6},''{"index_id":"metrica",'
        '"reward_percent":2},{"index_id":"key_goals","reward_percent":4},{"index_id":"video_cpc","reward_percent":10},'
        '{"index_id":"smart_banners","reward_percent":3},{"index_id":"k50","reward_percent":2},'
        '{"index_id":"retargeting","reward_percent":5}],"indexes_with_grade":[{"index_id":"rmp",'
        '"grades":[{"threshold_start":0,"threshold_end":1000000,"reward_percent":5},{"threshold_start":1000000,'
        '"threshold_end":15000000,"reward_percent":7},{"threshold_start":15000000,"threshold_end":30000000,'
        '"reward_percent":9},{"threshold_start":30000000,"threshold_end":50000000,"reward_percent":11},'
        '{"threshold_start":50000000,"threshold_end":null,"reward_percent":13}]}]}]}'
    )

    got = await client.get(URL.format(agency_id=1, contract_id=123456, service='direct'), expected_status=200)

    assert 'month' in got
    assert 'quarters' in got
    assert len(got['quarters']) == 4
    assert 'indexes_with_grade' in got['quarters'][1]

    service_discovery.grants.check_access_level.assert_awaited_with(yandex_uid=yandex_uid, agency_id=1)


async def test_get_media_meta(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.ALLOW

    service_discovery.rewards.get_contracts_info.return_value = [
        rewards_structs.ContractInfo(
            contract_id=123456,
            eid='1111/1',
            inn='123456',
            services=['direct', 'media'],
            finish_date=datetime(2022, 3, 1),
            payment_type='postpayment',
            type=ContractType.prof,
            is_crisis=True,
        )
    ]

    service_discovery.rewards.get_calculator_meta.return_value = rewards_structs.GetCalculatorMetaResponse(
        result='{"month": {"indexes": [{"index_id": "revenue", "reward_percent": 13}, '
               '{"index_id": "early_payment", "reward_percent": 2}]}, '
               '"quarter": {"indexes_with_grade": [{"index_id": "revenue_growth", "grades": '
               '[{"threshold_start": 0, "threshold_end": 1.2, "reward_percent": 0}, '
               '{"threshold_start": 1.2, "threshold_end": 1.4, "reward_percent": 5}, '
               '{"threshold_start": 1.4, "threshold_end": 1.6, "reward_percent": 7}, '
               '{"threshold_start": 1.6, "threshold_end": 1.8, "reward_percent": 9}, '
               '{"threshold_start": 1.8, "threshold_end": 2, "reward_percent": 11}, '
               '{"threshold_start": 2, "reward_percent": 12}]}]}, '
               '"semiyear": {"indexes": [{"index_id": "revenue_new_clients", "reward_percent": 10}]}}'
    )

    got = await client.get(URL.format(agency_id=1, contract_id=123456, service='media'), expected_status=200)

    assert 'month' in got
    assert 'quarter' in got
    assert 'semiyear' in got

    service_discovery.grants.check_access_level.assert_awaited_with(yandex_uid=yandex_uid, agency_id=1)


async def test_get_video_meta(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.ALLOW

    service_discovery.rewards.get_contracts_info.return_value = [
        rewards_structs.ContractInfo(
            contract_id=123456,
            eid='1111/1',
            inn='123456',
            services=['direct', 'media', 'video'],
            finish_date=datetime(2022, 3, 1),
            payment_type='postpayment',
            type=ContractType.prof,
            is_crisis=True,
        )
    ]

    service_discovery.rewards.get_calculator_meta.return_value = rewards_structs.GetCalculatorMetaResponse(
        result='{"month": {"indexes": [{"index_id": "revenue", "reward_percent": 13}, '
               '{"index_id": "early_payment", "reward_percent": 2}]}, '
               '"quarter": {"indexes": [{"index_id": "outstream", "reward_percent": 12}], '
               '"indexes_with_grade": [{"index_id": "revenue_growth", "grades": [{"threshold_start": 0, '
               '"threshold_end": 1.2, "reward_percent": 0}, {"threshold_start": 1.2, "threshold_end": 1.4, '
               '"reward_percent": 6}, {"threshold_start": 1.4, "threshold_end": 1.6, "reward_percent": 8},'
               ' {"threshold_start": 1.6, "threshold_end": 1.8, "reward_percent": 10}, '
               '{"threshold_start": 1.8, "threshold_end": 2, "reward_percent": 12}, '
               '{"threshold_start": 2, "reward_percent": 14}]}]}, '
               '"semiyear": {"indexes": [{"index_id": "revenue_new_clients", "reward_percent": 10}]}}'
    )

    got = await client.get(URL.format(agency_id=1, contract_id=123456, service='video'), expected_status=200)

    assert 'month' in got
    assert 'quarter' in got
    assert 'semiyear' in got

    service_discovery.grants.check_access_level.assert_awaited_with(yandex_uid=yandex_uid, agency_id=1)
