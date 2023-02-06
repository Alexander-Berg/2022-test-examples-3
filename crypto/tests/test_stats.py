from __future__ import print_function

import datetime
import mock
import sys
import pytest
import yaml

from yatest.common import work_path
from library.python import resource
from yql_utils import yql_binary_path

from crypta.graph.metrics.stats_base.lib import MetricaRunner, MetricsProcessor, upload_to_solomon
from crypta.lib.python.yql_runner.tests import load_fixtures, canonize_output, clean_up


def get_data():
    return [
        {
            "name": "select_type",
            "disposition": "filesystem",
            "type": "file",
            "content": work_path("bsyeti-configs/select_type_active.json"),
        },
        {
            "name": "ab_experiments_config",
            "disposition": "filesystem",
            "type": "file",
            "content": work_path("bigb_ab_production_config.json"),
        },
    ]


class TestStatsBase(object):

    """Check is correctly work metrics"""

    BASE_PATH = "crypta/graph/metrics/stats_base/lib"

    def test_upload_to_solomon(self):
        data = [
            {
                "fielddate": datetime.date.today().strftime("%Y-%m-%d"),
                "version": "v1",
                "fieldname": "z_old_metric_x_mobile_desktop_percent",
                "kind": "z_old_metric",
                "ratio": 0.1951165318,
                "percentage": 19.5116531817,
            },
            {"fielddate": "2019-03-08"},
        ]

        solomon_conf = yaml.full_load(resource.find("/configs/storage.yaml"))["solomon"]
        with mock.patch("solomon.solomon.ThrottledPushApiReporter._push"):
            upload_to_solomon(solomon_conf, data)

    def test_configs_yql(self):
        """All yql config queries should be available in resources"""
        config = yaml.full_load(resource.find("/configs/metrics.yaml"))["metrics"]
        counter = 0
        for metrica in config:
            if metrica["prefix"] == "exp_stats":
                continue
            assert resource.find(metrica["query"] + ".j2")
            counter += 1
        assert counter == 25

    @mock.patch.dict("os.environ", {"YT_TOKEN": "FAKE", "ENV_TYPE": "FAKE"})
    @clean_up(observed_paths=("//home", "//cooked_logs", "//logs", "//statbox"))
    @load_fixtures(
        (
            "//home/crypta/production/state/graph/v2/matching/vertices_no_multi_profile",
            "/v2_data.json",
            "/v2_data.attrs.json",
        ),
        (
            "//home/crypta/production/state/radius/metrics/2019-02-13/yuid_rlogin/yuid_rlogin_final_stable",
            "/yuid_rlogin.json",
            "/yuid_rlogin.attrs.json",
        ),
        ("//logs/bs-proto-event-log/1d/2019-02-13", "/logs_proto_event.json", "/logs_proto_event.attrs.json"),
        ("//cooked_logs/bs-chevent-cooked-log/1d/2019-02-13", "/logs_chevent.json", "/logs_chevent.attrs.json"),
        (
            "//logs/beh-profile-hit-log/1h/2019-02-13T11:00:00",
            "/beh_profile_hit_log.json",
            "/beh_profile_hit_log.attrs.json",
        ),
        ("//home/taxi-dwh/export/crypta_user_profile/taxi_user_profile/taxi_user_profile", "/taxi.json", "/taxi.attrs.json"),
        ("//home/crypta/production/ids_storage/staff/dump", "/staff.json", "/staff.attrs.json"),
        (
            "//home/crypta/production/ids_storage/yandexuid/yuid_with_all_info",
            "/yuid_with_all_info.json",
            "/yuid_with_all_info.attrs.json",
        ),
        (
            "//home/crypta/production/state/graph/v2/matching/edges_by_crypta_id",
            "/edges_cid.json",
            "/edges_cid.attrs.json",
        ),
        (
            "//home/crypta/production/state/graph/v2exp/matching/edges_by_crypta_id",
            "/edges_cid.json",
            "/edges_cid.attrs.json",
        ),
        (
            "//home/crypta/production/state/graph/v2/matching/vertices_properties_by_crypta_id",
            "/vertices_properties_cid.json",
            "/vertices_properties_cid.attrs.json",
        ),
        (
            "//home/crypta/production/state/graph/v2exp/matching/vertices_properties_by_crypta_id",
            "/vertices_properties_cid.json",
            "/vertices_properties_cid.attrs.json",
        ),
        ("//home/antispam/export/crypta/DailyCalls/2019-02-13", "/antispam.json", "/antispam.attrs.json"),
        ("//logs/bs-watch-log/1d/2019-02-13", "/watch_log.json", "/watch_log.attrs.json"),
        (
            "//home/crypta/production/state/graph/2019-02-13/mobile/dev_info_yt",
            "/dev_info_day.json",
            "/dev_info_day.attrs.json",
        ),
        ("//home/crypta/production/state/graph/shared/merged/shared_tbl", "/shared.json", "/shared.attrs.json"),
        (
            "//home/crypta/production/ids_storage/device_id/app_metrica_month",
            "/device_id_info_month.json",
            "/device_id_info_month.attrs.json",
        ),
        (
            "//home/crypta/production/ids_storage/uuid/app_metrica_month",
            "/uuid_info_month.json",
            "/uuid_info_month.attrs.json",
        ),
        ("//home/crypta/public/indevice/last", "/indevice.json", "/indevice.attrs.json"),
        ("//home/msdata/user-profiles/v1/2019-02-13", "/plus_data.json", "/plus_data.attrs.json"),
        (
            "//home/crypta/production/state/graph/v2/soup/gaid_mm_device_id_app-metrica_mm",
            "/soup/gaid_mm_device_id_app-metrica_mm.json",
            "/soup/gaid_mm_device_id_app-metrica_mm.attrs.json",
        ),
        (
            "//home/crypta/production/state/graph/v2/soup/idfa_mm_device_id_app-metrica_mm",
            "/soup/idfa_mm_device_id_app-metrica_mm.json",
            "/soup/idfa_mm_device_id_app-metrica_mm.attrs.json",
        ),
        (
            "//home/crypta/production/state/graph/v2/soup/mm_device_id_uuid_app-metrica_mm",
            "/soup/mm_device_id_uuid_app-metrica_mm.json",
            "/soup/mm_device_id_uuid_app-metrica_mm.attrs.json",
        ),
        (
            "//home/crypta/production/state/graph/v2/soup/cooked/soup_edges",
            "/soup/soup_edges.json",
            "/soup/soup_edges.attrs.json",
        ),
        (
            "//home/bs/logs/JoinedEFHProfileHitLog/Shows/1h/2019-02-13T13:00:00",
            "/avg_profile_size.json",
            "/avg_profile_size.attrs.json",
        ),
    )
    @canonize_output
    @pytest.mark.parametrize("version,mode", (("v2", "run"), ("v2exp", "validate")))
    def test_run_metrics(self, version, mode, yt):
        """Should check is metrics run correctly"""
        print("Create YQL runner", file=sys.stderr)

        metrics = [
            metric
            for metric in yaml.full_load(resource.find("/configs/metrics.yaml"))["metrics"]
            if metric["prefix"] not in {"avito", "exp_stats", "stable", "retargeting"}
        ]
        for_tests = {item["prefix"] for item in metrics}
        assert for_tests == {
            "radius",
            "ads",
            "taxi",
            "staff",
            "humans",
            "antispam",
            "family",
            "plus",
            "yuids",
            "entropy",
            "device_id",
            "devices",
            "desktop_browser",
            "ads_coverage",
            "id_types",
            "conflict",
        }

        # call day metrics
        yql_task = MetricaRunner(
            metrics=metrics,
            version=version,
            source="//home/crypta/production/state/graph/v2/matching/vertices_no_multi_profile",
            date="2019-02-13",
            yt_proxy="localhost:{}".format(yt.yt_proxy_port),
            pool="xxx",
            mrjob_binary=yql_binary_path("yql/tools/mrjob/mrjob"),
            udf_resolver_binary=yql_binary_path("yql/tools/udf_resolver/udf_resolver"),
            udfs_dir=";".join([yql_binary_path("yql/udfs"), yql_binary_path("ydb/library/yql/udfs")]),
            is_embedded=True,
        )

        try:
            with mock.patch.object(yql_task, "get_libs", return_value=yql_task.get_libs() + get_data()):
                result = True
                if mode == "run":
                    result = yql_task.run()
                elif mode == "validate":
                    yql_task.validate()
                    # Invalid query fail here

                else:
                    assert False, "Unknown mode for {}".format(version)
        except:
            print(yql_task.render_query(), file=sys.stderr)
            raise

        print(dir(result))

        if mode == "run":
            result_metrics = set()
            for line in result[0]["Write"][0]["Data"]:
                assert line[0] == version  # is version
                assert line[1] == "1550059200"  # is timestamp of given date 12:00
                assert line[2] == "2019-02-13"  # is date
                # prefix should be field in name
                assert line[3] in for_tests
                assert line[4].split("_x_")[0] in for_tests
                result_metrics.add(line[3])
                # absolute and denominator should't has value
                assert line[5][0]
                assert line[6][0]

            assert result_metrics == for_tests

        return result

    @pytest.mark.skip(reason="todo (mskorokod): fixme")
    @mock.patch.dict("os.environ", {"YT_TOKEN": "FAKE", "ENV_TYPE": "FAKE"})
    @clean_up(observed_paths=("//home", "//cooked_logs", "//statbox", "//logs"))
    @load_fixtures(
        (
            "//home/crypta/production/state/graph/dicts/matching/exact_vertices_by_key",
            "/v1_data.json",
            "/v1_data.attrs.json",
        ),
        (
            "//home/crypta/production/state/radius/metrics/2019-02-13/yuid_rlogin/yuid_rlogin_final_stable",
            "/yuid_rlogin.json",
            "/yuid_rlogin.attrs.json",
        ),
        (
            "//home/crypta/production/ids_storage/yandexuid/yuid_with_all_info",
            "/yuid_with_all_info.json",
            "/yuid_with_all_info.attrs.json",
        ),
        ("//logs/bs-watch-log/1d/2019-02-13", "/watch_log.json", "/watch_log.attrs.json"),
        (
            "//home/crypta/production/state/graph/2019-02-13/mobile/dev_info_yt",
            "/dev_info_day.json",
            "/dev_info_day.attrs.json",
        ),
        ("//home/crypta/production/state/graph/shared/merged/shared_tbl", "/shared.json", "/shared.attrs.json"),
    )
    @canonize_output
    def test_run_metrics_v1(self, yt):
        """Should check is metrics run correctly for version 1"""
        print("Create YQL runner", file=sys.stderr)
        for_tests = {"radius", "entropy", "device_id"}
        metrics = filter(
            lambda item: item["prefix"] in for_tests, yaml.full_load(resource.find("/configs/metrics.yaml"))["metrics"]
        )
        # call day metrics
        yql_task = MetricaRunner(
            metrics=metrics,
            version="v1",
            source="//home/crypta/production/state/graph/dicts/matching/exact_vertices_by_key",
            date="2019-02-13",
            yt_proxy="localhost:{}".format(yt.yt_proxy_port),
            pool="xxx",
            mrjob_binary=yql_binary_path("yql/tools/mrjob/mrjob"),
            udf_resolver_binary=yql_binary_path("yql/tools/udf_resolver/udf_resolver"),
            udfs_dir=";".join([yql_binary_path("yql/udfs"), yql_binary_path("ydb/library/yql/udfs")]),
            is_embedded=True,
        )

        try:
            result = yql_task.run()
        except:
            print(yql_task.render_query(), file=sys.stderr)
            raise

        result_metrics = set()
        for line in result[0]["Write"][0]["Data"]:
            assert line[0] == "v1"  # is version
            assert line[1] == "1550059200"  # is timestamp of given date 12:00
            assert line[2] == "2019-02-13"  # is date
            # prefix should be field in name
            assert line[3] in for_tests
            assert line[4].split("_x_")[0] in for_tests
            result_metrics.add(line[3])
            # absolute and denominator should't has value
            assert line[5][0]
            assert line[6][0]

        assert result_metrics == for_tests
        return result

    @mock.patch.dict("os.environ", {"YT_TOKEN": "FAKE", "ENV_TYPE": "FAKE"})
    @clean_up(observed_paths=("//cooked_logs", "//logs", "//statbox"))
    @load_fixtures(
        ("//logs/bs-proto-event-log/1d/2019-02-13", "/logs_proto_event.json", "/logs_proto_event.attrs.json"),
        ("//cooked_logs/bs-chevent-cooked-log/1d/2019-02-13", "/logs_chevent.json", "/logs_chevent.attrs.json"),
        (
            "//logs/beh-profile-hit-log/1h/2019-02-13T11:00:00",
            "/beh_profile_hit_log.json",
            "/beh_profile_hit_log.attrs.json",
        ),
        (
            "//home/bs/logs/JoinedEFHProfileHitLog/Shows/1h/2019-02-13T13:00:00",
            "/avg_profile_size.json",
            "/avg_profile_size.attrs.json",
        ),
    )
    @canonize_output
    def test_run_metrics_money(self, yt):
        """Should check is denezjki is well (even if some tables not in YT)"""
        print("Create YQL runner", file=sys.stderr)
        for_tests = {"ads"}
        metrics = filter(
            lambda item: item["prefix"] in for_tests, yaml.full_load(resource.find("/configs/metrics.yaml"))["metrics"]
        )
        # call day metrics

        yql_task = MetricaRunner(
            metrics=metrics,
            version="v2",
            source="//home/crypta/production/state/graph/v2/matching/vertices_no_multi_profile",
            date="2019-02-13",
            yt_proxy="localhost:{}".format(yt.yt_proxy_port),
            pool="xxx",
            mrjob_binary=yql_binary_path("yql/tools/mrjob/mrjob"),
            udf_resolver_binary=yql_binary_path("yql/tools/udf_resolver/udf_resolver"),
            udfs_dir=";".join([yql_binary_path("yql/udfs"), yql_binary_path("ydb/library/yql/udfs")]),
            is_embedded=True,
        )

        try:
            with mock.patch.object(yql_task, "get_libs", return_value=yql_task.get_libs() + get_data()):
                return yql_task.run()
        except:
            print(yql_task.render_query(), file=sys.stderr)
            raise

    @mock.patch.dict("os.environ", {"YT_TOKEN": "FAKE", "ENV_TYPE": "FAKE"})
    @clean_up(observed_paths=("//home", "//cooked_logs", "//logs", "//statbox"))
    @load_fixtures(
        (
            "//home/crypta/production/state/graph/v2/matching/vertices_no_multi_profile",
            "/v2_data.json",
            "/v2_data.attrs.json",
        ),
        (
            "//home/crypta/production/state/radius/metrics/2019-02-13/yuid_rlogin/yuid_rlogin_final_stable",
            "/yuid_rlogin.json",
            "/yuid_rlogin.attrs.json",
        ),
    )
    @canonize_output
    def test_metrics_processor_all(self, yt):
        """Should check is metrics processor run correctly"""
        print("Create YQL runner", file=sys.stderr)
        for_tests = {"radius"}
        metrics = filter(
            lambda item: item["prefix"] in for_tests, yaml.full_load(resource.find("/configs/metrics.yaml"))["metrics"]
        )
        # call day metrics
        source = "//home/crypta/production/state/graph/v2/matching/vertices_no_multi_profile"
        mc_processor = MetricsProcessor("2019-02-13", "v2", metrics, source, None)
        result = list(
            mc_processor.run(
                yt_proxy="localhost:{}".format(yt.yt_proxy_port),
                pool="xxx",
                mrjob_binary=yql_binary_path("yql/tools/mrjob/mrjob"),
                udf_resolver_binary=yql_binary_path("yql/tools/udf_resolver/udf_resolver"),
                udfs_dir=";".join([yql_binary_path("yql/udfs"), yql_binary_path("ydb/library/yql/udfs")]),
                is_embedded=True,
            )
        )
        assert mc_processor.last_exception is None
        assert mc_processor.exception_counter == 0
        return result

    @mock.patch.dict("os.environ", {"YT_TOKEN": "FAKE", "ENV_TYPE": "FAKE"})
    @clean_up(observed_paths=("//home", "//cooked_logs", "//logs", "//statbox"))
    @load_fixtures(
        (
            "//home/crypta/production/state/graph/v2/matching/vertices_no_multi_profile",
            "/v2_data.json",
            "/v2_data.attrs.json",
        )
    )
    @canonize_output
    def test_metrics_run_yql_no_valid_metrics(self, yt):
        """Should check is metrics processor fallback correctly to each queries"""
        print("Create YQL runner", file=sys.stderr)
        for_tests = {"id_types"}
        metrics = filter(
            lambda item: item["prefix"] in for_tests, yaml.full_load(resource.find("/configs/metrics.yaml"))["metrics"]
        )
        # call day metrics
        source = "//home/crypta/production/state/graph/v2/matching/vertices_no_multi_profile"
        mc_processor = MetricsProcessor("2019-02-13", "v2", metrics, source, None)
        result = list(
            mc_processor.run(
                yt_proxy="localhost:{}".format(yt.yt_proxy_port),
                pool="xxx",
                mrjob_binary=yql_binary_path("yql/tools/mrjob/mrjob"),
                udf_resolver_binary=yql_binary_path("yql/tools/udf_resolver/udf_resolver"),
                udfs_dir=";".join([yql_binary_path("yql/udfs"), yql_binary_path("ydb/library/yql/udfs")]),
                is_embedded=True,
            )
        )
        assert mc_processor.last_exception is not None
        assert mc_processor.exception_counter == 1
        return result

    @mock.patch.dict("os.environ", {"YT_TOKEN": "FAKE", "ENV_TYPE": "FAKE"})
    @clean_up(observed_paths=("//home", "//cooked_logs", "//logs", "//statbox"))
    @load_fixtures(
        (
            "//home/crypta/production/state/graph/v2/matching/vertices_no_multi_profile",
            "/v2_data.json",
            "/v2_data.attrs.json",
        ),
        (
            "//home/crypta/production/state/radius/metrics/2019-02-13/yuid_rlogin/yuid_rlogin_final_stable",
            "/yuid_rlogin.json",
            "/yuid_rlogin.attrs.json",
        ),
    )
    @canonize_output
    def test_metrics_processor_each(self, yt):
        """Should check is metrics processor fallback correctly to each queries"""
        print("Create YQL runner", file=sys.stderr)
        for_tests = {"radius", "id_types"}
        metrics = filter(
            lambda item: item["prefix"] in for_tests, yaml.full_load(resource.find("/configs/metrics.yaml"))["metrics"]
        )
        # call day metrics
        source = "//home/crypta/production/state/graph/v2/matching/vertices_no_multi_profile"
        mc_processor = MetricsProcessor("2019-02-13", "v2", metrics, source, None)
        result = list(
            mc_processor.run(
                yt_proxy="localhost:{}".format(yt.yt_proxy_port),
                pool="xxx",
                mrjob_binary=yql_binary_path("yql/tools/mrjob/mrjob"),
                udf_resolver_binary=yql_binary_path("yql/tools/udf_resolver/udf_resolver"),
                udfs_dir=";".join([yql_binary_path("yql/udfs"), yql_binary_path("ydb/library/yql/udfs")]),
                is_embedded=True,
            )
        )
        assert mc_processor.last_exception is not None
        assert mc_processor.exception_counter == 1
        return result

    fixtures_for_yql_len = (
        (
            "//home/crypta/production/state/graph/v2/matching/vertices_no_multi_profile",
            "/v2_data.json",
            "/v2_data.attrs.json",
        ),
        (
            "//home/crypta/production/state/radius/metrics/2019-02-13/yuid_rlogin/yuid_rlogin_final_stable",
            "/yuid_rlogin.json",
            "/yuid_rlogin.attrs.json",
        ),
        ("//logs/bs-proto-event-log/1d/2019-02-13", "/logs_proto_event.json", "/logs_proto_event.attrs.json"),
        ("//cooked_logs/bs-chevent-cooked-log/1d/2019-02-13", "/logs_chevent.json", "/logs_chevent.attrs.json"),
        (
            "//logs/beh-profile-hit-log/1h/2019-02-13T11:00:00",
            "/beh_profile_hit_log.json",
            "/beh_profile_hit_log.attrs.json",
        ),
        ("//home/taxi-dwh/export/crypta_user_profile/taxi_user_profile/taxi_user_profile", "/taxi.json", "/taxi.attrs.json"),
        ("//home/crypta/production/ids_storage/staff/dump", "/staff.json", "/staff.attrs.json"),
        (
            "//home/crypta/production/ids_storage/yandexuid/yuid_with_all_info",
            "/yuid_with_all_info.json",
            "/yuid_with_all_info.attrs.json",
        ),
        (
            "//home/crypta/production/state/graph/v2/matching/edges_by_crypta_id",
            "/edges_cid.json",
            "/edges_cid.attrs.json",
        ),
        (
            "//home/crypta/production/state/graph/v2exp/matching/edges_by_crypta_id",
            "/edges_cid.json",
            "/edges_cid.attrs.json",
        ),
        ("//home/antispam/export/crypta/DailyCalls/2019-02-13", "/antispam.json", "/antispam.attrs.json"),
        ("//logs/bs-watch-log/1d/2019-02-13", "/watch_log.json", "/watch_log.attrs.json"),
        (
            "//home/crypta/production/state/graph/2019-02-13/mobile/dev_info_yt",
            "/dev_info_day.json",
            "/dev_info_day.attrs.json",
        ),
        ("//home/crypta/production/state/graph/shared/merged/shared_tbl", "/shared.json", "/shared.attrs.json"),
        ("//home/crypta/public/indevice/last", "/indevice.json", "/indevice.attrs.json"),
        ("//home/msdata/user-profiles/v1/2019-02-13", "/plus_data.json", "/plus_data.attrs.json"),
    )

    @pytest.mark.skip(reason="no way of currently testing this")
    @mock.patch.dict("os.environ", {"YT_TOKEN": "FAKE", "ENV_TYPE": "FAKE"})
    @clean_up(observed_paths=("//home", "//cooked_logs", "//logs", "//statbox"))
    @load_fixtures(*fixtures_for_yql_len)
    def test_super_long_query(self, yt):
        # run fail with crush
        metrics = [
            metric
            for metric in yaml.full_load(resource.find("/configs/metrics.yaml"))["metrics"]
            if metric["prefix"] not in {"exp_stats", "stable"}
        ]
        task = MetricaRunner(
            metrics=metrics,
            version="v2",
            source="//home/crypta/production/state/graph/v2/matching/vertices_no_multi_profile",
            date="2019-02-13",
            yt_proxy="localhost:{}".format(yt.yt_proxy_port),
            pool="xxx",
            mrjob_binary=yql_binary_path("yql/tools/mrjob/mrjob"),
            udf_resolver_binary=yql_binary_path("yql/tools/udf_resolver/udf_resolver"),
            udfs_dir=";".join([yql_binary_path("yql/udfs"), yql_binary_path("ydb/library/yql/udfs")]),
            is_embedded=True,
        )
        task.IS_SINGLE = True

        print(task.render_query(), file=sys.stderr)

        with mock.patch.object(task, "get_libs", return_value=task.get_libs() + get_data()):
            try:
                assert task.run()
            except:
                print(task.render_query(), file=sys.stderr)
                raise

    @pytest.mark.skip(reason="no run yql debug test")
    @mock.patch.dict("os.environ", {"YT_TOKEN": "FAKE", "ENV_TYPE": "FAKE"})
    @pytest.mark.parametrize("skip_mmetric", ("ads_coverage", "staff", "desktop_browser", "id_types"))
    @clean_up(observed_paths=("//home", "//cooked_logs", "//logs", "//statbox"))
    @load_fixtures(*fixtures_for_yql_len)
    def test_not_so_long_query(self, yt, skip_mmetric):
        # run ok without crush
        metrics = [
            metric
            for metric in yaml.full_load(resource.find("/configs/metrics.yaml"))["metrics"]
            if metric["prefix"] not in {"exp_stats", "stable", skip_mmetric}
        ]
        task = MetricaRunner(
            metrics=metrics,
            version="v2",
            source="//home/crypta/production/state/graph/v2/matching/vertices_no_multi_profile",
            date="2019-02-13",
            yt_proxy="localhost:{}".format(yt.yt_proxy_port),
            pool="xxx",
            mrjob_binary=yql_binary_path("yql/tools/mrjob/mrjob"),
            udf_resolver_binary=yql_binary_path("yql/tools/udf_resolver/udf_resolver"),
            udfs_dir=";".join([yql_binary_path("yql/udfs"), yql_binary_path("ydb/library/yql/udfs")]),
            is_embedded=True,
        )
        task.IS_SINGLE = True

        with mock.patch.object(task, "get_libs", return_value=task.get_libs() + get_data()):
            try:
                assert task.run()
            except:
                print(task.render_query(), file=sys.stderr)
                raise

    @pytest.mark.skip(reason="no run yql debug test")
    @mock.patch.dict("os.environ", {"YT_TOKEN": "FAKE", "ENV_TYPE": "FAKE"})
    @clean_up(observed_paths=("//home", "//cooked_logs", "//logs", "//statbox"))
    @load_fixtures(*fixtures_for_yql_len)
    def test_partial_long_query(self, yt):
        # run fail with crush
        metrics = [
            metric
            for metric in yaml.full_load(resource.find("/configs/metrics.yaml"))["metrics"]
            if metric["prefix"] not in {"exp_stats", "stable"}
        ]
        task = MetricaRunner(
            metrics=metrics,
            version="v2",
            source="//home/crypta/production/state/graph/v2/matching/vertices_no_multi_profile",
            date="2019-02-13",
            yt_proxy="localhost:{}".format(yt.yt_proxy_port),
            pool="xxx",
            mrjob_binary=yql_binary_path("yql/tools/mrjob/mrjob"),
            udf_resolver_binary=yql_binary_path("yql/tools/udf_resolver/udf_resolver"),
            udfs_dir=";".join([yql_binary_path("yql/udfs"), yql_binary_path("ydb/library/yql/udfs")]),
            is_embedded=True,
        )
        libs = task.get_libs()

        with mock.patch.object(task, "get_libs", return_value=libs + get_data()):
            try:
                assert task.run()
            except:
                print(libs, file=sys.stderr)
                print(task.render_query(), file=sys.stderr)
                raise
