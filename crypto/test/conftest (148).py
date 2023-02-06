import itertools
import logging

import flask
import pytest

from crypta.lib.python import test_utils
from crypta.lib.python.tvm import helpers
from crypta.siberia.bin.common.proto.describe_ids_response_pb2 import TDescribeIdsResponse
from library.python.protobuf.json import proto2json

logger = logging.getLogger(__name__)

pytest_plugins = [
    "crypta.lib.python.yt.test_helpers.fixtures",
    "crypta.lib.python.tvm.test_utils.fixtures",
]


@pytest.fixture(scope="session")
def tvm_src_id(tvm_api):
    return tvm_api.issue_id()


@pytest.fixture(scope="session")
def tvm_dst_id(tvm_api):
    return tvm_api.issue_id()


@pytest.fixture(scope="function")
def mock_siberia_core_server(tvm_api, tvm_src_id, tvm_dst_id):
    class MockSiberiaCoreServer(test_utils.FlaskMockServer):
        def __init__(self):
            super(MockSiberiaCoreServer, self).__init__("Siberia Core")
            self.commands = []
            self.user_set_id = itertools.count()

            @self.app.route('/user_sets/describe_ids', methods=["POST"])
            def describe_ids():
                assert tvm_src_id == tvm_api.check_service_ticket(tvm_dst_id, flask.request.headers[helpers.TVM_TICKET_HEADER_KEY]).src

                user_set_id = self.user_set_id.next()

                self.commands.append({
                    "user_set_id": user_set_id,
                    "ids": flask.request.json["Ids"],
                    "mode": flask.request.args["mode"],
                })

                return proto2json.proto2json(TDescribeIdsResponse(UserSetId=str(user_set_id)))

    with MockSiberiaCoreServer() as mock:
        yield mock
