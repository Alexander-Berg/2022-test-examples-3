from __future__ import print_function

import mock
import sys
from textwrap import dedent

import yql.library.embedded.python.run as embedded
from yql_utils import yql_binary_path
from crypta.graph.data_import.fuzzy2.lib import Fuzzy2Parser
from crypta.lib.python.yql_runner.tests import load_fixtures, canonize_output


@mock.patch.dict("os.environ", {"YT_TOKEN": "FAKE", "ENV_TYPE": "FAKE"})
@load_fixtures(
    ("//home/crypta/fake/state/graph/indevice/2018-04-10/fuzzy/dev_yuid_fuzzy2", "/fixtures/dev_yuid_fuzzy2.json", None),
    (
        "//home/crypta/fake/state/graph/indevice/2018-04-11/fuzzy/fuzzy2_metrica",
        "/fixtures/fuzzy2_metrica.json",
        "/fixtures/fuzzy2_metrica.spec.json",
    ),
    (
        "//home/crypta/fake/ids_storage/yandexuid/yuid_with_all_info_no_socdem",
        "/fixtures/yuid_with_all.json",
        "/fixtures/yuid_with_all.spec.json",
    ),
    ("//home/logfeller/logs/bs-watch-log/1d/2018-04-11", "/fixtures/bswatch_log.json", None),
)
@canonize_output
def test_fuzzy2(yt):
    """ Should check is fp parser correct """
    for is_day_task in (True, False):
        # call day task parser
        yql_task = Fuzzy2Parser(
            date_start="2018-04-05",
            date_end="2018-04-11",
            is_day_task=is_day_task,
            yt_proxy="localhost:{}".format(yt.yt_proxy_port),
            pool="fake",
            mrjob_binary=yql_binary_path("yql/tools/mrjob/mrjob"),
            udf_resolver_binary=yql_binary_path("yql/tools/udf_resolver/udf_resolver"),
            udfs_dir=";".join([yql_binary_path("yql/udfs"), yql_binary_path("ydb/library/yql/udfs")]),
            is_embedded=True,
        )

        try:
            yql_task.run()
        except:
            print(yql_task.render_query(), file=sys.stderr)
            raise

    def select_all(table):
        return list(yt.yt_client.read_table(table, format="json"))

    output_tables = (
        "//home/crypta/fake/state/graph/indevice/2018-04-11/fuzzy/fuzzy2_bswatch",
        "//home/crypta/fake/state/graph/indevice/2018-04-11/fuzzy/dev_yuid_fuzzy2",
        "//home/crypta/fake/state/graph/indevice/2018-04-11/fuzzy/dev_yuid_unperfect",
        "//home/crypta/fake/state/graph/v2/soup/dumps/idfa_yandexuid_fuzzy2_fuzzy2-indevice",
        "//home/crypta/fake/state/graph/v2/soup/dumps/gaid_yandexuid_fuzzy2_fuzzy2-indevice",
    )
    return {table: sorted(select_all(table)) for table in output_tables}


@mock.patch.dict("os.environ", {"YT_TOKEN": "FAKE", "ENV_TYPE": "FAKE"})
@load_fixtures(
    ("//home/id_storage/device_id", "/fixtures/ua_profiles_devid.json", None),
    ("//home/id_storage/yandexuid", "/fixtures/ua_profiles_yuid.json", None),
)
@canonize_output
def test_ua_profile(yt):
    yt_clusters = [{"name": "ytcluster", "cluster": "localhost:%d" % yt.yt_proxy_port}]
    factory = embedded.OperationFactory(
        yt_clusters=yt_clusters,
        mrjob_binary=yql_binary_path("yql/tools/mrjob/mrjob"),
        udf_resolver_binary=yql_binary_path("yql/tools/udf_resolver/udf_resolver"),
        udfs_dir=";".join([yql_binary_path("yql/udfs"), yql_binary_path("ydb/library/yql/udfs")]),
        user_data=[
            {
                "name": "ut_utils_lib.sql",
                "content": "/lib/ut_utils_lib.sql",
                "disposition": "resource",
                "type": "library",
            }
        ],
    )
    return factory.run(
        dedent(
            """\
        USE YtCluster;
        IMPORT .lib.ut_utils_lib SYMBOLS $encode_startup, $ua_profile_str;

        $validate = ($encoded, $original) -> {
            RETURN CASE
                WHEN $original LIKE '%|un|%' THEN $encoded  -- may be not the same
                ELSE Ensure($encoded, $encoded == $original, "Should be equal to original ua profile!")
            END;
        };

        SELECT
            Unwrap($validate(ua_profile_encoded, ua_profile)) AS ua_profile_encoded,
            Unwrap(ua_profile) AS ua_profile
        FROM (
            SELECT $encode_startup(os, os_version, manufacturer, device_type, model) AS ua_profile_encoded, ua_profile
            FROM `//home/id_storage/device_id`

            UNION ALL

            SELECT $ua_profile_str(UserAgent::Parse(ua)) AS ua_profile_encoded, ua_profile
            FROM `//home/id_storage/yandexuid`
        ) ORDER BY ua_profile_encoded, ua_profile;
    """
        ),
        syntax_version=1,
    ).yson_result()
