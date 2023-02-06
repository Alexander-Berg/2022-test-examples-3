from screenqual.filter.broken_areas.broken_video_detector import BrokenVideosAnalyser
from tests.unit.unit_test import UnitTest


class TestBrokenImagesDetector(UnitTest):
    def setUp(self):
        self.analyser = BrokenVideosAnalyser()

    def test_does_not_fire_on_good_screenshots(self):
        self.assert_no_anomaly("broken_videos/imgs/ok/1.png")

    def test_fires_on_screenshots_with_bad_yandex_images(self):
        self.assert_has_anomaly("broken_videos/imgs/not_ok/1.png")
