import asyncio
import os.path
import pytest

from crm.agency_cabinet.documents.server.src.db.engine import db
from crm.agency_cabinet.common.server.rpc.config import DbConfig
from crm.agency_cabinet.common.testing import run_alembic_command


RELATIVE_ALEMBIC_BIN_PATH = os.path.join('crm/agency_cabinet/documents/server/migrations/', 'documents-migrations')


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
