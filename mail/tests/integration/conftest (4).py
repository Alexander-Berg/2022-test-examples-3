import pytest

from yatest.common import source_path
from tests_common.holders import (
    UIDHolder,
    UIDRanges,
    UsersHolder,
)
from tests_common.coordinator_context import (
    make_coordinator,
    fill_coordinator_context,
)
from tests_common.pytest_bdd import context as ctx

from library.python.testing.pyremock.lib.pyremock import MockHttpServer
from mail.hound.devpack.components.application import Hound

from .steps import *  # noqa
from mail.devpack.tests.helpers.env import TestEnv


TestEnv.__test__ = False


def get_path(resource):
    path = source_path(resource).split("/")
    path.pop()
    return "/".join(path)


@pytest.fixture(scope="session", autouse=True)
def context():
    return ctx


@pytest.fixture(scope="session")
def coordinator():
    with make_coordinator(Hound) as coord:
        pyremock = MockHttpServer(coord.components[Hound].pyremock_port())
        pyremock.start()
        coord.pyremock = pyremock
        yield coord


@pytest.fixture(scope="session", autouse=True)
def feature_setup(request, context, coordinator):
    before_all(context, coordinator)
    request.addfinalizer(lambda: stop_pyremocks(coordinator))


@pytest.fixture(scope="function", autouse=True)
def step_setup(request, context):
    context.request = request


def before_all(context, coordinator):
    fill_coordinator_context(context, coordinator)
    context.hound = context.coordinator.components[Hound]

    context.get_free_uid = UIDHolder(
        UIDRanges.system,
        sharddb_conn=context.sharddb_conn,
    )
    context.users = UsersHolder()
    context.folders = {}


def pytest_bdd_before_scenario(request, feature, scenario):
    ctx.params = {}
    ctx.set_example_params(scenario)


def pytest_bdd_after_scenario(request, feature, scenario):
    ctx.users.forget()


def stop_pyremocks(coordinator):
    coordinator.pyremock.stop()
