from test_utils import TestParser


class TestYandexSearchGeoCommonWizardJSONParser(TestParser):

    def example(self):
        return self.parse_file("moscow_piter.json")

    def test_example_order(self):
        component = self.example()["components"][0]
        expected_order = "avia"
        assert expected_order == component["order"]
