import unittest
from collections import defaultdict

from crypta.graph.v1.python.matching.human_matching.stats import graph_quality_metrics
from crypta.graph.v1.python.rtcconf import config


class TestQualityMetrics(unittest.TestCase):
    def __init__(self, methodName="runTest"):
        self.maxDiff = None
        self.vertices_recs = [
            {"key": "0000", "id_type": "deviceid", "id_values": {}, "ua_profile": "m|phone|samsung|android|4"},
            {"key": "1111", "id_type": "deviceid", "id_values": {}},
            {
                "key": "2222",
                "id_type": "yuid-mobile",
                "id_values": {config.ID_TYPE_EMAIL: ["xxx"]},
                "ua_profile": "m|phone|samsung|android|4",
            },
            {
                "key": "3333",
                "id_type": "yuid-mobile",
                "id_values": {config.ID_TYPE_EMAIL: ["xxx"]},
                "ua_profile": "m|tablet|asus|android|4.1.2",
            },
            {
                "key": "4444",
                "id_type": "yuid-mobile",
                "id_values": {config.ID_TYPE_EMAIL: ["yyy"]},
                "ua_profile": "m|phone|un|android|4.4.2",
                "sex": "0.18559,0.814409",
                "region": 213,
            },
            {
                "key": "5555",
                "id_type": "yuid-desktop",
                "id_values": {config.ID_TYPE_FUID: ["123"]},
                "browser": "yandexbrowser|16.3.0.7843",
                "sex": "0.564762,0.435237",
                "region": 111,
            },
            {
                "key": "6666",
                "id_type": "yuid-desktop",
                "id_values": {config.ID_TYPE_FUID: ["123"]},
                "browser": "yandexbrowser|16.3.0.7844",
                "sex": "0.13847,0.861529",
            },
        ]

        super(TestQualityMetrics, self).__init__(methodName)

    def test_crypta_id_stats(self):
        q = graph_quality_metrics.crypta_id_stats(self.vertices_recs)

        self.assertEqual(
            q,
            {
                "browsers": ["yandexbrowser|16.3.0.7843", "yandexbrowser|16.3.0.7844"],
                "browsers_count": 2,
                "crypta_id_size": 7,
                "devid_device_type_counts": {"phone": 1, "unknown": 1},
                "devids": ["1111", "0000"],
                "devids_count": 2,
                "genders": {"F": 2, "N": 1, "U": 2},
                "id_values": {"email": ["xxx", "yyy"], "fuid": ["123"]},
                "mobile_only": "mobile",
                "mobile_ua_profiles": ["m|phone|samsung|android|4"],
                "mobile_ua_profiles_count": 1,
                "regions": [213, 111],
                "regions_count": 2,
                "unknown_devid_ua": [],
                "unknown_devid_ua_count": 0,
                "yuid_device_type_counts": {"phone": 2, "tablet": 1, "unknown": 2},
                "yuid_mobile_desktop_counts": {"m": 3},
                "yuid_ua_profiles": [
                    "m|phone|samsung|android|4",
                    "m|tablet|asus|android|4.1.2",
                    "m|phone|un|android|4.4.2",
                ],
                "yuid_ua_profiles_count": 3,
                "yuids": ["3333", "4444", "6666", "2222", "5555"],
                "yuids_count": 5,
            },
        )

    def make_stats_rec(self, known_params):
        default_values = {
            "browsers": [],
            "browsers_count": 0,
            "crypta_id_size": 0,
            "devid_device_type_counts": dict(),
            "devids": [],
            "devids_count": 0,
            "regions": [],
            "regions_count": 0,
            "genders": dict(),
            "id_values": dict(),
            "mobile_ua_profiles": [],
            "mobile_ua_profiles_count": 0,
            "unknown_devid_ua": [],
            "unknown_devid_ua_count": 0,
            "yuid_device_type_counts": [],
            "yuid_mobile_desktop_counts": [],
            "yuid_ua_profiles": [],
            "yuid_ua_profiles_count": 0,
            "yuids": [],
            "yuids_count": 0,
        }

        default_values.update(known_params)
        return default_values

    def test_crypta_id_quality_metrics_penalties(self):
        too_many_browsers = {"browsers": ["a", "b", "c", "d", "e", "f"], "browsers_count": 6}
        q = graph_quality_metrics.crypta_id_quality_metrics(self.make_stats_rec(too_many_browsers))
        self.assertEqual(q["browsers_penalty"], 0.33333333333333337)

        ok_browsers = {"browsers": ["a", "b", "c", "d"], "browsers_count": 4}
        q = graph_quality_metrics.crypta_id_quality_metrics(self.make_stats_rec(ok_browsers))
        self.assertEqual(q["browsers_penalty"], 0)

        too_many_devices = {"devids": ["a", "b", "c", "d"], "devids_count": 4}
        q = graph_quality_metrics.crypta_id_quality_metrics(self.make_stats_rec(too_many_devices))
        self.assertEqual(q["devices_penalty"], 0.25)

        single_gender1 = {"genders": {"M": 3, "N": 1}}
        single_gender2 = {"genders": {"F": 1, "U": 1}}
        q1 = graph_quality_metrics.crypta_id_quality_metrics(self.make_stats_rec(single_gender1))
        q2 = graph_quality_metrics.crypta_id_quality_metrics(self.make_stats_rec(single_gender2))
        self.assertEqual(q1["sex_penalty"], 0)
        self.assertEqual(q2["sex_penalty"], 0)

        multi_gender1 = {"genders": {"M": 3, "F": 1}}
        multi_gender2 = {"genders": {"M": 1, "F": 1, "N": 1}}
        q1 = graph_quality_metrics.crypta_id_quality_metrics(self.make_stats_rec(multi_gender1))
        q2 = graph_quality_metrics.crypta_id_quality_metrics(self.make_stats_rec(multi_gender2))
        self.assertEqual(q1["sex_penalty"], 0.5)
        self.assertEqual(q2["sex_penalty"], 1)

        to_many_values1 = {"id_values": {config.ID_TYPE_LOGIN: ["a", "b", "c", "d", "e", "f"]}}
        to_many_values2 = {"id_values": {config.ID_TYPE_LOGIN: ["a", "b", "c", "d", "e", "f", "g"]}}
        q1 = graph_quality_metrics.crypta_id_quality_metrics(self.make_stats_rec(to_many_values1))
        q2 = graph_quality_metrics.crypta_id_quality_metrics(self.make_stats_rec(to_many_values2))
        self.assertEqual(q1["too_many_values_penalty"], 0.5)
        self.assertEqual(q2["too_many_values_penalty"], 0.5714285714285714)

    def test_clustering_value_split_metrics(self):
        values_split_case = {
            1: [
                {"key": "2222", "id_type": "yuid-mobile", "id_values": {config.ID_TYPE_EMAIL: ["xxx"]}},
                {"key": "3333", "id_type": "yuid-mobile", "id_values": {config.ID_TYPE_EMAIL: ["yyy"]}},
                {"key": "4444", "id_type": "yuid-mobile", "id_values": {config.ID_TYPE_FUID: ["123"]}},
            ],
            2: [
                {"key": "5555", "id_type": "yuid-desktop", "id_values": {config.ID_TYPE_EMAIL: ["xxx"]}},
                {"key": "6666", "id_type": "yuid-desktop", "id_values": {config.ID_TYPE_FUID: ["123"]}},
            ],
        }
        values_not_split_case = {
            1: [
                {"key": "2222", "id_type": "yuid-mobile", "id_values": {config.ID_TYPE_EMAIL: ["xxx"]}},
                {"key": "3333", "id_type": "yuid-mobile", "id_values": {config.ID_TYPE_EMAIL: ["xxx"]}},
                {"key": "4444", "id_type": "yuid-mobile", "id_values": {config.ID_TYPE_FUID: ["xxx"]}},
            ],
            2: [
                {"key": "5555", "id_type": "yuid-desktop", "id_values": {config.ID_TYPE_EMAIL: ["yyy"]}},
                {"key": "6666", "id_type": "yuid-desktop", "id_values": {config.ID_TYPE_FUID: ["123"]}},
            ],
        }

        q = graph_quality_metrics.clustering_quality_metrics(values_split_case)
        self.assertEqual(q["split_id_values_penalty"], 0.6666666666666666)
        self.assertEqual(q["split_values"], {"123": 1, "xxx": 1})
        q = graph_quality_metrics.clustering_quality_metrics(values_not_split_case)
        self.assertEqual(q["split_id_values_penalty"], 0)
        self.assertEqual(q["split_values"], dict())

    def test_clustering_gender_split_metrics(self):
        genders_clustered_good = {
            1: [
                {"key": "2222", "id_type": "yuid-mobile", "sex": "0.16,0.84"},
                {"key": "3333", "id_type": "yuid-mobile", "sex": "0.17,0.83"},
                {"key": "4444", "id_type": "yuid-mobile", "sex": "0.18,0.82"},
            ],
            2: [
                {"key": "5555", "id_type": "yuid-desktop", "sex": "0.8,0.2"},
                {"key": "6666", "id_type": "yuid-desktop", "sex": "0.7,0.3"},
            ],
        }
        genders_clustered_bad = {
            1: [
                {"key": "2222", "id_type": "yuid-mobile", "sex": "0.16,0.84"},
                {"key": "3333", "id_type": "yuid-mobile", "sex": "0.17,0.83"},
                {"key": "4444", "id_type": "yuid-mobile", "sex": "0.7,0.3"},
            ],
            2: [
                {"key": "5555", "id_type": "yuid-desktop", "sex": "0.8,0.2"},
                {"key": "6666", "id_type": "yuid-desktop", "sex": "0.18,0.82"},
            ],
        }

        q = graph_quality_metrics.clustering_quality_metrics(genders_clustered_good)
        self.assertEqual(q["sex_penalty"], 0)
        q = graph_quality_metrics.clustering_quality_metrics(genders_clustered_bad)
        self.assertEqual(q["sex_penalty"], 0.8333333333333334)

    def test_clustering_mobile_desk_split_metrics(self):
        cant_split_mobile_from_desktop_anyway = {
            1: [{"key": "2222", "id_type": "yuid-mobile", "ua_profile": "m|phone"}],
            2: [{"key": "3333", "id_type": "yuid-mobile", "ua_profile": "m|tablet"}],
        }
        split_desktop_from_mobile = {
            1: [
                {"key": "2222", "id_type": "yuid-mobile", "ua_profile": "m|phone"},
                {"key": "2222", "id_type": "yuid-desktop", "ua_profile": "d|desk"},
            ],
            2: [{"key": "3333", "id_type": "yuid-mobile", "ua_profile": "m|tablet"}],
        }
        not_split_desktop_from_mobile = {
            1: [
                {"key": "2222", "id_type": "yuid-mobile", "ua_profile": "m|phone"},
                {"key": "3333", "id_type": "yuid-desktop", "ua_profile": "d|desk"},
            ],
            2: [
                {"key": "4444", "id_type": "yuid-mobile", "ua_profile": "m|tablet"},
                {"key": "5555", "id_type": "yuid-mobile", "ua_profile": "d|desk"},
            ],
        }

        q = graph_quality_metrics.clustering_quality_metrics(cant_split_mobile_from_desktop_anyway)
        self.assertEqual(q["mobile_only_penalty"], 0)
        q = graph_quality_metrics.clustering_quality_metrics(split_desktop_from_mobile)
        self.assertEqual(q["mobile_only_penalty"], 0.5)
        q = graph_quality_metrics.clustering_quality_metrics(not_split_desktop_from_mobile)
        self.assertEqual(q["mobile_only_penalty"], 0)

    def test_clustering_browser_and_device_limits(self):
        too_many_browsers = defaultdict(list)
        ok_browsers = defaultdict(list)
        too_many_devices = defaultdict(list)
        ok_devices = defaultdict(list)
        for component in range(3):
            for yuid in range(6):
                too_many_browsers[component].append(
                    {"key": str(yuid), "id_type": "yuid-mobile", "browser": "browser" + str(yuid)}
                )
                ok_browsers[component].append(
                    {"key": str(yuid), "id_type": "yuid-mobile", "browser": "browser" + str(component)}
                )
                too_many_devices[component].append(
                    {"key": str(yuid), "id_type": "deviceid", "browser": "browser" + str(yuid)}
                )
            ok_devices[component].append({"key": str(component), "id_type": "deviceid"})

        q = graph_quality_metrics.clustering_quality_metrics(too_many_browsers)
        self.assertEqual(q["browsers_penalty"], 0.3333333333333333)
        q = graph_quality_metrics.clustering_quality_metrics(ok_browsers)
        self.assertEqual(q["browsers_penalty"], 0)

        q = graph_quality_metrics.clustering_quality_metrics(too_many_devices)
        self.assertEqual(q["devices_penalty"], 0.5)
        q = graph_quality_metrics.clustering_quality_metrics(ok_devices)
        self.assertEqual(q["devices_penalty"], 0)

    def test_convert_pairs_to_vertices_components(self):
        from crypta.graph.v1.python.matching.human_matching.graph_clustering import (
            Edge,
            Component,
            edges_to_vertices_format,
        )

        pairs = [
            Edge(
                {
                    "id1": "111",
                    "id2": "222",
                    "pair_type": "d_y",
                    "pair_source": "s1",
                    "id1_ua": "p1",
                    "id2_ua": "p_will_be_overriden",
                    "id1_browser": "b1",
                    "id2_browser": "b_will_be_overriden",
                    "id1_sex": "0.16,0.84",
                    "id2_sex": "0.18,0.82",
                    "id1_region": 213,
                }
            ),
            Edge(
                {
                    "id1": "222",
                    "id2": "333",
                    "pair_type": "y_y",
                    "pair_source": "s1",
                    "id1_ua": "p11",
                    "id2_ua": "p22",
                    "id1_browser": "b11",
                    "id2_browser": "b22",
                    "id1_sex": "0.16,0.84",
                    "id2_sex": "0.18,0.82",
                }
            ),
        ]

        v2c = edges_to_vertices_format([Component(pairs, 3, "1")])

        self.assertEqual(
            v2c["1"],
            [
                {
                    "browser": "b1",
                    "id_type": "deviceid",
                    "id_values": {},
                    "key": "111",
                    "region": 213,
                    "sex": "0.16,0.84",
                    "ua_profile": "p1",
                },
                {
                    "browser": "b22",
                    "id_type": "yuid",
                    "id_values": {},
                    "key": "333",
                    "region": None,
                    "sex": "0.18,0.82",
                    "ua_profile": "p22",
                },
                {
                    "browser": "b11",
                    "id_type": "yuid",
                    "id_values": {},
                    "key": "222",
                    "region": None,
                    "sex": "0.16,0.84",
                    "ua_profile": "p11",
                },
            ],
        )
