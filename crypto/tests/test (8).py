from __future__ import print_function

import mock
import sys

from yql_utils import yql_binary_path

from crypta.graph.data_import.metrika_user_params.lib import MetrikaUserParamsParser, MobmetPreprocessing
from crypta.lib.python.yql_runner.tests import load_fixtures, canonize_output


SOURCE_METRIKA_DIR = "//home/metrika/userparams"

TEST_DATE = "2020-11-16"


def get_input_params_path(table_name):
    return "{}/{}".format(SOURCE_METRIKA_DIR, table_name)


@mock.patch.dict("os.environ", {"YT_TOKEN": "FAKE", "ENV_TYPE": "FAKE"})
@load_fixtures(
    (get_input_params_path("params_01"), "/fixtures/params_01.json", "/fixtures/params.spec.json"),
    (get_input_params_path("params_02"), "/fixtures/params_02.json", "/fixtures/params.spec.json"),
)
@canonize_output
def test_metrika_user_params(yt):
    output = "//crypta/production/state/graph/v2/soup/active_dumps/{date}/metrika-userparams".format(date=TEST_DATE)

    yql_task = MetrikaUserParamsParser(
        date=TEST_DATE,
        yt_proxy="localhost:{}".format(yt.yt_proxy_port),
        pool="crypta_fake",
        mrjob_binary=yql_binary_path("yql/tools/mrjob/mrjob"),
        udf_resolver_binary=yql_binary_path("yql/tools/udf_resolver/udf_resolver"),
        udfs_dir=";".join([yql_binary_path("yql/udfs"), yql_binary_path("ydb/library/yql/udfs")]),
        is_embedded=True,
        metrika_dir=SOURCE_METRIKA_DIR,
        output=output,
    )

    try:
        yql_task.run()
    except:
        print(yql_task.render_query(), file=sys.stderr)
        raise

    return list(yt.yt_client.read_table(output, format="json"))


@mock.patch.dict("os.environ", {"YT_TOKEN": "FAKE", "ENV_TYPE": "FAKE"})
@load_fixtures(
    (
        get_input_params_path("param_owners_01"),
        "/fixtures/param_owners_01.json",
        "/fixtures/param_owners_01.spec.json"
    ),
    (
        "//home/metrica-analytics/firstsvet/MOBMET/applications_and_sites/app_id_and_domains",
        "/fixtures/mobmet_app_id_and_domains.json",
        "/fixtures/mobmet_app_id_and_domains.spec.json"
    ),
    (
        "//home/metrica-analytics/firstsvet/MOBMET/applications_and_sites/counters_and_domains",
        "/fixtures/mobmet_counters_and_domains.json",
        "/fixtures/mobmet_counters_and_domains.spec.json"
    ),
)
@canonize_output
def test_mobmet_preprocessing(yt):
    output_cross_mobmet = "//crypta/production/state/graph/v2/soup/preprocess/cross_mobmet"
    output_params_owners = "//crypta/production/state/graph/v2/soup/preprocess/metrika_params_owners"

    yql_task = MobmetPreprocessing(
        date=TEST_DATE,
        yt_proxy="localhost:{}".format(yt.yt_proxy_port),
        pool="crypta_fake",
        mrjob_binary=yql_binary_path("yql/tools/mrjob/mrjob"),
        udf_resolver_binary=yql_binary_path("yql/tools/udf_resolver/udf_resolver"),
        udfs_dir=";".join([yql_binary_path("yql/udfs"), yql_binary_path("ydb/library/yql/udfs")]),
        is_embedded=True,
        metrika_dir=SOURCE_METRIKA_DIR,
        mobmet_app_id_and_domains="//home/metrica-analytics/firstsvet/MOBMET/applications_and_sites/app_id_and_domains",
        mobmet_counters_and_domains="//home/metrica-analytics/firstsvet/MOBMET/applications_and_sites/counters_and_domains",
        output_cross_mobmet=output_cross_mobmet,
        output_params_owners=output_params_owners,
    )

    try:
        yql_task.run()
    except:
        print(yql_task.render_query(), file=sys.stderr)
        raise

    def select_all(table):
        return list(yt.yt_client.read_table(table, format="json"))

    output_tables = (output_cross_mobmet, output_params_owners)

    return {table: sorted(select_all(table)) for table in output_tables}
