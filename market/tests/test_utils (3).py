import pytest

from market.dynamic_pricing_parsing.build_watson_parsers.lib.utils import get_subdomains, flatten_unique


def test_get_subdomain():
    all_domains = list(get_subdomains("https://www.bbb.spb.ozon.ru", max_level=2))

    assert "ru" not in all_domains
    assert "ozon.ru" in all_domains
    assert "spb.ozon.ru" in all_domains
    assert "bbb.spb.ozon.ru" in all_domains


def test_flatten_unique():
    result = ['/catalog/', '\\w+/(?!\\?)', '/personal/cart/']
    assert flatten_unique([['/personal/cart/'], ['\\w+/(?!\\?)'], None, ['/catalog/']]) == result
