import logging

import flask
from library.python.protobuf.json import (
    json2proto,
    proto2json,
)
import pytest
import requests

from crypta.lib.python import test_utils
from crypta.siberia.bin.common import test_helpers
from crypta.siberia.bin.core.proto import add_users_request_pb2
import crypta.siberia.bin.users_uploader.lib.test_helpers as users_uploader_test_helpers

pytest_plugins = [
    "crypta.lib.python.tvm.test_utils.fixtures",
    "crypta.lib.python.yt.test_helpers.fixtures",
]

logger = logging.getLogger(__name__)


@pytest.fixture
def source_table_path():
    return "//source/src_table"


@pytest.fixture(scope="function")
def mock_siberia_core_server(user_set):
    with MockSiberiaCoreServer(user_set) as mock:
        yield mock


@pytest.fixture
def tvm_src_id(tvm_api):
    return tvm_api.issue_id()


@pytest.fixture
def tvm_dst_id(tvm_api):
    return tvm_api.issue_id()


@pytest.fixture
def config_path(yt_stuff, mock_siberia_core_server, tvm_src_id, tvm_dst_id, source_table_path):
    return users_uploader_test_helpers.get_config_path(
        yt_proxy=yt_stuff.get_server(),
        source_table_path=source_table_path,
        siberia_host=mock_siberia_core_server.host,
        siberia_port=mock_siberia_core_server.port,
        user_set_id=1,
        tvm_src_id=tvm_src_id,
        tvm_dst_id=tvm_dst_id,
        fields_id_types={
            "user_yandexuid": "yandexuid",
        },
    )


class MockSiberiaCoreServer(test_utils.FlaskMockServer):
    def __init__(self, user_set):
        super(MockSiberiaCoreServer, self).__init__("Siberia Core")
        self.commands = []

        @self.app.route('/users/add', methods=["POST"])
        def add_users():
            users = add_users_request_pb2.TAddUsersRequest()
            json2proto.json2proto(flask.request.data, users, json2proto.Json2ProtoConfig(map_as_object=True))

            test_response_code = users.Users[0].Attributes.get("test_response_code")
            code = requests.codes.ok if not test_response_code else test_response_code.Values[0]

            self.add_command("/users/add", code)
            return flask.make_response(test_helpers.make_simple_response(str(code)), code)

        @self.app.route('/user_sets/update', methods=["POST"])
        def user_sets_update():
            code = requests.codes.ok
            self.add_command("/user_sets/update", code)
            return flask.make_response(test_helpers.make_simple_response(str(code)), code)

        @self.app.route("/user_sets/describe", methods=["POST"])
        def user_sets_describe():
            code = requests.codes.ok
            self.add_command("/user_sets/describe", code)
            return flask.make_response(test_helpers.make_simple_response(str(code)), code)

        @self.app.route("/user_sets/get", methods=["GET"])
        def user_sets_get():
            code = requests.codes.ok
            self.add_command("/user_sets/get", code)
            return flask.make_response(proto2json.proto2json(user_set, proto2json.Proto2JsonConfig(map_as_object=True)), code)

    def add_command(self, method, code):
        command = {
            "method": method,
            "body": flask.request.json,
            "query": flask.request.args,
            "code": code,
        }
        self.commands.append(command)
