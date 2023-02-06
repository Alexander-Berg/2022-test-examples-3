import pytest
from test_utils import TestParser


class TestYandexClassifiedsParser(TestParser):
    @pytest.mark.parametrize("input_filename,output_filename", [
        ("test.html", "output.json")
    ])
    def test_parser(self, input_filename, output_filename):
        parse_result = self.parse_file(input_filename)
        expected_output = self._read_json_file(output_filename)
        assert parse_result == expected_output
