import pytest

from test_utils import TestParser


class TestYandexProductsParser(TestParser):
    @pytest.mark.parametrize("path, expected_count", [
        ("test_products.json", 4),
        ("test_images_pv.json", 3),
    ])
    def test_component_count(self, path, expected_count):
        components = self.read_components(path)
        assert len(components) == expected_count

    @pytest.mark.parametrize("path, component, expected_title", [
        ("test_products.json", 0, "SVETLOV Кольцо из красного золота с фианитами 017052 - вес 1.66 гр."),
        ("test_products.json", 1, "Обручальное кольцо с бриллиантом 0.025 карат из красного золота 61810 VESNA jewelry"),
        ("test_images_pv.json", 1, "Куртка женская Slim, черная"),
    ])
    def test_component_title(self, path, component, expected_title):
        components = self.read_components(path)
        assert components
        assert components[component].get("text.title") == expected_title

    @pytest.mark.parametrize("path, component, expected_url", [
        ("test_products.json", 0, "direct_url_stub1"),
        ("test_products.json", 1, "direct_url_stub2"),
        ("test_images_pv.json", 1, "direct_url_stub3"),
    ])
    def test_component_url(self, path, component, expected_url):
        components = self.read_components(path)
        assert components
        assert components[component].get("page-url") == expected_url

    @pytest.mark.parametrize("path, component, max_price", [
        ("test_products.json", 0, 8850.0),
        ("test_products.json", 1, 20584.0),
        ("test_products.json", 3, None),
        ("test_images_pv.json", 1, 6927.0),
    ])
    def test_component_max_price(self, path, component, max_price):
        components = self.read_components(path)
        assert components
        assert components[component].get("long.marketMaxPrice") == max_price

    @pytest.mark.parametrize("path, component, min_price", [
        ("test_products.json", 0, 8850.0),
        ("test_products.json", 1, 20584.0),
        ("test_products.json", 3, 8500.0),
        ("test_images_pv.json", 1, 6927.0),
    ])
    def test_component_min_price(self, path, component, min_price):
        components = self.read_components(path)
        assert components
        assert components[component].get("long.marketMinPrice") == min_price

    @pytest.mark.parametrize("path, component, old_price", [
        ("test_products.json", 0, None),
        ("test_products.json", 1, 21000.0),
        ("test_products.json", 3, None),
        ("test_images_pv.json", 1, 6927.0),
    ])
    def test_component_old_price(self, path, component, old_price):
        components = self.read_components(path)
        assert components
        assert components[component].get("long.marketOriginalPrice") == old_price

    @pytest.mark.parametrize("path, component, ware_md5", [
        ("test_products.json", 0, "L20AcIwrOdkLYhvVcgkDyQ"),
        ("test_products.json", 1, None),
    ])
    def test_component_ware_md5(self, path, component, ware_md5):
        components = self.read_components(path)
        assert components
        assert components[component].get("text.wareMd5") == ware_md5

    @pytest.mark.parametrize("path, component, feed_id", [
        ("test_products.json", 0, "475690"),
        ("test_products.json", 1, None),
    ])
    def test_component_feed_id(self, path, component, feed_id):
        components = self.read_components(path)
        assert components
        assert components[component].get("text.feedId") == feed_id

    @pytest.mark.parametrize("path, component, offer_id", [
        ("test_products.json", 0, "598852.023264.49642"),
        ("test_products.json", 1, None),
    ])
    def test_component_offer_id(self, path, component, offer_id):
        components = self.read_components(path)
        assert components
        assert components[component].get("text.offerId") == offer_id

    @pytest.mark.parametrize("path, component, shop_id", [
        ("test_products.json", 0, "431782"),
        ("test_products.json", 1, None),
    ])
    def test_component_shop_id(self, path, component, shop_id):
        components = self.read_components(path)
        assert components
        assert components[component].get("text.shopId") == shop_id

    @pytest.mark.parametrize("path, component, image_url", [
        ("test_products.json", 0, None),
        ("test_products.json", 1, "https://avatars.mds.yandex.net/get-marketpic/1451847/pica25f909c6987b43224ad963f57ca18d1/orig"),
        ("test_images_pv.json", 1, "https://im0-tub-ru.yandex.net/i?id=75f9c811a8c537a04671358ffd0974ba-l&n=33&w=600&h=600"),
    ])
    def test_component_image_url(self, path, component, image_url):
        components = self.read_components(path)
        assert components
        assert components[component].get("url.offerImageUrl") == image_url

    @pytest.mark.parametrize("path, component, discount", [
        ("test_products.json", 0, 0),
        ("test_products.json", 1, 0.03),
    ])
    def test_component_discount(self, path, component, discount):
        components = self.read_components(path)
        assert components
        assert components[component].get("double.marketDiscount") == discount

    @pytest.mark.parametrize("path, component, rating", [
        ("test_products.json", 0, None),
        ("test_products.json", 2, 4.09),
    ])
    def test_component_rating(self, path, component, rating):
        components = self.read_components(path)
        assert components
        assert components[component].get("double.marketRating") == rating

    @pytest.mark.parametrize("path, component, product_id", [
        ("test_products.json", 0, None),
        ("test_products.json", 2, 999042001),
    ])
    def test_component_product_id(self, path, component, product_id):
        components = self.read_components(path)
        assert components
        assert components[component].get("long.marketProductId") == product_id

    @pytest.mark.parametrize("path, component, sku_offer_count", [
        ("test_products.json", 0, None),
        ("test_products.json", 2, 27),
    ])
    def test_component_sku_offer_count(self, path, component, sku_offer_count):
        components = self.read_components(path)
        assert components
        assert components[component].get("long.skuOffersCount") == sku_offer_count

    @pytest.mark.parametrize("path, component, sku_id", [
        ("test_products.json", 0, None),
        ("test_products.json", 2, 101417387738),
    ])
    def test_component_sku_id(self, path, component, sku_id):
        components = self.read_components(path)
        assert components
        assert components[component].get("long.skuId") == sku_id

    @pytest.mark.parametrize("path, component, sku_min_price", [
        ("test_products.json", 0, None),
        ("test_products.json", 2, 8390),
    ])
    def test_component_sku_min_price(self, path, component, sku_min_price):
        components = self.read_components(path)
        assert components
        assert components[component].get("long.skuMinPrice") == sku_min_price

    @pytest.mark.parametrize("path, component, sku_max_price", [
        ("test_products.json", 0, None),
        ("test_products.json", 2, 16234),
    ])
    def test_component_sku_max_price(self, path, component, sku_max_price):
        components = self.read_components(path)
        assert components
        assert components[component].get("long.skuMaxPrice") == sku_max_price

    @pytest.mark.parametrize("path, filter_id, filter_values", [
        ("test_products.json", "glprice", ["found"]),
        ("test_products.json", "7893318", ["15873446", "13324544", "12734125"]),
    ])
    def test_filter_values(self, path, filter_id, filter_values):
        parse_result = self.parse_file(path)
        assert parse_result["json.filters"][filter_id] == filter_values

    def test_current_category(self):
        parse_result = self.parse_file("test_products.json")
        assert parse_result["json.category"] == {"name": "Украшения", "nid": 27233050, "hid": 15068776}
