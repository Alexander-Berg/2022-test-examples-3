import pytest

from yatest.common import network

from mail.devpack.lib.components.base import FakeRootComponent
from mail.collie.devpack.components.colliedb import CollieDb
from mail.pg.mopsdb.devpack.components.mopsdb import MopsDb
from mail.pg.queuedb.devpack.components.queuedb import QueueDb
from mail.pg.cachedb.devpack.cachedb import CacheDb
from mail.devpack.lib.coordinator import Coordinator
from mail.devpack.tests.helpers.env import TestEnv

from mail.pypg.pypg.reflected import connect, async_connect
from mail.pypg.pypg.reflected import reflect_db as pypg_reflect_db
from contextlib import contextmanager, closing


class PuliDatabases(FakeRootComponent):
    NAME = 'puli-databases'
    DEPS = [CollieDb, MopsDb, QueueDb, CacheDb]


class Context(object):
    pass


@pytest.fixture(scope="session", autouse=True)
def context():
    return Context()


@pytest.fixture(scope="session", autouse=True)
def feature_setup(request, context):
    start_databases(context)

    def feature_teardown():
        stop_databases(context)

    request.addfinalizer(feature_teardown)


@pytest.fixture(scope="function", autouse=True)
def step_setup(request, context):
    context.request_id = request.node.name
    reset_databases(context)


def reset_databases(context):
    reset_colliedb(context)
    reset_mopsdb(context)
    reset_queuedb(context)
    reset_cachedb(context)


def reset_colliedb(context):
    purge_tables = [
        'collie.directory_events',
        'collie.service_sync'
    ]
    for table in purge_tables:
        context.colliedb.execute('DELETE FROM {}'.format(table))


def reset_mopsdb(context):
    purge_tables = [
        'operations.tasks',
        'operations.user_locks'
    ]
    for table in purge_tables:
        context.mopsdb.execute('DELETE FROM {}'.format(table))


def reset_cachedb(context):
    purge_tables = [
        'cachedb.cache',
    ]
    for table in purge_tables:
        context.cachedb.execute('DELETE FROM {}'.format(table))


def reset_queuedb(context):
    purge_tables = [
        'queue.tasks',
        'queue.processed_tasks'
    ]
    for table in purge_tables:
        context.queuedb.execute('DELETE FROM {}'.format(table))


def start_databases(context):
    root_component = PuliDatabases
    port_manager = network.PortManager()
    context.coordinator = Coordinator(TestEnv(port_manager, root_component), root_component)
    context.coordinator.start()
    context.started_components = [root_component]
    context.colliedb = context.coordinator.components[CollieDb]
    context.mopsdb = context.coordinator.components[MopsDb]
    context.cachedb = context.coordinator.components[CacheDb]
    context.queuedb = context.coordinator.components[QueueDb]


def stop_databases(context):
    for c in context.started_components:
        context.coordinator.stop(c)


@contextmanager
def reflect_mopsdb(context, sync=True, **kwargs):
    schemas = ('code', 'impl', 'mops', 'operations')
    with reflect_db(context.mopsdb, schemas, sync, **kwargs) as db:
        yield db


@contextmanager
def reflect_cachedb(context, sync=True, **kwargs):
    schemas = ('code', 'impl', 'cachedb')
    with reflect_db(context.cachedb, schemas, sync, **kwargs) as db:
        yield db


@contextmanager
def reflect_queuedb(context, sync=True, **kwargs):
    schemas = ('code', 'queue')
    with reflect_db(context.queuedb, schemas, sync, **kwargs) as db:
        yield db


@contextmanager
def reflect_db(db, schemas, sync=True, **kwargs):
    if sync:
        with closing(connect(db.dsn(), **kwargs)) as conn:
            yield pypg_reflect_db(conn, schemas)
    else:
        with closing(async_connect(db.dsn(), **kwargs)) as conn:
            yield pypg_reflect_db(conn, schemas)
