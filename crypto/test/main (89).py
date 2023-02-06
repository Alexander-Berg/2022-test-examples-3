import copy

import yatest.common
from yt.wrapper import ypath

from crypta.lib.python import (
    time_utils,
    yaml_config,
)
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.siberia.bin.common import test_helpers
from crypta.siberia.bin.common import yt_schemas
from crypta.siberia.bin.user_data_uploader.lib import config_fields


def convert_to_bytes(data):
    return [dict([k, v.encode("utf-8") if isinstance(v, str) else v] for k, v in item.items()) for item in data]


def test_user_data_uploader(local_ydb, local_yt, local_yt_and_yql_env, config, config_path, ydb_token):
    _create_old_crypta_id_user_data_table(local_ydb)

    user_data_yt_table = config[config_fields.USER_DATA_YT_TABLE]

    env = copy.deepcopy(local_yt_and_yql_env)
    env[time_utils.CRYPTA_FROZEN_TIME_ENV] = "1400000000"
    env["YDB_TOKEN"] = ydb_token

    tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/siberia/bin/user_data_uploader/bin/crypta-siberia-user-data-uploader"),
        args=["--config", config_path],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (
                tables.YsonTable("user_data_yt.yson", user_data_yt_table, on_write=tables.OnWrite(attributes={"schema": yt_schemas.get_crypta_id_user_data_schema()})),
                tests.TableIsNotChanged(),
            ),
        ],
        env=env,
    )

    assert not local_ydb.client.list_directory(test_helpers.YDB_PATHS.ExperimentalCryptaIdUserDataRootDir)

    return test_helpers.dump_crypta_id_user_data_dir(local_ydb)


def test_user_data_uploader_with_version(local_ydb, local_yt, local_yt_and_yql_env, config, config_path, ydb_token):
    version = "version_1_1"

    _create_old_crypta_id_user_data_table(local_ydb, version=version)

    user_data_yt_table = ypath.ypath_join(config[config_fields.CUSTOM_USER_DATA_YT_DIR], version)

    env = copy.deepcopy(local_yt_and_yql_env)
    env[time_utils.CRYPTA_FROZEN_TIME_ENV] = "1400000000"
    env["YDB_TOKEN"] = ydb_token

    tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/siberia/bin/user_data_uploader/bin/crypta-siberia-user-data-uploader"),
        args=[
            "--config", config_path,
            "--version", version,
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (
                tables.YsonTable("user_data_yt.yson", user_data_yt_table, on_write=tables.OnWrite(attributes={"schema": yt_schemas.get_crypta_id_user_data_schema()})),
                tests.TableIsNotChanged(),
            ),
        ],
        env=env,
    )

    assert not local_ydb.client.list_directory(test_helpers.YDB_PATHS.CryptaIdUserDataDir)

    return test_helpers.dump_experimental_crypta_id_user_data_dir(local_ydb, version)


def _create_old_crypta_id_user_data_table(local_ydb, version=None):
    old_ydb_table_name = "1300000000"

    test_helpers.create_crypta_id_user_data_table(local_ydb, old_ydb_table_name, version=version)
    test_helpers.upload_crypta_id_user_data_table(local_ydb, old_ydb_table_name, convert_to_bytes(yaml_config.load(yatest.common.test_source_path("data/user_data_ydb.yaml"))), version=version)
