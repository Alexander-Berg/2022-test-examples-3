from __future__ import print_function

import mock
import sys

from yql_utils import yql_binary_path
from crypta.graph.data_import.webvisor.lib import WebVisorParser
from crypta.lib.python.yql_runner.tests import load_fixtures, canonize_output


TEST_DATE = "2018-10-02"
PREFIX_PATH = "//home/crypta/production/state"


def get_test_yuid_raw_path(table_name):
    return "{prefix}/graph/{date}/yuid_raw/{table}".format(
        prefix=PREFIX_PATH,
        date=TEST_DATE,
        table=table_name
    )


@mock.patch.dict("os.environ", {"YT_TOKEN": "FAKE", "ENV_TYPE": "FAKE"})
@load_fixtures(
    ("//home/logfeller/logs/bs-webvisor-log/1d/2018-10-02", "/fixtures/webvisor.json", "/fixtures/webvisor.spec.json")
)
@canonize_output
def test_webvisor(yt):
    """ Should check is fp parser correct """

    output_tables = dict(
        webvisor_processed="{prefix}/webvisor_processed/{date}".format(prefix=PREFIX_PATH, date=TEST_DATE),
        yuid_raw_email=get_test_yuid_raw_path("yuid_with_email_webvisor"),
        yuid_raw_phone=get_test_yuid_raw_path("yuid_with_phone_webvisor"),
        soup_table="{prefix}/graph/v2/soup/day/tmp/{date}/webvisor".format(prefix=PREFIX_PATH, date=TEST_DATE),
    )

    print("Create YQL runner", file=sys.stderr)
    yql_task = WebVisorParser(
        date=TEST_DATE,
        yt_proxy="localhost:{}".format(yt.yt_proxy_port),
        pool="crypta_fake",
        mrjob_binary=yql_binary_path("yql/tools/mrjob/mrjob"),
        udf_resolver_binary=yql_binary_path("yql/tools/udf_resolver/udf_resolver"),
        udfs_dir=";".join([yql_binary_path("yql/udfs"), yql_binary_path("ydb/library/yql/udfs")]),
        is_embedded=True,
        webvisor_tolerance=True,
        **output_tables
    )

    try:
        print("Start YQL runner", file=sys.stderr)
        yql_task.run()
    except Exception:
        print(yql_task.render_query(), file=sys.stderr)
        raise

    def select_all(table):
        return list(yt.yt_client.read_table(table, format="json"))

    return {table: sorted(select_all(table)) for table in sorted(output_tables.itervalues())}
