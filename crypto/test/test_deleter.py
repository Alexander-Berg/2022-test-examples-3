import datetime
import json
import time

import pytest
import requests
import yatest
import yt.wrapper as yt

from crypta.cm.services.common.data.python.id import TId
from crypta.cm.services.common.data.python.matched_id import TMatchedId
from crypta.cm.services.common.test_utils import helpers
from crypta.cm.services.calc_expire.lib.python import schemas
from crypta.lib.python import templater
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)


BACKUP_DIR = "//backup"
ERRORS_DIR = "//errors"
SOURCE_DIR = "//source"
BACKUP_TTL_DAYS = 1

EXT_ID_TYPE_WITH_SHORT_TTL = "ext_ns_short_ttl"
EXT_ID_SHORT_TTL = 1

pytestmark = pytest.mark.mutator_template_args(custom_ttls=[(EXT_ID_TYPE_WITH_SHORT_TTL, EXT_ID_SHORT_TTL)])


def upload_body_to_match(body):
    ext_id = body["ext_id"]
    matched_id = body["ids"][0]
    return (
        TId(ext_id["type"], ext_id["value"]),
        TMatchedId(TId(matched_id["type"], matched_id["value"]), 0, 0, {})
    )


def upload(cm_client, matches):
    for ext_id, matched_id in matches:
        helpers.upload_and_identify(cm_client, ext_id, [matched_id])


def remove_match_ts(identify_response):
    del identify_response[0]["match_ts"]
    return identify_response


@pytest.fixture
def tvm_src_id(tvm_ids):
    return tvm_ids.full_permissions


@pytest.fixture
def config_path(cm_client, local_yt_with_dyntables, tvm_api, tvm_ids, tvm_src_id):
    context = {
        "environment": "qa",
        "handle": "delete",
        "subclient": "deleter",
        "yt_proxy": local_yt_with_dyntables.get_server(),
        "yt_pool": "pool",
        "src_dir": SOURCE_DIR,
        "err_dir": ERRORS_DIR,
        "backup_dir": BACKUP_DIR,
        "backup_ttl_days": BACKUP_TTL_DAYS,
        "dst_hosts": [cm_client.host.replace("http://", "")],
        "max_rps_per_job": 100,
        "max_rps": 1000,
        "tvm_src_id": tvm_src_id,
        "tvm_dst_id": tvm_ids.api,
    }
    output_path = "config.yaml"
    templater.render_file(
        yatest.common.source_path("crypta/cm/services/common/ext_id_mapper/bundle/config.yaml"),
        output_path,
        context,
        strict=True,
    )
    return output_path


def test_cm_deleter(cm_client, local_yt_with_dyntables, config_path, tvm_api, tvm_src_id):
    diff = tests.Diff()
    expiration_test = tests.ExpirationTime(ttl=datetime.timedelta(days=BACKUP_TTL_DAYS))

    with open(yatest.common.test_source_path("data/matches.json")) as f:
        matches = [upload_body_to_match(x) for x in json.loads(f.read())]

    ext_ids = [match[0] for match in matches]
    for ext_id in ext_ids:
        helpers.check_not_identify(cm_client, ext_id)

    upload(cm_client, matches)
    time.sleep(EXT_ID_SHORT_TTL * 2)

    results = tests.yt_test(
        yt_client=local_yt_with_dyntables.get_yt_client(),
        binary=yatest.common.binary_path("crypta/cm/services/common/ext_id_mapper/bin/crypta-cm-ext-id-mapper"),
        args=[
            "--config", config_path
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema("1400000000.yson", yt.ypath_join(SOURCE_DIR, "1400000000"), schemas.calc_expire_schema()), tests.IsAbsent()),
            (tables.get_yson_table_with_schema("1500000000.yson", yt.ypath_join(SOURCE_DIR, "1500000000"), schemas.calc_expire_schema()), tests.IsAbsent())
        ],
        output_tables=[
            (tables.YsonTable("backup_1400000000.yson", yt.ypath_join(BACKUP_DIR, "1400000000")), [diff, expiration_test]),
            (tables.YsonTable("backup_1500000000.yson", yt.ypath_join(BACKUP_DIR, "1500000000")), [diff, expiration_test]),
            (tables.YsonTable("errors_1400000000.yson", yt.ypath_join(ERRORS_DIR, "1400000000")), [tests.IsAbsent()]),
            (tables.YsonTable("errors_1500000000.yson", yt.ypath_join(ERRORS_DIR, "1500000000")), [tests.IsAbsent()])
        ],
        env={
            "TVM_SECRET": tvm_api.get_secret(tvm_src_id),
        }
    )

    time.sleep(10)

    identify_results = [cm_client.identify(ext_id) for ext_id in ext_ids]

    results.append([
        remove_match_ts(json.loads(result.text)) if result.status_code == requests.codes.ok else str(result.text)
        for result in identify_results
    ])

    return results
