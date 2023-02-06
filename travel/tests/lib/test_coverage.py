import unittest
from datetime import datetime, timedelta

from travel.avia.admin.lib.coverage import compute_coverage


class TestComputeCoverage(unittest.TestCase):
    steps = [10, 20, 25, 40]
    window = 10
    answer = 35

    def test(self):
        assert compute_coverage(self.steps, self.window) == self.answer

    def test_with_timedelta(self):
        start_timestamp = datetime(2018, 12, 1)
        steps = [start_timestamp + timedelta(seconds=step) for step in self.steps]
        assert compute_coverage(steps, timedelta(seconds=self.window)) == timedelta(seconds=self.answer)
