from test_utils import TestParser


class TestYandexSearchResultWizUnisearchJobJSONParser(TestParser):

    def test_base(self):
        example = self.parse_file("test.json")
        url_0 = "https://hh.ru/vacancy/54726130"

        assert url_0 == example["components"][0]["componentUrl"]["pageUrl"]
