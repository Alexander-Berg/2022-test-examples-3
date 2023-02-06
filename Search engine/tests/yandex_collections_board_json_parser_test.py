# -*- coding: utf-8 -*-

from test_utils import TestParser, cleanup_falsy


class TestYandexCollectionsBoardJSONParser(TestParser):

    def test_empty(self):
        components = self.parse_file("empty.json")["components"]

        assert components is not None
        assert len(components) == 0

    def test_image_and_video(self):
        parsed = cleanup_falsy(self.parse_file("image_and_video.json"))
        expected = cleanup_falsy(self._read_json_file("image_and_video_output.json"))

        assert parsed == expected

    def test_not_full_info(self):
        parsed = cleanup_falsy(self.parse_file("not_full_info.json"))
        expected = cleanup_falsy(self._read_json_file("not_full_info_output.json"))

        assert parsed == expected

    def test_width_and_heght(self):
        component = self.parse_file("width_and_height.json")["components"][0]
        dimensions = component["site-links"][0]["dimension.imageDimension"]

        assert dimensions["w"] == 1600
        assert dimensions["h"] == 1200
