from test_utils import TestParser


class TestYandexRimJSONParser(TestParser):

    def test_size(self):
        components = self.read_components('data.json')
        assert len(components) == 10

    def test_content(self):
        components_ch = self.read_components('data.json')
        components_co = self._read_json_file('components.json')['components']
        assert len(components_ch) == len(components_co)
        content_keys = ['text.title', 'componentUrl', 'imageCandidates']
        for component_ch, component_co in zip(components_ch, components_co):
            for key in content_keys:
                val_ch = component_ch.get(key)
                val_co = component_co.get(key)
                assert val_ch == val_co
