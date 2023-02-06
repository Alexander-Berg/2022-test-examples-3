import flask
import pytest

from crypta.dmp.yandex.bin.common.python import config_fields
from crypta.lib.python import test_utils

pytest_plugins = [
    "crypta.lib.python.solomon.test_utils.fixtures",
    "crypta.lib.python.yql.test_helpers.fixtures",
]


UPLOAD_TO_AUDIENCE_ROBOT_NAME = "crypta-robot-audience"

SEGMENTS = [
    {"id": 1, "type": "uploading", "source_name": UPLOAD_TO_AUDIENCE_ROBOT_NAME},
    {"id": 2, "type": "xyz", "source_name": UPLOAD_TO_AUDIENCE_ROBOT_NAME},
    {"id": 3, "type": "uploading", "source_name": "xyz"},
    {"id": 4, "type": "uploading", "source_name": UPLOAD_TO_AUDIENCE_ROBOT_NAME},
    {"id": 5, "type": "uploading", "source_name": UPLOAD_TO_AUDIENCE_ROBOT_NAME},
    {"id": 6, "type": "uploading"}
]

GRANTS = {
    1: ["x"],
    2: ["x"],
    3: ["x"],
    4: [],
    5: ["x", "y", "z"]
}


@pytest.fixture(scope="function")
def mock_audience_server():
    mock = test_utils.FlaskMockServer("MockAudienceServer")

    @mock.app.route("/v1/management/segments")
    def public_list_segments():
        return flask.jsonify(segments=SEGMENTS)

    @mock.app.route("/v1/management/segment/<int:segment_id>/grants")
    def public_list_grants(segment_id):
        return flask.jsonify(grants=GRANTS[segment_id])

    with mock:
        yield mock


@pytest.fixture(scope="function")
def config(local_yt, mock_audience_server, mock_solomon_server):
    return {
        config_fields.YT_PROXY: local_yt.get_server(),
        config_fields.YT_POOL: "pool",
        config_fields.YT_TMP_DIR: "//tmp",
        config_fields.GRAPHITE_SOURCE_HOST: "local.local",
        config_fields.DMP_LOGIN: "dmp-xxx",
        config_fields.SEND_METRICS_TO_GRAPHITE: False,
        config_fields.OUT_META_TABLE: "//dmp/out/meta",
        config_fields.EXT_ID_BINDINGS_TABLE: "//dmp/ext_id_bindings",
        config_fields.YANDEXUID_BINDINGS_TABLE: "//dmp/yandexuid_bindings",
        config_fields.COVERAGE_METRICS_DAYS: [0, 1],
        config_fields.UPLOAD_TO_AUDIENCE_ROBOT_NAME: UPLOAD_TO_AUDIENCE_ROBOT_NAME,
        config_fields.AUDIENCE_LOGIN: "dmp-xxx-audience",
        config_fields.AUDIENCE_URL: "http://{}".format(mock_audience_server.host),
        config_fields.AUDIENCE_PORT: mock_audience_server.port,
        config_fields.OBTAIN_AUDIENCE_METRICS: True,
        config_fields.SOLOMON: {
            "url": mock_solomon_server.url_prefix,
            "project": "crypta_dmp",
            "cluster": "qa",
            "service": "stats",
        },
    }
