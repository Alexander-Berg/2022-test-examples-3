import logging

import flask
from library.python.protobuf.json import proto2json
import pytest

from crypta.lib.python import test_utils
from crypta.siberia.bin.common.proto import stats_pb2

logger = logging.getLogger(__name__)

pytest_plugins = [
    "crypta.lib.python.yt.test_helpers.fixtures",
]


@pytest.fixture
def user_set_retries():
    return {
        0: 0,
        1: 1,
        2: 2,
        10: 10,
    }


@pytest.fixture
def user_sets(user_set_retries):
    return list(user_set_retries.keys())


@pytest.fixture(scope="function")
def mock_siberia_core_server(user_set_retries):
    class MockSiberiaCoreServer(test_utils.FlaskMockServer):
        def __init__(self, user_set_retries):
            super(MockSiberiaCoreServer, self).__init__("Siberia Core")
            self.commands = []
            self.user_set_retries = dict(user_set_retries)

            @self.app.route('/user_sets/get_stats')
            def get_user_set():
                user_set_id = int(flask.request.args["user_set_id"])

                proto = stats_pb2.TStats()
                proto.Info.Ready = self.user_set_retries[user_set_id] <= 0
                proto.Info.ProcessedUsersCount = 1
                proto.UserDataStats.Counts.Total = 1

                response = proto2json.proto2json(proto)

                self.user_set_retries[user_set_id] -= 1

                self.commands.append({
                    "user_set_id": user_set_id,
                    "response": response,
                })

                return response

    with MockSiberiaCoreServer(user_set_retries) as mock:
        yield mock
