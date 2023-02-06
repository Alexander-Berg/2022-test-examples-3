from screenqual.filter.red_text_detector.red_text_detector import RedTextDetector
from tests.unit.unit_test import UnitTest


class TestRedTextDetector(UnitTest):
    def setUp(self):
        self.analyser = RedTextDetector()

    def test_simple_red_text(self):
        self.assert_has_anomaly('red_text_detector/1.png')

    def test_red_text_and_image(self):
        self.assert_has_anomaly('red_text_detector/2.png')

    def test_red_text_and_image_v2(self):
        self.assert_has_anomaly('red_text_detector/3.jpg')

    def test_good_text_and_small_red_image(self):
        self.assert_no_anomaly('red_text_detector/18.png')

    def test_good_text_and_red_image(self):
        self.assert_no_anomaly('red_text_detector/61.png')
