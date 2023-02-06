import unittest
import pytest

from matching.human_matching.stats.radius import radius_metrics_calc


class TestGet_metrics(unittest.TestCase):


    def get_prec_rec(self, rlogin_graph, crypta_graph):
        logins_to_metrics = radius_metrics_calc.get_metrics_per_login(rlogin_graph, crypta_graph)
        precisions = [(lm.prec_a, lm.prec_b) for lm in logins_to_metrics.values()]
        recalls = [(lm.rec_a, lm.rec_b) for lm in logins_to_metrics.values()]
        prec, rec, _ = radius_metrics_calc.average_metrics(precisions, recalls)
        return prec, rec


    def test_metrics_reference(self):
        """
        Test case https://beta.wiki.yandex-team.ru/jandekspoisk/externalsource/cryptamatchingmetrics/assessors/examples/
        """
        rlogin_graph = {'rlogin1': [{'yuid': 'mob1'}, {'yuid': 'mob2'}, {'yuid': 'mob3'},
                                    {'yuid': 'desk1'}, {'yuid': 'desk2'}, {'yuid': 'desk3'}]}
        crypta_graph = {'cid1': [{'yuid': 'mob1'}, {'yuid': 'desk1'}, {'yuid': 'desk2'}],
                        'cid2': [{'yuid': 'mob3'}, {'yuid': 'desk3'}, {'yuid': 'desk4'}]}

        prec, rec = self.get_prec_rec(rlogin_graph, crypta_graph)
        similarity = radius_metrics_calc.similarity(rlogin_graph, crypta_graph)

        self.assertEqual(prec['mean.opt'], 0.75)
        self.assertEqual(rec['mean.opt'], 0.6)
        self.assertEqual(prec['cumulative'], 0.75)
        self.assertEqual(rec['cumulative'], 0.6)
        self.assertEqual(similarity, 5 / 7.0)


    def test_metrics_perfect_match(self):
        rlogin_graph = {'a': [{'yuid': '1'}, {'yuid': '2'}, {'yuid': '3'}],
                        'b': [{'yuid': '4'}, {'yuid': '5'}]}
        crypta_graph = {'x': [{'yuid': '1'}, {'yuid': '2'}, {'yuid': '3'}],
                        'y': [{'yuid': '4'}, {'yuid': '5'}]}

        prec, rec = self.get_prec_rec(rlogin_graph, crypta_graph)

        self.assertEqual(prec['mean.opt'], 1.0)
        self.assertEqual(rec['mean.opt'], 1.0)
        self.assertEqual(prec['cumulative'], 1.0)
        self.assertEqual(rec['cumulative'], 1.0)

    def test_metrics_found_1_more(self):
        rlogin_graph = {'a': [{'yuid': '1'}, {'yuid': '3'}],
                        'b': [{'yuid': '4'}, {'yuid': '5'}]}
        crypta_graph = {'x': [{'yuid': '1'}, {'yuid': '2_found_more'}, {'yuid': '3'}],
                        'y': [{'yuid': '4'}, {'yuid': '5'}]}

        prec, rec = self.get_prec_rec(rlogin_graph, crypta_graph)

        self.assertEqual(prec['cumulative'], 2.0 / 3)
        self.assertEqual(rec['cumulative'], 1.0)

    def test_metrics_found_2_more(self):
        rlogin_graph = {'a': [{'yuid': '1'}, {'yuid': '3'}],
                        'b': [{'yuid': '4'}]}
        crypta_graph = {'x': [{'yuid': '1'}, {'yuid': '2_found_more'}, {'yuid': '3'}],
                        'y': [{'yuid': '4'}, {'yuid': '5_found_more'}]}

        prec, rec = self.get_prec_rec(rlogin_graph, crypta_graph)

        self.assertEqual(prec['cumulative'], 1.0 / 3)
        self.assertEqual(rec['cumulative'], 1.0)

    def test_metrics_found_2_less(self):
        rlogin_graph = {'a': [{'yuid': '1'}, {'yuid': '2'}, {'yuid': '3_not_found'}],
                        'b': [{'yuid': '4'}, {'yuid': '5_not_found'}]}
        crypta_graph = {'x': [{'yuid': '1'}, {'yuid': '2'}],
                        'y': [{'yuid': '4'}]}

        prec, rec = self.get_prec_rec(rlogin_graph, crypta_graph)

        self.assertEqual(prec['cumulative'], 1.0)
        self.assertEqual(rec['cumulative'], 1.0 / 3)

    def test_metrics_found_3_less(self):
        rlogin_graph = {'a': [{'yuid': '1'}, {'yuid': '2'}, {'yuid': '3_not_found'}, {'yuid': '4_not_found'}],
                        'b': [{'yuid': '4'}, {'yuid': '5_not_found'}]}
        crypta_graph = {'x': [{'yuid': '1'}, {'yuid': '2'}],
                        'y': [{'yuid': '4'}]}

        prec, rec = self.get_prec_rec(rlogin_graph, crypta_graph)

        self.assertEqual(prec['cumulative'], 1.0)
        self.assertEqual(rec['cumulative'], 1.0 / 4)

    def test_metrics_found_2_less_1_more(self):
        rlogin_graph = {'a': [{'yuid': '1'}, {'yuid': '2'}, {'yuid': '3_not_found'}],
                        'b': [{'yuid': '4'}, {'yuid': '5_not_found'}]}
        crypta_graph = {'x': [{'yuid': '1'}, {'yuid': '2'}],
                        'y': [{'yuid': '4'}, {'yuid': '6_found_more'}]}

        prec, rec = self.get_prec_rec(rlogin_graph, crypta_graph)

        self.assertEqual(prec['cumulative'], 1.0 / 2)
        self.assertEqual(rec['cumulative'], 1.0 / 3)

    def test_metrics_found_all_in_2_crypta_ids(self):
        rlogin_graph = {'a': [{'yuid': '1'}, {'yuid': '2'}, {'yuid': '3'}]}
        crypta_graph = {'x': [{'yuid': '1'}, {'yuid': '2'}],
                        'y': [{'yuid': '3'}, {'yuid': '4_some_other'}]}

        prec, rec = self.get_prec_rec(rlogin_graph, crypta_graph)

        self.assertEqual(prec['mean.opt'], 1.0 / 2)
        self.assertEqual(rec['mean.opt'], 1.0 / 2)

    def test_metrics_found_all_but_2_users_in_one_crypta_id(self):
        rlogin_graph = {'a': [{'yuid': '1'}, {'yuid': '2'}, {'yuid': '3'}],
                        'b': [{'yuid': '4'}, {'yuid': '5'}]}
        crypta_graph = {'x': [{'yuid': '1'}, {'yuid': '2'}, {'yuid': '3'}, {'yuid': '4'}, {'yuid': '5'}]}

        prec, rec = self.get_prec_rec(rlogin_graph, crypta_graph)

        self.assertEqual(prec['mean.opt'], 3.0 / 8)
        self.assertEqual(rec['mean.opt'], 1.0)

    def test_single_devices_are_not_taken_into_account(self):
        rlogin_graph = {'a': [{'yuid': '1'}],
                        'b': [{'yuid': '2'}]}
        crypta_graph = {'x': [{'yuid': '1'}],
                        'y': [{'yuid': '2'}]}

        logins_to_metrics = radius_metrics_calc.get_metrics_per_login(rlogin_graph, crypta_graph)
        precs = [(lm.prec_a, lm.prec_b) for lm in logins_to_metrics.values()]
        recs = [(lm.rec_a, lm.rec_b) for lm in logins_to_metrics.values()]

        self.assertEqual(len(precs), 0)
        self.assertEqual(len(recs), 0)


if __name__ == "__main__":
    unittest.main()
