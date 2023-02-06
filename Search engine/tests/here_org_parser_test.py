# coding: utf-8

from test_utils import TestParser


class TestHereOrgParser(TestParser):

    def data(self):
        return self.parse_file("data.json")

    def test_data_component_count(self):
        components = self.data()['components']
        assert 1 == len(components)

    def test_data_title(self):
        expected_title = u"Pushkin Konditerskaya (Кафе Пушкинь)"
        actual_title = self.data()['components'][0]['title']
        assert expected_title == actual_title

    def test_data_snippet(self):
        expected_snippet = "Tverskoy bul'var, 26<br/>Moskva<br/>125009"
        actual_snippet = self.data()['components'][0]['snippet']
        assert expected_snippet == actual_snippet

    def test_data_coordinates(self):
        component = self.data()['components'][0]
        assert 55.76345 == component['latitude']
        assert 37.60447 == component['longitude']

    # def test_empty_view(self):
    #    assert 0 == len(self.parse_file("empty_view.json")['components'])
