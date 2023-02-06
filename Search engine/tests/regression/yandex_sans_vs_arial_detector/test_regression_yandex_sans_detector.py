from screenqual.filter.yandex_sans_vs_arial_detector.yandex_sans_vs_arial_detector import YandexSansVsArialDetector
from tests.regression.regression_test import TestRegression
import cv2
import random
import unittest


def resize(filename, img):
    fx = random.gauss(1.0, 0.3)
    fy = fx + random.gauss(0.0, 0.1)
    fx = min(max(0.5, fx), 2.0)
    fy = min(max(0.5, fy), 2.0)
    return cv2.resize(img, None, fx=fx, fy=fy)


class TestRegressionYandexSansVsArialDetector(TestRegression):
    def test_fscore_on_ys_vs_arial_db1(self):
        self.fscore(["/ys/db1/arial/"], ["/ys/db1/ys/"],
                    YandexSansVsArialDetector(), .9, "yandex sans detector: db1 vs arial", ["jpg"], resize)

    def test_fscore_on_ys_vs_helvetica_db1(self):
        self.fscore(["/ys/db1/helvetica/"], ["/ys/db1/ys/"],
                    YandexSansVsArialDetector(), .9, "yandex sans detector: db1 vs helvetica", ["jpg"], resize)

    @unittest.skip("Too much for Travis CI")
    def test_fscore_on_ys_vs_arial_db2(self):
        self.fscore(["/ys/db1/arial/"], ["/ys/db1/ys/"],
                    YandexSansVsArialDetector(), .9, "yandex sans detector: db2 vs arial", ["jpg"], resize)

    @unittest.skip("Too much for Travis CI")
    def test_fscore_on_ys_vs_helvetica_db2(self):
        self.fscore(["/ys/db1/helvetica/"], ["/ys/db1/ys/"],
                    YandexSansVsArialDetector(), .9, "yandex sans detector: db2 vs helvetica", ["jpg"], resize)
