import json

import yatest.common
from yt.wrapper import (
    ypath,
    yson,
)

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    cypress,
    tables,
    tests,
)


DECODED_BUNDLE_FIELD = "decoded_bundle"


def test_facebook_ads_parsing(local_yt, local_yt_and_yql_env, config_file, config):
    bar_navig_schema = schema_utils.get_strict_schema(schema_utils.yt_schema_from_dict({
        "yandexuid": "string",
        DECODED_BUNDLE_FIELD: "string",
        "unixtime": "string",
    }, sort_by=["yandexuid"]))
    diff_test = tests.Diff()

    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/adhoc/parse_facebook_ads/bin/crypta-adhoc-parse-facebook-ads"),
        args=[
            "--config", config_file,
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.YsonTable(
                "{}.yson".format(date),
                ypath.ypath_join(config.BarNavig.SourceDir, date),
                on_write=tables.OnWrite(
                    attributes={"schema": bar_navig_schema},
                    row_transformer=_row_transformer,
                ),
            ), [tests.TableIsNotChanged()])
            for date in ("2021-07-21", "2021-07-22")
        ],
        output_tables=[
            (cypress.CypressNode(config.OutputDir), tests.TestNodesInMapNode([diff_test], tag="output")),
            (tables.YsonTable("tracker.yson", config.BarNavig.TrackTable), [diff_test]),
        ],
        env=local_yt_and_yql_env,
    )


def _row_transformer(row):
    decoded_bundle = row[DECODED_BUNDLE_FIELD]
    if decoded_bundle is not None:
        row[DECODED_BUNDLE_FIELD] = json.dumps(yson.yson_to_json(decoded_bundle)).replace(" ", "")
    return row
