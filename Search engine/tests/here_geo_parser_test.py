from test_utils import TestParser


class TestHereGeoParser(TestParser):

    def data(self):
        return self.parse_file("data.json")

    def test_data_component_count(self):
        components = self.data()['components']
        assert 10 == len(components)

    def test_data_title(self):
        expected_title = "Moscow, WI, United States"
        actual_title = self.data()['components'][0]['title']
        assert expected_title == actual_title

    def test_data_coordinates(self):
        component = self.data()['components'][0]
        assert 42.8486 == component['latitude']
        assert -89.88936 == component['longitude']

    def test_empty_view(self):
        assert 0 == len(self.parse_file("empty_view.json")['components'])
