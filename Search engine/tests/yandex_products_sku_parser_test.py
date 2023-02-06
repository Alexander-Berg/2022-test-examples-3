import pytest

from test_utils import TestParser


class TestYandexProductsSKUParser(TestParser):
    @pytest.mark.parametrize("path, expected_title", [
        ("test_products.json", "Смартфон Apple iPhone 13 128 ГБ, тёмная ночь"),
    ])
    def test_serp_title(self, path, expected_title):
        serp = self.parse_file(path)
        assert serp["text.title"] == expected_title

    @pytest.mark.parametrize("path, expected_image", [
        ("test_products.json", "https://avatars.mds.yandex.net/get-mpic/5376414/img_id9062539055468758161.jpeg/orig"),
    ])
    def test_serp_image(self, path, expected_image):
        serp = self.parse_file(path)
        assert serp["imageadd"]["url"] == expected_image

    @pytest.mark.parametrize("path, expected_count", [
        ("test_products.json", 2),
    ])
    def test_offers_count(self, path, expected_count):
        offers = self.read_components(path)
        assert len(offers) == expected_count

    @pytest.mark.parametrize("path, offer, expected_domain_name", [
        ("test_products.json", 0, "ya.ru"),
        ("test_products.json", 1, "gsmbutik.ru"),
    ])
    def test_domain_title(self, path, offer, expected_domain_name):
        offers = self.read_components(path)
        assert offers
        assert offers[offer].get("text.domain") == expected_domain_name

    @pytest.mark.parametrize("path, offer, expected_domain_url", [
        ("test_products.json", 0, "market.yandex.ru"),
        ("test_products.json", 1, "gsmbutik.ru"),
    ])
    def test_domain_url(self, path, offer, expected_domain_url):
        offers = self.read_components(path)
        assert offers
        assert offers[offer].get("url.domain") == expected_domain_url

    @pytest.mark.parametrize("path, expected_rating", [
        ("test_products.json", 4.74),
    ])
    def test_rating(self, path, expected_rating):
        serp = self.parse_file(path)
        assert serp["double.marketRating"] == expected_rating

    @pytest.mark.parametrize("path, offer, expected_offer_url", [
        ("test_products.json", 0, "https://market.yandex.ru/smartfony/apple/apple-iphone-13-128gb-red"),
        ("test_products.json", 1, "https://gsmbutik.ru/smartfony/apple/apple-iphone-13-128gb-red"),
    ])
    def test_offer_url(self, path, offer, expected_offer_url):
        offers = self.read_components(path)
        assert offers
        assert offers[offer].get("page-url") == expected_offer_url

    @pytest.mark.parametrize("path, offer, expected_offer_price", [
        ("test_products.json", 0, "62150"),
        ("test_products.json", 1, "621500"),
    ])
    def test_offer_price(self, path, offer, expected_offer_price):
        offers = self.read_components(path)
        assert offers
        assert offers[offer].get("long.price") == expected_offer_price

    @pytest.mark.parametrize("path, offer, expected_offer_discount", [
        ("test_products.json", 0, None),
        ("test_products.json", 1, {'oldMin': '66240', 'percent': 5, 'isBestDeal': False, 'absolute': '3400'}),
    ])
    def test_offer_discount(self, path, offer, expected_offer_discount):
        offers = self.read_components(path)
        assert offers
        assert offers[offer].get("json.discount") == expected_offer_discount

    @pytest.mark.parametrize("path, offer, expected_offer_currency", [
        ("test_products.json", 0, "RUR"),
        ("test_products.json", 1, "RUR"),
    ])
    def test_offer_currency(self, path, offer, expected_offer_currency):
        offers = self.read_components(path)
        assert offers
        assert offers[offer].get("text.currency") == expected_offer_currency
