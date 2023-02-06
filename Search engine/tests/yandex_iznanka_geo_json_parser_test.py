# -*- coding: utf-8 -*-

from test_utils import TestParser


class TestYandexIznankaGeoJSONParser(TestParser):

    def pushkin(self):
        return self.parse_file("pushkin.json")

    def test_pushkin_component_count(self):
        components = self.pushkin()['components']
        assert 1 == len(components)

    def test_pushkin_snippet(self):
        expected_snippet = u"Россия, Москва, Тверской бульвар, 26А<br/>+7 (495) 739-00-33<br/>ежедневно, круглосуточно<br/>Ресторан<br/>Кафе<br/>Банкетный зал"
        actual_snippet = self.pushkin()['components'][0]['snippet']
        assert expected_snippet == actual_snippet

    def test_pushkin_coordinates(self):
        component = self.pushkin()['components'][0]
        assert 37.604949 == component['longitude']
        assert 55.763737 == component['latitude']
