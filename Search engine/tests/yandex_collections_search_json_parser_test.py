# -*- coding: utf-8 -*-

from test_utils import TestParser, cleanup_falsy


class TestYandexCollectionsSearchJSONParser(TestParser):

    def test_multiple_collections(self):
        parsed = cleanup_falsy(self.parse_file("multiple_collections.json"))
        expected = cleanup_falsy(self._read_json_file("multiple_collections_output.json"))

        assert parsed == expected

    def test_no_16x9_thumb(self):
        component = self.parse_file("no_16x9_thumb.json")["components"][0]

        assert component["imageadd"]["url"] == \
            "https://avatars.mds.yandex.net/get-pdb-teasers/image-not-found/thumb"

    def test_empty_result(self):
        components = self.parse_file("empty_result.json")["components"]

        assert len(components) == 0
