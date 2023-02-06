import yatest.common

from yt import yson

from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)

OFFER_TABLE = "//offers"
MAPPING_TABLE = "//mapping"
RESULT_TABLE = "//result"


def get_offer_schema():
    schema = yson.YsonList([
        dict(name="hash", type="string", required=False),
        dict(name="trn_id", type="int64", required=False),
        dict(name="date", type="string", required=False),
        dict(name="product_id", type="int64", required=False),
        dict(name="qty", type="double", required=False),
        dict(name="value", type="double", required=False),
    ])
    schema.attributes["strict"] = True
    return schema


def get_mapping_schema():
    schema = yson.YsonList([
        dict(name="id", type="string", required=False, sort_order="ascending"),
        dict(name="id_type", type="string", required=False, sort_order="ascending"),
        dict(name="target_id", type="string", required=False),
        dict(name="target_id_type", type="string", required=False),
        dict(name="date_begin", type="string", required=False),
        dict(name="date_end", type="string", required=False),
    ])
    schema.attributes["strict"] = True
    schema.attributes["uinique_keys"] = True
    return schema


def test_join_with_crypta_id(local_yt, local_yt_and_yql_env):
    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/adhoc/smart/tasks/join_with_crypta_id/bin/join_with_crypta_id"),
        args=[
            "--yt-proxy", local_yt.get_server(),
            "--yt-tmp-dir", "//tmp",
            "--counter-id", "100",
            "--offer-table", OFFER_TABLE,
            "--mapping-table", MAPPING_TABLE,
            "--result-table", RESULT_TABLE,
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema("offers", OFFER_TABLE, get_offer_schema()), [tests.TableIsNotChanged()]),
            (tables.get_yson_table_with_schema("mapping", MAPPING_TABLE, get_mapping_schema()), [tests.TableIsNotChanged()]),
        ],
        output_tables=[
            (tables.YsonTable("result", RESULT_TABLE), [tests.Diff()]),
        ],
        env=local_yt_and_yql_env,
    )
