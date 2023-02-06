from test_utils import TestParser


class TestYandexWizuslugiJSONParser(TestParser):
    def example(self):
        return self.parse_file("test.json")

    def test_example_url(self):
        component = self.example()["components"][0]
        url = "https://yandex.ru/uslugi/search?worker_id=dfbb9c37-c435-411f-81fd-f9019dc6136f"
        pos = "0"

        assert url == component["page-url"]
        assert pos == component["rank"]
