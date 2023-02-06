from __future__ import print_function

import itertools
import mock
import sys

from yql_utils import yql_binary_path
from crypta.graph.data_import.yuid_info_month.lib import YuidInfoMonthAggregator
from crypta.lib.python.yql_runner.tests import load_fixtures, canonize_output, read_resource


def load_fixtures_impl(yt):
    """ Test data stored in one file should be regrouped by dates to separate tabls """
    assert isinstance(yt.yt_proxy_port, int)
    # load fixtures
    for table_template, fixture, spec_path, schema_path in (
        (
            "//home/crypta/fake/state/graph/{date}/yuid_raw/yuid_with_info",
            "/fixtures/yuid_with_info_day.json",
            "/fixtures/yuid_with_info_day_spec.json",
            "/fixtures/yuid_with_info_day_schema.json",
        ),
        (
            "//home/crypta/fake/profiles/export/profiles_for_14days",
            "/fixtures/profiles_for_14days.json",
            None,
            "/fixtures/profiles_for_14days_schema.json",
        ),
    ):

        recs = list(sorted(read_resource(fixture), key=lambda item: item.get("id_date", None)))
        row_spec = next(read_resource(spec_path, by_rows=False)) if spec_path else None
        schema = next(read_resource(schema_path, by_rows=False)) if schema_path else None

        for date, recs in itertools.groupby(recs, key=lambda item: item.get("id_date", None)):
            table = table_template.format(date=date)

            attributes = dict()
            if row_spec:
                attributes["_yql_row_spec"] = row_spec
            if schema:
                attributes["schema"] = schema

            yt.yt_client.create("table", table, recursive=True, attributes=attributes)
            yt.yt_client.write_table(table, list(recs), format="json")


@mock.patch.dict("os.environ", {"YT_TOKEN": "FAKE", "ENV_TYPE": "FAKE"})
@load_fixtures(load_fixtures_impl)
@canonize_output
def test_yuid_info_month(yt):
    """ Should check is metrica parser correct """
    print("Create YQL runner", file=sys.stderr)
    yql_task = YuidInfoMonthAggregator(
        date_start="2018-03-04",
        date_end="2018-03-10",
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

    output_tables = ("//home/crypta/fake/ids_storage/yandexuid/yuid_with_all_info",)
    return {table: sorted(select_all(table)) for table in output_tables}
