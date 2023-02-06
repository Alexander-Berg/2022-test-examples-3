# -*- coding: utf-8 -*-
import asyncio
import os.path
import pytest
import json
from crm.agency_cabinet.common.testing import run_alembic_command
from crm.agency_cabinet.rewards.server.config import DATABASE_SETTINGS
from crm.agency_cabinet.rewards.server.src.db.engine import db


RELATIVE_ALEMBIC_BIN_PATH = os.path.join('crm/agency_cabinet/rewards/server/migrations/', 'rewards-migrations')


pytest_plugins = ['crm.agency_cabinet.common.server.common.pytest.plugin']


def pytest_collection_modifyitems(items):
    for item in items:
        item.add_marker(pytest.mark.asyncio)


@pytest.fixture(scope='session')
def event_loop():
    yield asyncio.get_event_loop()


@pytest.fixture(scope='session', autouse=True)
async def db_bind():
    await db.set_bind(bind=DATABASE_SETTINGS['dsn'])
    run_alembic_command(RELATIVE_ALEMBIC_BIN_PATH, 'upgrade head')


@pytest.fixture
def prof_postpayment_meta():
    return json.dumps({
        "month": {
            "indexes": [
                {
                    "index_id": "revenue",
                    "reward_percent": 5.5
                },
                {
                    "index_id": "early_payment",
                    "reward_percent": 2
                },
                {
                    "index_id": "rsya",
                    "reward_percent": 5
                }
            ]
        },
        "quarters": [
            {
                "indexes": [
                    {
                        "index_id": "conversion_autostrategy",
                        "reward_percent": 3
                    },
                    {
                        "index_id": "metrica",
                        "reward_percent": 1
                    },
                    {
                        "index_id": "key_goals",
                        "reward_percent": 2
                    },
                    {
                        "index_id": "search_autotargeting_not_uac",
                        "reward_percent": 6
                    }
                ]
            },
            {
                "indexes": [
                    {
                        "index_id": "conversion_autostrategy",
                        "reward_percent": 3
                    },
                    {
                        "index_id": "metrica",
                        "reward_percent": 1
                    },
                    {
                        "index_id": "key_goals",
                        "reward_percent": 2
                    },
                    {
                        "index_id": "search_autotargeting_not_uac",
                        "reward_percent": 6
                    }
                ]
            },
            {
                "indexes": [
                    {
                        "index_id": "conversion_autostrategy",
                        "reward_percent": 2
                    },
                    {
                        "index_id": "metrica",
                        "reward_percent": 1
                    },
                    {
                        "index_id": "key_goals",
                        "reward_percent": 3
                    },
                    {
                        "index_id": "search_autotargeting_not_uac",
                        "reward_percent": 6
                    }
                ]
            },
            {
                "indexes": [
                    {
                        "index_id": "conversion_autostrategy",
                        "reward_percent": 2
                    },
                    {
                        "index_id": "metrica",
                        "reward_percent": 1
                    },
                    {
                        "index_id": "key_goals",
                        "reward_percent": 3
                    },
                    {
                        "index_id": "search_autotargeting_not_uac",
                        "reward_percent": 6
                    }
                ]
            }
        ]
    })
