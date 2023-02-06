import unittest
import numpy as np
from tests.regression.precision_recall_calculator import PrecisionRecallCalculator


class TestPrecisionRecallCalculator(unittest.TestCase):

    def test_empty_pr_ctor_returns_none(self):
        pr_ctor = PrecisionRecallCalculator()
        self.assertEqual(pr_ctor.precision(), np.inf)
        self.assertEqual(pr_ctor.recall(), np.inf)
        self.assertTrue(np.isnan(pr_ctor.fscore()))
        print(pr_ctor)

    def test_pr_ctor_handles_one_value_correctly(self):
        pr_ctor = PrecisionRecallCalculator()
        pr_ctor.add_true_positive()
        self.assertAlmostEqual(pr_ctor.precision(), 1., delta=1e-16)
        self.assertAlmostEqual(pr_ctor.recall(),    1., delta=1e-16)
        self.assertAlmostEqual(pr_ctor.fscore(),    1., delta=1e-16)
        print(pr_ctor)

    def test_pr_ctor_handles_one_fp_correctly(self):
        pr_ctor = PrecisionRecallCalculator()
        pr_ctor.add_false_positive()
        self.assertAlmostEqual(pr_ctor.precision(), 0., delta=1e-16)
        self.assertEqual(pr_ctor.recall(), np.inf)
        self.assertEqual(pr_ctor.fscore(), np.inf)
        print(pr_ctor)

    def test_pr_ctor_handles_one_fn_correctly(self):
        pr_ctor = PrecisionRecallCalculator()
        pr_ctor.add_false_negative()
        self.assertEqual(pr_ctor.precision(), np.inf)
        self.assertAlmostEqual(pr_ctor.recall(), 0., delta=1e-16)
        self.assertEqual(pr_ctor.fscore(), np.inf)
        print(pr_ctor)

    def test_pr_ctor_handles_one_fn_and_one_fp_correctly(self):
        pr_ctor = PrecisionRecallCalculator()
        pr_ctor.add_false_negative()
        pr_ctor.add_false_positive()
        self.assertAlmostEqual(pr_ctor.precision(), 0., delta=1e-16)
        self.assertAlmostEqual(pr_ctor.recall(),    0., delta=1e-16)
        self.assertEqual(pr_ctor.fscore(), np.inf)
        print(pr_ctor)

    def test_pr_ctor_handles_many_values_correctly(self):
        pr_ctor = PrecisionRecallCalculator()
        for i in range(10):
            pr_ctor.add_true_positive()
        for i in range(5):
            pr_ctor.add_false_positive()
        for i in range(15):
            pr_ctor.add_false_negative()
        p = 10. / 15
        r = 10. / 25
        fscore = 2 * p * r / (p + r)

        self.assertAlmostEqual(pr_ctor.precision(), p, delta=1e-16)
        self.assertAlmostEqual(pr_ctor.recall(),    r, delta=1e-16)
        self.assertAlmostEqual(pr_ctor.fscore(),    fscore, delta=1e-16)
        print(pr_ctor)

    def test_pr_ctor_expect_found_interface_works_correctly(self):
        pr_ctor = PrecisionRecallCalculator()
        for i in range(71):
            pr_ctor.expected(True).found(True)
        for i in range(29):
            pr_ctor.expected(False).found(True)
        for i in range(35):
            pr_ctor.expected(True).found(False)
        p = 71. / 100
        r = 71. / 106
        fscore = 2 * p * r / (p + r)

        self.assertAlmostEqual(pr_ctor.precision(), p, delta=1e-16)
        self.assertAlmostEqual(pr_ctor.recall(),    r, delta=1e-16)
        self.assertAlmostEqual(pr_ctor.fscore(),    fscore, delta=1e-16)
        print(pr_ctor)
