import unittest
from modules.data_receiver import extract_metrics
from modules.project_models import Metric


class TestDataReceiver(unittest.TestCase):

    def test_parse_one_metric(self):
        metrics, errors = extract_metrics("[name:KPI1, unit:kap, proved:70, target:900]")
        self.assertEqual(0, len(errors))
        self.assertEqual(1, len(metrics))
        self.assertEqual(Metric("KPI1", "kap", proved=70, target=900), metrics[0])

    def test_parse_plus_char_in_metric(self):
        metrics, errors = extract_metrics("[name:KPI1, unit:kap, proved:+35, target:+280]")
        self.assertEqual(0, len(errors))
        self.assertEqual(1, len(metrics))
        self.assertEqual(Metric("KPI1", "kap", proved=35, target=280), metrics[0])

    def test_parse_two_metrics(self):
        metrics, errors = extract_metrics("""
        [name:KPI1, unit:kap1, proved:250, target:1000]
        [name:KPI2, unit:kap2, proved:270, target:900]
        """)
        self.assertEqual(0, len(errors))
        self.assertEqual(2, len(metrics))
        self.assertEqual(Metric("KPI1", "kap1", proved=250, target=1000), metrics[0])
        self.assertEqual(Metric("KPI2", "kap2", proved=270, target=900), metrics[1])

    def test_description_without_metric(self):
        metrics, errors = extract_metrics("""
        just text
        expecting some exception!
        """)
        self.assertEqual(len(metrics), 0)
        self.assertEqual(len(errors), 0)

    def test_two_metrics_one_with_invalid_data(self):
        metrics, errors = extract_metrics("""
        [name:KPI1, unit:kap1, proved:250, target:1000]
        [name:KPI2, unit:kap2, proved,270]
        """)
        self.assertEqual(len(errors), 1)
        self.assertEqual(len(metrics), 1)
        self.assertEqual("KPI1", metrics[0].Name)

    def test_metrics_with_equal_fields(self):
        metrics, errors = extract_metrics("""
        [name:KPI1, unit:kap1, proved:100, target:800]
        [name:KPI3, unit:kap3, proved:270, target:900]
        """)
        self.assertEqual(len(errors), 0)
        self.assertEqual(len(metrics), 2)

    def test_metrics_with_comments(self):
        metrics, errors = extract_metrics("""
        [name:KPI1, unit:kap1, proved:100, target:800] # а тут у нас какой-то комментарий
        """)
        self.assertEqual(len(errors), 0)
        self.assertEqual(len(metrics), 1)

    def test_metrics_with_increment(self):
        metrics, errors = extract_metrics("""
        [name:KPI1, unit:kap1, proved:100, increment:800] # а тут у нас какой-то комментарий
        """)
        self.assertEqual(len(errors), 0)
        self.assertEqual(len(metrics), 1)
        self.assertEqual(metrics[0].Increment, 800)
        self.assertEqual(metrics[0].Target, 0)


if __name__ == '__main__':
    unittest.main()
