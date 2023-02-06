import os

import flask
import pytest

from crypta.dmp.common.upload_to_audience import upload
from crypta.lib.python import test_utils
import crypta.lib.python.tvm.helpers as tvm
from crypta.lib.python.yt import yt_helpers

pytest_plugins = [
    "crypta.lib.python.tvm.test_utils.fixtures",
]


@pytest.fixture(scope="function")
def segment_id():
    return 111111


@pytest.fixture(scope="function")
def db(segment_id, grants):
    return {segment_id: set(grants)}


@pytest.fixture(scope="function")
def mock_audience_server(db):
    class MockAudienceServer(test_utils.FlaskMockServer):
        def __init__(self):
            super(MockAudienceServer, self).__init__("MockAudienceServer")

            self.db = db

            @self.app.route("/v1/management/segment/<int:segment_id>/grants")
            def public_list_grants(segment_id):
                grants = [{"user_login": login} for login in sorted(list(self.db[segment_id]))]

                return flask.jsonify(grants=grants)

            @self.app.route("/v1/management/segment/<int:segment_id>/grant", methods=["PUT"])
            def public_add_grant(segment_id):
                data = flask.request.get_json(force=True)
                login = data["grant"]["user_login"]

                if login in self.db[segment_id]:
                    raise Exception("{} already has login {} in grants".format(segment_id, login))

                self.db[segment_id].add(login)
                return flask.jsonify(grant={})

            @self.app.route("/v1/management/segment/<int:segment_id>/grant", methods=["DELETE"])
            def public_delete_grant(segment_id):
                login = flask.request.args["user_login"]

                if login not in self.db[segment_id]:
                    raise Exception("{} hasn't got login {} in grants".format(segment_id, login))

                self.db[segment_id].remove(login)

                return flask.jsonify(success=True)

            @self.app.route("/v1/management/client/segments")
            def list_segments():
                segments = [{"id": id_, "name": "Segment {} [{}]".format(id_, id_), "status": ""} for id_ in sorted(list(self.db.keys()))]

                return flask.jsonify(segments=segments)

    with MockAudienceServer() as mock:
        yield mock


@pytest.fixture(scope="function")
def audience_upload_reducer(mock_audience_server, tvm_api):
    src_tvm_id = tvm_api.issue_id()

    os.environ[yt_helpers.get_yt_secure_vault_env_var_for(upload.UPLOAD_AUDIENCE_TVM_ID_VAR)] = str(src_tvm_id)
    os.environ[yt_helpers.get_yt_secure_vault_env_var_for(upload.UPLOAD_AUDIENCE_TVM_SECRET_VAR)] = tvm_api.get_secret(src_tvm_id)
    os.environ[yt_helpers.get_yt_secure_vault_env_var_for(upload.AUDIENCE_API_TVM_ID_VAR)] = str(tvm_api.issue_id())
    os.environ[yt_helpers.get_yt_secure_vault_env_var_for(upload.SHARE_AUDIENCE_OAUTH_TOKEN_VAR)] = "FAKE_TOKEN"
    os.environ[yt_helpers.get_yt_secure_vault_env_var_for(tvm.TVM_TEST_PORT_ENV_VAR)] = os.environ[tvm.TVM_TEST_PORT_ENV_VAR]

    reducer = upload.AudienceUploadReducer(
        login="fake_login",
        audience_url="http://{}".format(mock_audience_server.host),
        audience_port=mock_audience_server.port,
    )
    reducer.start()
    return reducer
