from __future__ import print_function

import mock
import sys

from yql_utils import yql_binary_path
from crypta.graph.data_import.passport.lib import PassportParser
from crypta.lib.python.yql_runner.tests import load_fixtures, canonize_output


@mock.patch.dict("os.environ", {"YT_TOKEN": "FAKE", "ENV_TYPE": "FAKE"})
@load_fixtures(
    (
        "//home/passport/production/userdata/2022-02-15",
        "/fixtures/passport.json",
        "/fixtures/passport.attrs.json",
    ),
)
@canonize_output
def test_passport(yt):
    """Should check is fp parser correct"""
    local_yt = yt
    print("Create YQL runner", file=sys.stderr)
    # call app metrica day parser
    yql_task = PassportParser(
        date="2022-02-15",
        yt_proxy="localhost:{}".format(local_yt.yt_proxy_port),
        pool="crypta_fake",
        mrjob_binary=yql_binary_path("yql/tools/mrjob/mrjob"),
        udf_resolver_binary=yql_binary_path("yql/tools/udf_resolver/udf_resolver"),
        udfs_dir=";".join([yql_binary_path("yql/udfs"), yql_binary_path("ydb/library/yql/udfs")]),
        is_embedded=True,
    )

    try:
        print("Start YQL runner", file=sys.stderr)
        yql_task.run(
            userdata_tbl="//home/passport/production/userdata/2022-02-15",
            soup_out_dir="//home/crypta/soup/dump",
        )
    except Exception:
        print(yql_task.render_query(), file=sys.stderr)
        raise

    def select_all(table):
        return list(local_yt.yt_client.read_table(table, format="json"))

    output_tables = local_yt.yt_client.search("//home/crypta/soup/dump", node_type=["table"], follow_links=True)
    return {table: sorted(select_all(table)) for table in output_tables}
