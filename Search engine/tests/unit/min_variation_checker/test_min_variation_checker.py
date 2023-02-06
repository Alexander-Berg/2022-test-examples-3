from screenqual.filter.trivial_checkers.min_variation_checker import MinVariationChecker
from tests.unit.unit_test import UnitTest


class TestMinVariationChecker(UnitTest):
    def setUp(self):
        self.analyser = MinVariationChecker()

    def test_does_not_fire_on_good_screenshot1(self):
        self.assert_no_anomaly("min_variation_checker/good1.png")

    def test_does_not_fire_on_good_screenshot2(self):
        self.assert_no_anomaly("min_variation_checker/good2.png")

    def test_does_not_fire_on_document_screenshot(self):
        self.assert_no_anomaly("min_variation_checker/0_10_1.png")

    def test_does_not_fire_on_sparse_document_screenshot(self):
        self.assert_no_anomaly("min_variation_checker/1_14.png")

    def test_does_not_fire_on_sparse_fullpage_screenshot(self):
        self.assert_no_anomaly("min_variation_checker/sparse_fullpage.jpg")

    def test_does_not_fire_on_very_sparse_fullpage_screenshot(self):
        self.assert_no_anomaly("min_variation_checker/very_sparse_fullpage.jpg")

    def test_does_not_fire_on_imgs_screenshot(self):
        self.assert_no_anomaly("min_variation_checker/5_0.png")

    def test_fires_on_white_screenshot(self):
        self.assert_has_anomaly("min_variation_checker/white.png")

    def test_fires_on_checkerboard(self):
        self.assert_has_anomaly("min_variation_checker/checkers.png")