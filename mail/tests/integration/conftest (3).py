import logging
import pytest
import random
import string
import sys

from library.python.testing.pyremock.lib.pyremock import MockHttpServer

from mail.collie.devpack.components.root import CollieService
from mail.collie.devpack.components.collie import Collie, CollieDirectorySyncWorker, CollieSyncWorker
from mail.devpack.tests.helpers.fixtures import coordinator_context


@pytest.fixture(scope='session', autouse=True)
def context():
    with coordinator_context(CollieService) as coord:
        coord.pyremocks = dict()
        coord.request_id = 'collie_tests'
        for collie in (Collie, CollieDirectorySyncWorker, CollieSyncWorker):
            pyremock = MockHttpServer(coord.components[collie].pyremock_port())
            pyremock.start()
            coord.pyremocks[collie] = pyremock
        coord.log = logging.getLogger('collie_integration_tests')
        stderr_handler = logging.StreamHandler(sys.stderr)
        stderr_handler.setFormatter(logging.Formatter('[%(asctime)s] %(message)s'))
        coord.log.addHandler(stderr_handler)
        coord.log.setLevel(logging.DEBUG)
        yield coord


@pytest.fixture(scope='session', autouse=True)
def setup_killer(request, context):
    request.addfinalizer(lambda: stop_pyremocks(context))


def pytest_bdd_before_scenario(request, feature, scenario):
    request_id = generate_request_id()
    context = request.getfixturevalue('context')
    context.log.debug('[%s] Scenario: %s', request_id, scenario.name)
    context.scenario_name = scenario.name
    context.request_id = request_id


def pytest_bdd_before_step_call(request, feature, scenario, step, step_func, step_func_args):
    context = request.getfixturevalue('context')
    context.log.debug('[%s] Step %s', context.request_id, step.name)


def stop_pyremocks(context):
    for v in context.pyremocks.values():
        v.stop()


def generate_request_id():
    return ''.join(random.choice(string.digits + 'abcdef') for _ in xrange(32))
