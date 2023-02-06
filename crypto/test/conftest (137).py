import urllib.parse

import pytest
import yatest.common

from crypta.lib.python import (
    templater,
    time_utils,
    yaml_config,
)
from crypta.lib.python.test_utils.environment_context_manager import EnvironmentContextManager
from crypta.profile.services.upload_direct_exports_tanker_names_to_yt.bin.test import api
from crypta.profile.services.upload_direct_exports_tanker_names_to_yt.proto.config_pb2 import TConfig


pytest_plugins = [
    "crypta.lib.python.yql.test_helpers.fixtures",
    "crypta.lib.python.yt.test_helpers.fixtures",
]


@pytest.fixture
def lab_mock():
    with api.MockCryptaApi() as api_mock:
        yield api_mock


@pytest.fixture(scope="function")
def config_file(local_yt, yt_stuff, lab_mock):
    config_file_path = yatest.common.test_output_path("config.yaml")

    templater.render_file(
        yatest.common.source_path("crypta/profile/services/upload_direct_exports_tanker_names_to_yt/bundle/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": local_yt.get_server(),
            "replica_yt_proxy": yt_stuff.get_server(),
            "api_url": urllib.parse.urljoin(lab_mock.url_prefix, "swagger.json"),
            "segment_type_tanker_keys": {
                "orgvisits": "crypta_custom_audience_segment_type_orgvisits",
                "segment": "crypta_custom_audience_segment_type_interest",
                "good_type": "crypta_custom_audience_segment_type_good_type",
                "good_type2": "crypta_custom_audience_segment_type_good_type2",
            },
        },
        strict=True,
    )

    return config_file_path


@pytest.fixture(scope="function")
def config(config_file):
    return yaml_config.parse_config(TConfig, config_file)


@pytest.fixture(scope="function")
def frozen_time():
    with EnvironmentContextManager({time_utils.CRYPTA_FROZEN_TIME_ENV: '1600000000'}):
        yield
