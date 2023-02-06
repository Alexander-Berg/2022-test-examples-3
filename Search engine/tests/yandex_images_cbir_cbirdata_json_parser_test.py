# -*- coding: utf-8 -*-

from test_utils import TestParser
from base_parsers import JSONSerpParser

ComponentTypes = JSONSerpParser.MetricsMagicNumbers.ComponentTypes
Alignments = JSONSerpParser.MetricsMagicNumbers.Alignments


class TestYandexImagesCbirCbirdataJSONParser(TestParser):

    def test_parse(self):
        components = self.parse_file('serp.json')['components']
        assert len(components) == 12

        true_component = {
            'type': 'COMPONENT',
            'componentInfo': {
                'type': ComponentTypes.SEARCH_RESULT,
                'alignment': Alignments.LEFT
            },
            'componentUrl': {
                'pageUrl': u'https://yandex.uz/collections/card/58c387fa215a84c8f16f4f03/'
            },
            'imageadd': {
                'url': u'https://avatars.mds.yandex.net/get-pdb/27625/06f0b875-c846-4c87-99ac-47837cbfa2a6/s1200?webp=false',
                'candidates': [
                    u'https://avatars.mds.yandex.net/get-pdb/27625/06f0b875-c846-4c87-99ac-47837cbfa2a6/s1200?webp=false'
                ]
            },
            'long.crc': 17642568704154995708,
            'long.image_width': 750,
            'long.image_height': 1125
        }

        assert components[0] == true_component
