from screenqual.filter.red_text_detector.red_text_detector import RedTextDetector
from tests.regression.regression_test import TestRegression


class TestRegressionRedTextDetector(TestRegression):

    def test_fscore_red_text_on_desktop(self):
        self.fscore(["/red_text/desktop/bad/"],
                    ["/red_text/desktop/good/"],
                    RedTextDetector(), .9, "red text detector: desktop", extensions=["png", "jpg"])

    def test_fscore_red_text_on_mobile(self):
        self.fscore(["/red_text/mobile/bad/"],
                    ["/red_text/mobile/good/"],
                    RedTextDetector(), .9, "red text detector: mobile", extensions=["png", "jpg"])
