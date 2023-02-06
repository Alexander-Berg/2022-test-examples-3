import unittest
from modules.metric_models import Metric
from modules import project_constants
from unittest import skip

import json


class TestMetrics(unittest.TestCase):
    @staticmethod
    def GetDataDir():
        return "tests/tests_resources/data_storage_metrics_data"

    def test_metric_read_from_json_and_serialization(self):
        dictView = {"Name": "m1", "Unit": "u1", "Base": 1, "Proved": 2, "Released": 3, "InProgress": 4, "Planned": 5, "Increment": 6, "Target": 0, "AggregatedTarget": 8, "ViewData": []}
        loadMetric = Metric(jsonRepr=dictView)
        createdMetric = Metric("m1", "u1", 1, 2, 3, 4, 5, 6, 0, 8)
        self.assertEqual(createdMetric, loadMetric)
        self.assertEqual(createdMetric.ToJson(), dictView)

    @skip("validation is disabled now")
    def test_simple_metric_read_from_broken_json(self):
        files = ["simple_metric_no_target.json",
                 "simple_metric_target_zero.json",
                 "simple_metric_target_zero.json",
                 "simple_metric_empty_name.json"]
        for f in files:
            filepath = "{}/{}".format(self.GetDataDir(), f)
            with open(filepath) as f:
                jsonRepr = json.load(f)
            try:
                Metric(jsonRepr=jsonRepr)
                self.assertTrue(False, "Десерелизация сломаной метрики должна бросать исключение. Некорректный файл: '{}'".format(filepath))
            except Exception:
                self.assertTrue(True)

    def test_aggregated_metric_aggregate_unknown_status_failed(self):
        unknownStatus = ""
        self.assertTrue(unknownStatus not in project_constants.AVAILABLE_STATUSES)
        aggregated = Metric("m1", "", target=20)
        try:
            aggregated.Aggregate(Metric("m2", "", 50), unknownStatus)
            self.assertTrue(False)
        except Exception:
            self.assertTrue(True)

    def test_metric_validation_target_increment_incompatibility(self):
        try:
            Metric("m1", "u1", target=1, increment=2)
            self.assertFalse(True)
        except Exception:
            self.assertTrue(True)

    @skip("still not ready")
    def test_build_metric_view(self):
        pass


if __name__ == '__main__':
    unittest.main()
