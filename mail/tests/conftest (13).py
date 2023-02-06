import pytest
import os

from datetime import timedelta
from library.python import resource

from .context import *  # noqa


@pytest.fixture(scope="session", autouse=True)
def resource_setup(context):
    with open(os.path.expanduser('query.conf.sql'), 'wb') as f:
        f.write(resource.find('query.conf.sql'))


@pytest.fixture(scope="function", autouse=True)
def config_setup(context):
    context.config = dict(
        vars=dict(
            cachedb_purge=dict(
                count=1,
                interval='1 month',
            ),
        ),
        dbsignals=dict(
            query_conf='query.conf.sql'
        ),
        datasources=dict(
            colliedb=dict(
                host='localhost',
                port=context.colliedb.port(),
                dbname=context.colliedb.NAME,
                user='collie',
            ),
            mopsdb=dict(
                host='localhost',
                port=context.mopsdb.port(),
                dbname=context.mopsdb.NAME,
                user='tech',
            ),
            queuedb=dict(
                host='localhost',
                port=context.queuedb.port(),
                dbname=context.queuedb.NAME,
                user='tech',
            ),
            cachedb=dict(
                host='localhost',
                port=context.cachedb.port(),
                dbname=context.cachedb.NAME,
                user='cachedb',
            ),
        ),
        dbstats=dict(
            colliedb=[
                dict(
                    name='colliedb_minutes_since_ml_sync',
                    period=timedelta(seconds=60)
                ),
                dict(
                    name='colliedb_minutes_since_staff_sync',
                    period=timedelta(seconds=60)
                ),
                dict(
                    name='colliedb_total_processing_users',
                    period=timedelta(seconds=60)
                )
            ],
            mopsdb=[
                dict(
                    name='mopsdb_total_operations_chunks',
                    period=60
                ),
                dict(
                    name='mopsdb_alive_locks',
                    period=60
                )
            ],
            queuedb=[
                dict(
                    name='queuedb_active_tasks',
                    period=60,
                    row_signal=True,
                    sigopt_suffix='axxv',
                ),
                dict(
                    name='queuedb_processed_tasks',
                    period=60,
                    row_signal=True,
                    sigopt_suffix='axxv',
                )
            ],
        )
    )
