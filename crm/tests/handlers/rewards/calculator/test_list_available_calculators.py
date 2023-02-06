from crm.agency_cabinet.common.consts import ContractType
from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.common import structs as grants_structs
from crm.agency_cabinet.rewards.common import structs as rewards_structs


URL = '/api/agencies/{agency_id}/calculator'


async def test_access_deny(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.DENY
    await client.get(URL.format(agency_id=1), expected_status=403)


async def test_get_all_contracts_data(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.ALLOW

    service_discovery.rewards.list_available_calculators.return_value = rewards_structs.ListAvailableCalculatorsResponse(
        calculators_descriptions=[
            rewards_structs.CalculatorDescription(
                contract_id=123456,
                service='media',
                contract_type=ContractType.base.value,
                version='2021'),
            rewards_structs.CalculatorDescription(
                contract_id=123456,
                service='video',
                contract_type=ContractType.base.value,
                version='2021'),
            rewards_structs.CalculatorDescription(
                contract_id=123457,
                service='media',
                contract_type=ContractType.prof.value,
                version='2022')])

    got = await client.get(URL.format(agency_id=1), expected_status=200)

    assert got == {'items': [{'contract_id': 123456,
                              'contract_type': 1,
                              'service': 'media',
                              'version': '2021'},
                             {'contract_id': 123456,
                              'contract_type': 1,
                              'service': 'video',
                              'version': '2021'},
                             {'contract_id': 123457,
                              'contract_type': 2,
                              'service': 'media',
                              'version': '2022'}
                             ]}

    service_discovery.grants.check_access_level.assert_awaited_with(yandex_uid=yandex_uid, agency_id=1)
    service_discovery.rewards.list_available_calculators.assert_awaited_with(agency_id=1, contract_id=None)


async def test_get_one_contract_data(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.ALLOW

    service_discovery.rewards.list_available_calculators.return_value = rewards_structs.ListAvailableCalculatorsResponse(
        calculators_descriptions=[
            rewards_structs.CalculatorDescription(
                contract_id=123456,
                service='media',
                contract_type='base',
                version='2021'),
            rewards_structs.CalculatorDescription(
                contract_id=123456,
                service='video',
                contract_type='base',
                version='2021'),
        ])

    got = await client.get(URL.format(agency_id=1) + '?contract_id=123456', expected_status=200)

    assert got == {'items': [{'contract_id': 123456,
                              'contract_type': 1,
                              'service': 'media',
                              'version': '2021'},
                             {'contract_id': 123456,
                              'contract_type': 1,
                              'service': 'video',
                              'version': '2021'}]}

    service_discovery.grants.check_access_level.assert_awaited_with(yandex_uid=yandex_uid, agency_id=1)
    service_discovery.rewards.list_available_calculators.assert_awaited_with(agency_id=1, contract_id=123456)
