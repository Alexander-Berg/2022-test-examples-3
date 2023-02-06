import datetime
import json
import time

import pytest
import yatest
import yt.wrapper as yt

from crypta.cm.services.common.data.python.id import TId
from crypta.cm.services.common.data.python.matched_id import TMatchedId
from crypta.cm.services.common.serializers.id.string.python import id_string_serializer
from crypta.cm.services.common.test_utils import (
    helpers,
    yt_kv_utils,
)
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)


def upload_body_to_match(body):
    ext_id = body["ext_id"]
    matched_id = body["ids"][0]
    return (
        TId(ext_id["type"], ext_id["value"]),
        TMatchedId(TId(matched_id["type"], matched_id["value"]))
    )


def upload(cm_client, matches):
    for ext_id, matched_id in matches:
        helpers.upload_and_identify(cm_client, ext_id, [matched_id])


@pytest.fixture
def tvm_src_id(tvm_ids):
    return tvm_ids.full_permissions


def test_cm_toucher(cm_client, local_yt_with_dyntables, config_path, config, tvm_api, tvm_src_id, yt_kv):
    diff = tests.Diff()
    expiration_test = tests.ExpirationTime(ttl=datetime.timedelta(days=config.BackupTtlDays))
    is_absent_test = tests.IsAbsent()

    with open(yatest.common.test_source_path("data/matches.json")) as f:
        matches = [upload_body_to_match(x) for x in json.loads(f.read())]

    ext_ids = [match[0] for match in matches]
    for ext_id in ext_ids:
        helpers.check_not_identify(cm_client, ext_id)

    upload(cm_client, matches)

    yt_results = tests.yt_test(
        yt_client=local_yt_with_dyntables.get_yt_client(),
        binary=yatest.common.binary_path("crypta/cm/services/toucher/bin/crypta-cm-toucher"),
        args=[
            "--config", config_path
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.YsonTable("1400000000.yson", yt.ypath_join(config.InputDir, "1400000000")), is_absent_test),
            (tables.YsonTable("1500000000.yson", yt.ypath_join(config.InputDir, "1500000000")), is_absent_test)
        ],
        output_tables=[
            (tables.YsonTable("backup_1400000000.yson", yt.ypath_join(config.BackupDir, "1400000000")), [diff, expiration_test]),
            (tables.YsonTable("backup_1500000000.yson", yt.ypath_join(config.BackupDir, "1500000000")), [diff, expiration_test]),
            (tables.YsonTable("errors_1400000000.yson", yt.ypath_join(config.ErrorsDir, "1400000000")), [is_absent_test]),
            (tables.YsonTable("errors_1500000000.yson", yt.ypath_join(config.ErrorsDir, "1500000000")), [is_absent_test])
        ],
        env={
            "TVM_SECRET": tvm_api.get_secret(tvm_src_id),
        }
    )

    time.sleep(10)

    identify_results = {
        id_string_serializer.ToString(ext_id): yt_kv_utils.read_match(yt_kv, ext_id).GetTouch()
        for ext_id in ext_ids
    }

    return {
        "yt": yt_results,
        "ext_ids": identify_results,
    }
