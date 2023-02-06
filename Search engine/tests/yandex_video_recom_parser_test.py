# -*- coding: utf-8 -*-

from test_utils import TestParser


class TestYandexVideoRecommendationsParser(TestParser):

    def test_parse(self):
        components = self.parse_file('data.json')['components']

        print("components are")
        print(components)

        assert len(components) == 50
        assert components[0]['text.cardId'] == "45ae6b26dd0141c882f08c5307f5e7fb"
        assert components[0]['componentUrl']['viewUrl'] == 'frontend.vh.yandex.ru'
        assert components[1]['text.duration'] == 212
