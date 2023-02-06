import pytest
import random

import tests_common.pytest_bdd
from library.python.testing.pyremock.lib.pyremock import MockHttpServer
from mail.barbet.devpack.components.barbet import BarbetDevpack as Barbet
from mail.devpack.tests.helpers.fixtures import coordinator_context
from tests_common.coordinator_context import fill_coordinator_context
from tests_common.holders import (
    UIDHolder,
    UIDRanges,
    UsersHolder,
)

from .steps import *  # noqa


@pytest.fixture(scope="session", autouse=True)
def context():
    return tests_common.pytest_bdd.context


@pytest.fixture(scope="session")
def barbet_coordinator():
    with coordinator_context(Barbet) as coord:
        pyremock = MockHttpServer(coord.components[Barbet].pyremock_port())
        pyremock.start()
        coord.pyremock = pyremock
        yield coord


@pytest.fixture(scope="session", autouse=True)
def feature_setup(request, context, barbet_coordinator):
    before_all(context, barbet_coordinator)
    request.addfinalizer(lambda: barbet_coordinator.pyremock.stop())


def before_all(context, barbet_coordinator):
    fill_coordinator_context(context, barbet_coordinator)
    context.barbet = context.coordinator.components[Barbet]
    context.get_free_uid = UIDHolder(
        UIDRanges.system,
        sharddb_conn=context.sharddb_conn,
    )
    context.users = UsersHolder()


def random_mix(a, b, c_a, c_b):
    mix = random.sample(a, k=c_a)
    mix.extend(random.sample(b, k=c_b))
    random.shuffle(mix)
    return mix
