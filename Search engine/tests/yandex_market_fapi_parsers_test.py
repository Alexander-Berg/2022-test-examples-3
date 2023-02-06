import pytest

from test_utils import TestParser


class TestMarketFapiParser(TestParser):
    @pytest.mark.parametrize("path", [
        "honor.json",
    ])
    def test_touch_knowledge_graph(self, path):
        parsed = self.parse_file(path)
        components = parsed["components"]
        assert components
