from __future__ import print_function

import json
import mock
import sys

from library.python import resource
from yql_utils import yql_binary_path

from crypta.graph.data_import.fp_parser.lib import FingerPrintParser
from crypta.lib.python.yql_runner.tests import load_fixtures, canonize_output


def read_resource(fname):
    for line in resource.find(fname).splitlines():
        yield json.loads(line)


def extract_tskv(row):
    if "value" in row:
        row["value"] = dict(pair.split("=", 1) for pair in row["value"].split("\t"))
    return row


@mock.patch.dict("os.environ", {"YT_TOKEN": "FAKE", "ENV_TYPE": "FAKE"})
@load_fixtures(
    ("//home/logfeller/logs/bs-watch-log/1d/2018-03-04", "/fixtures/bswatch_log.json"),
    ("//home/logfeller/logs/passport-log/1d/2018-03-04", "/fixtures/passport_log.json"),
    ("//home/logfeller/logs/bar-navig-log/1d/2018-03-04", "/fixtures/barnavig_log.json"),
    (
        "//home/logfeller/logs/search-proto-reqans-log/1d/2018-03-04",
        "/fixtures/reqans_log.json",
        "/fixtures/reqans_log.attrs.json",
    ),
    (
        "//home/logfeller/logs/search-report-alice-log/1d/2018-03-04",
        "/fixtures/reqans_log.json",
        "/fixtures/reqans_log.attrs.json",
    ),
)
@canonize_output
def test_fp_parser(yt):
    """Should check is fp parser correct"""
    print("Create YQL runner", file=sys.stderr)
    # call app metrica day parser
    yql_task = FingerPrintParser(
        date="2018-03-04",
        yt_proxy="localhost:{}".format(yt.yt_proxy_port),
        pool="crypta_fake",
        mrjob_binary=yql_binary_path("yql/tools/mrjob/mrjob"),
        udf_resolver_binary=yql_binary_path("yql/tools/udf_resolver/udf_resolver"),
        udfs_dir=";".join([yql_binary_path("yql/udfs"), yql_binary_path("ydb/library/yql/udfs")]),
        is_embedded=True,
    )

    try:
        print("Start YQL runner", file=sys.stderr)
        yql_task.run()
    except Exception:
        print(yql_task.render_query(), file=sys.stderr)
        raise

    def select_all(table):
        return list(yt.yt_client.read_table(table, format="json"))

    output_tables = (
        "//home/crypta/fake/state/graph/2018-03-04/yuids_ua_day",
        "//home/crypta/fake/state/graph/2018-03-04/yuids_desk_day",
        "//home/crypta/fake/state/graph/2018-03-04/yuids_mob_day",
        "//home/crypta/fake/state/graph/2018-03-04/yuid_raw/yuid_with_login_fp",
        "//home/crypta/fake/state/graph/2018-03-04/yuid_raw/yuid_with_vk_fp",
        "//home/crypta/fake/state/graph/2018-03-04/yuid_raw/yuid_with_ok_fp",
        "//home/crypta/fake/state/graph/2018-03-04/yuid_raw/yuid_with_puid_fp",
        "//home/crypta/fake/state/graph/2018-03-04/yuid_raw/yuid_with_info",
        "//home/crypta/fake/state/graph/v2/soup/day/tmp/2018-03-04/passport",
        "//home/crypta/fake/state/graph/v2/soup/day/tmp/2018-03-04/reqans",
    )
    return {table: sorted(map(extract_tskv, select_all(table))) for table in output_tables}
