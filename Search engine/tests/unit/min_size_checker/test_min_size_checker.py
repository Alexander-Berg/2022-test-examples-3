from screenqual.filter.trivial_checkers.min_size_checker import MinSizeChecker
from tests.unit.unit_test import UnitTest


class TestMinSizeChecker(UnitTest):
    def setUp(self):
        self.analyser = MinSizeChecker()

    def test_does_not_fire_on_good_screenshot(self):
        self.assert_no_anomaly("min_size_checker/good.png")

    def test_fires_on_tiny_screenshot(self):
        self.assert_has_anomaly("min_size_checker/tiny.png")

    def test_fires_on_1x1_screenshot(self):
        self.assert_has_anomaly("min_size_checker/1x1.png")

    def test_fires_on_misread_screenshot(self):
        self.assert_has_anomaly("min_size_checker/no_such_file.png")
