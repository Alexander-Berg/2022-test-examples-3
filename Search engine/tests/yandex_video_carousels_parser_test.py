# -*- coding: utf-8 -*-

from test_utils import TestParser


class TestYandexVideoCarouselsParser(TestParser):

    def test_parse(self):
        components = self.parse_file('data.json')['components']

        print("components are")
        print(components)

        assert len(components) == 100
        assert components[0]['text.cardId'] == "ruw284825"
        assert components[1]['text.onto_id'] == "ruw273195"
