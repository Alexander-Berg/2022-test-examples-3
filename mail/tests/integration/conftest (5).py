import logging

import mail.pypg.pypg.connect
import pytest
import tests_common.pytest_bdd
from mail.devpack.lib.components.base import FakeRootComponent
from mail.devpack.lib.components.fakebb import FakeBlackbox
from mail.devpack.lib.components.fbbdb import FbbDb
from mail.devpack.lib.components.mdb import Mdb
from mail.devpack.lib.components.mulcagate import Mulcagate
from mail.devpack.lib.components.sharddb import ShardDb
from mail.devpack.lib.components.sharpei import Sharpei
from mail.york.devpack.components.york import York
from mail.devpack.tests.helpers.fixtures import coordinator_context
from mail.husky.devpack.components.api import HuskyApi
from mail.husky.devpack.components.root import HuskyService
from mail.pg.huskydb.devpack.components.huskydb import HuskyDb
from mail.shiva.devpack.components.shiva import Shiva
from ora2pg.app import config_file
from tests_common.coordinator_context import dict2args, perform_with_retries
from tests_common.holders import (
    UIDHolder,
    UIDRanges,
    UsersHolder,
)


from .step_types_defs import *  # noqa
from .steps import *  # noqa

log = logging.getLogger(__name__)


class TestService(FakeRootComponent):
    NAME = "test-service"
    DEPS = [HuskyService, Shiva]


@pytest.fixture(scope="session", autouse=True)
def context():
    return tests_common.pytest_bdd.context


@pytest.fixture(scope="session", autouse=True)
def feature_setup(context, coordinator):
    before_all(context, coordinator)


@pytest.fixture(scope="function", autouse=True)
def step_setup(request, context):
    context.request = request


@pytest.fixture(scope="session")
def coordinator(context):
    with coordinator_context(TestService) as coord:
        yield coord


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
    config_dict['sharddb'] = components[ShardDb].dsn() + ' user=root connect_timeout=5'
    return dict2args(config_dict)


def fill_coordinator_context(context, coordinator):
    context.coordinator = coordinator
    components = context.coordinator.components
    context.started_components = components.keys()
    context.config = get_devpack_config(components)
    context.husky_api = components[HuskyApi].host_port
    context.shiva = components[Shiva]
    context.def_shard_id = components[Mdb].config['mdb']['shards'][0]['id'],
    context.sharddb_conn = perform_with_retries(
        lambda: mail.pypg.pypg.connect.Connector(components[ShardDb].dsn(), autocommit=True).connect()
    )
    context.huskydb_conn = perform_with_retries(
        lambda: mail.pypg.pypg.connect.Connector(components[HuskyDb].dsn(), autocommit=True).connect()
    )
    context.fbbdb_conn = perform_with_retries(
        lambda: mail.pypg.pypg.connect.Connector(components[FbbDb].dsn(), autocommit=True).connect()
    )


def before_all(context, coordinator):
    fill_coordinator_context(context, coordinator)
    context.metadata = {}
    context.lids = {}

    context.get_free_uid = UIDHolder(
        UIDRanges.transfer,
        sharddb_conn=context.sharddb_conn,
        fbbdb_conn=context.fbbdb_conn,
    )
    context.users = UsersHolder()
    context.shard_users = UsersHolder()
    context.shard_deleted_users = UsersHolder()


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
