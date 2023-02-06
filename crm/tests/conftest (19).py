import asyncio
import os.path
import pytest

from crm.agency_cabinet.common.server.common.config import MdsConfig
from crm.agency_cabinet.common.server.rpc.config import DbConfig
from crm.agency_cabinet.common.testing import run_alembic_command
from crm.agency_cabinet.ord.server.src.db.engine import db
from crm.agency_cabinet.ord.server.src.db import models
from crm.agency_cabinet.ord.server.src.handler import Handler

RELATIVE_ALEMBIC_BIN_PATH = os.path.join('crm/agency_cabinet/ord/server/migrations/', 'ord-migrations')


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


@pytest.fixture(scope='session')
async def handler():
    mds_cfg = MdsConfig.from_environ({
        'MDS_ENDPOINT_URL': 'http://mds.endpoint.url',
        'MDS_ACCESS_KEY_ID': 1,
        'MDS_SECRET_ACCESS_KEY': 1
    })
    return Handler(mds_cfg)


@pytest.fixture(scope='session')
async def fixture_report_settings():
    await models.ReportSettings.delete.gino.status()  # there is pre-inserted rows from migration
    rows = [
        {
            'name': 'other',
            'display_name': 'Другое'
        },
        {
            'name': 'direct',
            'display_name': 'Директ',
            'settings': {
                'allow_create_ad_distributor_acts': False,
                'allow_create_clients': False,
                'allow_create_campaigns': False,
                'allow_edit_report': False,
            }
        },
        {
            'name': 'adfox',
            'display_name': 'Adfox',
            'settings': {
                'allow_create_ad_distributor_acts': False,
                'allow_create_clients': False,
                'allow_create_campaigns': False,
                'allow_edit_report': False,
            }
        },
    ]

    yield await models.ReportSettings.bulk_insert(rows)

    await models.ReportSettings.delete.gino.status()
