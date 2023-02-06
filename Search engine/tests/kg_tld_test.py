import pytest

from base_parsers import SerpParser, YandexJSONSerpParser
from google_web_parser import GoogleWebParser
from yandex_baobab.yandex_baobab_html_parser import YandexBaobabHTMLParser
from yandex_market_front_to_report_reqs_parser import MarketFrontToReportParser
from yandex_market_parsers import YandexMarketSearchParser


@pytest.mark.parametrize("parser_class, tld", [
    (SerpParser, "ru"),
    (YandexJSONSerpParser, "ru"),
    (YandexBaobabHTMLParser, "ru"),
    (GoogleWebParser, "kg"),
    (MarketFrontToReportParser, "ru"),
    (YandexMarketSearchParser, "ru"),
])
def test_kg_tld(parser_class, tld):
    tlds = parser_class().get_tlds()
    assert tlds["KG"] == tld
