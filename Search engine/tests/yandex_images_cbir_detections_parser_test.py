# -*- coding: utf-8 -*-

from test_utils import TestParser
from base_parsers import JSONSerpParser

ComponentTypes = JSONSerpParser.MetricsMagicNumbers.ComponentTypes
Alignments = JSONSerpParser.MetricsMagicNumbers.Alignments


class TestYandexImagesCbirDetectionsParser(TestParser):

    def test_parse(self):
        assert self.parse_file('crops.json') == self._read_json_file("crops.parsed.json")
