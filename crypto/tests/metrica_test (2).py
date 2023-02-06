from __future__ import print_function

import itertools
import mock
import sys

from crypta.graph.data_import.app_metrica_month.lib import MetricaMonthAggregator
from yql_utils import yql_binary_path
from crypta.lib.python.yql_runner.tests import load_fixtures, canonize_output, read_resource


def extract_dicts(row):
    if "features" not in row:
        return row
    if not row["features"]:
        row["features"] = {}
        return row

    features_dict = {}
    for feature in row["features"].split(";"):
        key, values = feature.split("-")
        values = dict(pair.split(":") for pair in values.split(","))
        features_dict[key] = values
    row["features"] = features_dict
    return row


def load_fixtures_impl(yt):
    """ Test data stored in one file should be regrouped by dates to separate tabls """
    prefix = "//home/crypta/fake/state/graph/{date}/mobile"
    assert isinstance(yt.yt_proxy_port, int)
    # load fixtures
    for table, fixture, spec in (
        (prefix + "/dev_info_yt", "/fixtures/dev_info_yt.json", "/fixtures/dev_info_yt.spec.json"),
        (prefix + "/uuid_info_yt", "/fixtures/uuid_info_yt.json", "/fixtures/uuid_info_yt.spec.json"),
    ):
        data = list(sorted(read_resource(fixture), key=lambda item: item["dates"][0]))
        row_spec = list(read_resource(spec))[0]
        for key, data in itertools.groupby(data, key=lambda item: item["dates"][0]):
            yt.yt_client.create(
                "table", table.format(date=key), recursive=True, attributes={"_yql_row_spec": row_spec}
            )
            yt.yt_client.write_table(table.format(date=key), list(data), format="json")


def batch(iterable, group=1):
    size = len(iterable)
    for indx in range(0, size, group):
        yield iterable[indx: min(indx + group, size)]


def load_fixtures_impl_day(yt):
    """ Test data stored in one file should be regrouped by dates to separate tabls """
    prefix_am = "//home/crypta/fake/state/graph/stream/extra_data/AppMetrikaTask/2018-04-27"
    prefix_rtb = "//home/crypta/fake/state/graph/stream/extra_data/RTBLogTask/2018-04-27"
    prefix_postback = "//home/crypta/fake/state/graph/stream/extra_data/PostbackLogTask/2018-04-27"
    # load fixtures
    for prefix, table, fixture, spec in (
        (prefix_am, "/dev_info_table", "/fixtures/dev_info_table.json", "/fixtures/dev_info_table.spec.json"),
        (prefix_am, "/uuid_info_table", "/fixtures/uuid_info_yt.json", "/fixtures/uuid_info_yt.spec.json"),
        (prefix_am, "/fuzzy2_metrica", "/fixtures/fuzzy2_metrica.json", "/fixtures/fuzzy2_metrica.spec.json"),
        (prefix_am, "/am_log_table", "/fixtures/am_log_table.json", "/fixtures/am_log_table.spec.json"),
        (prefix_rtb, "/ssp_apps_info_table", "/fixtures/rtb_extra_data.json", "/fixtures/rtb_extra_data.spec.json"),
        (prefix_postback, "/postback_apps_table", "/fixtures/postback_extra_data.json", "/fixtures/postback_extra_data.spec.json"),
    ):
        table = prefix + table + "/table-x-{name}"
        print(prefix, table, fixture, spec)
        data = list(read_resource(fixture))
        row_spec = list(read_resource(spec))[0]
        for offset, data in enumerate(batch(data, len(data) / 5)):
            yt.yt_client.create(
                "table", table.format(name=offset), recursive=True, attributes={"_yql_row_spec": row_spec}
            )
            yt.yt_client.write_table(table.format(name=offset), list(data), format="json")


@mock.patch.dict("os.environ", {"YT_TOKEN": "FAKE", "ENV_TYPE": "FAKE"})
@load_fixtures(load_fixtures_impl)
@canonize_output
def test_metrica_month(yt):
    """ Should check is metrica parser correct """
    print("Create YQL runner", file=sys.stderr)
    # call app metrica month parser
    yql_task = MetricaMonthAggregator(
        date_start="2018-04-27",
        date_end="2018-05-01",
        yt_proxy="localhost:{}".format(yt.yt_proxy_port),
        pool="xx",
        mrjob_binary=yql_binary_path("yql/tools/mrjob/mrjob"),
        udf_resolver_binary=yql_binary_path("yql/tools/udf_resolver/udf_resolver"),
        udfs_dir=";".join([yql_binary_path("yql/udfs"), yql_binary_path("ydb/library/yql/udfs")]),
        loglevel="INFO",
        limit=None,
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
        "//home/crypta/fake/ids_storage/device_id/app_metrica_month",
        "//home/crypta/fake/ids_storage/device_id/hash_month",
        "//home/crypta/fake/ids_storage/uuid/app_metrica_month",
    )
    return {table: sorted(map(extract_dicts, select_all(table))) for table in output_tables}


@mock.patch.dict("os.environ", {"YT_TOKEN": "FAKE", "ENV_TYPE": "FAKE"})
@load_fixtures(load_fixtures_impl_day)
@canonize_output
def test_metrica_day(yt):
    """ Should check is metrica parser correct aggregate from stream """
    print("Create YQL runner", file=sys.stderr)
    # call app metrica month parser
    yql_task = MetricaMonthAggregator(
        date_start="2018-04-27",
        date_end=None,
        is_month=False,
        yt_proxy="localhost:{}".format(yt.yt_proxy_port),
        pool="xx",
        mrjob_binary=yql_binary_path("yql/tools/mrjob/mrjob"),
        udf_resolver_binary=yql_binary_path("yql/tools/udf_resolver/udf_resolver"),
        udfs_dir=";".join([yql_binary_path("yql/udfs"), yql_binary_path("ydb/library/yql/udfs")]),
        loglevel="INFO",
        limit=None,
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
        "//home/crypta/fake/state/graph/2018-04-27/mobile/dev_info_yt",
        "//home/crypta/fake/state/graph/2018-04-27/mobile/uuid_info_yt",
        "//home/crypta/fake/state/graph/2018-04-27/mobile/account_manager/am_log",
        "//home/crypta/fake/state/graph/indevice/2018-04-27/fuzzy/fuzzy2_metrica",
    )
    return {table: sorted(map(extract_dicts, select_all(table))) for table in output_tables}
