import json
import os

import flask
import pytest
import yatest.common

from crypta.lib.python import (
    templater,
    test_utils,
    time_utils,
    yaml_config,
)
from crypta.tx.services.ltp_logos_export.lib.query.config_pb2 import TConfig


@pytest.fixture(scope="function")
def mock_reactor_api():
    class MockReactorApi(test_utils.FlaskMockServer):
        def __init__(self):
            super(MockReactorApi, self).__init__("MockReactorApi")

            @self.app.route("/api/v1/a/i/instantiate", methods=["POST"])
            def instantiate_artifact():
                return flask.jsonify(artifactInstanceId="1", creation_time=flask.request.json["userTimestamp"])

        def dump_requests(self):
            requests = super().dump_requests()
            for request in requests:
                for key in ("request_data", "response_data"):
                    request[key] = json.loads(request[key])
            return requests

    with MockReactorApi() as mock:
        yield mock


@pytest.fixture(scope="function")
def config_file(local_yt, mock_reactor_api):
    config_file_path = yatest.common.test_output_path("config.yaml")

    templater.render_file(
        yatest.common.source_path("crypta/tx/services/ltp_logos_export/bundle/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": local_yt.get_server(),
            "reactor_url": mock_reactor_api.url_prefix,
            "batch_size": 2,
            "min_date": "2020-09-10",
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
