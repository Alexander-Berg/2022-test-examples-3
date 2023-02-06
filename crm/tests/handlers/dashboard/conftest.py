import pytest
import typing
from datetime import timezone
from dateutil.relativedelta import relativedelta

from crm.agency_cabinet.rewards.server.src.db import models
from crm.agency_cabinet.common.consts import RewardsTypes, Services, START_FIN_YEAR_2021


@pytest.fixture(scope='module')
async def fixture_dashboard_rewards(fixture_contracts: typing.List[models.Contract]):
    rows = [
        {
            'contract_id': fixture_contracts[3].id,
            'type': RewardsTypes.month.value,
            'payment': 100,
            'period_from': START_FIN_YEAR_2021.replace(tzinfo=timezone.utc),
            'is_accrued': False,
            'is_paid': False,
            'payment_date': None,
            'predict': False,
        },
        {
            'contract_id': fixture_contracts[3].id,
            'type': RewardsTypes.month.value,
            'payment': 100,
            'period_from': (START_FIN_YEAR_2021 + relativedelta(months=4)).replace(tzinfo=timezone.utc),
            'is_accrued': False,
            'is_paid': False,
            'payment_date': None,
            'predict': True,
        },
        {
            'contract_id': fixture_contracts[3].id,
            'type': RewardsTypes.quarter.value,
            'payment': 100,
            'period_from': (START_FIN_YEAR_2021 + relativedelta(months=3)).replace(tzinfo=timezone.utc),
            'is_accrued': False,
            'is_paid': False,
            'payment_date': None,
            'predict': False,
        },
        {
            'contract_id': fixture_contracts[3].id,
            'type': RewardsTypes.semiyear.value,
            'payment': 100,
            'period_from': START_FIN_YEAR_2021.replace(tzinfo=timezone.utc),
            'is_accrued': False,
            'is_paid': False,
            'payment_date': None,
            'predict': False,
        },
    ]
    yield await models.Reward.bulk_insert(rows)

    await models.Reward.delete.gino.status()


@pytest.fixture(scope='module')
async def fixture_dashboard_service_rewards(fixture_dashboard_rewards: typing.List[models.Reward]):
    rows = [
        {
            'reward_id': fixture_dashboard_rewards[0].id,
            'service': Services.direct.value,
            'payment': 50,
            'revenue': 600,
            'discount_type': 7
        },
        {
            'reward_id': fixture_dashboard_rewards[0].id,
            'service': Services.business.value,
            'payment': 40,
            'revenue': 300,
            'discount_type': 12,
        },
        {
            'reward_id': fixture_dashboard_rewards[0].id,
            'service': Services.business.value,
            'payment': 160,
            'revenue': 1300,
            'discount_type': 15,
        },
        {
            'reward_id': fixture_dashboard_rewards[0].id,
            'service': Services.early_payment.value,
            'payment': 40,
            'revenue': None,
            'discount_type': 7,
        },
        {
            'reward_id': fixture_dashboard_rewards[1].id,
            'service': Services.direct.value,
            'payment': 100,
            'revenue': 800,
            'discount_type': 7,
        },
        {
            'reward_id': fixture_dashboard_rewards[2].id,
            'service': Services.direct.value,
            'payment': 100,
            'revenue': 1700,
            'discount_type': 7,
        },
        {
            'reward_id': fixture_dashboard_rewards[3].id,
            'service': Services.direct.value,
            'payment': 100,
            'revenue': 2000,
            'discount_type': 7,
        },
    ]
    yield await models.ServiceReward.bulk_insert(rows)

    await models.ServiceReward.delete.gino.status()
