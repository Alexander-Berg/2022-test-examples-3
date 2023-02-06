import json
import os
from datetime import timedelta

import psycopg2
import pytest

from sendr_aiopg import create_engine
from sendr_pytest import *  # noqa

from . import db

pytest_plugins = ['aiohttp.pytest_plugin']


@pytest.fixture
def loop(event_loop):
    return event_loop


@pytest.fixture(autouse=True, scope='session')
def db_configuration():
    if os.environ.get('QTOOLS_DB_USE_DOCKER_DB', '') == 'True':
        return {
            'dbname': 'sendr_qtools',
            'user': 'sendr_qtools',
            'password': 'P@ssw0rd',
            'host': '0.0.0.0',
            'port': '5442',
            'sslmode': 'disable',
            'connect_timeout': 2,
            'timeout': 5,
            'target_session_attrs': 'read-write',
        }
    else:
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
async def aiopg_engine(db_configuration):
    template_name = db_configuration['dbname']

    # Create database from template
    name = f'{template_name}_master'
    postgres_engine = await create_engine(**{**db_configuration, 'dbname': 'postgres'})
    async with postgres_engine.acquire() as postgres_conn:
        try:
            await postgres_conn.execute(
                f'CREATE DATABASE "{name}" TEMPLATE "{template_name}"',
            )
        except psycopg2.errors.DuplicateDatabase:
            pass
    postgres_engine.close()
    await postgres_engine.wait_closed()

    # Connect to new database
    engine = await create_engine(**{**db_configuration, **{'dbname': name}})

    async with engine.acquire() as conn:
        queries = await conn.execute("""
                SELECT 'TRUNCATE ' || input_table_name || ' CASCADE;' AS truncate_query FROM (
                    SELECT table_schema || '.' || table_name AS input_table_name
                    FROM information_schema.tables
                    WHERE table_schema = 'sendr_qtools'
                ) AS information;
            """)

        queries = await queries.fetchall()
        for query in queries:
            await conn.execute(query[0])

    # Yield actual engine
    yield engine

    engine.close()
    await engine.wait_closed()


@pytest.fixture
async def db_conn(aiopg_engine):
    async with aiopg_engine.acquire() as conn:
        yield conn


@pytest.fixture
def storage(db_conn):
    return db.Storage(db_conn)  # noqa


@pytest.fixture
def timeout():
    return timedelta(seconds=2)


@pytest.fixture
def transaction_timeout(timeout):
    return timeout


@pytest.fixture
def storage_context(aiopg_engine, db_conn, transaction_timeout):
    return db.StorageContext(
        db_engine=aiopg_engine,
        conn=db_conn,
        transact=True,
        transaction_timeout=transaction_timeout,
    )


@pytest.fixture
def logger(mocker):
    return mocker.MagicMock()
