from screenqual.filter.clipped_document_detector.clipped_document_detector import ClippedDocumentDetector
from tests.unit.unit_test import UnitTest


class TestClippedDocumentDetector(UnitTest):
    def setUp(self):
        self.analyser = ClippedDocumentDetector()

    def test_does_not_fire_on_good_text_document_screenshots(self):
        self.assert_no_anomaly("clipped_document_detector/ok.png")

    def test_does_not_fire_on_good_text_document_screenshot2(self):
        self.assert_no_anomaly("clipped_document_detector/1_14.png")

    def test_does_not_fire_on_good_text_document_screenshot3(self):
        self.assert_no_anomaly("clipped_document_detector/9_2.png")

    def test_does_not_fire_on_scrolling_images_vertical_document_screenshot(self):
        self.assert_no_anomaly("clipped_document_detector/5_0.png")

    def test_fires_on_scrolling_videos_vertical_document_screenshot(self):
        self.assert_has_anomaly("clipped_document_detector/5_7.png")

    def test_fires_on_text_document_screenshot_with_text_crossing_horizontal_line(self):
        self.assert_has_anomaly("clipped_document_detector/horizontal.png")

    def test_fires_on_text_document_screenshot_with_text_crossing_vertical_line(self):
        self.assert_has_anomaly("clipped_document_detector/vertical.png")

    def test_does_not_fire_on_text_document_screenshot_with_picture_crossing_vertical_line(self):
        self.assert_has_anomaly("clipped_document_detector/8_3.png")

    # def test_does_not_fire_on_text_document_screenshot_with_one_letter_near_vertical_line(self):
    #     self.assert_no_anomaly("clipped_document_detector/8_4_4.png")

    def test_on_text_document_screenshot_with_a_lot_of_text_near_vertical_line(self):
        self.assert_has_anomaly("clipped_document_detector/0_10_1.png")

    def test_on_text_fullpage_screenshot_without_header_horizontal(self):
        self.assert_has_anomaly("clipped_document_detector/b0kpcm.png")

    def test_good_fullpage_screenshot(self):
        self.assert_no_anomaly("clipped_document_detector/2.png")

    def test_good_fullpage_screenshot_without_header_many_img(self):
        self.assert_no_anomaly("clipped_document_detector/img.png")

    def test_on_text_fullpage_screenshot_without_header_vertical(self):
        self.assert_has_anomaly("clipped_document_detector/new_img.png")

    def test_on_text_fullpage_screenshot_fullpage_good(self):
        self.assert_no_anomaly("clipped_document_detector/full_page.png")

    def test_on_text_fullpage_screenshot_without_header_good(self):
        self.assert_no_anomaly("clipped_document_detector/without_header.png")

    def test_on_text_fullpage_screenshot_fullpage_mobile_good(self):
        self.assert_no_anomaly("clipped_document_detector/3.png")

    def test_on_text_fullpage_screenshot_fullpage_small_cut(self):
        self.assert_has_anomaly("clipped_document_detector/cut2.png")

    def test_on_text_fullpage_screenshot_fullpage_vertical(self):
        self.assert_has_anomaly("clipped_document_detector/cut_img1.png")

    def test_on_text_fullpage_screenshot_fullpage_horizontal(self):
        self.assert_has_anomaly("clipped_document_detector/cut_top1.png")
