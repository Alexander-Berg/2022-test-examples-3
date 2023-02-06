from __future__ import print_function

import datetime
import json
import mock
import sys

from library.python import resource
from yql_utils import yql_binary_path

from crypta.graph.data_import.soup.lib import AddDaySoup
from crypta.graph.data_import.soup.lib.task import SoupTask
from crypta.lib.python.yql_runner.tests import canonize_output, clean_up, execute, load_fixtures, unmount


class FakeDate(datetime.date):

    """ Fake date to mock immutable builtins """

    @classmethod
    def today(cls):
        return cls(2018, 9, 15)


@mock.patch.dict("os.environ", {"YT_TOKEN": "TESTING", "ENV_TYPE": "TESTING"})
@clean_up(observed_paths=("//home", "//logs"))
@load_fixtures(
    ("//home/crypta/fake/state/graph/v2/soup/day/idfa_mm_device_id_app-metrica_mm", "/fixtures/table_1.json"),
    ("//home/crypta/fake/state/graph/v2/soup/day/gaid_mm_device_id_app-metrica_mm", "/fixtures/table_2.json"),
    ("//home/crypta/fake/state/graph/v2/soup/day/mm_device_id_uuid_app-metrica_mm", "/fixtures/table_3.json"),
    ("//home/crypta/fake/state/graph/v2/soup/yandexuid_icookie_cookie_wl", "/fixtures/soup_wl.json"),
    ("//home/crypta/fake/state/graph/v2/soup/gaid_mm_device_id_app-metrica_mm", "/fixtures/soup_mm.json"),
    # this table should not be in processed (should check is not recursive lookup)
    ("//home/crypta/fake/state/graph/v2/soup/ids/yandexuid", "/fixtures/ids_yuid.json"),
)
@canonize_output
def test_soup(local_yt):
    """ Should check is metrica parser correct """
    print("Create YQL runner", file=sys.stderr)
    # call app metrica day parser
    yql_task = AddDaySoup(
        soup_dir="//home/crypta/fake/state/graph/v2/soup",
        date="2019-01-01",
        throw_before_date="2018-12-20",
        yt_proxy="localhost:{}".format(local_yt.yt_proxy_port),
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
        return list(local_yt.yt_client.read_table(table, format="json"))

    output_tables = local_yt.yt_client.search(
        "//home/crypta/fake/state/graph/v2/soup", node_type=["table"], follow_links=True
    )
    return {table: sorted(select_all(table)) for table in output_tables}


@mock.patch("datetime.date", FakeDate)
@mock.patch("time.time", mock.MagicMock(return_value=1562061078.828523))
@mock.patch("crypta.graph.data_import.soup.lib.task.SoupTask._set_expiration", lambda self: 42)
@canonize_output
def test_bt_task(local_yt, conf):
    """ Should check is metrika bt task work correct """

    @load_fixtures(
        ("{conf.paths.stream.storage}/table_1".format(conf=conf), "/fixtures/table_1.json"),
        ("{conf.paths.stream.storage}/table_2".format(conf=conf), "/fixtures/table_2.json"),
        ("{conf.paths.stream.storage}/table_3".format(conf=conf), "/fixtures/table_3.json"),
        ("{conf.paths.storage.soup}/yandexuid_icookie_cookie_wl".format(conf=conf), "/fixtures/soup_wl.json"),
        ("{conf.paths.storage.soup}/gaid_mm_device_id_app-metrica_mm".format(conf=conf), "/fixtures/soup_mm.json"),
        # this table should not be in processed (should check is not recursive lookup)
        ("{conf.paths.storage.soup}/ids/yandexuid".format(conf=conf), "/fixtures/ids_yuid.json"),
    )
    @clean_up(observed_paths=("//home",))
    def inner_test(local_yt, conf):
        attrs = json.loads(resource.find("/fixtures/attr_processed.json"))
        for table in xrange(1, 4):
            local_yt.yt_client.set(
                "{conf.paths.stream.storage}/table_{index}/@processed".format(conf=conf, index=table), attrs
            )

        task = SoupTask(run_date="2018-09-15", log_sources="mm", commit_full_day="false")
        execute(task)

        assert task.query_template == "soup.sql.j2"
        assert task.crypta_env == "develop"

        def select_all(table):
            return list(local_yt.yt_client.read_table(table, format="json"))

        unmount(local_yt.yt_client, conf.paths.stream.processed)

        output = {
            "soup": {
                path.replace(conf.paths.crypta_root, "//crypta_root"): sorted(select_all(path))
                for path in local_yt.yt_client.search(conf.paths.storage.soup, node_type=["table"], follow_links=True)
            },
            "stream": {
                path.replace(conf.paths.crypta_root, "//crypta_root"): sorted(select_all(path))
                for path in local_yt.yt_client.search(
                    conf.paths.stream.storage, node_type=["table"], follow_links=True
                )
            },
            "processed": sorted(select_all(conf.paths.stream.processed)),
        }

        return output

    return inner_test(local_yt, conf)
