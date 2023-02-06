# -*- coding: utf-8 -*-

from test_utils import TestParser


class TestYandexCollectionsSimilarBoardsJSONParser(TestParser):

    def test_boards(self):
        components = self.parse_file("boards.json")["components"]

        assert len(components) == 12
        assert all(["text.snippet" in x for x in components])
        assert all(["text.title" in x for x in components])
        assert all(["text.rubrics" in x for x in components])

    def test_boards_images(self):
        components = self.parse_file("boards.json")["components"]

        assert all(["url.imageUrl" in x for x in components])
        assert all(["https://avatars.mds.yandex.net/get-pdb-teasers/" in x["url.imageUrl"] for x in components])

    def test_boards_ids(self):
        components = self.parse_file("boards.json")["components"]

        assert all([len(x["text.snippet"]) == 24 for x in components])
