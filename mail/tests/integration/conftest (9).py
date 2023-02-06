from mail.devpack.lib.components.all_dumb import FakeRootComponent
from mail.devpack.lib.components.fakebb import FakeBlackbox
from mail.devpack.lib.components.mdb import Mdb
from mail.devpack.lib.components.fbbdb import FbbDb
from mail.devpack.lib.components.mulcagate import Mulcagate
from mail.devpack.lib.components.sharddb import ShardDb
from mail.devpack.lib.components.sharpei import Sharpei
from mail.york.devpack.components.york import York
from mail.devpack.tests.helpers.fixtures import coordinator_context
from mail.pg.huskydb.devpack.components.huskydb import HuskyDb

from ora2pg.app import config_file

import mail.pypg.pypg.connect
import tests_common.pytest_bdd
from tests_common.coordinator_context import dict2args, perform_with_retries
from tests_common.holders import (
    UIDHolder,
    UIDRanges,
    UsersHolder,
)

import pytest

from .step_types_defs import *  # noqa
from .steps import *  # noqa

import logging

log = logging.getLogger(__name__)


@pytest.fixture(scope="session", autouse=True)
def context():
    return tests_common.pytest_bdd.context


@pytest.fixture(scope="session", autouse=True)
def feature_setup(request, context, coordinator):
    before_all(context, coordinator)


@pytest.fixture(scope="function", autouse=True)
def step_setup(request, context):
    context.request = request


class TransferAll(FakeRootComponent):
    NAME = 'transfer_all'
    DEPS = [Mdb, ShardDb, HuskyDb, Sharpei, FakeBlackbox, Mulcagate, York]


def get_devpack_config(components):
    filename = config_file.env_to_config_file('devpack')
    file = config_file.read_config_file(filename)
    file = file.format(
        blackbox=components[FakeBlackbox].url,
        sharpei=components[Sharpei].api().location,
        huskydb_dsn=components[HuskyDb].dsn(),
        mulcagate=components[Mulcagate].host,
        mulcagate_port=components[Mulcagate].port,
        mulcagate_ca_path=components[Mulcagate].ssl_cert_path,
        york=components[York].yhttp.url,
    )
    config_dict = config_file.parse_config_file(file, filename)
    config_dict['maildb_dsn_suffix'] = 'user=root connect_timeout=5'
    config_dict['sharddb'] = components[ShardDb].dsn() + ' user=root connect_timeout=5'
    return dict2args(config_dict)


@pytest.fixture(scope="session")
def coordinator():
    with coordinator_context(TransferAll) as coord:
        yield coord


def fill_coordinator_context(context, coordinator):
    context.coordinator = coordinator
    components = context.coordinator.components
    context.started_components = components.keys()
    context.config = get_devpack_config(components)
    context.sharddb_conn = perform_with_retries(
        lambda: mail.pypg.pypg.connect.Connector(components[ShardDb].dsn(), autocommit=True).connect()
    )
    context.fbbdb_conn = perform_with_retries(
        lambda: mail.pypg.pypg.connect.Connector(components[FbbDb].dsn(), autocommit=True).connect()
    )


def before_all(context, coordinator):
    fill_coordinator_context(context, coordinator)
    context.first_shard_id = 1
    context.second_shard_id = 2

    def get_from_shard_id(to_shard_id):
        return 2 if to_shard_id == 1 else 1

    context.get_from_shard_id = get_from_shard_id

    context.get_free_uid = UIDHolder(
        UIDRanges.transfer,
        sharddb_conn=context.sharddb_conn,
        fbbdb_conn=context.fbbdb_conn,
    )
    context.users = UsersHolder()


def pytest_bdd_before_scenario(request, feature, scenario):
    log.debug('Started scenario %s', scenario.name)
    context = tests_common.pytest_bdd.context
    context.set_example_params(scenario)
    context.feature_name = feature.name


def pytest_bdd_after_scenario(request, feature, scenario):
    log.debug('Finished scenario %s', scenario.name)
    context = tests_common.pytest_bdd.context
    context.clear_scenario()
    context.users.forget()

    if 'replica' in context:
        context.replica.stop_replication()


def pytest_bdd_before_step_call(request, feature, scenario, step, step_func, step_func_args):
    log.debug('Matched on step [%s]', step.name)


def pytest_bdd_after_step(request, feature, scenario, step, step_func, step_func_args):
    log.debug('Finished step [%s]', step.name)
    tests_common.pytest_bdd.context.clear_step()
