# -*- coding: utf-8 -*-

from test_utils import TestParser
from base_parsers import JSONSerpParser

ComponentTypes = JSONSerpParser.MetricsMagicNumbers.ComponentTypes
Alignments = JSONSerpParser.MetricsMagicNumbers.Alignments


class TestYandexImagesCbirJSONParser(TestParser):

    def test_parse(self):
        components = self.parse_file('serp.json')['components']
        assert len(components) == 54

        true_component = {
            'type': 'COMPONENT',
            'componentInfo': {
                'type': ComponentTypes.SEARCH_RESULT,
                'alignment': Alignments.LEFT
            },
            'componentUrl': {
                'pageUrl': u'http://takskazat.ru/shutka_yumor/2011/02/07/zabavnye-zhivotnye.html'
            },
            'imageadd': {
                'url': u'http://www.takskazat.ru/uploads/images/00/00/01/2012/02/07/b8804047e5.jpg',
                'candidates': [
                    u'http://www.takskazat.ru/uploads/images/00/00/01/2012/02/07/b8804047e5.jpg'
                ]
            },
            'long.crc': 444124827116841429,
            'long.image_width': 500,
            'long.image_height': 320
        }

        assert components[0] == true_component
