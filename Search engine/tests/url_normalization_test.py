# -*- coding: utf-8 -*-

from normalization import normalize_url


def test_schema_is_removed():
    assert normalize_url("http://03market.ru/images/stories/R1/5236") == "03market.ru/images/stories/r1/5236"


def test_trailing_slash_is_removed():
    assert normalize_url("http://03market.ru/images/stories/R1/5236/") == "03market.ru/images/stories/r1/5236"


def test_www_removal():
    assert normalize_url("http://www.calenda.ru/topic/st02041a.jpg") == "calenda.ru/topic/st02041a.jpg"


def test_ad_key_is_removed():
    assert normalize_url(
        "http://market.yandex.ru/model.xml?hid=90635&modelid=10780948") == "market.yandex.ru/model.xml?hid=90635&modelid=10780948"


def test_crlf_and_tab_replacement():
    assert normalize_url("http://roslunnuu-dim.com.ua/ru/details?uid=194>\r\n    \t\t\t\t\t\t\t\t\t\t\t\t<p class=") == \
        "roslunnuu-dim.com.ua/ru/details?uid=194>%0d%0a    %09%09%09%09%09%09%09%09%09%09%09%09<p class="


def test_trailing_slash_for_empty_paths_is_present():
    assert normalize_url("https://ya.ru") == "ya.ru/"


def test_blank_url():
    assert normalize_url("") == ""


def test_unicode_input():
    normalized_from_unicode = normalize_url(u"https://news.yandex.ru/yandsearch?text=дивеево монастырь официальный сайт&lr=225&rpt=nnews2&rel=rel&grhow=clutop&from=serp")
    assert normalized_from_unicode == \
        u"news.yandex.ru/yandsearch?text=дивеево монастырь официальный сайт&lr=225&rpt=nnews2&rel=rel&grhow=clutop&from=serp"
    assert isinstance(normalized_from_unicode, str)


def test_non_unicode_cyrillic_url():
    normalized_from_cyrillic = normalize_url(
        "https://news.yandex.ru/yandsearch?text=дивеево монастырь официальный сайт&lr=225&rpt=nnews2&rel=rel&grhow=clutop&from=serp")
    assert normalized_from_cyrillic == u"news.yandex.ru/yandsearch?text=дивеево монастырь официальный сайт&lr=225&rpt=nnews2&rel=rel&grhow=clutop&from=serp"
    assert isinstance(normalized_from_cyrillic, str)
