from screenqual.filter.scrollbars_detector.scrollbars_detector import ScrollBarAnalyser
from tests.regression.regression_test import TestRegression


class TestRegressionScrollbarDetector(TestRegression):
    def test_fscore_on_scrollbar_detector(self):
        self.fscore(["/scrollbars/with_scrollbars/"], ["/scrollbars/without_scrollbars/"],
                    ScrollBarAnalyser(), .88, "scrollbars detector")
