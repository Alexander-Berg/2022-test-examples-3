import asyncio

import aioredis
import pytest
from telethon import TelegramClient
from telethon.errors import UserDeactivatedBanError, PhoneNumberBannedError

from lib.conf import settings


@pytest.fixture(scope='session')
def event_loop():
    """Create an instance of the default event loop for each test case."""
    loop = asyncio.get_event_loop_policy().new_event_loop()
    yield loop
    loop.close()


@pytest.fixture(scope="session")
async def telegram() -> TelegramClient:
    for config in settings.TELEGRAM_CLIENTS.values():
        try:
            client = TelegramClient(
                config.string_session,
                config.api_id,
                config.api_hash,
                use_ipv6=True,
                sequential_updates=True,
            )
            if not settings.TELEGRAM_IPV6:
                client.session.set_dc(2, settings.TELEGRAM_DC2_ADDRESS, 443)  # SPI-43902
            # Connect to the server
            await client.connect()
            # Issue a high level command to start receiving message
            await client.get_me()
            # Fill the entity cache
            await client.get_dialogs()
            break
        except (PhoneNumberBannedError, UserDeactivatedBanError):
            pass

    yield client

    await client.disconnect()
    await client.disconnected


@pytest.fixture(scope="session")
async def redis_master():
    return await aioredis.create_redis('redis://localhost', encoding='UTF-8')
