import json
import logging

import pytest

from sendr_aiopg import create_engine
from sendr_qlog.logging.adapters.logger import LoggerContext
from sendr_settings.db import SettingsStorage

pytest_plugins = ['aiohttp.pytest_plugin']


@pytest.fixture
def loop(event_loop):
    return event_loop


@pytest.fixture(autouse=True, scope='session')
def db_config():
    with open('pg_recipe.json') as f:
        db_params = json.load(f)

    return {
        'dbname': db_params['dbname'],
        'user': db_params['user'],
        'password': db_params['password'],
        'host': db_params['host'],
        'port': db_params['port'],
        'sslmode': 'disable',
        'connect_timeout': 2,
        'timeout': 5,
        'target_session_attrs': 'read-write',
    }


@pytest.fixture
async def db_engine(db_config, loop):
    engine = await create_engine(**db_config)

    async with engine.acquire() as conn:
        await conn.execute("""TRUNCATE public.settings;""")

    yield engine

    engine.close()
    await engine.wait_closed()


@pytest.fixture
async def db_conn(db_engine):
    async with db_engine.acquire() as conn:
        yield conn


@pytest.fixture
def storage(db_conn) -> SettingsStorage:
    return SettingsStorage(db_conn)


@pytest.fixture
def logger_mock():
    return LoggerContext(logging.getLogger('mock_logger'), {})
