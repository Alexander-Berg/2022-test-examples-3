import pytest
import yatest.common

from crypta.buchhalter.services.main.lib.backuper.config_pb2 import TConfig
from crypta.lib.python import (
    templater,
    test_utils,
    yaml_config,
)
from crypta.lib.python.yt.test_helpers import utils


pytest_plugins = [
    "crypta.lib.python.yql.test_helpers.fixtures",
]


@pytest.fixture(scope="session")
def mock_sandbox_server():
    with test_utils.mock_sandbox_server_with_udf("BIGB_UDF", "yql/udfs/bigb/libbigb_udf.so") as mock:
        yield mock


@pytest.fixture(scope="function")
def date():
    return "2020-11-05"


@pytest.fixture(scope="function")
def config_file(local_yt, mock_sandbox_server, date):
    config_file_path = yatest.common.test_output_path("config.yaml")

    templater.render_file(
        yatest.common.source_path("crypta/buchhalter/services/main/config/backuper/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": local_yt.get_server(),
            "output_ttl_days": utils.get_unexpired_ttl_days_for_daily(date),
            "bigb_udf_url": mock_sandbox_server.get_udf_url(),
        },
    )
    return config_file_path


@pytest.fixture(scope="function")
def config(config_file):
    return yaml_config.parse_config(TConfig, config_file)
