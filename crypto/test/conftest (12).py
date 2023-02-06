import pytest
import yatest.common

from crypta.lib.python import templater
from crypta.lib.python import test_utils


pytest_plugins = [
    "crypta.lib.python.solomon.test_utils.fixtures",
    "crypta.lib.python.yql.test_helpers.fixtures",
]


@pytest.fixture
def db_path():
    return "//db/replica"


@pytest.fixture(scope="session")
def mock_sandbox_server():
    with test_utils.mock_sandbox_server_with_udf("CRYPTA_CM_UDF", "yql/udfs/crypta/cm/libcrypta_cm_udf.so") as mock:
        yield mock


@pytest.fixture
def config_path(local_yt, mock_sandbox_server, mock_solomon_server, db_path):
    context = dict(
        replicas=[
            {
                "yt-proxy": local_yt.get_server(),
                "dc": "man"
            },
            {
                "yt-proxy": local_yt.get_server(),
                "dc": "vla"
            }
        ],
        yt_pool="pool",
        yt_tmp_dir="//tmp",
        db_path=db_path,
        scheduling_tag_filter="",
        environment_type="xxx",
        crypta_cm_udf_url=mock_sandbox_server.get_udf_url(),
        solomon_url=mock_solomon_server.url_prefix,
    )
    output_path = "config.yaml"
    templater.render_file(
        yatest.common.source_path("crypta/cm/services/db_stats/bundle/config.yaml"),
        output_path,
        context,
        strict=True
    )
    return output_path
