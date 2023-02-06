from screenqual.filter.yandex_sans_vs_arial_detector.yandex_sans_vs_arial_detector import YandexSansVsArialDetector
from tests.unit.unit_test import UnitTest


class TestYandexSansVsArialDetector(UnitTest):
    def setUp(self):
        self.analyser = YandexSansVsArialDetector()

    def test_does_not_fire_on_cut_ys(self):
        self.assert_no_anomaly("yandex_sans_vs_arial_detector/tiny_ys.png")

    def test_fires_on_fullpage_ys(self):
        self.assert_no_anomaly("yandex_sans_vs_arial_detector/fullpage_ys.png")

    def test_fires_on_cut_arial(self):
        self.assert_has_anomaly("yandex_sans_vs_arial_detector/tiny_arial.png")

    def test_fires_on_fullpage_arial(self):
        self.assert_has_anomaly("yandex_sans_vs_arial_detector/fullpage_arial.png")
