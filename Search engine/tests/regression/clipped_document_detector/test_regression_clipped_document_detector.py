from screenqual.filter.clipped_document_detector.clipped_document_detector import ClippedDocumentDetector
from tests.regression.regression_test import TestRegression


class TestRegressionClippedDocumentDetector(TestRegression):

    def test_fscore_on_desktop_text_documents(self):
        self.fscore(["/document_screenshots_cut/desktop/text/"],
                    ["/document_screenshots/desktop/text/"],
                    ClippedDocumentDetector(), .9, "clipped document detector: desktop, text")

    def test_fscore_on_mobile_text_documents(self):
        self.fscore(["/document_screenshots_cut/mobile/text/"],
                    ["/document_screenshots/mobile/text/"],
                    ClippedDocumentDetector(), .9, "clipped document detector: mobile, text")

    def test_fscore_on_desktop_verticals_documents(self):
        self.fscore(["/document_screenshots_cut/desktop/verticals/"],
                    ["/document_screenshots/desktop/verticals/"],
                    ClippedDocumentDetector(), .9, "clipped document detector: desktop, verticals")

    def test_fscore_on_mobile_verticals_documents(self):
        self.fscore(["/document_screenshots_cut/mobile/verticals/"],
                    ["/document_screenshots/mobile/verticals/"],
                    ClippedDocumentDetector(), .9, "clipped document detector: mobile, verticals")

    def test_fscore_on_desktop_fullpage_with_header(self):
        self.fscore(["/document_screenshots_cut/desktop/fullpage/"],
                    ["/document_screenshots/desktop/fullpage/"],
                    ClippedDocumentDetector(), .9, "clipped fullpage detector: desktop, fullpage")

    def test_fscore_on_mobile_verticals_without_header(self):
        self.fscore(["/document_screenshots_cut/mobile/without_header/"],
                    ["/document_screenshots/mobile/without_header/"],
                    ClippedDocumentDetector(), .9, "clipped fullpage detector: mobile without header")

    def test_fscore_on_mobile_fullpage(self):
        self.fscore(["/document_screenshots_cut/mobile/fullpage/"],
                    ["/document_screenshots/mobile/fullpage/"],
                    ClippedDocumentDetector(), .9, "clipped fullpage detector: mobile, fullpage")
