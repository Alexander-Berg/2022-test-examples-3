import json
import re

import flask
import pytest

from crypta.cm.services.common import quoter_clients
from crypta.lib.python.test_utils.flask_mock_server import FlaskMockServer


pytest_plugins = [
    "crypta.cm.services.common.test_utils.fixtures",
]


def dict_to_tuple(d):
    return tuple(sorted(d.iteritems()))


METRICS = {
    dict_to_tuple({"project": "yt", "service": "accounts", "cluster": "yt-cluster-1", "account": "crypta"}): {
        "tablet_static_memory_in_gb": 1682.,
        "tablet_static_memory_limit_in_gb": 1733.,
    },
    dict_to_tuple({"project": "yt", "service": "accounts", "cluster": "yt-cluster-2", "account": "crypta"}): {
        "tablet_static_memory_in_gb": "NaN",
        "tablet_static_memory_limit_in_gb": 1733.,
    },
    dict_to_tuple({"project": "yt", "service": "accounts", "cluster": "yt-cluster-1", "account": "bigb"}): {
        "tablet_static_memory_in_gb": 51150.,
        "tablet_static_memory_limit_in_gb": 51200.,
    },
    dict_to_tuple({"project": "yt", "service": "accounts", "cluster": "yt-cluster-2", "account": "bigb"}): {
        "tablet_static_memory_in_gb": 51151.,
        "tablet_static_memory_limit_in_gb": 51200.,
    },
}


def parse_solomon_program(text):
    match = re.match(r"(\w+)\(\{([^}]+)\}\)", text)
    if match is None:
        raise Exception("Text is not a solomon program with function of sensors: {}".format(text))
    aggregation_function = match.group(1)
    labels = dict(
        pair.split("=") for pair in match.group(2).replace("'", "").replace(" ", "").split(",")
    )
    return aggregation_function, labels


class MockSolomonServer(FlaskMockServer):
    def __init__(self, name="MockSolomonServer"):
        super(MockSolomonServer, self).__init__(name)

        @self.app.route("/api/v2/projects/<project_id>/sensors/data", methods=["POST"])
        def data(project_id):
            assert "yt" == project_id
            try:
                body = json.loads(flask.request.data)
            except ValueError:
                return "Request body is not valid json: {}".format(flask.request.data), 500
            assert "program" in body
            assert "from" in body
            assert "to" in body

            _, labels = parse_solomon_program(body["program"])
            sensor = labels.pop("sensor")
            labels_tuple = dict_to_tuple(labels)

            assert labels_tuple in METRICS, labels_tuple
            scalar = METRICS[dict_to_tuple(labels)][sensor]
            return flask.jsonify({"scalar": scalar})

        @self.app.route("/push", methods=["POST"])
        def push():
            return "OK"

        @self.app.route("/api/v2/push", methods=["POST"])
        def push_v2():
            return "OK"

    def dump_push_requests(self):
        result = []

        for request in self.dump_requests():
            if request["path"] in ("/push", "/api/v2/push"):
                item = dict(request["args"].items() + json.loads(request["request_data"]).items())

                for sensor in item["sensors"]:
                    del sensor["ts"]

                result.append(item)

        return result


@pytest.fixture(scope="module")
def mock_solomon_server():
    with MockSolomonServer() as mock:
        yield mock


@pytest.fixture(scope="module")
def quoter_client(quoter):
    return quoter_clients.QuoterClient(quoter.host, quoter.port)


@pytest.fixture(scope="module")
def quoter_http_client(quoter_http_proxy):
    return quoter_clients.QuoterHttpClient(quoter_http_proxy.host, quoter_http_proxy.port)
