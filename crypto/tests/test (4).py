from __future__ import print_function

import mock
import sys

from crypta.graph.data_import.autoru_log.lib import AutoRuParser
from yql_utils import yql_binary_path
from crypta.lib.python.yql_runner.tests import load_fixtures, canonize_output


TEST_DATE = "2019-01-31"
SOURCE_LOGFELLER_TABLE = "//home/logfeller/logs/vertis-event-log/1d/{date}".format(date=TEST_DATE)
SOURCE_WAREHOUSE_TABLE = "//home/verticals/broker/prod/warehouse/auto/events/1d/{date}".format(date=TEST_DATE)


@mock.patch.dict("os.environ", {"YT_TOKEN": "FAKE", "ENV_TYPE": "FAKE"})
@load_fixtures(
    (SOURCE_LOGFELLER_TABLE, "/fixtures/logfeller_table.json", "/fixtures/logfeller_table.spec.json"),
    (SOURCE_WAREHOUSE_TABLE, "/fixtures/warehouse_table.json", "/fixtures/warehouse_table.spec.json"),
)
@canonize_output
def test_auto_ru(yt):
    """ Should check is fp parser correct """

    def get_test_out_path(table_name):
        return "//home/crypta/production/state/graph/v2/soup/day/{data}/{table}".format(
            data=TEST_DATE, table=table_name
        )

    autoid_mm_device_id_out_path = get_test_out_path("autoid_mm_device_id_autoru_event_log-autoru")
    autoid_yandexuid_out_path = get_test_out_path("auto_id_yandexuid_autoru_event_log-autoru")
    autoid_phone_out_path = get_test_out_path("auto_id_phone_autoru_event_log-autoru")
    autoid_gaid_out_path = get_test_out_path("autoid_gaid_autoru_event_log-autoru")
    autoid_idfa_out_path = get_test_out_path("autoid_idfa_autoru_event_log-autoru")
    gaid_mm_device_id_out_path = get_test_out_path("gaid_mm_device_id_event_log-autoru")
    idfa_mm_device_id_out_path = get_test_out_path("idfa_mm_device_id_event_log-autoru")

    yql_task = AutoRuParser(
        date=TEST_DATE,
        yt_proxy="localhost:{}".format(yt.yt_proxy_port),
        pool="crypta_fake",
        mrjob_binary=yql_binary_path("yql/tools/mrjob/mrjob"),
        udf_resolver_binary=yql_binary_path("yql/tools/udf_resolver/udf_resolver"),
        udfs_dir=";".join([yql_binary_path("yql/udfs"), yql_binary_path("ydb/library/yql/udfs")]),
        is_embedded=True,
        logfeller_logs=SOURCE_LOGFELLER_TABLE,
        warehouse_logs=SOURCE_WAREHOUSE_TABLE,
        autoid_mm_device_id=autoid_mm_device_id_out_path,
        autoid_yandexuid=autoid_yandexuid_out_path,
        autoid_phone=autoid_phone_out_path,
        autoid_gaid=autoid_gaid_out_path,
        autoid_idfa=autoid_idfa_out_path,
        gaid_mm_device_id=gaid_mm_device_id_out_path,
        idfa_mm_device_id=idfa_mm_device_id_out_path,
    )

    try:
        yql_task.run()
    except:
        print(yql_task.render_query(), file=sys.stderr)
        raise

    def select_all(table):
        return list(yt.yt_client.read_table(table, format="json"))
    output_tables = (autoid_mm_device_id_out_path, autoid_yandexuid_out_path, autoid_phone_out_path,
                     autoid_gaid_out_path, autoid_idfa_out_path, gaid_mm_device_id_out_path, idfa_mm_device_id_out_path)

    return {table: sorted(select_all(table)) for table in output_tables}
