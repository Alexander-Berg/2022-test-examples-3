from screenqual.filter.banner_detector.banner_detector import BannerDetector
from tests.unit.unit_test import UnitTest


class TestBannerDetector(UnitTest):
    analyser = BannerDetector()

    def test_img_with_banner_common(self):
        self.assert_has_anomaly('banner_detector/img/with_banner/2.jpg')

    def test_img_with_banner_elongated(self):
        self.assert_has_anomaly('banner_detector/img/with_banner/1.jpg')

    def test_img_with_banner_other_kind_1(self):
        self.assert_has_anomaly('banner_detector/img/with_banner/3.jpg')

    def test_img_with_banner_other_kind_2(self):
        self.assert_has_anomaly('banner_detector/img/with_banner/4.jpg')

    def test_img_without_banner_kind_1(self):
        self.assert_no_anomaly('banner_detector/img/without_banner/1.jpg')

    def test_img_without_banner_elongated(self):
        self.assert_no_anomaly('banner_detector/img/without_banner/2.jpg')

    def test_img_without_banner_kind_2(self):
        self.assert_no_anomaly('banner_detector/img/without_banner/3.jpg')
