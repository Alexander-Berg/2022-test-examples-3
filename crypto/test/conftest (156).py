import pytest
import yatest.common

from crypta.lib.python import (
    templater,
    yaml_config,
)
from crypta.siberia.bin.custom_audience.build_apps_for_suggester.proto.config_pb2 import TConfig


pytest_plugins = [
    "crypta.lib.python.yql.test_helpers.fixtures",
]


@pytest.fixture(scope="function")
def config_file(local_yt):
    config_file_path = yatest.common.test_output_path("config.yaml")

    templater.render_file(
        yatest.common.source_path("crypta/siberia/bin/custom_audience/build_apps_for_suggester/bundle/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": local_yt.get_server(),
            "additional_yt_clusters": [],
        },
        strict=True,
    )

    return config_file_path


@pytest.fixture(scope="function")
def config(config_file):
    return yaml_config.parse_config(TConfig, config_file)
