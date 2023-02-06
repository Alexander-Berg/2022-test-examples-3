from screenqual.filter.similarity_checkers.general_fullpage_similarity_checker import GeneralFullpageSimilarityChecker
from tests.unit.unit_test import UnitTest


class TestGeneralFullpageSimilarityChecker(UnitTest):
    def setUp(self):
        self.analyser = GeneralFullpageSimilarityChecker()

    def test_does_not_fire_on_good_screenshot1(self):
        self.assert_no_anomaly("similarity_checkers/good1.png")

    def test_does_not_fire_on_screenshot_wo_header_and_footer(self):
        self.assert_no_anomaly("similarity_checkers/wo_hf.png")

    def test_does_not_fire_on_good_screenshot2(self):
        self.assert_no_anomaly("similarity_checkers/good2.png")

    def test_does_not_fire_on_white_areas(self):
        self.assert_no_anomaly("similarity_checkers/lotofwhite.png")

    def test_does_not_fire_on_mobile(self):
        self.assert_no_anomaly("similarity_checkers/mobile.jpg")

    def test_does_not_fire_on_google(self):
        self.assert_no_anomaly("similarity_checkers/google.png")

    def test_does_not_fire_on_twitter(self):
        self.assert_no_anomaly("similarity_checkers/long1.png")

    def test_does_not_fire_on_yandex_main_screenshot(self):
        self.assert_no_anomaly("similarity_checkers/yndx_main.png")

    def test_fires_on_mostly_white_screenshot(self):
        self.assert_has_anomaly("similarity_checkers/mostly_white.png")

    def test_fires_on_white_screenshot(self):
        self.assert_has_anomaly("similarity_checkers/white.png")

    def test_fires_on_white_twitter(self):
        self.assert_has_anomaly("similarity_checkers/85sj2.png")

    def test_fires_on_checkerboard(self):
        self.assert_has_anomaly("similarity_checkers/checkers.png")

