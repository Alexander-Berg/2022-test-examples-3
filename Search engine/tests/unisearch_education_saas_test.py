from test_utils import TestParser


class TestUnisearchEducationSaas(TestParser):

    def test_base(self):
        example = self.parse_file("test.json")
        url_0 = "https://intuit.ru/studies/courses/12179/1172/info"
        title_0 = "Введение в программирование на Python"
        url_2 = "https://www.specialist.ru/course/python-bi"
        title_2 = "Python для бизнес - аналитики"
        cnt = 13
        assert url_0 == example["components"][0]["componentUrl"]["pageUrl"]
        assert title_0 == example["components"][0]["text.title"]
        assert url_2 == example["components"][2]["componentUrl"]["pageUrl"]
        assert title_2 == example["components"][2]["text.title"]
        assert cnt == example["long.docsFound"]
