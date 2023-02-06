# -*- coding: utf-8 -*-

from test_utils import TestParser


class TestYandexVideoAliceJSONParser(TestParser):

    def test_search_result(self):
        parsed = self.parse_file('main_results.json')
        assert parsed['text.onto_type'] == "series"
        assert parsed['components'][0]['componentUrl']['viewUrl'] == "kinopoisk.ru"
        assert parsed['components'][0]['componentUrl']['viewUrl'] == "kinopoisk.ru"
        dur = parsed['components'][2]['text.videoDuration']
        assert (int(dur.split(':')[0]) * 60 + int(dur.split(':')[1])) == 1185

        collections = self.parse_file('collections.json')
        assert collections["text.show_parent_collection"] == 1
        assert collections['components'][0]['componentUrl']['pageUrl'] == "https://frontend.vh.yandex.ru/player/446ede715f6a9732b82d822e048a2f17"
        assert collections['components'][1]['text.videoOntoId'] == "ruw6815260"
