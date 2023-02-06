import pytest

from test_utils import TestParser


class TestYandexClassifiedsPreparer(TestParser):
    @pytest.mark.parametrize("input_filename,expected_output_filename", [
        ("0_input.json", "0_expected_output.json"),
        ("1_input.json", "1_expected_output.json"),
        ("2_input.json", "2_expected_output.json"),
    ])
    def test_preparer(self, input_filename, expected_output_filename):
        self.compare_preparer_output(input_filename, expected_output_filename, host='o.yandex.ru')
