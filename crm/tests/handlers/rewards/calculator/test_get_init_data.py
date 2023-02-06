from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.common.consts import CalculatorServiceType
from crm.agency_cabinet.grants.common import structs as grants_structs
from crm.agency_cabinet.rewards.common import structs as rewards_structs
from crm.agency_cabinet.rewards.client import NoSuchCalculatorDataException


URL = '/api/agencies/{agency_id}/calculator/init?contract_id={contract_id}&service={service}&version={version}'


async def test_access_deny(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.DENY
    await client.get(URL.format(agency_id=1, contract_id=123456, service='direct', version='2021'), expected_status=403)


async def test_validation_query_parameters(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.ALLOW
    await client.get(
        '/api/agencies/{agency_id}/calculator/init?contract_id={contract_id}'.format(agency_id=1, contract_id=123456),
        expected_status=422
    )

    await client.get(
        '/api/agencies/{agency_id}/calculator/init?service={service}'.format(agency_id=1, service=123456),
        expected_status=422
    )

    await client.get(
        '/api/agencies/{agency_id}/calculator/init'.format(agency_id=1),
        expected_status=422
    )

    await client.get(URL.format(agency_id=1, contract_id=123456, service='unknown', version='2021'), expected_status=422)


async def test_get_direct_init(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.ALLOW

    service_discovery.rewards.get_calculator_data.return_value = rewards_structs.GetCalculatorDataResponse(
        result='{"months": [{"grades": ''[{"grade_id": "A", "domains_count": 3, "revenue_average": 7176345.426750123}, '
        '{"grade_id": "B", "domains_count": 12, "revenue_average": 2128742.5424999995},'
        '{"grade_id": "C", "domains_count": 31, "revenue_average": 507331.2416018652},'
        '{"grade_id": "D", "domains_count": 76, "revenue_average": 23861.992501233402}],'
        '"indexes": [{"revenue": 27188378.864928298, "index_id": "conversion_autostrategy"},'
        '{"revenue": 58588881.36666666, "index_id": "early_payment"}, '
        '{"revenue": 9916645.481047032, "index_id": "k50"}, '
        '{"revenue": 20642860.739909828, "index_id": "key_goals"}, '
        '{"revenue": 57872780.57286661, "index_id": "metrica"}, '
        '{"revenue": 380337.54249253875, "index_id": "rmp"}, '
        '{"revenue": 24417814.844747413, "index_id": "rsya"}, '
        '{"revenue": 12113965.084667044, "index_id": "smart_banners"}, '
        '{"revenue": 409850.6197489128, "index_id": "video_cpc"}], '
        '"predict": false, "period_from": "2021-03-01T00:00:00"}]}'
    )

    got = await client.get(URL.format(agency_id=1, contract_id=123456, service='direct', version='2021'), expected_status=200)
    assert len(got['months']) == 1
    assert len(got['months'][0]['grades']) == 4
    assert len(got['months'][0]['indexes']) == 9

    assert got['months'][0] == \
        {
            "grades": [
                {"grade_id": "A", "domains_count": 3, "revenue_average": 7176345.426750123},
                {"grade_id": "B", "domains_count": 12, "revenue_average": 2128742.5424999995},
                {"grade_id": "C", "domains_count": 31, "revenue_average": 507331.2416018652},
                {"grade_id": "D", "domains_count": 76, "revenue_average": 23861.992501233402}
            ],
            "indexes": [
                {"revenue": 27188378.864928298, "index_id": "conversion_autostrategy"},
                {"revenue": 58588881.36666666, "index_id": "early_payment"},
                {"revenue": 9916645.481047032, "index_id": "k50"},
                {"revenue": 20642860.739909828, "index_id": "key_goals"},
                {"revenue": 57872780.57286661, "index_id": "metrica"},
                {"revenue": 380337.54249253875, "index_id": "rmp"},
                {"revenue": 24417814.844747413, "index_id": "rsya"},
                {"revenue": 12113965.084667044, "index_id": "smart_banners"},
                {"revenue": 409850.6197489128, "index_id": "video_cpc"}
            ],
            "predict": False,
            "period_from": "2021-03-01T00:00:00"
        }

    service_discovery.grants.check_access_level.assert_awaited_with(yandex_uid=yandex_uid, agency_id=1)
    service_discovery.rewards.get_calculator_data.assert_awaited_with(
        agency_id=1,
        contract_id=123456,
        service=CalculatorServiceType.direct,
        version='2021'

    )


async def test_get_direct_init_data_not_found(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.ALLOW
    service_discovery.rewards.get_calculator_data.side_effect = NoSuchCalculatorDataException()

    await client.get(URL.format(agency_id=1, contract_id=123456, service='direct', version='2021'), expected_status=404)

    service_discovery.grants.check_access_level.assert_awaited_with(yandex_uid=yandex_uid, agency_id=1)
    service_discovery.rewards.get_calculator_data.assert_awaited_with(
        agency_id=1,
        contract_id=123456,
        service=CalculatorServiceType.direct,
        version='2021'
    )
