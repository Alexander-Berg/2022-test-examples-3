import pytest

from test_utils import TestParser


class TestOzonSearchParser(TestParser):
    @pytest.mark.parametrize("path, expected_count", [
        ("0_split_system.html", 4),
        ("1_external_hard_drive.html", 0),
        ("2_floor_air_conditioning.html", 4),
        ("3_smartphone.html", 4),
        ("4_iphone_12_mini.html", 0),
    ])
    def test_component_count(self, path, expected_count):
        parsed = self.parse_file(path)
        components = parsed["components"]
        assert len(components) == expected_count

    @pytest.mark.parametrize("path, price, loyalty_price", [
        ("0_split_system.html", 21990, None),
        ("5_tv.html", 69335, 66919),
    ])
    def test_first_component_params(self, path, price, loyalty_price):
        parsed = self.parse_file(path)
        components = parsed["components"]
        assert len(components) > 0
        first_component = components[0]
        assert (first_component.get("page-url", None) or "") != ""
        assert (first_component.get("text.title", None) or "") != ""
        assert first_component["long.marketMinPrice"] == price
        assert first_component.get("long.marketLoyaltyPrice") == loyalty_price

    @pytest.mark.parametrize("path, real_region_name, real_url", [
        ("0_split_system.html", u"Ижевск", "https://www.ozon.ru/category/konditsionery-i-split-sistemy-10726"),
        ("2_floor_air_conditioning.html", u"Санкт-Петербург",
         "https://www.ozon.ru/category/konditsionery-i-split-sistemy-10726"),
        ("3_smartphone.html", u"Саратов", "https://www.ozon.ru/category/smartfony-15502"),
    ])
    def test_real_region_and_url(self, path, real_region_name, real_url):
        parsed = self.parse_file(path)
        assert (parsed.get("text.real_region", None) or "") == real_region_name
        assert (parsed.get("text.redirect_url", None) or "")[:len(real_url)] == real_url

    @pytest.mark.parametrize("path, url_for_redirect", [
        ("1_external_hard_drive.html", "https://www.ozon.ru/category/vneshnie-zhestkie-diski-15711"),
        ("4_iphone_12_mini.html", "https://www.ozon.ru/category/smartfony-15502/apple-26303000"),
    ])
    def test_url_for_redirect(self, path, url_for_redirect):
        parsed = self.parse_file(path)
        assert (parsed.get("text.url_to_redirect", None) or "")[:len(url_for_redirect)] == url_for_redirect
