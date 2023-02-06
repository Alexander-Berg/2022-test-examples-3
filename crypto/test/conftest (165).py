import logging
import os

import flask
import pytest
import requests

from crypta.lib.python import (
    test_utils,
    time_utils,
)
import crypta.lib.python.tvm.helpers as tvm_helpers
from crypta.siberia.bin.common import test_helpers
from crypta.siberia.bin.common.data.proto import (
    user_set_status_pb2,
    user_set_type_pb2,
)
import crypta.siberia.bin.expirator.lib.test_helpers as expirator_test_helpers


logger = logging.getLogger(__name__)
pytest_plugins = [
    "crypta.lib.python.tvm.test_utils.fixtures",
    "crypta.lib.python.ydb.test_helpers.fixtures",
]

MATERIALIZED = user_set_type_pb2.TUserSetType().Materialized
NOT_MATERIALIZED = user_set_type_pb2.TUserSetType().NotMaterialized
READY = user_set_status_pb2.TUserSetStatus().Ready
META_DATA_ONLY = user_set_status_pb2.TUserSetStatus().MetaDataOnly
NOT_READY = user_set_status_pb2.TUserSetStatus().NotReady
NOT_FOUND_ID = 42


@pytest.fixture(scope="session")
def crypta_frozen_time():
    return 1500000000


@pytest.fixture(scope="function")
def setup(local_ydb):
    local_ydb.remove_all()
    test_helpers.create_user_sets_table(local_ydb)


@pytest.fixture(scope="session")
def tvm_src_id(tvm_api):
    return tvm_api.issue_id()


@pytest.fixture(scope="session")
def tvm_dst_id(tvm_api):
    return tvm_api.issue_id()


@pytest.fixture
def config_path(local_ydb, mock_siberia_core_server, tvm_src_id, tvm_dst_id, tvm_api):
    return expirator_test_helpers.get_config_path(
        ydb_endpoint=local_ydb.endpoint,
        ydb_database=local_ydb.database,
        siberia_host=mock_siberia_core_server.host,
        siberia_port=mock_siberia_core_server.port,
        tvm_src_id=tvm_src_id,
        tvm_dst_id=tvm_dst_id,
        tvm_api_port=tvm_api.port,
    )


@pytest.fixture(scope="function")
def mock_siberia_core_server(setup, tvm_src_id, tvm_dst_id, tvm_api):
    class MockSiberiaCoreServer(test_utils.FlaskMockServer):
        def _process_command(self, method):
            assert tvm_src_id == tvm_api.check_service_ticket(tvm_dst_id, flask.request.headers[tvm_helpers.TVM_TICKET_HEADER_KEY]).src
            assert 1 == len(flask.request.args.getlist("user_set_id"))

            user_set_id = int(flask.request.args["user_set_id"])

            self.commands.append(dict(method=method, user_set_id=user_set_id))

            return flask.make_response(test_helpers.make_simple_response("OK"), requests.codes.ok if user_set_id != NOT_FOUND_ID else requests.codes.not_found)

        def __init__(self):
            super(MockSiberiaCoreServer, self).__init__("Siberia Core")
            self.commands = []

            @self.app.route("/user_sets/remove", methods=["DELETE"])
            def remove_user_set():
                return self._process_command("/user_sets/remove")

            @self.app.route("/user_sets/remove_data", methods=["DELETE"])
            def remove_user_set_data():
                return self._process_command("/user_sets/remove_data")

    with MockSiberiaCoreServer() as mock:
        yield mock


@pytest.fixture(scope="session")
def env(tvm_api, tvm_src_id, crypta_frozen_time):
    return dict(os.environ, **{
        time_utils.CRYPTA_FROZEN_TIME_ENV: str(crypta_frozen_time),
        "YDB_TOKEN": "_FAKE_YDB_TOKEN_",
        "TVM_SECRET": tvm_api.get_secret(tvm_src_id),
    })


@pytest.fixture(scope="session")
def to_delete_1(crypta_frozen_time):
    return dict(id=1, title="TO_DELETE_1", expiration_time=crypta_frozen_time - 1, type=NOT_MATERIALIZED, status=READY)


@pytest.fixture(scope="session")
def to_delete_2(crypta_frozen_time):
    return dict(id=2, title="TO_DELETE_2", expiration_time=crypta_frozen_time - 1, type=MATERIALIZED, status=READY)


@pytest.fixture(scope="session")
def to_delete_3(crypta_frozen_time):
    return dict(id=6, title="TO_DELETE_3", expiration_time=crypta_frozen_time - 1, type=MATERIALIZED, status=NOT_READY)


@pytest.fixture(scope="session")
def to_delete_not_found(crypta_frozen_time):
    return dict(id=NOT_FOUND_ID, title="TO_DELETE_NOT_FOUND", expiration_time=crypta_frozen_time - 1, type=MATERIALIZED, status=READY)


@pytest.fixture(scope="session")
def to_keep_1(crypta_frozen_time):
    return dict(id=3, title="TO_KEEP_1", expiration_time=crypta_frozen_time, type=NOT_MATERIALIZED, status=READY)


@pytest.fixture(scope="session")
def to_keep_2(crypta_frozen_time):
    return dict(id=4, title="TO_KEEP_2", expiration_time=crypta_frozen_time, type=MATERIALIZED, status=READY)


@pytest.fixture(scope="session")
def to_keep_3(crypta_frozen_time):
    return dict(id=5, title="TO_KEEP_3", expiration_time=crypta_frozen_time - 1, type=MATERIALIZED, status=META_DATA_ONLY)
