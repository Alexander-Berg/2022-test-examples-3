from test_utils import TestParser


class TestDummyHTMLParser(TestParser):

    def dummy(self):
        return self.parse_file("dummy.html")

    def test_dummy_title(self):
        assert "Flying Circus" == self.dummy()['title']
