import pytest

from inspect import iscoroutinefunction

import os

from mail.nwsmtp.tests.lib import CLUSTERS, EXCLUDE_CLUSTERS
from mail.nwsmtp.tests.lib.default_conf import Conf, make_conf
from mail.nwsmtp.tests.lib.env import Env, get_env
from mail.nwsmtp.tests.lib.users import User
from mail.nwsmtp.tests.lib.util import is_corp

from mail.nwsmtp.tests.fixtures.users import *  # noqa
from mail.nwsmtp.tests.fixtures.dkim_domains import *  # noqa

_markers = {back.replace("-", ""): back for back in CLUSTERS}


def pytest_collection_modifyitems(session, config, items):
    for item in items:
        if iscoroutinefunction(item.obj):
            # Mark coroutine tests with "asyncio"
            item.add_marker(pytest.mark.asyncio)


@pytest.fixture()
def cluster(request):
    return request.param


def pytest_generate_tests(metafunc):
    # does any fixture depends on this fixture?
    if "cluster" not in metafunc.fixturenames:
        return

    get_marker = metafunc.definition.get_closest_marker

    # looking for "pytest.mark.cluster()" marker
    marker = get_marker("cluster")
    if marker:
        clusters = marker.args[0]
    else:
        # looking for "pytest.mark.{cluster}" marker
        markers = filter(None, [get_marker(name) for name in _markers])
        clusters = [_markers[m.name] for m in markers]
    if not clusters:
        raise RuntimeError("fixture \"cluster\" is requested but no markers found")

    add_clusters = set(os.getenv('NWSMTP_ADD_CLUSTERS', '').split(',')) & CLUSTERS
    clusters = list(set(clusters) - (EXCLUDE_CLUSTERS - add_clusters))

    metafunc.parametrize("cluster", clusters, indirect=True)


def pytest_configure(config):
    markers = ["mxbackout", "mxfront", "yaback", "smtp", "mxbackcorp",
               "mxcorp", "smtpcorp", "cluster"]

    for marker in markers:
        config.addinivalue_line("markers", marker)


@pytest.fixture
def conf(cluster) -> Conf:
    with make_conf(cluster) as conf:
        yield conf


@pytest.fixture
async def env(cluster, users) -> Env:
    async with get_env(cluster, users) as env:
        yield env


@pytest.fixture
def sender(cluster, prod_sender, corp_sender) -> User:
    if is_corp(cluster):
        return corp_sender
    return prod_sender


@pytest.fixture
def rcpt(cluster, prod_rcpt, corp_rcpt) -> User:
    if is_corp(cluster):
        return corp_rcpt
    return prod_rcpt
