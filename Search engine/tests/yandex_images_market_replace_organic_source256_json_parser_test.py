from test_utils import TestParser


class TestYandexImagesMarketReplaceOrganicSource256JSONParser(TestParser):

    def test_size_commercial(self):
        components = self.read_components('data_commercial.json')
        assert len(components) == 80

    def test_size_not_commercial(self):
        components = self.read_components('data_not_commercial.json')
        assert len(components) == 56

    def test_content_commercial(self):
        components_ch = self.read_components('data_commercial.json')
        components_co = self._read_json_file('components_commercial.json')['components']
        assert len(components_ch) == len(components_co)
        content_keys = ['text.title', 'text.source', 'text.snippet', 'componentUrl', 'imageCandidates']
        for component_ch, component_co in zip(components_ch, components_co):
            for key in content_keys:
                val_ch = component_ch.get(key)
                val_co = component_co.get(key)
                assert val_ch == val_co

    def test_content_not_commercial(self):
        components_ch = self.read_components('data_not_commercial.json')
        components_co = self._read_json_file('components_not_commercial.json')['components']
        assert len(components_ch) == len(components_co)
        content_keys = ['text.title', 'text.source', 'text.snippet', 'componentUrl', 'imageCandidates']
        for component_ch, component_co in zip(components_ch, components_co):
            for key in content_keys:
                val_ch = component_ch.get(key)
                val_co = component_co.get(key)
                assert val_ch == val_co
