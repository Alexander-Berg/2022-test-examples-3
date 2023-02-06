import pytest

from datetime import datetime
from decimal import Decimal

from crm.agency_cabinet.common.consts import Services
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.grants.common import structs as grants_structs
from crm.agency_cabinet.rewards.common import structs as rewards_structs

URL = '/api/agencies/{agency_id}/rewards/dashboard?year=2021'


@pytest.mark.parametrize(
    ('grants_return_value', 'rewards_return_value', 'expected'),
    [
        (
            grants_structs.AccessLevel.ALLOW,
            [
                rewards_structs.DashboardItem(
                    contract_id=202806,
                    service=Services.direct,
                    active=True,
                    rewards=rewards_structs.DashboardRewardsMap(
                        month=[
                            rewards_structs.DashboardReward(
                                reward=Decimal('100'),
                                reward_percent=Decimal('7.5'),
                                predict=False,
                                period_from=datetime(2021, 3, 1, 0, 0, 0)
                            ),
                            rewards_structs.DashboardReward(
                                reward=Decimal('200'),
                                reward_percent=Decimal('3.5'),
                                predict=True,
                                period_from=datetime(2021, 5, 1, 0, 0, 0)
                            ),
                        ],
                        quarter=[
                            rewards_structs.DashboardReward(
                                reward=Decimal('100'),
                                reward_percent=Decimal('7.5'),
                                predict=False,
                                period_from=datetime(2021, 3, 1, 0, 0, 0)
                            )
                        ],
                        semiyear=[
                            rewards_structs.DashboardReward(
                                reward=Decimal('100'),
                                reward_percent=Decimal('7.5'),
                                predict=False,
                                period_from=datetime(2021, 3, 1, 0, 0, 0)
                            )
                        ]
                    ),
                    updated_at=datetime(2021, 6, 1, 0, 0, 0)
                ),
                rewards_structs.DashboardItem(
                    contract_id=202806,
                    service=Services.zen,
                    active=False,
                    rewards=None,
                    updated_at=None
                ),
            ],
            {
                'dashboard': [
                    {
                        'contract_id': 202806,
                        'service': 'direct',
                        'active': True,
                        'rewards': {
                            'month': [
                                {
                                    'reward': 100.0,
                                    'reward_percent': 7.5,
                                    'predict': False,
                                    'period_from': '2021-03-01T00:00:00',
                                },
                                {
                                    'reward': 200.0,
                                    'reward_percent': 3.5,
                                    'predict': True,
                                    'period_from': '2021-05-01T00:00:00',
                                }
                            ],
                            'quarter': [
                                {
                                    'reward': 100.0,
                                    'reward_percent': 7.5,
                                    'predict': False,
                                    'period_from': '2021-03-01T00:00:00',
                                }
                            ],
                            'semiyear': [
                                {
                                    'reward': 100.0,
                                    'reward_percent': 7.5,
                                    'predict': False,
                                    'period_from': '2021-03-01T00:00:00',
                                }
                            ]
                        },
                        'updated_at': '2021-06-01T00:00:00'
                    },
                    {
                        'contract_id': 202806,
                        'service': 'zen',
                        'active': False,
                        'rewards': None,
                        'updated_at': None
                    }
                ]
            },
        ),
        (
            grants_structs.AccessLevel.ALLOW,
            [],
            {
                'dashboard': []
            },
        ),
    ]
)
async def test_get_dashboard(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int,
                             grants_return_value,
                             rewards_return_value,
                             expected):
    service_discovery.grants.check_access_level.return_value = grants_return_value
    service_discovery.rewards.get_dashboard.return_value = rewards_return_value

    got = await client.get(URL.format(agency_id=1), expected_status=200)

    assert got == expected

    service_discovery.grants.check_access_level.assert_awaited_with(yandex_uid=yandex_uid, agency_id=1)
    service_discovery.rewards.get_dashboard.assert_awaited_with(1, 2021, None, None)
