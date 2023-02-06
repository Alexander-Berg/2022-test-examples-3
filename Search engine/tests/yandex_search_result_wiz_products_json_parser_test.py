import pytest

from test_utils import TestParser


class TestYandexSearchResultWizProductsJSONParser(TestParser):
    @pytest.mark.parametrize("path, expected_count", [
        ("test.json", 2),
    ])
    def test_component_count(self, path, expected_count):
        components = self.read_components(path)
        assert len(components) == expected_count

    @pytest.mark.parametrize("path, component, expected_title", [
        ("test.json", 0, "Смартфон Apple \u0007[iPhone\u0007] \u0007[13\u0007] 128GB Global, тёмная ночь  черный"),
        ("test.json", 1, "Apple \u0007[iPhone\u0007] \u0007[13\u0007] 128GB Starlight  cияющая звезда"),
    ])
    def test_component_title(self, path, component, expected_title):
        components = self.read_components(path)
        assert components
        assert components[component].get("text.title") == expected_title

    @pytest.mark.parametrize("path, component, sku_id", [
        ("test.json", 0, 101446177750),
        ("test.json", 1, None),
    ])
    def test_component_sku_id(self, path, component, sku_id):
        components = self.read_components(path)
        assert components
        assert components[component].get("long.skuId") == sku_id

    @pytest.mark.parametrize("path, component, sku_offer_count", [
        ("test.json", 0, 54),
        ("test.json", 1, None),
    ])
    def test_component_sku_offer_count(self, path, component, sku_offer_count):
        components = self.read_components(path)
        assert components
        assert components[component].get("long.skuOffersCount") == sku_offer_count

    @pytest.mark.parametrize("path, component, show_uid", [
        ("test.json", 0, "1"),
        ("test.json", 1, "2"),
    ])
    def test_component_url(self, path, component, show_uid):
        components = self.read_components(path)
        assert components
        assert components[component].get("text.showuid") == show_uid
