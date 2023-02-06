import wrapper_lib
from test_utils import TestParser


class TestYandexRelatedVideoStageOneParser(TestParser):

    def test_preparer(self):
        parser = self.get_parser()

        basket_query = self._read_json_file("preparer", "stage_one_preparer_input.json")
        params = {
            "cgi": "lang=ru&request=[{\"block\":\"b-page_type_search\"}]"
        }
        prepared = parser.prepare(0, basket_query, "yandex.ru", params)

        expected = self._read_json_file("preparer", "stage_one_preparer_output.json")
        self.compare_preparer_content(prepared, expected)

    def test_parser(self):
        parser = self.get_parser()

        soy_output_line = self._read_json_file("stage_one_soy_output_table.json")
        parsed = wrapper_lib.process_row(parser, soy_output_line)

        assert "id" in parsed
        assert "method" in parsed
        assert "uri" in parsed
        assert "headers" in parsed
        assert len(parsed["headers"]) > 0
        assert "cookies" in parsed
        assert len(parsed["cookies"]) > 0
        assert "userdata" in parsed

        assert parsed["id"] == "2227"

        uri = parsed["uri"]
        assert "relatedVideo=yes" in uri
        assert "related_family=moderate" in uri
        assert "related_porno=null" in uri
        assert "related_orig_text=url%3Ahttp%3A%2F%2Fwww.youtube.com%2Fwatch%3Fv%3DZ7dC-UJpn2A" in uri
        assert "text=%D1%82%D0%BE%D0%BD%D1%8F+%D0%BF%D1%80%D0%BE%D1%82%D0%B8%D0%B2+%D0%B2%D1%81%D0" \
               "%B5%D1%85+%D1%81%D0%BC%D0%BE%D1%82%D1%80%D0%B5%D1%82%D1%8C" in uri
        assert "pron" in uri
