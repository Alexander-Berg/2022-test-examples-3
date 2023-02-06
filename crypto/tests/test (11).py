from __future__ import print_function

import mock
import sys

from crypta.graph.data_import.visit_log.lib import VisitParser
from yql_utils import yql_binary_path
from crypta.lib.python.yql_runner.tests import load_fixtures, canonize_output


TEST_DATE = "2020-05-15"
SOURCE_TABLE = "//home/logfeller/logs/visit-v2-log/1d/{date}".format(date=TEST_DATE)
SOURCE_PRIVATE_TABLE = "//logs/visit-v2-private-log/1d/{date}".format(date=TEST_DATE)


def get_log_schema():
    return [
        {"name": "UserID", "required": False, "type": "uint64", "sort_order": "ascending"},
        {"name": "Goals_CallPhoneNumber", "required": False, "type": "any"},
    ]


def get_logs():
    return (
        {"UserID": 5555, "Goals_CallPhoneNumber": ["", "79058595417", ""]},
        {"UserID": 3163327321576515206, "Goals_CallPhoneNumber": ["+79856844044"]},
        {"UserID": 4938737081541971461, "Goals_CallPhoneNumber": ["", "796723816XX", "7495236xxxx"]},
        {"UserID": 6191758311566573779, "Goals_CallPhoneNumber": ["", ""]},
        {"UserID": 8682265231558301423},
        {"UserID": 9003589421574096811, "Goals_CallPhoneNumber": None},
    )


def load_fixtures_impl(yt):
    """ Load fixtures from hardcoded data """
    for table in (SOURCE_TABLE, SOURCE_PRIVATE_TABLE):
        yt.yt_client.create("table", table, recursive=True, attributes={"schema": get_log_schema()})
        yt.yt_client.write_table(table, get_logs(), format="json")


@mock.patch.dict("os.environ", {"YT_TOKEN": "FAKE", "ENV_TYPE": "FAKE"})
@load_fixtures(load_fixtures_impl)
@canonize_output
def test_visit_log(yt):
    """ Should check is fp parser correct """

    table_name = "phone_yandexuid_metrika-offline-conversion_visit_log"
    phone_yandexuid_out_path = "//home/crypta/production/state/graph/v2/soup/day/{data}/{table}".format(
        data=TEST_DATE, table=table_name
    )

    yql_task = VisitParser(
        date=TEST_DATE,
        yt_proxy="localhost:{}".format(yt.yt_proxy_port),
        pool="crypta_fake",
        mrjob_binary=yql_binary_path("yql/tools/mrjob/mrjob"),
        udf_resolver_binary=yql_binary_path("yql/tools/udf_resolver/udf_resolver"),
        udfs_dir=";".join([yql_binary_path("yql/udfs"), yql_binary_path("ydb/library/yql/udfs")]),
        is_embedded=True,
        visit_log=SOURCE_TABLE,
        visit_private_log=SOURCE_PRIVATE_TABLE,
        out_phone_yuid_soup=phone_yandexuid_out_path,
    )

    try:
        yql_task.run()
    except Exception:
        print(yql_task.render_query(), file=sys.stderr)
        raise

    return sorted(yt.yt_client.read_table(phone_yandexuid_out_path, format="json"))
