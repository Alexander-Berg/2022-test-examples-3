from unittest.mock import patch

import pytest
from aioredis import Redis

from lib.services.telegram_account_rotator.account_pool import TelegramAccountPool, AccountGroups, PoolIsEmpty, \
    AccountBanned

TEST_CLIENTS_CONFIG = {
    'phone1': 'value_stub1',
    'phone2': 'value_stub2',
    'phone3': 'value_stub3',
}


@pytest.fixture(scope='function')
@patch('lib.conf.settings.TELEGRAM_CLIENTS', TEST_CLIENTS_CONFIG)
async def pool(redis_master):
    redis = await redis_master
    return await TelegramAccountPool(redis_factory=lambda: redis).init()


@pytest.mark.asyncio
async def test_ban(pool: TelegramAccountPool):
    redis: Redis = await pool.redis

    await pool.ban('phone1')
    creators = await redis.smembers(AccountGroups.CREATORS.group_name)
    assert set(creators) == {'phone2', 'phone3'}
    managers = await redis.smembers(AccountGroups.MANAGERS.group_name)
    assert set(managers) == {'phone2', 'phone3'}
    banned = await redis.smembers(AccountGroups.BANNED.group_name)
    assert set(banned) == {'phone1'}


@pytest.mark.asyncio
async def test_forbid_creation(pool: TelegramAccountPool):
    redis: Redis = await pool.redis

    await pool.forbid_creation('phone2')
    creators = await redis.smembers(AccountGroups.CREATORS.group_name)
    assert set(creators) == {'phone1', 'phone3'}
    managers = await redis.smembers(AccountGroups.MANAGERS.group_name)
    assert set(managers) == {'phone1', 'phone2', 'phone3'}
    banned = await redis.smembers(AccountGroups.BANNED.group_name)
    assert set(banned) == set()


@pytest.mark.asyncio
async def test_amnesty_all(pool: TelegramAccountPool):
    redis: Redis = await pool.redis

    await pool.ban('phone1')
    await pool.forbid_creation('phone2')

    await pool.amnesty_all()
    creators = await redis.smembers(AccountGroups.CREATORS.group_name)
    assert set(creators) == {'phone1', 'phone2', 'phone3'}
    managers = await redis.smembers(AccountGroups.MANAGERS.group_name)
    assert set(managers) == {'phone1', 'phone2', 'phone3'}
    banned = await redis.smembers(AccountGroups.BANNED.group_name)
    assert set(banned) == set()


@pytest.mark.asyncio
async def test_get_clean_client(pool):
    await pool.ban('phone1')  # Banned
    await pool.forbid_creation('phone2')  # Maxed out limits
    expect_clean_phone = 'phone3'

    config = await pool.get_clean_client()
    assert config == TEST_CLIENTS_CONFIG[expect_clean_phone]


@pytest.mark.asyncio
async def test_get_clean_or_maxed_out_limits_client(pool):
    await pool.ban('phone1')  # Banned
    await pool.ban('phone2')  # Banned
    expect_clean_phone = 'phone3'

    config = await pool.get_clean_or_maxed_out_limits_client()
    assert config == TEST_CLIENTS_CONFIG[expect_clean_phone]

    await pool.forbid_creation('phone3')  # Maxed out limits

    config = await pool.get_clean_or_maxed_out_limits_client()
    assert config == TEST_CLIENTS_CONFIG[expect_clean_phone]


@pytest.mark.asyncio
async def test_no_more_creators(pool):
    await pool.ban('phone1')  # Banned
    await pool.forbid_creation('phone2')  # Maxed out limits
    await pool.forbid_creation('phone3')  # Maxed out limits

    with pytest.raises(PoolIsEmpty):
        await pool.get_clean_client()


@pytest.mark.asyncio
async def test_no_more_managers(pool):
    await pool.ban('phone1')  # Banned
    await pool.ban('phone2')  # Banned
    await pool.ban('phone3')  # Banned

    with pytest.raises(PoolIsEmpty):
        await pool.get_clean_or_maxed_out_limits_client()


@pytest.mark.asyncio
async def test_get_client(pool):
    await pool.ban('phone1')  # Banned
    await pool.ban('phone2')  # Banned
    expect_clean_phone = 'phone3'

    config = await pool.get_client(expect_clean_phone)
    assert config == TEST_CLIENTS_CONFIG[expect_clean_phone]


@pytest.mark.asyncio
async def test_get_client_is_banned(pool):
    await pool.ban('phone1')  # Banned
    await pool.ban('phone2')  # Banned
    await pool.ban('phone3')  # Banned

    with pytest.raises(AccountBanned):
        await pool.get_client('phone3')
