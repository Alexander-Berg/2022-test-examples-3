import pytest

from datetime import datetime

from crm.agency_cabinet.agencies.common import structs as agencies_structs
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.grants.common import structs as grants_structs
from crm.agency_cabinet.rewards.common import structs as rewards_structs
from crm.agency_cabinet.common.consts import ContractType

URL = '/api/agencies/{agency_id}'


async def test_access_deny(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.DENY
    await client.get(URL.format(agency_id=1), expected_status=403)


async def test_not_found(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.ALLOW
    service_discovery.agencies.get_agencies_info.return_value = []
    await client.get(URL.format(agency_id=1), expected_status=404)


@pytest.mark.parametrize(
    ('grants_return_value', 'agencies_return_value', 'rewards_return_value', 'expected'),
    [
        (
            grants_structs.AccessLevel.ALLOW,
            [
                agencies_structs.AgencyInfo(1, 'агентство 1', 'phone_1', 'agency_1@yandex.ru', 'https://agency_1.ru', 'Address 1', 'Address 2'),
            ],
            [
                rewards_structs.ContractInfo(
                    contract_id=202806,
                    eid='32240/15',
                    inn='7825677325',
                    payment_type='предоплата',
                    services=['direct'],
                    finish_date=datetime(2022, 3, 1),
                    type=ContractType.prof,
                    is_crisis=True,
                )
            ],
            {
                'agency_id': 1,
                'name': 'агентство 1',
                'phone': 'phone_1',
                'email': 'agency_1@yandex.ru',
                'site': 'https://agency_1.ru',
                'actual_address': 'Address 1',
                'legal_address': 'Address 2',
                'contracts': [
                    {
                        'contract_id': 202806,
                        'eid': '32240/15',
                        'finish_date': '2022-03-01T00:00:00',
                        'inn': '7825677325',
                        'payment_type': 'предоплата',
                        'type': 2,
                        'is_crisis': True,
                    }
                ]
            },
        ),
    ]
)
async def test_agency_info(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int,
                           grants_return_value,
                           agencies_return_value,
                           rewards_return_value,
                           expected):
    service_discovery.grants.check_access_level.return_value = grants_return_value
    service_discovery.agencies.get_agencies_info.return_value = agencies_return_value
    service_discovery.rewards.get_contracts_info.return_value = rewards_return_value

    got = await client.get(URL.format(agency_id=1), expected_status=200)

    assert got == expected

    service_discovery.grants.check_access_level.assert_awaited_with(yandex_uid=yandex_uid, agency_id=1)
    service_discovery.agencies.get_agencies_info.assert_awaited_with([1])
