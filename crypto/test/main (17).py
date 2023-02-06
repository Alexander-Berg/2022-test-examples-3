import pytest
import yatest.common

import yt.wrapper as yt

from crypta.lib.python import yaml_config
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests
)


DMP_XXX_UPLOAD_TO_AUDIENCE_ERRORS_DIR = "//xxx"
DMP_YYY_UPLOAD_TO_AUDIENCE_ERRORS_DIR = "//yyy"
DMP_ZZZ_UPLOAD_TO_AUDIENCE_ERRORS_DIR = "//zzz"


@pytest.fixture(scope="function")
def config(yt_stuff, mock_juggler_server):
    return {
        "yt-proxy": yt_stuff.get_server(),
        "yt-pool": "pool",
        "juggler-host": mock_juggler_server.host,
        "juggler-port": mock_juggler_server.port,
        "juggler-source": "SOURCE",
        "custom-checks": [{
            "path": DMP_XXX_UPLOAD_TO_AUDIENCE_ERRORS_DIR,
            "juggler_host": "JUGGLER_HOST",
            "juggler_service": "XXX_JUGGLER_SERVICE"
        }, {
            "path": DMP_YYY_UPLOAD_TO_AUDIENCE_ERRORS_DIR,
            "juggler_host": "JUGGLER_HOST",
            "juggler_service": "YYY_JUGGLER_SERVICE"
        }, {
            "path": DMP_ZZZ_UPLOAD_TO_AUDIENCE_ERRORS_DIR,
            "juggler_host": "JUGGLER_HOST",
            "juggler_service": "ZZZ_JUGGLER_SERVICE"
        }]
    }


def test_basic(yt_stuff, mock_juggler_server, config):
    tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/dmp/common/check_upload_to_audience_errors/bin/crypta-dmp-check-upload-to-audience-errors"),
        args=[
            "--config", yaml_config.dump(config)
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.YsonTable("dmp_xxx_1300000000.yson", yt.ypath_join(DMP_XXX_UPLOAD_TO_AUDIENCE_ERRORS_DIR, "1300000000")), None),
            (tables.YsonTable("dmp_xxx_1400000000.yson", yt.ypath_join(DMP_XXX_UPLOAD_TO_AUDIENCE_ERRORS_DIR, "1400000000")), None),
            (tables.YsonTable("dmp_xxx_1500000000.yson", yt.ypath_join(DMP_XXX_UPLOAD_TO_AUDIENCE_ERRORS_DIR, "1500000000")), None),
            (tables.YsonTable("dmp_yyy_1500000000.yson", yt.ypath_join(DMP_YYY_UPLOAD_TO_AUDIENCE_ERRORS_DIR, "1500000000")), None),
            (tables.YsonTable("dmp_zzz_1400000000.yson", yt.ypath_join(DMP_ZZZ_UPLOAD_TO_AUDIENCE_ERRORS_DIR, "1400000000")), None),
            (tables.YsonTable("dmp_zzz_1500000000.yson", yt.ypath_join(DMP_ZZZ_UPLOAD_TO_AUDIENCE_ERRORS_DIR, "1500000000")), None)
        ],
        output_tables=[]
    )
    return mock_juggler_server.dump_events_requests()
