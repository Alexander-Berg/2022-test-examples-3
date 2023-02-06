import copy
import logging
import os

import pytest
import yatest

from crypta.lib.python import (
    time_utils,
    yaml_config,
)
from crypta.lib.python.logbroker.test_helpers import consumer_utils
from crypta.lib.python.yt.test_helpers import tables
from crypta.siberia.bin.common import test_helpers
import crypta.siberia.bin.make_crypta_id_user_data.lib.test_helpers as make_crypta_id_user_data
import crypta.siberia.bin.make_id_to_crypta_id.lib.test_helpers as make_id_to_crypta_id
import crypta.siberia.bin.matching_table_uploader.lib.test_helpers as matching_table_uploader
import crypta.siberia.bin.user_data_uploader.lib.test_helpers as user_data_uploader


logger = logging.getLogger(__name__)
pytest_plugins = [
    "crypta.lib.python.juggler.test_utils.fixtures",
    "crypta.lib.python.tvm.test_utils.fixtures",
    "crypta.lib.python.ydb.test_helpers.fixtures",
    "crypta.lib.python.yql.test_helpers.fixtures",
    "crypta.siberia.bin.common.test_helpers.fixtures",
    "crypta.siberia.bin.core.lib.test_helpers.fixtures",
    "crypta.siberia.bin.describer.lib.test_helpers.fixtures",
    "crypta.siberia.bin.make_id_to_crypta_id.lib.test_helpers.fixtures",
    "crypta.siberia.bin.matching_table_uploader.lib.test_helpers.fixtures",
    "crypta.siberia.bin.mutator.lib.test_helpers.fixtures",
    "crypta.siberia.bin.segmentator.lib.test_helpers.fixtures",
]


@pytest.fixture(scope="session")
def ydb_token():
    return "_FAKE_YDB_TOKEN_"


@pytest.fixture(scope="function")
def env(tvm_api, tvm_ids, ydb_token, local_yt, local_yt_and_yql_env):
    environment = dict(
        os.environ,
        YDB_TOKEN=ydb_token,
        TVM_SECRET=tvm_api.get_secret(tvm_ids.full_permissions),
        LOCAL_YT_SERVER=local_yt.get_server(),
    )
    environment.update(local_yt_and_yql_env)
    return environment


@pytest.fixture(scope="function", autouse=True)
def setup(local_ydb, change_log_logbroker_client, describe_log_logbroker_client, segmentate_log_logbroker_client):
    logger.info("SETUP")

    consumer_utils.read_all(change_log_logbroker_client.create_consumer(), timeout=1)
    consumer_utils.read_all(describe_log_logbroker_client.create_consumer(), timeout=1)
    consumer_utils.read_all(segmentate_log_logbroker_client.create_consumer(), timeout=1)

    local_ydb.remove_all()
    test_helpers.create_user_sets_table(local_ydb)
    test_helpers.create_user_set_stats_table(local_ydb)
    test_helpers.create_id_to_crypta_id_dir(local_ydb)
    test_helpers.create_crypta_id_user_data_dir(local_ydb)


@pytest.fixture
def matching_types():
    return ["login"]


@pytest.fixture
def crypta_id_user_data_cypress_path():
    return "//crypta_id_user_data"


@pytest.fixture(scope="function")
def yandexuid_user_data_yt(local_yt):
    table = tables.get_yson_table_with_schema(
        yatest.common.test_source_path("data/yandexuid_user_data.yson"),
        "//yandexuid_user_data",
        make_crypta_id_user_data.get_yandexuid_user_data_schema(),
    )
    table.write_to_local(local_yt.get_yt_client())
    return table


@pytest.fixture(scope="function")
def yandexuid_to_crypta_id_yt(local_yt):
    table = tables.get_yson_table_with_schema(
        yatest.common.test_source_path("data/yandexuid_to_crypta_id.yson"),
        "//yandexuid_to_crypta_id",
        make_crypta_id_user_data.get_yandexuid_to_crypta_id_schema(),
    )
    table.write_to_local(local_yt.get_yt_client())
    return table


@pytest.fixture(scope="function")
def crypta_id_user_data_yt(local_yt, yandexuid_user_data_yt, yandexuid_to_crypta_id_yt, crypta_id_user_data_cypress_path, env):
    table = tables.YsonTable(None, crypta_id_user_data_cypress_path)

    config_path = make_crypta_id_user_data.get_config_path(
        yt_proxy=local_yt.get_server(),
        yandexuid_user_data_table_path=yandexuid_user_data_yt.cypress_path,
        yandexuid_to_crypta_id_table_path=yandexuid_to_crypta_id_yt.cypress_path,
        crypta_id_user_data_table_path=table.cypress_path,
    )

    binary = yatest.common.binary_path("crypta/siberia/bin/make_crypta_id_user_data/bin/crypta-make-crypta-id-user-data")
    yatest.common.execute([binary, "--config", config_path], env=env)

    return table


@pytest.fixture(scope="function", autouse=True)
def crypta_id_user_data_ydb(local_yt, local_ydb, mock_sandbox_server, crypta_id_user_data_yt, ydb_token, env, setup):
    config_path = user_data_uploader.get_config_path(
        yt_proxy=local_yt.get_server(),
        user_data_yt_table=crypta_id_user_data_yt.cypress_path,
        custom_user_data_yt_dir="//custom/crypta_id_user_data",
        ydb_endpoint=local_ydb.endpoint,
        ydb_database=local_ydb.database,
        crypta_sampler_udf_url=mock_sandbox_server.get_udf_url(),
        denominator=1,
        rest=0,
    )

    user_data_uploader.add_ydb_token_to_yql(yaml_config.load(config_path), ydb_token)

    binary = yatest.common.binary_path("crypta/siberia/bin/user_data_uploader/bin/crypta-siberia-user-data-uploader")
    yatest.common.execute([binary, "--config", config_path], env=env)


@pytest.fixture(scope="function")
def matching_table_uploader_input_table(local_yt, make_id_to_crypta_id_config_path, make_id_to_crypta_id_config, env):
    config_path = make_id_to_crypta_id_config_path
    config = make_id_to_crypta_id_config

    for local_path, cypress_path, schema in [
        (None, config.DirectClientIdToPuidTable, make_id_to_crypta_id.get_direct_client_id_to_puid_table_schema()),
        (None, config.PuidToCryptaIdTable, make_id_to_crypta_id.get_matching_table_schema()),
        (yatest.common.test_source_path("data/login_to_crypta_id.yson"), config.MatchingTables[0], make_id_to_crypta_id.get_matching_table_schema()),
    ]:
        table = tables.get_yson_table_with_schema(local_path, cypress_path, schema)
        method = table.write_to_local if local_path is not None else table.create_on_local
        method(local_yt.get_yt_client())

    env = copy.deepcopy(env)
    env[time_utils.CRYPTA_FROZEN_TIME_ENV] = "1500000000"

    binary = yatest.common.binary_path("crypta/siberia/bin/make_id_to_crypta_id/bin/crypta-siberia-make-id-to-crypta-id")
    yatest.common.execute([binary, "--config", config_path], env=env)

    return config.OutputTable


@pytest.fixture(scope="function", autouse=True)
def id_to_crypta_id_ydb(local_yt, ydb_token, env, setup, matching_table_uploader_config_path, matching_table_uploader_config):
    config_path = matching_table_uploader_config_path
    config = matching_table_uploader_config

    matching_table_uploader.add_ydb_token_to_yql(config, ydb_token)

    binary = yatest.common.binary_path("crypta/siberia/bin/matching_table_uploader/bin/crypta-siberia-matching-table-uploader")
    yatest.common.execute([binary, "--config", config_path], env=env)
