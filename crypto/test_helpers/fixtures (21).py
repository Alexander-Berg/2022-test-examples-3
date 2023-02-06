import os

import pytest
import yatest.common

from crypta.lib.python import (
    templater,
    yaml_config,
)
from crypta.siberia.bin.matching_table_uploader.lib.uploader.config_pb2 import TConfig


@pytest.fixture(scope="function")
def matching_table_uploader_config_path(local_yt, local_ydb, mock_sandbox_server, matching_table_uploader_input_table):
    working_dir = yatest.common.test_output_path("matching_table_uploader")

    if not os.path.isdir(working_dir):
        os.makedirs(working_dir)

    config_path = os.path.join(working_dir, "config.yaml")
    context = {
        "environment": "qa",
        "yt_proxy": local_yt.get_server(),
        "ydb_endpoint": local_ydb.endpoint,
        "ydb_database": local_ydb.database,
        "input_table": matching_table_uploader_input_table,
        "crypta_sampler_udf_url": mock_sandbox_server.get_udf_url(),
        "denominator": 1,
        "rest": 0,
    }
    templater.render_file(
        yatest.common.source_path("crypta/siberia/bin/matching_table_uploader/bundle/config.yaml"),
        config_path,
        context,
        strict=True
    )
    return config_path


@pytest.fixture(scope="function")
def matching_table_uploader_config(matching_table_uploader_config_path, ydb_token):
    os.environ["YDB_TOKEN"] = ydb_token
    return yaml_config.parse_config(TConfig, matching_table_uploader_config_path)
