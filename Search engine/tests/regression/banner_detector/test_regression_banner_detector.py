from screenqual.filter.banner_detector.banner_detector import BannerDetector
from tests.regression.regression_test import TestRegression


class TestRegressionBannerDetector(TestRegression):
    def test_fscore_banner_images(self):
        self.fscore(["/banner_detector/with_banner/"], ["/banner_detector/without_banner/"],
                    BannerDetector(), .99, "banner_detector detector", extensions=["jpg"])
