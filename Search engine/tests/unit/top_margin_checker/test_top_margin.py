from screenqual.filter.top_margin_detector.top_margin_checker import TopMarginChecker
from tests.unit.unit_test import UnitTest


class TestTopMarginChecker(UnitTest):
    def setUp(self):
        self.analyser = TopMarginChecker()

    def test_does_not_fire_on_good_screenshots(self):
        self.assert_no_anomaly('top_margin_checker/imgs/kittens_regular.png')

    def test_fires_on_screenshot_with_large_margin(self):
        self.assert_has_anomaly('top_margin_checker/imgs/kittens_large_margin.png')


class TestTopMarginCheckerWithZeroThreshold(UnitTest):
    def setUp(self):
        self.analyser = TopMarginChecker(0)

    def test_fires_on_screenshot_if_threshold_is_zero(self):
        self.assert_has_anomaly('top_margin_checker/imgs/kittens_no_margin.png')
