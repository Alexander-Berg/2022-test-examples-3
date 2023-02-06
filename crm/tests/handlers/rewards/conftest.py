import pytest
import typing

from datetime import datetime, timedelta
from decimal import Decimal

from crm.agency_cabinet.rewards.server.src.db import models
from crm.agency_cabinet.common.consts import RewardsTypes, Services, DocumentType


@pytest.fixture(scope='module')
async def fixture_rewards(fixture_contracts: typing.List[models.Contract]):
    rows = [
        {
            'contract_id': fixture_contracts[0].id,
            'type': RewardsTypes.month.value,
            'payment': Decimal(100),
            'period_from': datetime.now() - timedelta(days=31),
            'is_accrued': False,
            'is_paid': False,
            'payment_date': None,
        },
        {
            'contract_id': fixture_contracts[0].id,
            'type': RewardsTypes.month.value,
            'payment': Decimal(90),
            'period_from': datetime.now() - timedelta(days=61),
            'is_accrued': True,
            'is_paid': True,
            'payment_date': datetime.now() - timedelta(days=45),
        },
        {
            'contract_id': fixture_contracts[1].id,
            'type': RewardsTypes.month.value,
            'payment': Decimal(0),
            'period_from': datetime.now(),
            'is_accrued': False,
            'is_paid': False,
            'payment_date': None,
        },
        {
            'contract_id': fixture_contracts[0].id,
            'type': RewardsTypes.month.value,
            'payment': Decimal(120),
            'period_from': datetime.now(),
            'is_accrued': False,
            'is_paid': False,
            'payment_date': None,
            'predict': True
        },
    ]
    yield await models.Reward.bulk_insert(rows)

    await models.Reward.delete.gino.status()


@pytest.fixture(scope='module')
async def fixture_service_rewards(fixture_rewards: typing.List[models.Reward]):
    rows = [
        {
            'reward_id': fixture_rewards[0].id,
            'service': Services.direct.value,
            'payment': Decimal(0.52),
            'revenue': Decimal(0.75),
            'discount_type': 7,
            'reward_percent': Decimal('5.44'),
        },
        {
            'reward_id': fixture_rewards[0].id,
            'service': Services.zen.value,
            'payment': Decimal(0.33),
            'revenue': Decimal(0.62),
            'discount_type': 40,
            'reward_percent': Decimal('7'),
        },
        {
            'reward_id': fixture_rewards[1].id,
            'service': Services.media.value,
            'payment': Decimal(100.41),
            'revenue': Decimal(1000.43),
            'discount_type': 2,
            'reward_percent': Decimal('5.44'),
        },
        {
            'reward_id': fixture_rewards[1].id,
            'service': Services.media.value,
            'payment': Decimal(200),
            'revenue': Decimal(3200),
            'discount_type': 1,
            'reward_percent': None
        },
        {
            'reward_id': fixture_rewards[2].id,
            'service': Services.direct.value,
            'payment': Decimal(0.15),
            'revenue': Decimal(10.74),
            'discount_type': 7,
            'reward_percent': Decimal('5.44'),
        },
        {
            'reward_id': fixture_rewards[3].id,
            'service': Services.direct.value,
            'payment': Decimal(0.22),
            'revenue': Decimal(0.22),
            'discount_type': 7,
            'reward_percent': Decimal('5.44'),
        },
        {
            'reward_id': fixture_rewards[3].id,
            'service': Services.zen.value,
            'payment': Decimal(0.22),
            'revenue': Decimal(0.66),
            'discount_type': 40,
            'reward_percent': Decimal('5.44'),
        },
    ]

    yield await models.ServiceReward.bulk_insert(rows)

    await models.ServiceReward.delete.gino.status()


@pytest.fixture(scope='module')
async def fixture_documents(fixture_rewards: typing.List[models.Reward]):
    rows = [
        {
            'reward_id': fixture_rewards[0].id,
            'yadoc_id': 123,
            'type': DocumentType.act.value,
            'name': 'Акт',
            'got_scan': True,
            'got_original': True,
            'sending_date': datetime.now()
        },
        {
            'reward_id': fixture_rewards[1].id,
            'yadoc_id': 123,
            'type': DocumentType.act.value,
            'name': 'Акт',
            'got_scan': True,
            'got_original': True,
            'sending_date': datetime.now()
        },
        {
            'reward_id': fixture_rewards[2].id,
            'yadoc_id': 123,
            'type': DocumentType.act.value,
            'name': 'Акт',
            'got_scan': True,
            'got_original': True,
            'sending_date': datetime.now()
        },
        {
            'reward_id': fixture_rewards[3].id,
            'yadoc_id': 123,
            'type': DocumentType.act.value,
            'name': 'Акт',
            'got_scan': True,
            'got_original': True,
            'sending_date': datetime.now()
        }
    ]

    yield await models.Document.bulk_insert(rows)

    await models.Document.delete.gino.status()
