TEST_DATA = {'dictName2': {'statistic': [{'totalCnt': 207, 'freshCnt': 57, 'time': 1350000000},
                                         {'totalCnt': 225, 'freshCnt': 75, 'time': 1350000300},
                                         {'totalCnt': 240, 'freshCnt': 90, 'time': 1350000600},
                                         {'totalCnt': 213, 'freshCnt': 63, 'time': 1350000900},
                                         {'totalCnt': 72, 'freshCnt': 12, 'time': 1350001200}],
                           'updateDate': 1350000000,
                           'generateDate': 1340000000},
             'dictName1': {'statistic': [{'totalCnt': 207, 'freshCnt': 57, 'time': 1350000000},
                                         {'totalCnt': 225, 'freshCnt': 75, 'time': 1350000300},
                                         {'totalCnt': 240, 'freshCnt': 90, 'time': 1350000600},
                                         {'totalCnt': 213, 'freshCnt': 63, 'time': 1350000900},
                                         {'totalCnt': 72, 'freshCnt': 12, 'time': 1350001200}],
                           'updateDate': 1350000000,
                           'generateDate': 1340000000}}

import unittest
from suggest_monitoring import data_processor
from mock import patch

class MetricsTest(unittest.TestCase):
    @patch('time.time')
    def test_getMetrics(self, timeMock):
        timeMock.return_value = 1350000900
        time, generateAge, updateAge, freshCnt, totalCnt = data_processor.DataProcessor().getMetrics(TEST_DATA)
        self.assertEqual(time, 1350000600)
        self.assertEqual(generateAge['dictName1'], 1340000000)
        self.assertEqual(updateAge['dictName1'], 1350000000)
        self.assertEqual(freshCnt['dictName1'], 90)
        self.assertEqual(totalCnt['dictName1'], 240)
