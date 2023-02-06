import pytest
import yatest.common

from crypta.lib.python import (
    templater,
    yaml_config,
)
from crypta.adhoc.parse_facebook_ads.lib.config_pb2 import TConfig


pytest_plugins = [
    "crypta.lib.python.yql.test_helpers.fixtures",
]


@pytest.fixture(scope="function")
def config_file(local_yt):
    config_file_path = yatest.common.test_output_path("config.yaml")

    templater.render_file(
        yatest.common.source_path("crypta/adhoc/parse_facebook_ads/bundle/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": local_yt.get_server(),
        },
        strict=True,
    )

    return config_file_path


@pytest.fixture(scope="function")
def config(config_file):
    return yaml_config.parse_config(TConfig, config_file)
