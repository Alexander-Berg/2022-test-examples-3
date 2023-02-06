import datetime
import zlib

from google.protobuf import json_format
import pytest
import yatest.common
import yt.wrapper as yt
from yt import yson

from crypta.cm.services.calc_expire.lib.python import schemas
from crypta.cm.services.common.proto import match_pb2
from crypta.lib.python import (
    templater,
    time_utils,
)
from crypta.lib.python.yt.dyntables import kv_schema
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)

SRC_TABLE = "//source"
TO_EXPIRE_DIR = "//to_expire"
ERRORS_DIR = "//errors"
ERRORS_TTL_DAYS = 1
CRYPTA_FROZEN_TIME = "1570100000"


@pytest.fixture(scope="function")
def config_path(yt_stuff):
    context = {
        "environment": "qa",
        "yt_proxy": yt_stuff.get_server(),
        "yt_pool": "pool",
        "src_table": SRC_TABLE,
        "dst_dir": TO_EXPIRE_DIR,
        "errors_dir": ERRORS_DIR,
        "errors_ttl_days": ERRORS_TTL_DAYS,
        "scheduling_tag_filter": "",
    }
    output_path = "config.yaml"
    templater.render_file(
        yatest.common.source_path("crypta/cm/services/calc_expire/bundle/config.yaml"),
        output_path,
        context,
        strict=True
    )
    return output_path


def test_basic(yt_stuff, config_path):
    diff = tests.Diff()

    errors_test = [
        diff,
        tests.ExpirationTime(datetime.timedelta(days=ERRORS_TTL_DAYS)),
        tests.SchemaEquals(schemas.calc_expire_errors_schema()),
    ]

    to_expire_test = [
        diff,
        tests.SchemaEquals(schemas.calc_expire_schema())
    ]

    with open(yatest.common.test_source_path("data/input.yson")) as f:
        rows = list(yson.load(f, yson_type="list_fragment"))
        for row in rows:
            try:
                row["value"] = zlib.compress(json_format.ParseDict(row["value"], match_pb2.TMatch()).SerializeToString())
            except Exception:
                pass

    formatted_input = yatest.common.test_output_path("output.yson")
    with open(formatted_input, "w") as output:
        yson.dump(rows, output, yson_type="list_fragment")

    return tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/cm/services/calc_expire/bin/crypta-cm-calc-expire"),
        args=[
            "--config", config_path,
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema(formatted_input, SRC_TABLE, kv_schema.get()), tests.TableIsNotChanged()),
        ],
        output_tables=[
            (tables.YsonTable("to_expire.yson", yt.ypath_join(TO_EXPIRE_DIR, CRYPTA_FROZEN_TIME), yson_format="pretty"), to_expire_test),
            (tables.YsonTable("errors.yson", yt.ypath_join(ERRORS_DIR, CRYPTA_FROZEN_TIME), yson_format="pretty"), errors_test),
        ],
        env={
            time_utils.CRYPTA_FROZEN_TIME_ENV: CRYPTA_FROZEN_TIME,
        },
    )
