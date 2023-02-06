import asyncio
import os.path
import pytest
from unittest.mock import create_autospec

from crm.agency_cabinet.common.server.rpc.config import DbConfig
from crm.agency_cabinet.common.testing import run_alembic_command
from crm.agency_cabinet.grants.server.src.db.engine import db
from crm.agency_cabinet.grants.server.src.db import models
from crm.agency_cabinet.grants.server.src.handler import Handler
from crm.agency_cabinet.common.blackbox import BlackboxClient

RELATIVE_ALEMBIC_BIN_PATH = os.path.join('crm/agency_cabinet/grants/server/migrations/', 'grants-migrations')


def pytest_collection_modifyitems(items):
    for item in items:
        item.add_marker(pytest.mark.asyncio)


@pytest.fixture(scope='session')
def event_loop():
    yield asyncio.get_event_loop()


@pytest.fixture(scope='session', autouse=True)
async def db_bind():
    run_alembic_command(RELATIVE_ALEMBIC_BIN_PATH, 'upgrade head')
    cfg = DbConfig.from_environ()
    await db.set_bind(bind=str(cfg.get_dsn()))


@pytest.fixture()
async def fixture_role():
    role = await models.Role.create(
        yandex_uid=123456789,
        agency_id=1,
        email='test@yandex.ru',
        is_main=True
    )

    yield role

    await role.delete()


@pytest.fixture()
async def fixture_yandex_role():
    role = await models.Role.create(
        yandex_uid=987654321,
        agency_id=None,
        email='test-yandex@yandex.ru',
        staff_login='test_yandex'
    )

    yield role

    await role.delete()


@pytest.fixture()
async def fixture_not_active_role():
    role = await models.Role.create(
        yandex_uid=111111111,
        agency_id=1,
        email='test-not-active@yandex.ru',
        is_main=True,
        is_active=False
    )

    yield role

    await role.delete()


@pytest.fixture(scope='session')
async def blackbox_client():
    return create_autospec(BlackboxClient)


@pytest.fixture(scope='session')
async def handler(blackbox_client):
    return Handler(blackbox_client)
