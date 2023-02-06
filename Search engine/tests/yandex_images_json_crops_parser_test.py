from test_utils import TestParser


class TestYandexImagesJsonCropsParser(TestParser):

    def test_size(self):
        components = self.read_components('data.json')
        assert len(components) == 40

    def test_content(self):
        components_ch = self.read_components('data.json')
        components_co = self._read_json_file('components.json')['components']
        assert len(components_ch) == len(components_co)
        for component_ch, component_co in zip(components_ch, components_co):
            assert component_ch.get('json.crops_info') == component_co.get('json.crops_info')

    def test_size_qcomm(self):
        components = self.read_components('data_qcomm.json')
        assert len(components) == 83

    def test_content_qcomm(self):
        components_ch = self.read_components('data_qcomm.json')
        components_co = self._read_json_file('components_qcomm.json')['components']
        assert len(components_ch) == len(components_co)
        for component_ch, component_co in zip(components_ch, components_co):
            assert component_ch.get('json.crops_info') == component_co.get('json.crops_info')
