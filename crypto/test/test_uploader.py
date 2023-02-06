import datetime

import pytest
import yatest
import yt.wrapper as yt

from crypta.cm.services.common.data.python.id import TId
from crypta.cm.services.common.data.python.matched_id import TMatchedId
from crypta.cm.services.common.test_utils import (
    helpers,
)
from crypta.cm.services.common.db_sync import to_upload_schema
from crypta.lib.python import templater
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)


BACKUP_DIR = "//backup"
ERRORS_DIR = "//errors"
SOURCE_DIR = "//source"
BACKUP_TTL_DAYS = 1
REF_MATCHES = [(
    TId("ext_ns", "external_id_1"),
    TMatchedId(TId("yandexuid", "15"), 0, 0, {"synt": "0"})
), (
    TId("ext_ns", "external_id_2"),
    TMatchedId(TId("yandexuid", "18446744073709551615"), 0, 0, {"synt": "1"})
)]


@pytest.fixture
def tvm_src_id(tvm_ids):
    return tvm_ids.upload_only


@pytest.fixture
def config_path(cm_client, local_yt_with_dyntables, tvm_api, tvm_ids, tvm_src_id):
    context = {
        "environment": "qa",
        "yt_proxy": local_yt_with_dyntables.get_server(),
        "src_dir": SOURCE_DIR,
        "err_dir": ERRORS_DIR,
        "backup_dir": BACKUP_DIR,
        "backup_ttl_days": BACKUP_TTL_DAYS,
        "dst_hosts": [cm_client.host.replace("http://", "")],
        "max_rps_per_job": 50,
        "max_rps": 500,
        "tvm_src_id": tvm_src_id,
        "tvm_dst_id": tvm_ids.api,
        "frozen_time": 1500000100,
    }
    output_path = "config.yaml"
    templater.render_file(
        yatest.common.source_path("crypta/cm/services/uploader/bundle/config.yaml"),
        output_path,
        context,
        strict=True
    )
    return output_path


def test_cm_uploader(cm_client, local_yt_with_dyntables, config_path, tvm_api, tvm_src_id):
    diff = tests.Diff()
    expiration_test = tests.ExpirationTime(ttl=datetime.timedelta(days=BACKUP_TTL_DAYS))

    for ext_id, _ in REF_MATCHES:
        helpers.check_not_identify(cm_client, ext_id)

    result = tests.yt_test(
        yt_client=local_yt_with_dyntables.get_yt_client(),
        binary=yatest.common.binary_path("crypta/cm/services/uploader/bin/crypta-cm-uploader"),
        args=[
            "--config", config_path
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema("1500000000.yson", yt.ypath_join(SOURCE_DIR, "1500000000"), to_upload_schema.get()), tests.IsAbsent()),
        ],
        output_tables=[
            (tables.YsonTable("backup_1500000000.yson", yt.ypath_join(BACKUP_DIR, "1500000000")), [diff, expiration_test]),
            (tables.YsonTable("errors_1500000000.yson", yt.ypath_join(ERRORS_DIR, "1500000000")), [tests.IsAbsent()]),
        ],
        env={
            "TVM_SECRET": tvm_api.get_secret(tvm_src_id),
        }
    )

    for ext_id, matched_id in REF_MATCHES:
        helpers.check_identify(cm_client, ext_id, [matched_id])

    return result
