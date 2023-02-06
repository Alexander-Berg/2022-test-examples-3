from screenqual.filter.similarity_checkers.general_fullpage_similarity_checker import GeneralFullpageSimilarityChecker
from tests.regression.regression_test import TestRegression


class TestRegressionGeneralFullpageSimilarityChecker(TestRegression):
    def test_fscore_on_common_screenshots(self):
        self.fscore(["/yandex_fullpage_similarity_checker/desktop/bad/",
                     "/yandex_fullpage_similarity_checker/mobile/bad/"],
                    ["/yandex_fullpage_similarity_checker/desktop/good/",
                     "/yandex_fullpage_similarity_checker/mobile/good/",
                     "/yandex_fullpage_wo_header_and_footer/"],
                    GeneralFullpageSimilarityChecker(), .9, "general fullpage similarity checker: all fullpages",
                    ["png", "jpg"])
