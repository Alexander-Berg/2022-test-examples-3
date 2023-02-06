import pytest

from test_utils import TestParser


class TestYandexUslugiSaasServiceCardParser(TestParser):
    def example(self):
        return self.parse_file("test.json")

    def test_first_component(self):
        component = self.example()["components"][0]
        expected_component = {
            "page-url": "https://yandex.ru/uslugi/search?worker_id=2a94df85-ede4-4295-89f3-6e188abadd76",
            "snippet": "",
            "title": "Занятие с репетитором по математике",
            "type": "SEARCH_RESULT",
            "rank": "0",
            "alignment": "LEFT",
            "media-links": [],
            "site-links": []
        }

        cnt = 2194

        assert expected_component == component
        assert cnt == self.example()["documents-found"]

    def test_second_component(self):
        component = self.example()["components"][2]
        expected_component = {
            "page-url": "https://yandex.ru/uslugi/search?worker_id=b40bb6c6-958c-42dc-91d3-3dd39e0063fd",
            "snippet": "1500р/час + 500р/каждые следующие 30 мин.\n+1000р/выезд к ученику",
            "title": "Занятие с репетитором по математике",
            "type": "SEARCH_RESULT",
            "rank": "1",
            "alignment": "LEFT",
            "media-links": [],
            "site-links": []
        }

        cnt = 2194

        assert expected_component == component
        assert cnt == self.example()["documents-found"]

    @pytest.mark.parametrize("input_filename,expected_output_filename", [
        ("0_input.json", "0_expected_output.json"),
        ("1_input.json", "1_expected_output.json"),
        ("2_input.json", "2_expected_output.json"),
    ])
    def test_preparer(self, input_filename, expected_output_filename):
        self.compare_preparer_output(input_filename, expected_output_filename)
