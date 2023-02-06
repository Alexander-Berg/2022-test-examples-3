import unittest

from lib.KPI import KPI


class KPITest(unittest.TestCase):
    def test_get(self):
        exception = False
        try:
            KPI.get(service='ozon')
        except Exception:
            exception = True
        self.assertEqual(True, exception)

        self.assertEqual(True, isinstance(KPI.get(service='market'), dict))

        exception = False
        try:
            KPI.get(service='market', date='invalid date')
        except Exception:
            exception = True
        self.assertEqual(True, exception)

        self.assertEqual(True, isinstance(KPI.get(service='market', date='last'), dict))

        exception = False
        try:
            self.assertEqual(True, isinstance(KPI.get(service='market', date='last', metric='DAU_invalid')))
        except Exception:
            exception = True
        self.assertEqual(True, exception)

        self.assertEqual(True, isinstance(KPI.get(service='market', date='last', metric='DAU_avg'), list))

        exception = False
        try:
            self.assertEqual(True, isinstance(KPI.get(service='market', date='last', metric='DAU_avg', month=14), list))
        except Exception:
            exception = True
        self.assertEqual(True, exception)

        self.assertEqual(True, isinstance(KPI.get(service='market', date='last', metric='DAU_avg', month=12), int))


if __name__ == '__main__':
    unittest.main()
