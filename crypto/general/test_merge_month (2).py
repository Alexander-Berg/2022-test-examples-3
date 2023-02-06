import unittest
import allure
from crypta.graph.v1.python.matching.yuid_matching import graph_merge_month


class TestMergeMonth(unittest.TestCase):
    def parse_reduce_incremental_merge_output(self, recs):
        yuid_raw_month = []
        long_live_stats = []
        outdated_stats = []
        for r in recs:
            if r["@table_index"] == 0:
                yuid_raw_month.append(r)
            elif r["@table_index"] == 1:
                long_live_stats.append(r)
            else:
                outdated_stats.append(r)

        return yuid_raw_month, long_live_stats, outdated_stats

    def test_incremental_merge_no_yuid_raw(self):
        yuid_all_recs = [
            {
                "yuid": "1",
                "id_type": "it1",
                "source_type": "st1",
                "it1_st1_dates": {"value1": {"2016-05-05": 1}},
                "all_dates": ["2016-05-05"],
                "@table_index": 0,
            }
        ]

        index_map = {"it1_st1": 0}

        result = graph_merge_month.reduce_incremental_merge(None, yuid_all_recs, index_map, "2016-05-20", 30)
        result = list(result)
        allure.attach("Reduce incremental merge result", str(result))
        self.assertTrue(len(result) == 0)

    def test_incremental_merge_has_today_activity(self):
        yuid_raw_today = [
            {"id_value": "a", "id_date": "2016-05-20", "id_type": "it1", "source_type": "st1", "@table_index": 1}
        ]

        index_map = {"it1_st1": 0}

        result = graph_merge_month.reduce_incremental_merge(None, yuid_raw_today, index_map, "2016-05-20", 30)
        yuid_raw_month, long_live_stats, outdated_stats = self.parse_reduce_incremental_merge_output(result)
        allure.attach("yuid_raw_month", str(yuid_raw_month))
        allure.attach("long_live_stats", str(long_live_stats))
        allure.attach("outdated_stats", str(outdated_stats))
        self.assertTrue(len(yuid_raw_month) == 1)
        self.assertTrue(len(long_live_stats) == 1)
        self.assertTrue(len(outdated_stats) == 0)

    def test_incremental_merge_active_by_yuid_all(self):
        yuid_all_recs = [
            {
                "yuid": "1",
                "id_type": "it1",
                "source_type": "st1",
                "it1_st1_dates": {"value1": {"2016-05-05": 1}},
                "all_dates": ["2016-05-05"],
                "@table_index": 0,
            }
        ]

        yuid_raw_old = [
            {"id_value": "a", "id_date": "2016-04-10", "id_type": "it1", "source_type": "st1", "@table_index": 1}
        ]

        index_map = {"it1_st1": 0}

        result = graph_merge_month.reduce_incremental_merge(
            None, yuid_all_recs + yuid_raw_old, index_map, "2016-05-20", 30
        )
        yuid_raw_month, long_live_stats, outdated_stats = self.parse_reduce_incremental_merge_output(result)
        allure.attach("yuid_raw_month", str(yuid_raw_month))
        allure.attach("long_live_stats", str(long_live_stats))
        allure.attach("outdated_stats", str(outdated_stats))
        self.assertTrue(len(yuid_raw_month) == 1)
        self.assertTrue(len(long_live_stats) == 1)
        self.assertTrue(len(outdated_stats) == 0)

    def test_incremental_merge_not_active_by_yuid_all(self):
        yuid_all_recs = [
            {
                "yuid": "1",
                "id_type": "it1",
                "source_type": "st1",
                "it1_st1_dates": {"value1": {"2016-04-10": 1}},
                "all_dates": ["2016-04-10"],  # only from outdated source
                "@table_index": 0,
            }
        ]

        yuid_raw_old = [
            {"id_value": "a", "id_date": "2016-04-10", "id_type": "it1", "source_type": "st1", "@table_index": 1}
        ]

        index_map = {"it1_st1": 0}

        result = graph_merge_month.reduce_incremental_merge(
            None, yuid_all_recs + yuid_raw_old, index_map, "2016-05-20", 30
        )
        yuid_raw_month, long_live_stats, outdated_stats = self.parse_reduce_incremental_merge_output(result)
        allure.attach("yuid_raw_month", str(yuid_raw_month))
        allure.attach("long_live_stats", str(long_live_stats))
        allure.attach("outdated_stats", str(outdated_stats))
        self.assertTrue(len(yuid_raw_month) == 0)
        self.assertTrue(len(long_live_stats) == 0)
        self.assertTrue(len(outdated_stats) == 1)
