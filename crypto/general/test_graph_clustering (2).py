import unittest

from crypta.graph.v1.python.matching.human_matching import graph_clustering
from crypta.graph.v1.python.matching.human_matching.graph_clustering import Edge, Component


class TestGraphClustering(unittest.TestCase):
    def test_reassign_corner_values_ok(self):
        edges1 = [
            Edge({"id1": 1, "id2": 2, "pair_type": "y_y", "pair_source": "s1", "id_value": 123}),
            Edge({"id1": 2, "id2": 3, "pair_type": "y_y", "pair_source": "s2", "id_value": 456}),
            Edge({"id1": 2, "id2": 3, "pair_type": "y_y", "pair_source": "s3", "id_value": 456}),
            Edge({"id1": 3, "id2": 4, "pair_type": "y_y", "pair_source": "s1", "id_value": 123}),
        ]

        edge_to_be_cut_off = Edge({"id1": 5, "id2": 6, "pair_type": "y_y", "pair_source": "s1", "id_value": 123})
        edges2 = [
            edge_to_be_cut_off,
            Edge({"id1": 6, "id2": 7, "pair_type": "y_y", "pair_source": "s2", "id_value": 666}),
        ]

        removed_edges = [
            Edge({"id1": 4, "id2": 5, "pair_type": "y_y", "pair_source": "s1", "id_value": 123}),
            Edge({"id1": 4, "id2": 5, "pair_type": "y_y", "pair_source": "s2", "id_value": 456}),
        ]

        components = [Component(edges1, 4, 1), Component(edges2, 3, 2)]

        self.assertIn(edge_to_be_cut_off, components[1].edges)

        after_reassign, _ = graph_clustering.reassign_corner_values(components, removed_edges)

        self.assertIn(removed_edges[0], after_reassign[0].edges)
        self.assertIn(removed_edges[1], after_reassign[0].edges)
        self.assertNotIn(edge_to_be_cut_off, after_reassign[1].edges)

    def test_reassign_corner_values_ok_vice_versa(self):
        edge_to_be_cut_off = Edge({"id1": 5, "id2": 6, "pair_type": "y_y", "pair_source": "s1", "id_value": 123})
        edges1 = [
            Edge({"id1": 5, "id2": 6, "pair_type": "y_y", "pair_source": "s1", "id_value": 123}),
            Edge({"id1": 6, "id2": 61, "pair_type": "y_y", "pair_source": "s1", "id_value": 777}),
            edge_to_be_cut_off,
        ]

        edges2 = [
            Edge({"id1": 2, "id2": 3, "pair_type": "y_y", "pair_source": "s2", "id_value": 456}),
            Edge({"id1": 2, "id2": 3, "pair_type": "y_y", "pair_source": "s3", "id_value": 456}),
            Edge({"id1": 3, "id2": 4, "pair_type": "y_y", "pair_source": "s1", "id_value": 123}),
        ]

        removed_edges = [
            Edge({"id1": 4, "id2": 5, "pair_type": "y_y", "pair_source": "s1", "id_value": 123}),
            Edge({"id1": 4, "id2": 5, "pair_type": "y_y", "pair_source": "s2", "id_value": 456}),
        ]

        components = [Component(edges1, 3, 2), Component(edges2, 4, 1)]

        self.assertIn(edge_to_be_cut_off, components[0].edges)

        after_reassign, _ = graph_clustering.reassign_corner_values(components, removed_edges)

        self.assertIn(removed_edges[0], after_reassign[1].edges)
        self.assertIn(removed_edges[1], after_reassign[1].edges)
        self.assertNotIn(edge_to_be_cut_off, after_reassign[0].edges)

    def test_reassign_corner_values_second_is_too_small_to_cut_off(self):
        edges1 = [
            Edge({"id1": 2, "id2": 3, "pair_type": "y_y", "pair_source": "s2", "id_value": 456}),
            Edge({"id1": 2, "id2": 3, "pair_type": "y_y", "pair_source": "s3", "id_value": 456}),
            Edge({"id1": 3, "id2": 4, "pair_type": "y_y", "pair_source": "s1", "id_value": 123}),
        ]

        edges2 = [Edge({"id1": 5, "id2": 6, "pair_type": "y_y", "pair_source": "s1", "id_value": 123})]

        removed_edges = [
            Edge({"id1": 5, "id2": 4, "pair_type": "y_y", "pair_source": "s1", "id_value": 123}),
            Edge({"id1": 5, "id2": 4, "pair_type": "y_y", "pair_source": "s2", "id_value": 456}),
        ]

        components = [Component(edges1, 4, 1), Component(edges2, 2, 2)]

        after_reassign, _ = graph_clustering.reassign_corner_values(components, removed_edges)

        self.assertNotIn(removed_edges[0], after_reassign[0].edges)
        self.assertNotIn(removed_edges[1], after_reassign[0].edges)

    def test_reassign_corner_values_no_best_component_found(self):
        edges1 = [
            Edge({"id1": 2, "id2": 3, "pair_type": "y_y", "pair_source": "s2", "id_value": 456}),
            Edge({"id1": 2, "id2": 3, "pair_type": "y_y", "pair_source": "s3", "id_value": 456}),
            Edge({"id1": 3, "id2": 4, "pair_type": "y_y", "pair_source": "s1", "id_value": 123}),
        ]

        edges2 = [Edge({"id1": 5, "id2": 6, "pair_type": "y_y", "pair_source": "s1", "id_value": 123})]

        removed_edges = [
            Edge({"id1": 5, "id2": 4, "pair_type": "y_y", "pair_source": "s1", "id_value": 123}),
            Edge({"id1": 5, "id2": 6, "pair_type": "y_y", "pair_source": "s2", "id_value": 456}),
        ]

        components = [Component(edges1, 4, 1), Component(edges2, 2, 2)]

        after_reassign, _ = graph_clustering.reassign_corner_values(components, removed_edges)

        self.assertNotIn(removed_edges[0], after_reassign[0].edges)
        self.assertNotIn(removed_edges[1], after_reassign[0].edges)
        self.assertNotIn(removed_edges[0], after_reassign[1].edges)
        self.assertNotIn(removed_edges[1], after_reassign[1].edges)

    def test_can_split_human_limit(self):
        cc = graph_clustering.ClusteringConfig(cluster_multi_values_over_human_limit=True)

        edges1 = [
            Edge({"id1": 2, "id2": 3, "pair_type": "y_y", "pair_source": "s2", "id_type": "phone", "id_value": 456}),
            Edge({"id1": 2, "id2": 3, "pair_type": "y_y", "pair_source": "s3", "id_type": "email", "id_value": 456}),
            Edge({"id1": 3, "id2": 4, "pair_type": "y_y", "pair_source": "s1", "id_type": "login", "id_value": 123}),
        ]
        can_split_limit_not_exceeded = Component(edges1, 3).can_split(cc)
        self.assertEqual(can_split_limit_not_exceeded, False)

        edges2 = [
            Edge({"id1": 2, "id2": 3, "pair_type": "y_y", "pair_source": "s2", "id_type": "phone", "id_value": 456}),
            Edge({"id1": 2, "id2": 3, "pair_type": "y_y", "pair_source": "s3", "id_type": "phone", "id_value": 789}),
            Edge({"id1": 2, "id2": 3, "pair_type": "y_y", "pair_source": "s3", "id_type": "phone", "id_value": 987}),
            Edge({"id1": 3, "id2": 4, "pair_type": "y_y", "pair_source": "s1", "id_type": "login", "id_value": 123}),
        ]
        can_split_two_phones = Component(edges2, 3).can_split(cc)
        self.assertEqual(can_split_two_phones, True)

        edges_have_some_devid_yuid = [
            Edge({"id1": 2, "id2": 3, "pair_type": "y_y", "pair_source": "s2", "id_type": "phone", "id_value": 456}),
            Edge({"id1": 2, "id2": 3, "pair_type": "y_y", "pair_source": "s3", "id_type": "phone", "id_value": 789}),
            Edge({"id1": 2, "id2": 3, "pair_type": "y_y", "pair_source": "s3", "id_type": "phone", "id_value": 987}),
            Edge({"id1": 3, "id2": 4, "pair_type": "y_y", "pair_source": "s1", "id_type": "login", "id_value": 123}),
            Edge({"id1": "devid1", "id2": 4, "pair_type": "d_y", "pair_source": "s1"}),
            Edge({"id1": "devid2", "id2": 4, "pair_type": "d_y", "pair_source": "s1"}),
        ]
        can_split_devid_doesnt_break = Component(edges_have_some_devid_yuid, 3).can_split(cc)
        self.assertEqual(can_split_devid_doesnt_break, True)

        edges_have_not_human_id_type = [
            Edge({"id1": 2, "id2": 3, "pair_type": "y_y", "pair_source": "s2", "id_type": "ui", "id_value": 456}),
            Edge({"id1": 2, "id2": 3, "pair_type": "y_y", "pair_source": "s3", "id_type": "ui", "id_value": 789}),
            Edge({"id1": 2, "id2": 3, "pair_type": "y_y", "pair_source": "s4", "id_type": "ui", "id_value": 789}),
            Edge({"id1": 3, "id2": 4, "pair_type": "y_y", "pair_source": "s1", "id_type": "login", "id_value": 123}),
        ]
        can_split_not_human_id_type = Component(edges_have_not_human_id_type, 3).can_split(cc)
        self.assertEqual(can_split_not_human_id_type, False)
