import os

import pytest
import yatest.common

from crypta.s2s.services.calc_stats.lib.config_pb2 import TConfig
from crypta.lib.python import (
    templater,
    yaml_config,
)


pytest_plugins = [
    "crypta.lib.python.solomon.test_utils.fixtures",
    "crypta.lib.python.yql.test_helpers.fixtures",
]


@pytest.fixture
def index_file():
    index = [{
        "client": "client1",
        "conversion_name_to_goal_ids": {
            "conversion_name_1": [1111],
            "conversion_name_2": [2222, 6666],
        },
    }, {
        "client": "client2",
        "conversion_name_to_goal_ids": {
            "conversion_name_3": [3333],
        },
    }, {
        "client": "client3",
        "static_goal_id": 7777,
    }]
    return yaml_config.dump(index, yatest.common.test_output_path("index.yaml"))


@pytest.fixture
def config_file(local_yt, mock_solomon_server):
    config_file_path = yatest.common.test_output_path("config.yaml")

    templater.render_file(
        yatest.common.source_path("crypta/s2s/config/calc_stats_config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": local_yt.get_server(),
            "solomon_schema": "http",
            "solomon_host": "localhost",
            "solomon_port": mock_solomon_server.port,
        },
    )
    return config_file_path


@pytest.fixture
def config(config_file):
    os.environ["SOLOMON_TOKEN"] = "XXX"
    return yaml_config.parse_config(TConfig, config_file)
