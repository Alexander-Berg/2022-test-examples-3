from screenqual.filter.similarity_checkers.yandex_fullpage_similarity_checker import YandexFullpageSimilarityChecker
from tests.regression.regression_test import TestRegression


class TestRegressionYandexFullpageSimilarityChecker(TestRegression):
    def test_fscore_on_desktop_screenshots(self):
        self.fscore(["/yandex_fullpage_similarity_checker/desktop/bad/"],
                    ["/yandex_fullpage_similarity_checker/desktop/good/"],
                    YandexFullpageSimilarityChecker(), .9, "yandex fullpage similarity checker: desktop")

    def test_fscore_on_mobile_screenshots(self):
        self.fscore(["/yandex_fullpage_similarity_checker/mobile/bad/"],
                    ["/yandex_fullpage_similarity_checker/mobile/good/"],
                    YandexFullpageSimilarityChecker(), .7, "yandex fullpage similarity checker: mobile", ["jpg"])
