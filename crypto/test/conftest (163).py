import os

import pytest
from yabs.server.proto.keywords import keywords_data_pb2
import yatest.common

from crypta.lib.python import (
    templater,
    time_utils,
    yaml_config,
)
from crypta.siberia.bin.custom_audience.to_bigb_collector.proto.config_pb2 import TConfig


pytest_plugins = [
    "crypta.lib.python.yt.test_helpers.fixtures",
]


@pytest.fixture(scope="function")
def config_file(yt_stuff):
    config_file_path = yatest.common.test_output_path("config.yaml")

    templater.render_file(
        yatest.common.source_path("crypta/siberia/bin/custom_audience/to_bigb_collector/bundle/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": yt_stuff.get_server(),
            "id_type": "yandex_id",
            "table_suffix": "yandexuid",
            "source_dir": "//source/dir",
            "keyword_id": keywords_data_pb2.KW_CRYPTA_CUSTOM_AUDIENCE_HOST_CLUSTERS,
        },
        strict=True,
    )

    return config_file_path


@pytest.fixture(scope="function")
def config(config_file):
    return yaml_config.parse_config(TConfig, config_file)


@pytest.fixture
def frozen_time():
    result = "1600000000"
    os.environ[time_utils.CRYPTA_FROZEN_TIME_ENV] = result
    yield result


@pytest.fixture
def date(frozen_time):
    yield time_utils.get_current_moscow_datetime().date().isoformat()
