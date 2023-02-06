from screenqual.filter.broken_areas.broken_video_detector import BrokenVideosAnalyser
from tests.regression.regression_test import TestRegression


class TestRegressionBrokenVideos(TestRegression):
    def test_fscore_on_broken_videos(self):
        self.fscore(["/broken_videos/bad/"], ["/broken_videos/good/"],
                    BrokenVideosAnalyser(), .88, "broken videos detector")
