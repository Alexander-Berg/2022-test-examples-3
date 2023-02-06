from test_utils import TestParser

from yandex_baobab.yandex_baobab_scraper_parser import YandexBaobabScraperParser


class TestYandexBaobabScraperParser(TestParser):
    _parser_class = YandexBaobabScraperParser

    def test_component_count(self):
        components = self.parse_file("test.html")['components']
        assert 11 == len(components)

    def test_snippet(self):
        components = self.parse_file("test.html")['components']
        for component in components:
            snippet = component.get("snippet")
            assert snippet and snippet != ""

    def test_data_cid(self):
        component = self.parse_file("test.html")['components'][0]
        assert component["serp-component-debug-dump"]["data-cid"] == 0

    def test_protocol(self):
        components = self.parse_file("protocol.html")['components']
        for component in components:
            check_protocol(component["page-url"])

    def test_wizard_transport(self):
        component = self.parse_file("wizard_transport.html")['components'][0]
        assert component['type'] == "WIZARD"
        assert component['wizardType'] == "WIZARD_TRANSPORT"

    def test_wizard_panorama(self):
        component = self.parse_file("wizard_panorama.html")['components'][1]
        assert component['type'] == "WIZARD"
        assert component['wizardType'] == "WIZARD_PANORAMA"

    def test_wizard_maps(self):
        component = self.parse_file("wizard_maps.html")['components'][0]
        assert component['type'] == "WIZARD"
        assert component['wizardType'] == "WIZARD_MAPS"

    def test_wizard_route(self):
        component = self.parse_file("wizard_route.html")['components'][0]
        assert component["type"] == "WIZARD"
        assert component["wizardType"] == "WIZARD_ROUTE"

    def test_wizard_orgmn(self):
        component = self.parse_file("wizard_orgmn.html")['components'][0]
        assert component["type"] == "WIZARD"
        assert component["wizardType"] == "WIZARD_ORGMN"


def check_protocol(url):
    assert url.startswith("http://") or url.startswith("https://")
