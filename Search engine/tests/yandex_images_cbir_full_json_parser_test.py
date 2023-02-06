# -*- coding: utf-8 -*-

from test_utils import TestParser
from base_parsers import JSONSerpParser

ComponentTypes = JSONSerpParser.MetricsMagicNumbers.ComponentTypes
Alignments = JSONSerpParser.MetricsMagicNumbers.Alignments


class TestYandexImagesCbirFullJSONParser(TestParser):

    def test_parse(self):
        assert self.parse_file('serp.json') == self._read_json_file("serp.parsed.json")
        assert self.parse_file('serp_empty.json') == self._read_json_file("serp_empty.parsed.json")
        assert self.parse_file('serp_market.json') == self._read_json_file("serp_market.parsed.json")
        assert self.parse_file('serp_market_inactive.json') == self._read_json_file("serp_market_inactive.parsed.json")
        assert self.parse_file('serp_clothes.json') == self._read_json_file("serp_clothes.parsed.json")
