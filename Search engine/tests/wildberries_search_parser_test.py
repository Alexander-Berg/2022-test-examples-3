import pytest

from test_utils import TestParser


class TestWildberriesSearchParser(TestParser):
    @pytest.mark.parametrize("path, expected_count", [
        ("touch-winter-jacket.html", 105),
        ("desktop-winter-jacket.html", 105),
    ])
    def test_component_count(self, path, expected_count):
        parsed = self.parse_file(path)
        components = parsed["components"]
        assert len(components) == expected_count

    @pytest.mark.parametrize("path, price, original_price", [
        ("touch-winter-jacket.html", 3075, 5990),
        ("desktop-winter-jacket.html", 4900, 10000),
    ])
    def test_first_component_prices(self, path, price, original_price):
        parsed = self.parse_file(path)
        components = parsed["components"]
        assert len(components) > 0, "should be components"
        first_component = components[0]
        assert first_component["long.marketMinPrice"] == price
        assert first_component["long.marketOriginalPrice"] == original_price

    @pytest.mark.parametrize("path, image_url", [
        ("touch-winter-jacket.html", "touch-winter-jacket_files/40911572-1.jpg"),
        ("desktop-winter-jacket.html", "desktop-winter-jacket_files/48409175-1.avif"),
    ])
    def test_first_component_url(self, path, image_url):
        parsed = self.parse_file(path)
        components = parsed["components"]
        assert len(components) > 0, "should be components"
        first_component = components[0]
        assert first_component["url.offerImageUrl"] == image_url

    @pytest.mark.parametrize("path, rating", [
        ("touch-winter-jacket.html", 5),
        ("desktop-winter-jacket.html", None),
    ])
    def test_first_component_rating(self, path, rating):
        parsed = self.parse_file(path)
        components = parsed["components"]
        assert len(components) > 0, "should be components"
        first_component = components[0]
        assert first_component.get("double.productRating") == rating
