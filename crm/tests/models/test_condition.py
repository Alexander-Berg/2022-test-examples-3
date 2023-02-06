# -*- coding: utf-8 -*-
import datetime

import pytest

from crm.agency_cabinet.rewards.server.src.db.models import Condition, Contract, Reward, ServiceReward


@pytest.fixture(scope='module')
async def fixture_contract():
    contract = await Contract.create(
        eid='test2',
        agency_id=123123123,
        payment_type='post',
        type='premium',
    )

    yield contract

    await contract.delete()


@pytest.fixture(scope='module')
async def fixture_reward(fixture_contract: Contract):
    yield await Reward.create(
        contract_id=fixture_contract.id,
        type='hy',
        payment='0',
        period_from=datetime.datetime.now(),
        is_prof=True
    )

    await Reward.delete.gino.status()


@pytest.fixture(scope='module')
async def fixture_service_reward(fixture_reward: Reward):
    yield await ServiceReward.create(
        reward_id=fixture_reward.id,
        service='direct',
        payment='0',
        revenue='0',
        discount_type=0
    )

    await ServiceReward.delete.gino.status()


@pytest.fixture(scope='module')
async def fixture_condition(fixture_service_reward: ServiceReward):
    yield await Condition.create(
        service_reward_id=fixture_service_reward.id,
        message='error',
    )

    await Condition.delete.gino.status()


async def test_create_condition(fixture_condition: Condition) -> None:
    assert fixture_condition.message == 'error'
