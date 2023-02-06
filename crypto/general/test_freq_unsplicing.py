import unittest

from matching.yuid_matching.graph_dict import IdActivity, calculate_freq_weights
from matching.yuid_matching import graph_unsplice


class TestFreqUnsplicing(unittest.TestCase):

    def test_freq_weight_priority(self):
        id1 = IdActivity("1", "a")
        id1.dates_activity = {
            "2016-08-01": 1,
            "2016-08-09": 1
        }

        id2 = IdActivity("2", "b")
        id2.dates_activity = {
            "2016-08-01": 1
        }

        id3 = IdActivity("3", "c")
        id3.dates_activity = {
            "2016-07-13": 1,
            "2016-07-31": 2,
            "2016-08-01": 34,
            "2016-08-02": 126,
            "2016-08-03": 141,
            "2016-08-04": 282,
            "2016-08-05": 84,
            "2016-08-06": 9,
            "2016-08-07": 22,
            "2016-08-08": 39,
            "2016-08-09": 55
        }

        id4 = IdActivity("4", "d")
        id4.dates_activity = {
            "2016-08-02": 1,
            "2016-08-08": 1
        }

        a = calculate_freq_weights([id1, id2, id3, id4], 10)
        in_desc_priority = [id_a.id_value for id_a, weight in a]

        self.assertListEqual(in_desc_priority, ['3', '1', '4', '2'])

    def test_freq_metrics(self):

        dates_activity_with_oldest_date = [
            "2016-07-12",
        ]

        dates_activity_with_newest_date = [
            "2016-10-10",
        ]

        dates_activity_with_one_date = [
            "2016-09-02",
        ]

        dates_activity_no_oldest_date = [
            "2016-07-13",
            "2016-07-31",
            "2016-08-01",
            "2016-09-02",
            "2016-09-03",
            "2016-09-04",
            "2016-10-10",
        ]

        dates_activity_no_newest_date = [
            "2016-07-12",
            "2016-07-13",
            "2016-07-31",
            "2016-08-01",
            "2016-09-02",
            "2016-09-03",
            "2016-09-04",
        ]

        all_dates = [
            "2016-07-12",
            "2016-07-13",
            "2016-07-31",
            "2016-08-01",
            "2016-09-02",
            "2016-09-03",
            "2016-09-04",
            "2016-10-10",
        ]


        # typical scenario, when activity is stored for a month
        max_dates = 30
        m1 = graph_unsplice.freq_metric(dates_activity_with_oldest_date, all_dates, max_dates)
        self.assertEqual(m1, 0.13769250182778495)

        m2 = graph_unsplice.freq_metric(dates_activity_with_newest_date, all_dates, max_dates)
        self.assertEqual(m2, 0.2466666666666667)

        m3 = graph_unsplice.freq_metric(dates_activity_with_one_date, all_dates, max_dates)
        self.assertEqual(m3, 0.10545617580246913)

        m4 = graph_unsplice.freq_metric(all_dates, all_dates, max_dates)
        self.assertEqual(m4, 1)

        m5 = graph_unsplice.freq_metric(dates_activity_no_oldest_date, all_dates, max_dates)
        self.assertEqual(m5, 1 - m1)

        m6 = graph_unsplice.freq_metric(dates_activity_no_newest_date, all_dates, max_dates)
        self.assertEqual(m6, 1 - m2)


        # assume we store more days than expected (long-live sources)
        # TODO: check assumptions
        max_dates = 4
        m1 = graph_unsplice.freq_metric(dates_activity_with_oldest_date, all_dates, max_dates)
        self.assertEqual(m1, 0.4782968999999997)  # having old day beyond store interval is cool (history is preferred)

        m2 = graph_unsplice.freq_metric(dates_activity_with_newest_date, all_dates, max_dates)
        self.assertEqual(m2, 0.10000000000000009)  # last activity is not than cool

        m3 = graph_unsplice.freq_metric(dates_activity_with_one_date, all_dates, max_dates)
        self.assertEqual(m3, 0.07290000000000003)

        m4 = graph_unsplice.freq_metric(all_dates, all_dates, max_dates)
        self.assertEqual(m4, 1)

        m5 = graph_unsplice.freq_metric(dates_activity_no_oldest_date, all_dates, max_dates)
        self.assertEqual(m5, 1 - m1)

        m6 = graph_unsplice.freq_metric(dates_activity_no_newest_date, all_dates, max_dates)
        self.assertEqual(m6, 1 - m2)
