# -*- coding: utf-8 -*-

from test_utils import TestParser


class TestYandexVideoOttDelayedParser(TestParser):

    def test_parse(self):
        components = self.parse_file('data.json')['components']

        print("components are")
        print(components)

        assert len(components) == 20
        assert components[0]['text.filmId'] == "4e7c6d933dff14428eaa7b1522a00c7f"
        assert components[1]['text.duration'] == 3265
