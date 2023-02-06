import pytest

from crypta.lib.python import yaml_config
from crypta.siberia.bin.common import test_helpers
import crypta.siberia.bin.user_data_uploader.lib.test_helpers as user_data_uploader_test_helpers


pytest_plugins = [
    "crypta.siberia.bin.common.test_helpers.fixtures",
    "crypta.lib.python.ydb.test_helpers.fixtures",
    "crypta.lib.python.yql.test_helpers.fixtures",
]


@pytest.fixture(scope="function")
def config_path(local_yt, local_ydb, mock_sandbox_server):
    return user_data_uploader_test_helpers.get_config_path(
        yt_proxy=local_yt.get_server(),
        user_data_yt_table="//user_data",
        custom_user_data_yt_dir="//custom/user_data",
        ydb_endpoint=local_ydb.endpoint,
        ydb_database=local_ydb.database,
        crypta_sampler_udf_url=mock_sandbox_server.get_udf_url(),
        denominator=10,
        rest=0,
    )


@pytest.fixture(scope="function")
def config(config_path):
    return yaml_config.load(config_path)


@pytest.fixture(scope="function")
def ydb_token():
    return "YDB_TOKEN_XXX"


@pytest.fixture(scope="function", autouse=True)
def setup(local_ydb, config, ydb_token):
    local_ydb.remove_all()

    test_helpers.create_crypta_id_user_data_dir(local_ydb)
    test_helpers.create_experimental_crypta_id_user_data_root_dir(local_ydb)

    user_data_uploader_test_helpers.add_ydb_token_to_yql(config, ydb_token)
