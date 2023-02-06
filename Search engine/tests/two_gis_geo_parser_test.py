import pytest

from test_utils import TestParser


class TestTwoGisGeoParser(TestParser):

    @pytest.mark.parametrize("input_filename,expected_output_filename", [
        ("0_input.json", "0_expected_output.json")
    ])
    def test_preparer(self, input_filename, expected_output_filename):
        self.compare_preparer_output(input_filename, expected_output_filename)

    @pytest.mark.parametrize("input_filename,expected_output_filename", [
        ("0_empty.html", "0_expected_output.json"),
        ("1_single_house.html", "1_expected_output.json"),
        ("2_several_verticals.html", "2_expected_output.json"),
        ("3_orgs.html", "3_expected_output.json"),
    ])
    def test_parse(self, input_filename, expected_output_filename):
        parse_result = self.parse_file(input_filename)
        expected_output = self._read_json_file(expected_output_filename)
        assert parse_result == expected_output

    def test_zoom_param(self):
        parser = self.get_parser()
        # zoom in [2, 20]
        assert parser._get_zoom({'spnX': 0.48, 'zoom': 0}) == 11.55
        assert parser._get_zoom({'spnX': 365, 'zoom': 0}) == 2
        assert parser._get_zoom({'spnX': 0.0008, 'zoom': 0}) == 20

        assert parser._get_zoom({'spnX': 0.48, 'zoom': 12}) == 12
        assert parser._get_zoom({'spnX': 0.48, 'zoom': 1}) == 2
        assert parser._get_zoom({'spnX': 0.48, 'zoom': 22}) == 20

        assert parser._get_zoom({'spnX': 0.48}) == 11.55

        # default zoom 14
        assert parser._get_zoom({}) == 14
