import pytest
from yatest.common import network

from mail.devpack.lib.coordinator import Coordinator
from mail.pg.mopsdb.devpack.components.mopsdb import MopsDb
from mail.devpack.tests.helpers.env import TestEnv
from mail.pypg.pypg.reflected import connect, async_connect
from mail.pypg.pypg.reflected import reflect_db as pypg_reflect_db
from contextlib import contextmanager, closing

import logging

log = logging.getLogger(__name__)


class Context(object):
    def dsn(self):
        return self.mopsdb.dsn()

    def connect(self, **kwargs):
        return connect(self.dsn(), **kwargs)

    def async_connect(self, **kwargs):
        return async_connect(self.dsn(), **kwargs)

    @contextmanager
    def reflect_db(self, sync=True, **kwargs):
        schemas = ('code', 'impl', 'mops', 'operations', 'util')
        if sync:
            with closing(self.connect(**kwargs)) as conn:
                yield pypg_reflect_db(conn, schemas)
        else:
            with closing(self.async_connect(**kwargs)) as conn:
                yield pypg_reflect_db(conn, schemas)


@pytest.fixture(scope="session", autouse=True)
def context():
    return Context()


@pytest.fixture(scope="session", autouse=True)
def feature_setup(request, context):
    start_db(context)

    def feature_teardown():
        stop_db(context)

    request.addfinalizer(feature_teardown)


@pytest.fixture(scope="function", autouse=True)
def step_setup(request, context):
    context.request_id = request.node.name
    clear_mopsdb(context)


def clear_mopsdb(context):
    purge_tables = [
        'operations.tasks',
        'operations.recent_tasks',
        'operations.banned_users',
        'operations.change_log',
        'operations.user_locks',
        'mops.operations',
        'mops.message_chunks',
        'mops.change_log',
    ]
    for table in purge_tables:
        context.mopsdb.execute('DELETE FROM %s' % table)


def start_db(context):
    start_component = MopsDb
    port_manager = network.PortManager()
    context.coordinator = Coordinator(TestEnv(port_manager, start_component), start_component)
    context.coordinator.start()
    context.started_components = [start_component]
    context.mopsdb = context.coordinator.components[MopsDb]


def stop_db(context):
    for c in context.started_components:
        context.coordinator.stop(c)
