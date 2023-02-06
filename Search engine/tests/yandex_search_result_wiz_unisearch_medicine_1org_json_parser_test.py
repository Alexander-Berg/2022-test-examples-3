from test_utils import TestParser


class TestYandexSearchResultWizUnisearchMedicine1OrgJSONParser(TestParser):

    def test_base(self):
        example = self.parse_file("test.json")

        assert example == {'long.wizard_position': 1, 'long.has_data': True}
