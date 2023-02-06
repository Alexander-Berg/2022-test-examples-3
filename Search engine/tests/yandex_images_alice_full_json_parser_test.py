# -*- coding: utf-8 -*-

from test_utils import TestParser
from base_parsers import JSONSerpParser

ComponentTypes = JSONSerpParser.MetricsMagicNumbers.ComponentTypes
Alignments = JSONSerpParser.MetricsMagicNumbers.Alignments


class TestYandexImagesAliceFullJSONParser(TestParser):

    def test_parse(self):
        assert self.parse_file('market_serp.json') == self._read_json_file("market_serp.parsed.json")
        assert self.parse_file('entity_serp.json') == self._read_json_file("entity_serp.parsed.json")
        assert self.parse_file('clothes_serp.json') == self._read_json_file("clothes_serp.parsed.json")
        assert self.parse_file('barcode_serp.json') == self._read_json_file("barcode_serp.parsed.json")
        assert self.parse_file('ocr_serp.json') == self._read_json_file("ocr_serp.parsed.json")
        assert self.parse_file('museum_serp.json') == self._read_json_file("museum_serp.parsed.json")
