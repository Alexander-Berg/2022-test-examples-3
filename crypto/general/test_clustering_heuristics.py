import unittest

from matching.human_matching.graph_clustering import weight_single_source_y_y_edge_rec, \
    weight_single_source_d_y_edge_rec, \
    weight_multi_edge_old, weight_multi_edge_new, ClusteringConfig, Edge
from rtcconf import config


class TestClusteringHeuristics(unittest.TestCase):
    def test_base_weight(self):
        weight_y_y = weight_single_source_y_y_edge_rec({'pair_type': 'y_y', 'id_type': config.ID_TYPE_FUID},
                                                       ClusteringConfig(use_sex=False,
                                                                    use_region=False,
                                                                    use_values=False))

        self.assertEquals(weight_y_y, 1.0)

        weight_y_y = weight_single_source_y_y_edge_rec({'pair_type': 'y_y', 'id_type': config.ID_TYPE_FUID},
                                                       ClusteringConfig(use_sex=True,
                                                                    use_region=True,
                                                                    use_values=True))

        self.assertEquals(weight_y_y, 1.0)

        weight_d_y = weight_single_source_d_y_edge_rec({'pair_type': 'd_y', 'source_type': config.ID_SOURCE_TYPE_YABROWSER_IOS},
                                                    )

        self.assertEquals(weight_d_y, 1.0)

    def test_heuristic_weight(self):
        clustering_config = ClusteringConfig(use_sex=True,
                                             use_region=True,
                                             use_values=True)
        pair_with_bad_region = {'pair_type': 'y_y', 'id_type': config.ID_TYPE_FUID,
                    'id1_region': 123, 'id2_region': 234}
        weight1 = weight_single_source_y_y_edge_rec(pair_with_bad_region, clustering_config)
        self.assertEquals(weight1, 0.25)

        pair_with_bad_sex_lol = {'pair_type': 'y_y', 'id_type': config.ID_TYPE_FUID,
                                'id1_sex': '0.780,0.220', 'id2_sex': '0.418,0.582'}
        weight2 = weight_single_source_y_y_edge_rec(pair_with_bad_sex_lol, clustering_config)
        self.assertEquals(weight2, 0.6379999999999999)

        id1_values = {'a': {'1': 2, '2': 2}, 'b': {'1': 2}}
        id2_values = {'a': {'3': 2, '4': 2}, 'b': {'1': 2}}
        pair_with_bad_values = {'pair_type': 'y_y', 'id_type': config.ID_TYPE_FUID,
                                'id_value': 'b',
                                'id1_dates': id1_values, 'id2_dates': id2_values}
        weight3 = weight_single_source_y_y_edge_rec(pair_with_bad_values, clustering_config)
        self.assertEquals(weight3, 0.25)

        pair_with_bad_all = {'pair_type': 'y_y', 'id_type': config.ID_TYPE_FUID,
                                'id1_region': 123, 'id2_region': 234,
                                'id1_sex': '0.780,0.220', 'id2_sex': '0.418,0.582',
                                'id_value': 'b',
                                'id1_dates': id1_values, 'id2_dates': id2_values}
        weight4 = weight_single_source_y_y_edge_rec(pair_with_bad_all, clustering_config)
        self.assertEquals(weight4, 0.039874999999999994)

    recs = [
        {"match_type": "indev", "old_crypta_id": "7442694655393601941",
         "pair": "D6E92940-CD75-4423-9B71-C652A82D9243_7442694651393601941", "devid_yuid_dates": {"2016-09-10": 1},
         "id1": "D6E92940-CD75-4423-9B71-C652A82D9243", "id1_ua": "m|phone|apple|ios|10.0.1", "id1_browser": "",
         "id2_dates": None, "pair_type": "d_y", "id2": "7442694651393601941", "crypta_id_size": 14,
         "yuid_sources": ["oauth", "access_log", "startup", "sdk"], "id1_sex": None, "id1_dates": None,
         "key": "D6E92940-CD75-4423-9B71-C652A82D9243", "weight": 3, "source_type": "sdk",
         "id2_sex": "0.711154,0.288845", "match_chain": None, "id1_region": None,
         "id2_browser": "mobilesafari|10.0", "id2_ua": "m|phone|apple|ios|10.0.1", "component": "0",
         "crypta_id": "7442694655393601941", "id2_region": 1, "components_count": 1, "pair_source": "sdk_indev"},
        {"match_type": "indev", "old_crypta_id": "7442694655393601941",
         "pair": "D6E92940-CD75-4423-9B71-C652A82D9243_7442694651393601941", "devid_yuid_dates": {"2016-09-10": 1},
         "id1": "D6E92940-CD75-4423-9B71-C652A82D9243", "id1_ua": "m|phone|apple|ios|10.0.1", "id1_browser": "",
         "id2_dates": None, "pair_type": "d_y", "id2": "7442694651393601941", "crypta_id_size": 14,
         "yuid_sources": ["oauth", "access_log", "startup", "sdk"], "id1_sex": None, "id1_dates": None,
         "key": "D6E92940-CD75-4423-9B71-C652A82D9243", "weight": 3, "source_type": "access_log",
         "id2_sex": "0.711154,0.288845", "match_chain": None, "id1_region": None,
         "id2_browser": "mobilesafari|10.0", "id2_ua": "m|phone|apple|ios|10.0.1", "component": "0",
         "crypta_id": "7442694655393601941", "id2_region": 1, "components_count": 1,
         "pair_source": "access_log_indev"},
        {"match_type": "indev", "old_crypta_id": "7442694655393601941",
         "pair": "D6E92940-CD75-4423-9B71-C652A82D9243_7442694651393601941",
         "devid_yuid_dates": {"2016-09-09": 276}, "id1": "D6E92940-CD75-4423-9B71-C652A82D9243",
         "id1_ua": "m|phone|apple|ios|10.0.1", "id1_browser": "", "id2_dates": None, "pair_type": "d_y",
         "id2": "7442694651393601941", "crypta_id_size": 14,
         "yuid_sources": ["oauth", "access_log", "startup", "sdk"], "id1_sex": None, "id1_dates": None,
         "key": "D6E92940-CD75-4423-9B71-C652A82D9243", "weight": 1, "source_type": "oauth",
         "id2_sex": "0.711154,0.288845", "match_chain": {"login": {"fp": {
            "ilikeqt": {"2016-10-01": 10, "2016-09-12": 7, "2016-09-09": 10, "2016-09-05": 95, "2016-09-29": 7,
                        "2016-09-13": 5, "2016-09-08": 3, "2016-10-02": 10, "2016-09-11": 11, "2016-09-30": 14,
                        "2016-09-06": 20}}}, "puid": {"passport": {"97821852": {}}}}, "id1_region": None,
         "id2_browser": "mobilesafari|10.0", "id2_ua": "m|phone|apple|ios|10.0.1", "component": "0",
         "crypta_id": "7442694655393601941", "id2_region": 1, "components_count": 1, "pair_source": "oauth_indev"},
        {"match_type": "indev", "old_crypta_id": "7442694655393601941",
         "pair": "D6E92940-CD75-4423-9B71-C652A82D9243_7442694651393601941", "devid_yuid_dates": {"2016-09-10": 1},
         "id1": "D6E92940-CD75-4423-9B71-C652A82D9243", "id1_ua": "m|phone|apple|ios|10.0.1", "id1_browser": "",
         "id2_dates": None, "pair_type": "d_y", "id2": "7442694651393601941", "crypta_id_size": 14,
         "yuid_sources": ["oauth", "access_log", "startup", "sdk"], "id1_sex": None, "id1_dates": None,
         "key": "D6E92940-CD75-4423-9B71-C652A82D9243", "weight": 3, "source_type": "startup",
         "id2_sex": "0.711154,0.288845", "match_chain": None, "id1_region": None,
         "id2_browser": "mobilesafari|10.0", "id2_ua": "m|phone|apple|ios|10.0.1", "component": "0",
         "crypta_id": "7442694655393601941", "id2_region": 1, "components_count": 1, "pair_source": "startup_indev"}
    ]

    def test_multi_edge(self):

        edges = [Edge(r) for r in self.recs]

        edges, total = weight_multi_edge_old(edges, "d_y", ClusteringConfig())

        # there are 3 machine sources, but two of them are duplicates
        weights = [e.weight for e in edges]
        self.assertListEqual(weights, [1.0, 1.5, 1.0, 1.5])

        self.assertEqual(total, 5.0)

    def test_multi_edge_new(self):
        edges = [Edge(r) for r in self.recs]

        edges, total = weight_multi_edge_new(edges, "d_y", ClusteringConfig())

        # there are 3 machine sources, but two of them are duplicates
        weights = [e.weight for e in edges]
        self.assertListEqual(weights, [1.0, 1.5, 1.0, 1.5])

        self.assertEqual(total, 5.0)
