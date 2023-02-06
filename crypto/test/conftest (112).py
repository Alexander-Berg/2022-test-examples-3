from collections import defaultdict
import json

import flask
from library.python import resource
import mock
import pytest

from crypta.lib.python import (
    test_utils,
    time_utils,
)

pytest_plugins = [
    "crypta.lib.python.yt.test_helpers.fixtures",
    "crypta.lib.python.logbroker.test_helpers.fixtures",
    'crypta.profile.lib.test_helpers.fixtures',
]


@pytest.fixture
def patch_config(lb_patched_config):
    with mock.patch("crypta.profile.utils.api.segments.get_not_exported_segments",
                    return_value=defaultdict(set)), \
        mock.patch("crypta.profile.utils.api.segments.get_trainable_segments",
                   return_value=set(['1660'])):
        yield lb_patched_config


@pytest.fixture
def date():
    return '2020-10-09'


@pytest.fixture(scope="function")
def current_timestamp():
    timestamp = 1602234000  # 2020-10-09 09:00:00 GMT+0000
    with test_utils.EnvironmentContextManager({time_utils.CRYPTA_FROZEN_TIME_ENV: str(timestamp)}):
        yield timestamp


@pytest.fixture
def mock_crypta_api():
    class MockCryptaApi(test_utils.FlaskMockServer):
        def __init__(self):
            super(MockCryptaApi, self).__init__("Crypta API")
            self.swagger = json.loads(resource.find("/swagger.json"))

            def export(segment_id, keyword_id, tags, export_type_id):
                return {'segmentId': segment_id,
                        'keywordId': keyword_id,
                        'tags': tags,
                        'exportTypeId': export_type_id,
                        }

            def exports(export_list):
                return {'exports': export_list}

            def segment(export_list):
                return {'exports': export_list}

            @self.app.route("/lab/segment")
            def get_segment():
                result = [segment(exports([export(1, 557, ['abc'], 'crypta_id')])),
                          segment(exports([export(2, 444, ['crypta_id', 'abc'], 'crypta_id')])),
                          segment(exports([export(3, 557, ['crypta_id', 'abc'], 'yandexuid')])),
                          ]
                return flask.jsonify(result)

            @self.app.route("/swagger.json")
            @self.app.route("/")
            def swagger():
                return flask.jsonify(self.swagger)

    with MockCryptaApi() as mock:
        yield mock
