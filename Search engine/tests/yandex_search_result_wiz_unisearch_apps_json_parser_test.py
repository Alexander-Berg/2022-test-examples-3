from test_utils import TestParser


class TestYandexSearchResultWizUnisearchAppsJSONParser(TestParser):

    def test_base(self):
        example = self.parse_file("test.json")
        url_0 = "https://apps.apple.com/ru/app/id1055928034?l=ru"
        url_2 = "https://apps.apple.com/ru/app/id1140975072?l=ru"

        assert url_0 == example["components"][0]["componentUrl"]["pageUrl"]
        assert url_2 == example["components"][2]["componentUrl"]["pageUrl"]
