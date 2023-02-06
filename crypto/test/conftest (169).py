import pytest
import yatest.common

from crypta.lib.python import yaml_config
from crypta.siberia.bin.common import test_helpers
import crypta.siberia.bin.matching_table_uploader.lib.test_helpers as uploader_test_helpers


pytest_plugins = [
    "crypta.siberia.bin.common.test_helpers.fixtures",
    "crypta.siberia.bin.matching_table_uploader.lib.test_helpers.fixtures",
    "crypta.lib.python.ydb.test_helpers.fixtures",
    "crypta.lib.python.yql.test_helpers.fixtures",
]


@pytest.fixture(scope="function")
def matching_table_uploader_input_table():
    return "//home/crypta/qa/siberia/id_to_crypta_id"


@pytest.fixture(scope="function")
def config_path(matching_table_uploader_config_path):
    return matching_table_uploader_config_path


@pytest.fixture(scope="function")
def config(matching_table_uploader_config):
    return matching_table_uploader_config


@pytest.fixture(scope="function")
def ydb_token():
    return "YDB_TOKEN_XXX"


@pytest.fixture(scope="function", autouse=True)
def setup(local_ydb, config, ydb_token):
    ydb_table_name = "1300000000"
    test_helpers.create_id_to_crypta_id_dir(local_ydb)
    test_helpers.create_id_to_crypta_id_table(local_ydb, ydb_table_name)
    test_helpers.upload_id_to_crypta_id_table(local_ydb, ydb_table_name, yaml_config.load(yatest.common.test_source_path("data/id_to_crypta_id__ydb.yaml")))

    uploader_test_helpers.add_ydb_token_to_yql(config, ydb_token)
