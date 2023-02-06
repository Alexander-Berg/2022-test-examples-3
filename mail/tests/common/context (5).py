import pytest
from yatest.common import network

from mail.devpack.lib.coordinator import Coordinator
from mail.pg.spanieldb.devpack.components.spanieldb import SpanielDb
from mail.devpack.tests.helpers.env import TestEnv
from mail.pypg.pypg.reflected import connect, async_connect
from mail.pypg.pypg.reflected import reflect_db as pypg_reflect_db
from contextlib import contextmanager, closing

import logging

log = logging.getLogger(__name__)


class Context(object):
    def dsn(self):
        return self.spanieldb.dsn()

    def connect(self, **kwargs):
        return connect(self.dsn(), **kwargs)

    def async_connect(self, **kwargs):
        return async_connect(self.dsn(), **kwargs)

    @contextmanager
    def reflect_db(self, sync=True, **kwargs):
        schemas = ('code', 'spaniel')
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
    clear_spanieldb(context)


def clear_spanieldb(context):
    purge_tables = [
    ]
    for table in purge_tables:
        context.spanieldb.execute('DELETE FROM %s' % table)


def start_db(context):
    start_component = SpanielDb
    port_manager = network.PortManager()
    context.coordinator = Coordinator(TestEnv(port_manager, start_component), start_component)
    context.coordinator.start()
    context.started_components = [start_component]
    context.spanieldb = context.coordinator.components[SpanielDb]


def stop_db(context):
    for c in context.started_components:
        context.coordinator.stop(c)
