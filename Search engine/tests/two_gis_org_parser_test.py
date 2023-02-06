import pytest

from test_utils import TestParser


class TestTwoGisOrgParser(TestParser):

    @pytest.mark.parametrize("input_filename,expected_output_filename", [
        ("0_input.json", "0_expected_output.json")
    ])
    def test_preparer(self, input_filename, expected_output_filename):
        self.compare_preparer_output(input_filename, expected_output_filename)

    @pytest.mark.parametrize("input_filename,expected_output_filename", [
        ("two_gis_firm.html", "two_gis_firm_expected.json"),
        ("two_gis_serp.html", "two_gis_serp_expected.json"),
        ("two_gis_serp_single.html", "two_gis_serp_single_expected.json"),
        ("two_gis_toponym.html", "two_gis_toponym_expected.json"),
        ("two_gis_toponym_serp.html", "two_gis_toponym_serp_expected.json"),
    ])
    def test_parse(self, input_filename, expected_output_filename):
        parse_result = self.parse_file(input_filename)
        expected_output = self._read_json_file(expected_output_filename)
        assert parse_result == expected_output
