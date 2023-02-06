from __future__ import print_function

import mock
import os
import pwd

from crypta.graph.households.hh_match.lib import PrepareHH, FindMrcc, FinishHH
from crypta.lib.python.yql_runner.tests import load_fixtures, canonize_output, execute, clean_up


def select_all(yt, table_path):
    return list(yt.yt_client.read_table(table_path, format="json"))


@mock.patch.object(PrepareHH, "date", property(lambda self: "2018-10-19"))
@load_fixtures(
    ("//home/crypta/develop/state/graph/v2/matching/vertices_no_multi_profile_by_id_type", "/fixtures/matching.json"),
    ("//home/crypta/develop/state/graph/v2/export/ActiveIdentifiers", "/fixtures/matching.json"),
    ("//home/crypta/develop/state/households_new/storage/2018-08-08", "/fixtures/ip_dt_08.json"),
    ("//home/crypta/develop/state/households_new/storage/2018-09-09", "/fixtures/ip_dt_09.json"),
    ("//home/crypta/develop/state/households_new/storage/2018-10-10", "/fixtures/ip_dt_10.json"),
    ("//home/user_identification/homework/v2/prod/homework_unified_id", "/fixtures/hw.json"),
)
@canonize_output
@clean_up()
def test_run_hh_prepare(local_yt):
    """ Should check is hh prepare correct """
    local_yt.yt_client.create("map_node", "//tmp/{0}".format(pwd.getpwuid(os.getuid())[0]))

    task = PrepareHH()
    execute(task)

    output_tables = (
        "//home/crypta/develop/state/households_new/workdir/all_yuids",
        "//home/crypta/develop/state/households_new/workdir/crypta_id_crypta_id",
        "//home/crypta/develop/state/households_new/workdir/crypta_id_yuid",
        "//home/crypta/develop/state/households_new/workdir/homeless_tvs",
        "//home/crypta/develop/state/households_new/output/crypta_id_ip",
    )
    return {table: sorted(select_all(local_yt, table)) for table in output_tables}


@mock.patch.object(FindMrcc, "date", property(lambda self: "2018-10-19"))
@load_fixtures(("//home/crypta/develop/state/households_new/workdir/crypta_id_crypta_id", "/fixtures/ccid_ccid.json"))
@canonize_output
@clean_up()
def test_run_hh_mrcc(local_yt):
    """ Should check is hh find mrcc correct """
    task = FindMrcc()
    execute(task)
    output_tables = ("//home/crypta/develop/state/households_new/workdir/crypta_id_crypta_id",)
    return {table: sorted(select_all(local_yt, table)) for table in output_tables}


@mock.patch.object(FinishHH, "date", property(lambda self: "2018-10-19"))
@load_fixtures(
    ("//home/crypta/develop/state/households_new/workdir/all_yuids", "/fixtures/all_yuids.json"),
    ("//home/crypta/develop/state/households_new/workdir/crypta_id_crypta_id", "/fixtures/crypta_id_crypta_id.json"),
    ("//home/crypta/develop/state/households_new/workdir/crypta_id_yuid", "/fixtures/crypta_id_yuid.json"),
    ("//home/crypta/develop/state/households_new/workdir/homeless_tvs", "/fixtures/homeless_tvs.json"),
)
@canonize_output
@clean_up()
def test_run_hh_finish(local_yt):
    """ Should check is hh finishing correct """
    task = FinishHH()
    execute(task)

    output_tables = (
        "//home/crypta/develop/state/households_new/output/hh_crypta_id",
        "//home/crypta/develop/state/households_new/output/hh_crypta_id_reversed",
        "//home/crypta/develop/state/households_new/output/hh_enrich",
        "//home/crypta/develop/state/households_new/output/hh_match",
        "//home/crypta/develop/state/households_new/output/hh_reversed",
    )
    return {table: sorted(select_all(local_yt, table)) for table in output_tables}
