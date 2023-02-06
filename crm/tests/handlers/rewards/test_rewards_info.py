import datetime

import pytest

from crm.agency_cabinet.common.consts.service import Services
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.common.testing import BaseTestClient

from crm.agency_cabinet.agencies.common import structs as agencies_structs
from crm.agency_cabinet.grants.common import structs as grants_structs
from crm.agency_cabinet.rewards.common import structs as rewards_structs
from crm.agency_cabinet.rewards.client import NoSuchRewardException, UnsuitableAgency


LIST_REWARDS_URL = '/api/agencies/{agency_id}/rewards'
DETAILED_REWARD_INFO_URL = '/api/agencies/{agency_id}/rewards/{reward_id}'


async def test_access_deny(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.DENY
    await client.get(LIST_REWARDS_URL.format(agency_id=1), expected_status=403)
    await client.get(DETAILED_REWARD_INFO_URL.format(agency_id=1, reward_id=1), expected_status=403)


@pytest.mark.parametrize(
    (
        'grants_return_value',
        'rewards_return_value',
        'expected'
    ),
    [
        (
            grants_structs.AccessLevel.ALLOW,
            [
                rewards_structs.RewardInfo(
                    id=1,
                    contract_id=1111,
                    type='month',
                    services=['direct'],
                    got_scan=False,
                    got_original=False,
                    is_accrued=False,
                    is_paid=False,
                    payment='0.11',
                    period_from=datetime.datetime(2021, 1, 1),
                    payment_date=None),
            ],
            {
                'rewards': [
                    {
                        'id': 1,
                        'contract_id': 1111,
                        'type': 'month',
                        'services': ['direct'],
                        'got_scan': False,
                        'got_original': False,
                        'is_accrued': False,
                        'is_paid': False,
                        'payment': 0.11,
                        'period_from': datetime.datetime(2021, 1, 1).isoformat(),
                        'payment_date': None
                    }
                ]
            }
        )
    ]
)
async def test_rewards_info(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int,
                            grants_return_value, rewards_return_value, expected):
    service_discovery.grants.check_access_level.return_value = grants_return_value
    service_discovery.rewards.get_rewards_info.return_value = rewards_return_value
    got = await client.get(LIST_REWARDS_URL.format(agency_id=1), expected_status=200)

    assert got == expected

    service_discovery.grants.check_access_level.assert_awaited_with(yandex_uid=yandex_uid, agency_id=1)
    service_discovery.rewards.get_rewards_info.assert_awaited_with(
        1,
        filter_from=None,
        filter_to=None,
        filter_contract=None,
        filter_type=None,
        filter_is_paid=None
    )


@pytest.mark.parametrize((
    'grants_return_value',
    'agencies_return_value',
    'rewards_return_value',
    'expected'),
    [
        (
            grants_structs.AccessLevel.ALLOW,
            [
                agencies_structs.AgencyInfo(
                    0, 'агентство 1', 'phone_1', 'agency_1@yandex.ru', '', '', ''),
            ],
            rewards_structs.DetailedRewardInfo(
                id=1,
                contract_id=1,
                type='month',
                services=[
                    rewards_structs.DetailedServiceInfo(
                        service=service.value,
                        revenue='0.22',
                        currency='RUB',
                        reward_percent='5.44',
                        accrual='0.11',
                        error_message='Error',
                    ) for service in Services
                ],
                documents=[
                    rewards_structs.DocumentInfo(
                        id=1,
                        name='test',
                        sending_date=datetime.datetime(2021, 9, 1, 0, 0),
                        got_scan=True,
                        got_original=True
                    )
                ],
                status='no_information_available',
                accrual='0.11',
                payment='0.22',
                accrual_date=None,
                payment_date=None,
                period_from=datetime.datetime(2021, 8, 1, 0, 0),
                predict=False,
            ),
            {  # TODO: reorder
                'documents': [
                    {
                        'id': 1,
                        'name': 'test',
                        'sending_date': datetime.datetime(2021, 9, 1, 0, 0).isoformat(),
                        'got_scan': True,
                        'got_original': True
                    }
                ],
                'accrual': 0.11,
                'contract_id': 1,
                'accrual_date': None,
                'payment': 0.22,
                'payment_date': None,
                'services': [
                    {
                        'accrual': 0.11,
                        'reward_percent': 5.44,
                        'service': 'media',
                        'currency': 'RUB',
                        'revenue': 0.22,
                        'error_message': 'Error'
                    },
                    {
                        'accrual': 0.11,
                        'reward_percent': 5.44,
                        'service': 'direct',
                        'currency': 'RUB',
                        'revenue': 0.22,
                        'error_message': 'Error'
                    },
                    {
                        'accrual': 0.11,
                        'reward_percent': 5.44,
                        'service': 'zen',
                        'currency': 'RUB',
                        'revenue': 0.22,
                        'error_message': 'Error'
                    },
                    {
                        'accrual': 0.11,
                        'reward_percent': 5.44,
                        'service': 'video',
                        'currency': 'RUB',
                        'revenue': 0.22,
                        'error_message': 'Error'
                    },
                    {
                        'accrual': 0.11,
                        'reward_percent': 5.44,
                        'service': 'sprav',
                        'currency': 'RUB',
                        'revenue': 0.22,
                        'error_message': 'Error'
                    },
                    {
                        'accrual': 0.11,
                        'reward_percent': 5.44,
                        'service': 'business',
                        'currency': 'RUB',
                        'revenue': 0.22,
                        'error_message': 'Error'
                    },
                    {
                        'accrual': 0.11,
                        'reward_percent': 5.44,
                        'service': 'early_payment',
                        'currency': 'RUB',
                        'revenue': 0.22,
                        'error_message': 'Error'
                    },
                ],
                'period_from': datetime.datetime(2021, 8, 1, 0, 0).isoformat(),
                'type': 'month',
                'status': 'no_information_available',
                'id': 1,
                'predict': False,
            }
        )
    ]
)
async def test_detailed_reward_info(client: BaseTestClient,
                                    service_discovery: ServiceDiscovery,
                                    yandex_uid: int,
                                    grants_return_value,
                                    agencies_return_value,
                                    rewards_return_value,
                                    expected):
    service_discovery.grants.check_access_level.return_value = grants_return_value
    service_discovery.agencies.get_agencies_info.return_value = agencies_return_value
    service_discovery.rewards.get_detailed_reward_info.return_value = rewards_return_value

    agency_id = 0
    reward_id = 25673

    got = await client.get(DETAILED_REWARD_INFO_URL.format(agency_id=agency_id, reward_id=reward_id),
                           expected_status=200)

    assert got == expected

    service_discovery.grants.check_access_level.assert_awaited_with(yandex_uid=yandex_uid, agency_id=agency_id)
    service_discovery.rewards.get_detailed_reward_info.assert_awaited_with(agency_id, reward_id)


@pytest.mark.parametrize(('grants_return_value',
                          'rewards_side_effect'),
                         [(grants_structs.AccessLevel.ALLOW, UnsuitableAgency()),
                          (grants_structs.AccessLevel.ALLOW, NoSuchRewardException())
                          ])
async def test_detailed_reward_info_exception(client: BaseTestClient, service_discovery: ServiceDiscovery,
                                              yandex_uid: int,
                                              grants_return_value, rewards_side_effect):
    service_discovery.grants.check_access_level.return_value = grants_return_value
    service_discovery.rewards.get_detailed_reward_info.side_effect = rewards_side_effect
    agency_id = 0
    reward_id = 25673

    await client.get(DETAILED_REWARD_INFO_URL.format(agency_id=agency_id, reward_id=reward_id),
                     expected_status=404)
