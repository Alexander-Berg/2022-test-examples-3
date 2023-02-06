import yatest.common
from yt.wrapper import ypath

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.s2s.services.calc_stats.lib import calculator


DATE = "2021-11-11"


def test_calc_stats(local_yt, local_yt_and_yql_env, mock_solomon_server, config, config_file, index_file):
    output_files = tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/s2s/services/calc_stats/bin/crypta-s2s-calc-stats"),
        args=[
            "--config", config_file,
            "--index", index_file,
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (
                tables.get_yson_table_with_schema("uniform_postback.yson", ypath.ypath_join(config.BsUniformPostbackLog.SourceDir, DATE), get_uniform_postback_schema()),
                [tests.TableIsNotChanged()],
            ),
            (
                tables.get_yson_table_with_schema("mobile_postclick.yson", ypath.ypath_join(config.BsMobilePostclickLogDir, DATE), get_mobile_postclick_schema()),
                [tests.TableIsNotChanged()],
            ),
        ],
        output_tables=[
            (tables.YsonTable("track_table.yson", config.BsUniformPostbackLog.TrackTable, yson_format="pretty"), [tests.Diff()]),
        ],
        env=local_yt_and_yql_env,
    )

    solomon_requests = [serialize_solomon_request(request) for request in mock_solomon_server.dump_push_requests()]
    solomon_requests.sort()

    return output_files, solomon_requests


def get_uniform_postback_schema():
    return schema_utils.yt_schema_from_dict({
        "GoalID": "int64",
        "ExtPostBack": "string",
    })


def get_mobile_postclick_schema():
    return schema_utils.yt_schema_from_dict({
        "Goals_ID": "any",
        "ExtPostBack": "string",
    })


def serialize_solomon_request(request):
    parts = request["cluster"] + request["project"] + request["service"] + [
        request["sensors"][0]["labels"]["sensor"],
        request["sensors"][0]["labels"][calculator.Labels.client],
        request["sensors"][0]["labels"][calculator.Labels.conversion_name],
        str(request["sensors"][0]["labels"][calculator.Labels.goal_id]),
    ]
    return ".".join(parts), request["sensors"][0]["value"]
