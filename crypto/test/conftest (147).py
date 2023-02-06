import flask
import pytest
import requests

from crypta.lib.python import test_utils
from crypta.s2s.lib import test_helpers


pytest_plugins = [
    "crypta.lib.python.yt.test_helpers.fixtures",
]


class MockPostbackServer(test_utils.FlaskMockServer):
    def __init__(self):
        super(MockPostbackServer, self).__init__("Postback")
        self.requests = []

        @self.app.route('/postback', methods=["GET"])
        def postback():
            self.requests.append(flask.request.args)

            if flask.request.args["reqid"] == "Yclid3":
                return flask.make_response("Too many requests", requests.codes.too_many_requests)

            return flask.make_response("", requests.codes.ok)

    @property
    def postback_url(self):
        return "{}/postback".format(self.url_prefix)


@pytest.fixture(scope="function")
def mock_postback_server():
    with MockPostbackServer() as mock:
        yield mock


@pytest.fixture(scope="function")
def config_file(yt_stuff, mock_postback_server):
    return test_helpers.render_config_file(
        yt_proxy=yt_stuff.get_server(),
        postback_url=mock_postback_server.postback_url,
        postback_retries=2,
    )


@pytest.fixture(scope="function")
def config(config_file):
    return test_helpers.read_config(config_file)
