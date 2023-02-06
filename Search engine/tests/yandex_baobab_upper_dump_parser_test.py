from test_utils import TestParser

from yandex_baobab.yandex_baobab_upper_dump_parser import YandexBaobabUpperDumpParser


class TestYandexBaobabUpperDumpParser(TestParser):
    _parser_class = YandexBaobabUpperDumpParser

    def test_full_upper_props(self):
        dumped_props = self.parse_file("full_upper_props.html")['dumped_props']
        assert len(dumped_props) == 483
        assert "ApplyBlender.fmls" in dumped_props

    def test_apply_blender_fmls(self):
        dumped_props = self.parse_file("apply_blender_fmls.html")['dumped_props']
        assert len(dumped_props) == 1
        assert "ApplyBlender.fmls" in dumped_props
