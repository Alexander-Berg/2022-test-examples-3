import flask
import pytest

from crypta.dmp.yandex.bin.common.python import config_fields
from crypta.lib.python import test_utils

pytest_plugins = [
    "crypta.lib.python.tvm.test_utils.fixtures",
    "crypta.lib.python.yql.test_helpers.fixtures",
]


META_TABLE = "//dmp/clients/dmp_1/meta"
ERRORS_DIR = "//dmp/clients/dmp_1/errors"
YANDEXUID_BINDINGS_TABLE = "//dmp/clients/dmp_1/yandexuids_bindings"
TARIFF_PRICES_TABLE = "//dmp/tariff_prices"


@pytest.fixture(scope="function")
def mock_audience_server():
    class MockAudienceServer(test_utils.FlaskMockServer):
        def __init__(self):
            super(MockAudienceServer, self).__init__("MockAudienceServer")
            self.segments_count = 0

            @self.app.route("/v1/management/client/segments")
            def private_list_segments():
                return flask.jsonify(segments=[])

            @self.app.route("/v1/management/segment/<int:segment_id>/grants")
            def public_list_grants(segment_id):
                return flask.jsonify(grants=[])

            @self.app.route("/v1/management/client/segments/upload_file", methods=["POST"])
            def private_upload_segment():
                self.segments_count += 1
                return flask.jsonify(segment={"id": self.segments_count})

            @self.app.route("/v1/management/client/segment/<int:segment_id>/confirm", methods=["POST"])
            def private_confirm_segment(segment_id):
                return flask.jsonify(segment={})

            @self.app.route("/v1/management/client/segment/<int:segment_id>/modify_data", methods=["POST"])
            def private_modify_segment(segment_id):
                return flask.jsonify(segment={})

            @self.app.route("/v1/management/client/segment/<int:segment_id>", methods=["PUT"])
            def private_update_segment(segment_id):
                return flask.jsonify(segment={})

            @self.app.route("/v1/management/segment/<int:segment_id>/grant", methods=["PUT"])
            def public_add_grant(segment_id):
                return flask.jsonify(grant={})

            @self.app.route("/v1/management/client/segment/<int:segment_id>", methods=["DELETE"])
            def private_delete_segment(segment_id):
                return flask.jsonify(success=True)

            @self.app.route("/v1/management/segment/<int:segment_id>/grant", methods=["DELETE"])
            def public_delete_grant(segment_id):
                return flask.jsonify(success=True)

    with MockAudienceServer() as mock:
        yield mock


@pytest.fixture(scope="function")
def config(local_yt, mock_audience_server, tvm_api):
    return {
        config_fields.YT_PROXY: local_yt.get_server(),
        config_fields.YT_POOL: "crypta_adobe",
        config_fields.YT_TMP_DIR: "//tmp",
        config_fields.DMP_ID: 1,
        config_fields.META_TABLE: META_TABLE,
        config_fields.TARIFF_PRICES_TABLE: TARIFF_PRICES_TABLE,
        config_fields.YANDEXUID_BINDINGS_TABLE: YANDEXUID_BINDINGS_TABLE,
        config_fields.UPLOAD_TO_AUDIENCE_ERRORS_DIR: ERRORS_DIR,
        config_fields.AUDIENCE_LOGIN: "crypta-dmp-test-login",
        config_fields.UPLOAD_TO_AUDIENCE_MAX_CONCURRENT_JOBS: 10,
        config_fields.UPLOAD_TO_AUDIENCE_MEMORY_LIMIT: 200 * 1024 * 1024,
        config_fields.UPLOAD_TO_AUDIENCE_ERRORS_TTL_DAYS: 1,
        config_fields.AUDIENCE_URL: "http://{}".format(mock_audience_server.host),
        config_fields.AUDIENCE_PORT: mock_audience_server.port,
        config_fields.AUDIENCE_MIN_SEGMENT_SIZE: 1,
        config_fields.AUDIENCE_MAX_SEGMENT_SIZE: 2,
        config_fields.AUDIENCE_SRC_TVM_ID: tvm_api.issue_id(),
        config_fields.AUDIENCE_API_DST_TVM_ID: tvm_api.issue_id(),
    }
