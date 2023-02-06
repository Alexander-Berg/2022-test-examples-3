import collections
import datetime
import os

from grut.python.object_api.client import objects
import flask
import pytest
import yaml
import yatest.common

from crypta.lib.python import time_utils
from crypta.lib.python.logbroker.test_helpers import consumer_utils
from crypta.lib.python.test_utils import flask_mock_server


pytest_plugins = [
    "crypta.lib.python.yt.test_helpers.fixtures",
    "crypta.s2s.lib.test_helpers.fixtures",
]


@pytest.fixture(scope="function", autouse=True)
def setup(process_log_logbroker_client, object_api_client, client_id):
    consumer_utils.read_all(process_log_logbroker_client.create_consumer())

    with open(yatest.common.test_source_path("data/grut_conversion_source.yaml")) as f:
        conversion_sources = yaml.load(f)

    for conversion_source in conversion_sources:
        conversion_source["meta"]["client_id"] = client_id

    objects.create_objects(object_api_client, "conversion_source", conversion_sources)

    time_utils.set_current_time(int(datetime.datetime(year=2022, month=1, day=17, hour=0, minute=0, second=0).timestamp()))


@pytest.fixture(scope="function")
def process_commands():
    with open(yatest.common.test_source_path("data/process_command.yaml")) as f:
        return yaml.load(f)


@pytest.fixture(scope="function")
def max_order_age_days():
    return 20


@pytest.fixture(scope="function")
def max_backup_size():
    return 1


@pytest.fixture(scope="function")
def mock_cdp_api():
    OK_200 = {
        (11, 1111),
        (22, 2222),
        (44, 4444),
        (55, 5555),
    }

    ERROR_400 = {
        (33, 3333),
    }

    class MockCdpApi(flask_mock_server.FlaskMockServer):
        def __init__(self):
            super(MockCdpApi, self).__init__("MockCdpApi")
            self.requests = collections.defaultdict(list)
            self.output_dir = yatest.common.test_output_path("mock_cdp_api")

            os.makedirs(self.output_dir, exist_ok=True)

            @self.app.route("/cdp/internal/v1/upload_for_user/counter/<counter_id>/data/simple_orders", methods=['POST'])
            def simple_orders(counter_id):
                self.requests[counter_id].append((flask.request.args, self._canonize_files(flask.request.files)))

                uid = flask.request.args["uid"]
                merge_mode = flask.request.args["merge_mode"]
                delimiter_type = flask.request.args["delimiter_type"]

                assert "APPEND" == merge_mode
                assert "semicolon" == delimiter_type

                if (credentials := (int(counter_id), int(uid))) in OK_200:
                    return {}
                elif credentials in ERROR_400:
                    flask.abort(400)
                else:
                    flask.abort(500)

        def _canonize_file(self, file_storage):
            path = os.path.join(self.output_dir, file_storage.filename)
            with open(path, "wb") as f:
                file_storage.save(f)
            return yatest.common.canonical_file(path, local=True)

        def _canonize_files(self, files):
            return {
                file_storage.filename: self._canonize_file(file_storage)
                for _, file_storage in files.items(multi=True)
            }

    with MockCdpApi() as mock:
        yield mock
