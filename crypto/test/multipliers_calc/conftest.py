import pytest
import yatest.common

from crypta.buchhalter.services.main.lib.multipliers_calc.config_pb2 import TConfig
from crypta.lib.python import (
    templater,
    yaml_config,
)
from crypta.lib.python.yt.test_helpers import utils


pytest_plugins = [
    "crypta.lib.python.yql.test_helpers.fixtures",
]


@pytest.fixture(scope="function")
def date():
    return "2020-01-01"


@pytest.fixture(scope="function")
def config_file(local_yt, date):
    config_file_path = yatest.common.test_output_path("config.yaml")

    templater.render_file(
        yatest.common.source_path("crypta/buchhalter/services/main/config/multipliers_calc/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": local_yt.get_server(),
            "output_ttl_days": utils.get_unexpired_ttl_days_for_daily(date),
        },
    )
    return config_file_path


@pytest.fixture(scope="function")
def config(config_file):
    return yaml_config.parse_config(TConfig, config_file)
