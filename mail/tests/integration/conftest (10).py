import pytest
from yatest.common import network

from mail.devpack.lib.coordinator import Coordinator
from mail.pg.queuedb.devpack.components.queuedb import QueueDb
from mail.ymod_queuedb.devpack.components.queuedb_api import QueuedbApi
from mail.devpack.tests.helpers.env import TestEnv

import logging

log = logging.getLogger(__name__)


@pytest.fixture(scope="session", autouse=True)
def context():
    class Context(object):
        pass

    return Context()


@pytest.fixture(scope="session", autouse=True)
def feature_setup(request, context):
    start_devpack(context)

    def feature_teardown():
        stop_devpack(context)

    request.addfinalizer(feature_teardown)


@pytest.fixture(scope="function", autouse=True)
def step_setup(request, context):
    context.request_id = request.node.name
    clear_queuedb(context)


def clear_queuedb(context):
    purge_tables = [
        'queue.tasks',
        'queue.processed_tasks',
    ]
    for table in purge_tables:
        context.queuedb.execute('DELETE FROM %s' % table)


def start_devpack(context):
    start_component = QueuedbApi
    port_manager = network.PortManager()
    context.coordinator = Coordinator(TestEnv(port_manager, start_component), start_component)
    context.coordinator.start()
    context.started_components = [start_component]
    context.queuedb = context.coordinator.components[QueueDb]
    context.queuedb_api = context.coordinator.components[QueuedbApi]


def stop_devpack(context):
    for c in context.started_components:
        context.coordinator.stop(c)
