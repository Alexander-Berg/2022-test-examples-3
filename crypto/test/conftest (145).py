import datetime
import os

from grut.python.object_api.client import objects
import pytest
import yaml
import yatest.common

from crypta.lib.python import (
    templater,
    time_utils,
    yaml_config,
)
from crypta.lib.python.logbroker.test_helpers import consumer_utils
from crypta.s2s.services.scheduler.lib.config_pb2 import TConfig


pytest_plugins = [
    "crypta.lib.python.solomon.test_utils.fixtures",
    "crypta.s2s.lib.test_helpers.fixtures",
]


@pytest.fixture(scope="function", autouse=True)
def frozen_time():
    dt = time_utils.MOSCOW_TZ.localize(datetime.datetime(year=2020, month=10, day=10, hour=10))
    result = str(int(dt.timestamp()))
    os.environ[time_utils.CRYPTA_FROZEN_TIME_ENV] = result
    yield result


@pytest.fixture(scope="function", autouse=True)
def setup(download_log_logbroker_client, object_api_client, client_id):
    consumer_utils.read_all(download_log_logbroker_client.create_consumer())

    with open(yatest.common.test_source_path("data/grut_conversion_source.yaml")) as f:
        conversion_sources = yaml.load(f)

    for conversion_source in conversion_sources:
        conversion_source["meta"]["client_id"] = client_id

    objects.create_objects(object_api_client, "conversion_source", conversion_sources)


@pytest.fixture(scope="function")
def config_path(download_log_logbroker_config, grut_address, mock_solomon_server):
    config_file_path = yatest.common.test_output_path("config.yaml")

    context = {
        "environment": "qa",
        "grut_address": grut_address,
        "tvm_id": "0",
        "logbroker_server": download_log_logbroker_config.host,
        "logbroker_port": download_log_logbroker_config.port,
        "download_log_topic": download_log_logbroker_config.topic,
        "batch_size": 2,
        "solomon_url": mock_solomon_server.url_prefix,
    }

    templater.render_file(
        yatest.common.source_path("crypta/s2s/services/scheduler/bundle/config.yaml"),
        config_file_path,
        context,
        strict=True,
    )

    return config_file_path


@pytest.fixture(scope="function")
def config(config_path):
    os.environ["SOLOMON_TOKEN"] = "XXX"
    return yaml_config.parse_config(TConfig, config_path)
