from screenqual.filter.white_areas_detector.white_areas_detector import WhiteAreasAnalyser
from tests.unit.unit_test import UnitTest


class TestWhiteAreasDetector(UnitTest):
    def setUp(self):
        self.analyser = WhiteAreasAnalyser(max_white_area=0.3)

    def test_does_not_fire_on_bad_screenshots(self):
        self.assert_has_anomaly("white_areas/imgs/not_ok/1.png")

    def test_does_not_fire_on_good_screenshots(self):
        self.assert_no_anomaly("white_areas/imgs/ok/1.png")

