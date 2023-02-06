import os

import pytest
import yatest.common

from crypta.lib.python import (
    templater,
    yaml_config,
)
from crypta.siberia.bin.convert_to_user_data_stats.proto.convert_to_user_data_stats_job_config_pb2 import TConvertToUserDataStatsJobConfig


pytest_plugins = [
    "crypta.lib.python.yt.test_helpers.fixtures",
]


@pytest.fixture(scope="function")
def config_path(yt_stuff):
    working_dir = yatest.common.test_output_path("convert_to_user_data_stats")

    if not os.path.isdir(working_dir):
        os.makedirs(working_dir)

    config_path = os.path.join(working_dir, "config.yaml")
    templater.render_file(
        yatest.common.source_path("crypta/siberia/bin/convert_to_user_data_stats/bundle/config.yaml"),
        config_path,
        {
            "environment": "qa",
            "yt_proxy": yt_stuff.get_server(),
            "input_table_postfix": "lab/data/crypta_id/UserData",
            "output_table_postfix": "siberia/custom/crypta_id_user_data/by_crypta_id",
        },
        strict=True,
    )

    return config_path


@pytest.fixture(scope="function")
def config(config_path):
    return yaml_config.parse_config(TConvertToUserDataStatsJobConfig, config_path)
