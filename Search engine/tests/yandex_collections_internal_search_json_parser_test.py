# -*- coding: utf-8 -*-

from test_utils import TestParser, cleanup_falsy


class TestYandexCollectionsInternalSearchJSONParser(TestParser):

    def test_multiple_collections(self):
        parsed = cleanup_falsy(self.parse_file("serp.json"))
        expected = cleanup_falsy(self._read_json_file("parser_output.json"))

        assert parsed == expected
