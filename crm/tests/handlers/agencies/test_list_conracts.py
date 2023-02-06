from datetime import datetime
from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.common import structs as grants_structs
from crm.agency_cabinet.rewards.common import structs as rewards_structs
from crm.agency_cabinet.common.consts import ContractType


URL = '/api/agencies/{agency_id}/contracts'


async def test_access_deny(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.DENY
    await client.get(URL.format(agency_id=1), expected_status=403)


async def test_list_contracts(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.ALLOW
    service_discovery.rewards.get_contracts_info.return_value = [
        rewards_structs.ContractInfo(
            contract_id=202806,
            eid='32240/15',
            inn='7825677325',
            payment_type='предоплата',
            services=['direct'],
            finish_date=datetime(2022, 3, 1),
            type=ContractType.base,
            is_crisis=True,
        ),
        rewards_structs.ContractInfo(
            contract_id=202800,
            eid='32240/16',
            inn='7825677325',
            payment_type='предоплата',
            services=['direct'],
            finish_date=datetime(2022, 3, 1),
            type=ContractType.prof,
            is_crisis=True,
        ),
        rewards_structs.ContractInfo(
            contract_id=202801,
            eid='32240/17',
            inn='7825677325',
            payment_type='предоплата',
            services=['direct'],
            finish_date=datetime(2022, 3, 1),
            type=ContractType.aggregator,
            is_crisis=True,
        ),
    ]
    got = await client.get(URL.format(agency_id=1), expected_status=200)

    expected = {
        'contracts': [
            {
                'contract_id': 202806,
                'eid': '32240/15',
                'finish_date': '2022-03-01T00:00:00',
                'services': ['direct'],
                'inn': '7825677325',
                'payment_type': 'предоплата',
                'type': 1,
                'is_crisis': True,
            },
            {
                'contract_id': 202800,
                'eid': '32240/16',
                'finish_date': '2022-03-01T00:00:00',
                'services': ['direct'],
                'inn': '7825677325',
                'payment_type': 'предоплата',
                'type': 2,
                'is_crisis': True,
            },
            {
                'contract_id': 202801,
                'eid': '32240/17',
                'finish_date': '2022-03-01T00:00:00',
                'services': ['direct'],
                'inn': '7825677325',
                'payment_type': 'предоплата',
                'type': 3,
                'is_crisis': True,
            }

        ]
    }

    assert got == expected

    service_discovery.grants.check_access_level.assert_awaited_with(yandex_uid=yandex_uid, agency_id=1)
