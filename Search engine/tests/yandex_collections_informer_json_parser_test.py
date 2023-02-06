# -*- coding: utf-8 -*-
import json

from test_utils import TestParser, compare_urls


class TestYandexCollectionsInformerJSONParser(TestParser):

    def test_load(self):
        self._test_load_impl("informer.json", 15)
        self._test_load_impl("informer_with_features.json", 18)

    def test_parser(self):
        self._test_parser_impl("informer.json")
        self._test_parser_impl("informer_with_features.json")

    def test_preparer(self):
        test_data = self._read_json_file("preparer_test.json")
        preparer = self._get_parser_class()()
        for test in test_data:
            compare_urls(preparer._prepare_url(test["basket_query"], test["host"]), test["expected_url"])

    def _test_load_impl(self, filename, component_count):
        components = self.parse_file(filename)["components"]
        assert components
        assert len(components) == component_count

    def _test_parser_impl(self, filename):
        components = self.parse_file(filename)["components"]
        for board in components:
            assert board.get("componentUrl") is not None
            assert board.get("componentUrl").get("pageUrl") is not None

            assert board.get("text.title") is not None
            assert board.get("text.snippet") is not None
            assert board.get("imageadd") is not None
            image_url = board.get("imageadd").get("url")
            assert image_url is not None
            assert image_url == board.get("url.imageUrl")
            assert image_url == board.get("url.mimcaMdsUrl")

            if board.get("text.snippet") in self.relevance_values:
                ref_relevance = self.relevance_values[board.get("text.snippet")]
                if ref_relevance is None:
                    assert board.get("judgements.b2u_feature_relevance") is None
                else:
                    assert board.get("judgements.b2u_feature_relevance")["ts"] is not None
                    assert board.get("judgements.b2u_feature_relevance")["scale"] == "b2u_feature_relevance"
                    data = json.loads(board.get("judgements.b2u_feature_relevance")["name"])
                    assert data["value"] < ref_relevance + 0.001 and data["value"] > ref_relevance - 0.001
                    assert data.get("timestamp") is not None
                    assert data["name"] == "Relevance"

    relevance_values = {
        "5ca35385f4aa2500836ae638": 0.6638477445,
        "5c6eb8eecce5cd0071df9551": 0.349658668,
        "5a970b68acbcf6007ba347c9": -0.8704004288,
        "59c0ac73c75bad00d5ebe19a": None,
        "593551c3215a84c4781016fa": None
    }
