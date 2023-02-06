import logging
import pytest
import os
import json

import tests_common.pytest_bdd
from tests_common.fbbdb import User
from library.python.testing.pyremock.lib.pyremock import MockHttpServer
from mail.shiva.devpack.components.shiva import Shiva
from mail.devpack.lib.components.mdb import Mdb
from mail.devpack.lib.components.sharddb import ShardDb
from mail.devpack.lib.components.sharpei import Sharpei
from mail.devpack.lib.components.fakebb import FakeBlackbox
from mail.devpack.lib.components.fbbdb import FbbDb
from mail.devpack.lib.components.mulcagate import Mulcagate
from mail.devpack.tests.helpers.fixtures import coordinator_context
from mail.pg.huskydb.devpack.components.huskydb import HuskyDb
from mail.pg.queuedb.devpack.components.queuedb import QueueDb
from mail.callmeback.devpack.components.db import CallmebackDb

from yt.wrapper import YtClient

from .step_types_defs import extra_parsers # noqa
from .steps import *  # noqa

log = logging.getLogger(__name__)


@pytest.fixture(scope="session", autouse=True)
def context():
    return tests_common.pytest_bdd.context


@pytest.fixture(scope="session", autouse=True)
def feature_setup(request, context, coordinator):
    before_all(context, coordinator)
    request.addfinalizer(lambda: stop_pyremocks(coordinator))


@pytest.fixture(scope="function", autouse=True)
def step_setup(request, context):
    context.request = request


@pytest.fixture(scope="session")
def coordinator(context):
    with coordinator_context(Shiva) as coord:
        pyremock = MockHttpServer(coord.components[Shiva].pyremock_port())
        pyremock.start()
        coord.pyremock = pyremock
        yield coord


def get_config(components):
    return dict(
        maildb=components[Mdb].dsn(),
        maildb2=components[Mdb].shards[1].master.dsn(),
        sharddb=components[ShardDb].dsn(),
        huskydb=components[HuskyDb].dsn(),
        queuedb=components[QueueDb].dsn(),
        callmebackdb=components[CallmebackDb].dsn(),
        mulcagate=components[Mulcagate],
        shard_id=components[Mdb].config['mdb']['shards'][0]['id'],
        shard_id2=components[Mdb].config['mdb']['shards'][1]['id'],
        sharpei=components[Sharpei].api().location,
        fbbdb=components[FbbDb].dsn(),
        maildb_dsn_suffix='user=transfer connect_timeout=5',
    )


def fill_coordinator_context(context, coordinator):
    context.coordinator = coordinator
    context.coordinator.start()

    components = context.coordinator.components
    context.shiva = components[Shiva]
    context.started_components = components.keys()
    context.config = get_config(components)

    if 'YT_PROXY' in os.environ:
        context.yt = YtClient(proxy=os.environ['YT_PROXY'])

    def make_new_user_in_blackbox(name, uid=None):
        if uid:
            bb_response = components[FakeBlackbox].register("{name}@yandex.ru".format(name=name), uid=uid)
        else:
            bb_response = components[FakeBlackbox].register("{name}@yandex.ru".format(name=name))
        bb_response_dict = json.loads(bb_response.text)
        assert bb_response_dict['status'] == 'ok', "Can't create user in blackbox"
        uid = bb_response_dict['uid']
        return User(
            uid=uid,
            suid=uid*1000,
            login=name,
            db='pg'
        )

    def make_new_user(name, uid=None):
        user = make_new_user_in_blackbox(name, uid)
        response = components[Sharpei].api().conninfo(uid=user.uid, mode='master')
        assert response.status_code == 200, "Can't create user in sharpei"
        return user

    def get_user(user_name=None):
        if not user_name:
            user_name = getattr(context, 'last_user_name', None)
            if not user_name:
                raise RuntimeError("user_name not specified and no last_user_name in context")
        return context.users[user_name]

    context.make_new_user = make_new_user
    context.make_new_user_in_blackbox = make_new_user_in_blackbox
    context.users = {}
    context.get_user = get_user


def before_all(context, coordinator):
    fill_coordinator_context(context, coordinator)
    context.shiva_shards = {}
    context.req_params = {}


def stop_pyremocks(coordinator):
    coordinator.pyremock.stop()


def pytest_bdd_before_scenario(request, feature, scenario):
    log.debug('Started scenario %s', scenario.name)
    context = tests_common.pytest_bdd.context
    context.users = {}
    context.set_example_params(scenario)
    context.feature_name = feature.name
    context.scenario = scenario.name
    clear_shiva_shards(context)  # noqa: F405
    open_shard_registartion(context, context.config['shard_id'])  # noqa: F405


def pytest_bdd_after_scenario(request, feature, scenario):
    log.debug('Finished scenario %s', scenario.name)
    context = tests_common.pytest_bdd.context
    context.clear_scenario()
    context.last_user_name = None


def pytest_bdd_before_step_call(request, feature, scenario, step, step_func, step_func_args):
    log.debug('Matched on step [%s]', step.name)


def pytest_bdd_after_step(request, feature, scenario, step, step_func, step_func_args):
    log.debug('Finished step [%s]', step.name)
    tests_common.pytest_bdd.context.clear_step()
