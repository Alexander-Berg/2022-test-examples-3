import os

import pytest
import yatest.common

from crypta.lib.python import (
    templater,
    yaml_config,
)
from crypta.siberia.bin.make_id_to_crypta_id.lib.maker.config_pb2 import TConfig


@pytest.fixture(scope="function")
def make_id_to_crypta_id_config_path(local_yt, matching_types, crypta_id_user_data_cypress_path):
    working_dir = yatest.common.test_output_path("make_id_to_crypta_id")

    if not os.path.isdir(working_dir):
        os.makedirs(working_dir)

    config_path = os.path.join(working_dir, "config.yaml")
    templater.render_file(
        yatest.common.source_path("crypta/siberia/bin/make_id_to_crypta_id/bundle/config.yaml"),
        config_path,
        {
            "environment": "qa",
            "yt_proxy": local_yt.get_server(),
            "matching_types": matching_types,
            "crypta_id_user_data_table": crypta_id_user_data_cypress_path,
        },
        strict=True,
    )
    return config_path


@pytest.fixture(scope="function")
def make_id_to_crypta_id_config(make_id_to_crypta_id_config_path):
    return yaml_config.parse_config(TConfig, make_id_to_crypta_id_config_path)
