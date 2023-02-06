from screenqual.filter.trivial_checkers.min_colors_checker import MinColorsChecker
from tests.unit.unit_test import UnitTest


class TestMinColorsChecker(UnitTest):
    def setUp(self):
        self.analyser = MinColorsChecker()

    def test_does_not_fire_on_good_screenshot1(self):
        self.assert_no_anomaly("min_colors_checker/good1.png")

    def test_does_not_fire_on_good_screenshot2(self):
        self.assert_no_anomaly("min_colors_checker/good2.png")

    def test_fires_on_white_screenshot(self):
        self.assert_has_anomaly("min_colors_checker/white.png")

    def test_fires_on_checkerboard(self):
        self.assert_has_anomaly("min_colors_checker/checkers.png")