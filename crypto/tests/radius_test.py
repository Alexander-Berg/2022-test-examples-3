from __future__ import print_function

import mock
import sys

from yql_utils import yql_binary_path
from crypta.graph.data_import.radius_filter.lib import RadiusFilter
from crypta.lib.python.yql_runner.tests import load_fixtures, canonize_output


@mock.patch.dict("os.environ", {"YT_TOKEN": "FAKE", "ENV_TYPE": "FAKE"})
@load_fixtures(
    ("//home/crypta/fake/state/radius/log/2018-05-15/all_radius_ips", "/fixtures/ips.json"),
    ("//home/logfeller/logs/bs-watch-log/1d/2018-05-15", "/fixtures/bswatch_log.json"),
)
@canonize_output
def test_radius_filter(yt):
    """ Should check is fp parser correct """
    print("Create YQL runner", file=sys.stderr)
    # call app metrica day parser
    yql_task = RadiusFilter(
        date="2018-05-15",
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

    output_tables = ("//home/crypta/fake/state/graph/2018-05-15/raw_links/watch_log_filtered_by_radius",)
    return {table: sorted(select_all(table)) for table in output_tables}
