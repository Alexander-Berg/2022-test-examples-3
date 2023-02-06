from screenqual.filter.scrollbars_detector.scrollbars_detector import ScrollBarAnalyser
from tests.unit.unit_test import UnitTest


class TestScrollBarAnalyser(UnitTest):
    def setUp(self):
        self.analyser = ScrollBarAnalyser()

    def test_does_not_fire_on_good_screenshots(self):
        self.assert_no_anomaly('scrollbars_detector/imgs/without_scrollbars/1.png')

    def test_fires_on_screenshots_with_vertical_linux_scrollbars(self):
        self.assert_has_anomaly('scrollbars_detector/imgs/with_scrollbars/2.png')
        self.assert_has_anomaly('scrollbars_detector/imgs/with_scrollbars/3.png')

    def test_fires_on_screenshots_with_vertical_and_horizontal_windows_scrollbars(self):
        self.assert_has_anomaly('scrollbars_detector/imgs/with_scrollbars/4.png')

    def test_should_work_with_wiki_page(self):
        self.assert_no_anomaly('scrollbars_detector/imgs/without_scrollbars/wiki_page.png')

    def test_should_work_if_none_lines_returned_from_hough_lines_method(self):
        self.assert_no_anomaly('scrollbars_detector/imgs/without_scrollbars/none_lines.png')
