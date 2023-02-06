# coding=utf-8

import pytest
import json
from market.dynamic_pricing_parsing.build_watson_parsers.lib.main import MarketConfig, WatsonConfig


def __get_watson_formt(key, xpath, is_collection="field"):
    return json.dumps({
        "key": key,
        "sanitize": True,
        "type": is_collection,
        "xpath_selector": xpath
    })


__offers_watson_format = json.dumps(
    {
        "fields": [
            {
                "key": "offer_price_final",
                "sanitize": True,
                "type": "field",
                "xpath_selector": "//span[contains(@class,\"text-lg\")]"
            },
            {
                "key": "offer_price_before_discount",
                "sanitize": True,
                "type": "field",
                "xpath_selector": "//span[contains(@class,\"text-del\") and preceding-sibling::*[1][(name() = \"noindex\" or name() = \"noindex\") and translate(normalize-space(string(.)),'  \"','') = \"врозничнойсети\"]] | //span[contains(@class,\"text-del\")]"
            },
            {
                "key": "offer_not_available",
                "sanitize": True,
                "type": "field",
                "xpath_selector": "//input[@id=\"product-availablity\" and @value=\"0\"]/@value"
            }
        ],
        "key": "offers",
        "sanitize": True,
        "type": "collection",
        "xpath_selector": "/"
    }
)


def test_market_config_converter_all_keys():
    city_xpath = "(//SPAN[preceding-sibling::*][1][name() = \"SPAN\"])[2]"
    discounted_xpath = "//span[@title=\"Уценённый товар\"]/text()"
    name_xpath = "//h1[string-length(text()) < 512]"
    not_available_xpath = "//button/div/div[text()=\"Узнать о поступлении\"]/text()"
    param_name_xpath = "//div[@id=\"section-characteristics\"]/dt/div/*[position()=1]/text()"
    param_value_xpath = "//div[@id=\"section-characteristics\"]/div/*[position()=1]/text()"
    price_final_xpath = "//div[@lnk=\"Оформить\"]/span[contains(text(), \"₽\")][1][1]/text()"
    market_data = [("city", city_xpath, False, None, __get_watson_formt("city", city_xpath)),
                   ("discounted", discounted_xpath, False, None, __get_watson_formt("discounted", discounted_xpath)),
                   ("name", name_xpath, False, None, __get_watson_formt("name", name_xpath)),
                   ("not_available", not_available_xpath, False, None, __get_watson_formt("not_available", not_available_xpath)),
                   ("param_name", param_name_xpath, True, None, __get_watson_formt("param_name", param_name_xpath, 'list')),
                   ("param_value", param_value_xpath, True, None, __get_watson_formt("param_value", param_value_xpath, 'list')),
                   ("price_final", price_final_xpath, False, None, __get_watson_formt("price_final", price_final_xpath)),
                   ("offers", "/", True, None, __offers_watson_format),
                   ]

    config = MarketConfig(market_data)
    converted_config = config.convert()
    converted_keys = [row['key'] for row in converted_config]
    input_keys = [_[0] for _ in market_data]

    # test all keys are parsed
    assert set(input_keys) == set(converted_keys)


def test_watson_config_only_market_elem():
    host = "ozon.ru"
    regex = "(https?://([^/?#]+\\.)?ozon\\.ru/context/detail/id/.*|https://ozon\\.onelink\\.me/.*)"

    city_xpath = "(//SPAN[preceding-sibling::*][1][name() = \"SPAN\"])[2]"
    market_data = [("city", city_xpath, False, None, __get_watson_formt("city", city_xpath))]
    xpath_selector_in_lowercase = city_xpath.decode()

    fake_sovetnik_data = {"type": "shop",
                          "meta": {
                              "status": u"ok",
                              "restricted": False,
                              "history": [],
                              "comments": []
                          },
                          "attributes": {
                              "name": "h1",
                              "price": ".price"
                          }
                          }

    config = WatsonConfig(host=host,
                          regex=regex,
                          market_data=market_data,
                          sovetnik_data=fake_sovetnik_data,
                          watson_data=None)

    converted_config = config.convert()

    assert converted_config["AllowedHost"] == host
    assert converted_config["RegEx"] == regex
    assert converted_config["ParserId"] == host
    assert converted_config["PagesLimitKey"] == host
    assert len(converted_config['Config']) == 2

    assert len(filter(lambda elem: elem['key'] == 'market', converted_config['Config'])) == 1

    for elem in converted_config['Config']:
        if elem['key'] == 'market':
            assert len(elem['fields']) == 1
            assert elem['fields'][0]['key'] == 'city'
            assert elem['fields'][0]['xpath_selector'] == xpath_selector_in_lowercase
