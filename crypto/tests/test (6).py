from __future__ import print_function

import mock
import sys

from yql_utils import yql_binary_path
from crypta.graph.data_import.chevent_log.lib import CheventParser

from crypta.lib.python.yql_runner.tests import canonize_output, clean_up, load_fixtures


@mock.patch.dict("os.environ", {"YT_TOKEN": "FAKE", "ENV_TYPE": "FAKE"})
@clean_up(observed_paths=("//home", "//logs"))
@load_fixtures(("//logs/bs-chevent-log/1d/2019-10-03", "/fixtures/chevent.json"))
@canonize_output
def test_chevent_day(local_yt):
    """ Should check is chevnet parser correct """
    yql_task = CheventParser(
        date="2019-10-03",
        yt_proxy="localhost:{}".format(local_yt.yt_proxy_port),
        pool="xx",
        mrjob_binary=yql_binary_path("yql/tools/mrjob/mrjob"),
        udf_resolver_binary=yql_binary_path("yql/tools/udf_resolver/udf_resolver"),
        udfs_dir=";".join([yql_binary_path("yql/udfs"), yql_binary_path("ydb/library/yql/udfs")]),
        is_embedded=True,
    )

    try:
        yql_task.run()
    except Exception:
        print(yql_task.render_query(), file=sys.stderr)
        raise

    def select_all(table):
        return list(local_yt.yt_client.read_table(table, format="json"))

    output_tables = (
        "//home/crypta/fake/state/graph/v2/soup/day/2019-10-03/yandexuid_gaid_app-adv_event",
        "//home/crypta/fake/state/graph/v2/soup/day/2019-10-03/yandexuid_idfa_app-adv_event",
        "//home/crypta/fake/state/graph/v2/soup/day/2019-10-03/yandexuid_ifv_app-adv_event",
        "//home/crypta/fake/state/graph/v2/soup/day/2019-10-03/yandexuid_oaid_app-adv_event",
    )
    return {table: sorted(select_all(table)) for table in output_tables}
