# -*- coding: utf-8 -*-

from test_utils import TestParser


class TestYandexOrgProfileParser(TestParser):

    def get_parsed_profile(self):
        return self.parse_file("test.json")

    def test_profile_component_count(self):
        components = self.get_parsed_profile()['components']
        assert 1 == len(components)

    def test_profile_snippet(self):
        expected_snippet = u"Россия, Москва, площадь Европы, 2<br/>+7 (495) 941-80-20<br/>ежедневно, круглосуточно<br/>Гостиница"
        actual_snippet = self.get_parsed_profile()['components'][0]['snippet']
        assert expected_snippet == actual_snippet

    def test_profile_coordinates(self):
        component = self.get_parsed_profile()['components'][0]
        assert 37.568099 == component['longitude']
        assert 55.741881 == component['latitude']
