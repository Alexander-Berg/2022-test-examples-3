# coding=utf-8
import unittest
from watson_result_parsing import parse
from watson_result_parsing import Row
from watson_result_parsing import choose_price
from watson_result_parsing import parse_price
from watson_result_parsing import find_fractional_separator_index
from watson_result_parsing import _integer_part_parsing
from watson_result_parsing import _remove_chars
from watson_result_parsing import parse_recommendations
from watson_result_parsing import RecommendationRow
from watson_result_parsing import build_shop_parsing_config
from watson_result_parsing import REGEXP_DISCOUNTED
from watson_result_parsing import REGEXP_AVAILABLE
from test_data import *


class WatsonResultParsing(unittest.TestCase):
    def test_choose_price(self):
        self.assertEqual(123, choose_price(None, None, 123))
        self.assertEqual(123, choose_price(None, 123, None))
        self.assertEqual(123, choose_price(123, None, None))
        self.assertEqual(100, choose_price(123, None, 100))
        self.assertEqual(None, choose_price(None, None, None))

    def test_find_fractional_part(self):
        str = u"asdfas аь 1.2 asdfa"
        self.assertEqual(str.index('.'), find_fractional_separator_index(str))

        str = u"asdfas 1,2 asdfa аь"
        self.assertEqual(str.index(','), find_fractional_separator_index(str))

        str = u"asdfas 1,2 1.3 аь asdfa"
        self.assertEqual(str.index('.'), find_fractional_separator_index(str))

        str = u"asdfas аь 1.2 1,3 asdfa"
        self.assertEqual(str.index(','), find_fractional_separator_index(str))

        str = u"asdfas 1.2 1.3 asdfa аь"
        self.assertEqual(len(str) - 1 - str[::-1].index('.'), find_fractional_separator_index(str))

        str = u"asdfas 1,2 1,3  аь asdfa"
        self.assertEqual(len(str) - 1 - str[::-1].index(','), find_fractional_separator_index(str))

        str = u"asdfas аь asdfa"
        self.assertEqual(None, find_fractional_separator_index(str))

        str = u".asdfasаь,, asdfa."
        self.assertEqual(None, find_fractional_separator_index(str))

        str = u"asdfas 1. .1 asdfa. аь"
        self.assertEqual(None, find_fractional_separator_index(str))

        str = u"asdfas 1.3,2 asdfa . аь"
        self.assertEqual(str.index(','), find_fractional_separator_index(str))

    def test_price_parsing(self):
        self.assertEqual(1234, parse_price(u"1234"))
        self.assertEqual(1234, parse_price(u"  .1234,as,df."))
        self.assertEqual(1234, parse_price(u"  1234  "))
        self.assertEqual(1234, parse_price(u" 1  23 4 "))
        self.assertEqual(None, parse_price(u"1aа234"))

        self.assertEqual(12.3, parse_price(u"12.3"))
        self.assertEqual(12.3, parse_price(u"  ,,12.3. авв,."))
        self.assertEqual(12.3, parse_price(u"  12.3  a"))
        self.assertEqual(12.3, parse_price(u"  абв..1 2.3  a"))

        self.assertEqual(None, parse_price(u"12.3 7"))
        self.assertEqual(None, parse_price(u'12.3a7'))
        self.assertEqual(None, parse_price(u'12..3'))
        self.assertEqual(None, parse_price(u'12,,3'))
        self.assertEqual(None, parse_price(u'12,.3'))

    def test_integer_part_parsing(self):
        result = _integer_part_parsing("10.000.00")
        self.assertEqual(1000000, result)

    def test_remove_chars(self):
        self.assertEqual("eloworld", _remove_chars("hello world", [0, 2, 5]))
        with self.assertRaises(ValueError):
            _remove_chars("", [0, 2, 5])
        with self.assertRaises(ValueError):
            _remove_chars("", [-1, 2, 5])

        with self.assertRaises(ValueError):
            _remove_chars("", [-1, 2, 5])

        with self.assertRaises(ValueError):
            _remove_chars(None, [1, 2, 3])

        with self.assertRaises(ValueError):
            _remove_chars("", None)

    def test_sovetnik_only_wrong_json(self):
        self.assertEqual(Row.create_empty().__dict__, parse(sovetnik_only_wrong_json))

    def test_sovetnik_only_wrong_json_no_name_key(self):
        self.assertEqual(Row.create_empty().__dict__, parse(SovetnikElem(price=12.6, set_name=False).to_json()))

    def test_sovetnik_only_wrong_json_name_null(self):
        self.assertEqual(Row.create_empty().__dict__, parse(SovetnikElem(price=12.6).to_json()))

    def test_sovetnik_only_wrong_json_name_is_empty(self):
        self.assertEqual(Row.create_empty().__dict__, parse(SovetnikElem(price=12.6, name="").to_json()))

    def test_sovetnik_only_wrong_json_name_is_spaces(self):
        self.assertEqual(Row.create_empty().__dict__, parse(SovetnikElem(price=12.6, name="  ").to_json()))

    def test_sovetnik_only_wrong_json_no_price_key(self):
        self.assertEqual(Row.create_empty().__dict__, parse(SovetnikElem(name=u"Имя", set_price=False).to_json()))

    def test_sovetnik_only_wrong_json_price_null(self):
        self.assertEqual(Row.create_empty().__dict__, parse(SovetnikElem(name=u"Имя").to_json()))

    def test_sovetnik_only_wrong_json_price_is_not_a_number(self):
        self.assertEqual(Row.create_empty().__dict__, parse(SovetnikElem(name=u"Имя", price="asd").to_json()))

    def test_market_only_wrong_no_name_key(self):
        self.assertEqual(Row.create_empty().__dict__, parse(MarketElem("category", "city", "description", "picture",
                                                                       123, 123, 123, "name",
                                                                       set_name=False).to_json()))

    def test_market_only_wrong_name_is_space(self):
        input_json = MarketElem(category="category", city="city", description="description", picture="picture",
                                price_before_discount=123, price_discount=123, price_final=123, name="  ").to_json()
        self.assertEqual(Row.create_empty().__dict__, parse(input_json))

    def test_market_only_wrong_name_is_empty(self):
        input_json = MarketElem(category="category", city="city", description="description", picture="picture",
                                price_before_discount=123, price_discount=123, price_final=123, name="").to_json()
        self.assertEqual(Row.create_empty().__dict__, parse(input_json))

    def test_market_only_wrong_name_null(self):
        input_json = MarketElem(category="category", city="city", description="description", picture="picture",
                                price_before_discount=123, price_discount=123, price_final=123).to_json()
        self.assertEqual(Row.create_empty().__dict__, parse(input_json))

    def test_market_only_all_prices_is_null(self):
        input_json = MarketElem(category="category", description="",
                                name="name", set_price_before_discount=None, set_price_discount=None,
                                set_price_final=None).to_json()
        self.assertEqual(Row.create(category="category", isbn="",
                                    price=None, name="name", source=Row.MARKET_SOURCE).__dict__, parse(input_json))

    def test_market_only_all_prices_is_not_float(self):
        input_json = MarketElem(category="category", city="city", description="description", picture="picture",
                                name="name", price_before_discount="asdf", price_discount="asf",
                                price_final="").to_json()
        self.assertEqual(Row.create_empty().__dict__, parse(input_json))

    def test_watson_only_wrong_no_name_key(self):
        watson_elem = NormalWatsonElem(category="category", city="city", description="description", picture="picture",
                                       price_before_discount=123, price_discount=123, price_final=123,
                                       name="name", set_name=False)

        self.assertEqual(Row.create_empty().__dict__,
                         parse(NormalWatsonElemWithRecommendations.create_dummy(watson_elem).to_json()))

    def test_watson_only_wrong_name_is_space(self):
        watson_elem = NormalWatsonElem(category="category", city="city", description="description", picture="picture",
                                       price_before_discount=123, price_discount=123, price_final=123, name="  ")

        self.assertEqual(Row.create_empty().__dict__,
                         parse(NormalWatsonElemWithRecommendations.create_dummy(watson_elem).to_json()))

    def test_watson_only_wrong_name_is_empty(self):
        watson_elem = NormalWatsonElem(category="category", city="city", description="description", picture="picture",
                                       price_before_discount=123, price_discount=123, price_final=123, name="")
        self.assertEqual(Row.create_empty().__dict__,
                         parse(NormalWatsonElemWithRecommendations.create_dummy(watson_elem).to_json()))

    def test_watson_only_wrong_name_null(self):
        watson_elem = NormalWatsonElem(category="category", city="city", description="description", picture="picture",
                                       price_before_discount=123, price_discount=123, price_final=123)

        self.assertEqual(Row.create_empty().__dict__,
                         parse(NormalWatsonElemWithRecommendations.create_dummy(watson_elem).to_json()))

    def test_watson_only_all_prices_is_null(self):
        watson_elem = NormalWatsonElem(category="category", city="city", description="description", picture="picture",
                                       name="name", set_price_before_discount=None, set_price_discount=None,
                                       set_price_final=None)
        self.assertEqual(Row.create_empty().__dict__,
                         parse(NormalWatsonElemWithRecommendations.create_dummy(watson_elem).to_json()))

    def test_watson_only_all_prices_is_not_float(self):
        watson_elem = NormalWatsonElem(category="category", city="city", description="description", picture="picture",
                                       name="name", price_before_discount="asdf", price_discount="asf", price_final="")
        self.assertEqual(Row.create_empty().__dict__,
                         parse(NormalWatsonElemWithRecommendations.create_dummy(watson_elem).to_json()))

    def test_parse(self):
        self.assertEqual(Row.create_empty().__dict__,
                         parse(sovetnik_only_json))
        self.assertEqual(Row.create_empty().__dict__, parse(all_wrong_json))
        self.assertEqual(Row(u"Категория", u"Город", "", u"Описание", u"Имя", True, False, None, None,
                             123, 123, Row.MARKET_SOURCE, "", availability_by_default=False).__dict__,
                         parse(market_json))
        self.assertEqual(Row(u"Категория", u"Город", "", u"Описание", u"Имя",
                             True, False, 123, 100, 110, 110, Row.MARKET_SOURCE, "",
                             availability_by_default=True).__dict__, parse(all_json))
        self.assertEqual(Row("", u"Город", "", u"Описание", u"Имя",
                             True, False, 123, None, None, 123, Row.MARKET_SOURCE, "",
                             availability_by_default=True).__dict__,
                         parse(null_in_category_json))

    def test_parse_barcode(self):
        self.assertEqual(Row(u"Категория", u"Город", "", u"Описание", u"Имя",
                             True, False, 123, 100, 110, 110, Row.MARKET_SOURCE, "",
                             availability_by_default=True, barcode="12312312").__dict__, parse(all_json_with_barcode))

    def test_parse_with_watson(self):
        self.assertEqual(Row(u"Женская обувь", u"Москва", "", u"Описание", u"Угги Patrol",
                             True, False, None, None, 123, 123, Row.MARKET_SOURCE, u"красный").__dict__,
                         parse(with_watson_json))
        self.assertEqual(Row(u"", u"", "", u"Описание", u"Угги Patrol",
                             True, False, None, None, 123, 123, Row.WATSON_SOURCE, u"красный").__dict__,
                         parse(with_watson_wrong_market))

    def test_parse_recommendations_good_elem(self):
        other_product_dict = {"discount": "7 900₽",
                              "price": "3 916₽",
                              "title": u"Угги Bearpaw",
                              "url": "https://www.ozon.ru/context/detail/id/147177317/"}

        watson_elem = NormalWatsonElem(category="category", city="city",
                                       description="description", picture="picture",
                                       name="name", price_final=123)
        watson_elem_with_other_products = NormalWatsonElemWithRecommendations(normal_watson_elem=watson_elem,
                                                                              other_products=[other_product_dict])
        watson_elem_with_other_products = watson_elem_with_other_products.to_json()

        recommendations = parse_recommendations(watson_elem_with_other_products)
        expected = [RecommendationRow.new_with_empty_str_defaults(category=u"category", city=u"city",
                                                                  description=u"description",
                                                                  name=u"name", price_final=123,
                                                                  price_for_uc=123,
                                                                  recommended_product_url=None).__dict__,
                    RecommendationRow.new_with_empty_str_defaults(name=other_product_dict["title"],
                                                                  recommended_product_url=other_product_dict["url"],
                                                                  price_before_discount=7900, price_final=3916,
                                                                  price_for_uc=3916).__dict__]
        self.assertEqual(expected, recommendations)

    def test_parse_recommendations_no_good_recommendation(self):
        other_product_dict = {"discount": "7 900₽",
                              "price": "3 916₽",
                              "title": u"",
                              "url": "https://www.ozon.ru/context/detail/id/147177317/"}

        watson_elem = NormalWatsonElem(category="category", city="city",
                                       description="description", picture="picture",
                                       name="name", price_final=123)
        watson_elem_with_other_products = NormalWatsonElemWithRecommendations(normal_watson_elem=watson_elem,
                                                                              other_products=[other_product_dict])
        watson_elem_with_other_products = watson_elem_with_other_products.to_json()
        recommendations = parse_recommendations(watson_elem_with_other_products)
        expected = [RecommendationRow().__dict__]
        self.assertEqual(expected, recommendations)

    def test_parse_recommendations_real_data(self):
        recommendations = parse_recommendations(with_watson_json)
        urls = [u"https://www.ozon.ru/context/detail/id/147047548/",
                u"https://www.ozon.ru/context/detail/id/146685551/",
                u"https://www.ozon.ru/context/detail/id/143539115/"
                ]

        expected = [RecommendationRow.new_with_empty_str_defaults(name=u"Угги Patrol",
                                                                  description=u"Описание", price_final=None,
                                                                  price_for_uc=None,
                                                                  recommended_product_url=None).__dict__,
                    RecommendationRow.new_with_empty_str_defaults(name=u"Угги Winzor",
                                                                  recommended_product_url=urls[0],
                                                                  price_before_discount=11190, price_final=4476,
                                                                  price_for_uc=4476).__dict__,
                    RecommendationRow.new_with_empty_str_defaults(name=u"Угги Vitacci",
                                                                  recommended_product_url=urls[1],
                                                                  price_before_discount=9690, price_final=3876,
                                                                  price_for_uc=3876).__dict__,
                    RecommendationRow.new_with_empty_str_defaults(name=u"Угги Graciana",
                                                                  recommended_product_url=urls[2],
                                                                  price_final=6250, price_for_uc=6250).__dict__]
        self.assertEqual(expected, recommendations)

    def test_string_param_parsing(self):
        expected_params_string = u"Коллекция\tKids & Teens\n" + \
                                 u"Тип\tДекоративные\n" + \
                                 u"Ширина рулона, см\t53\n" + \
                                 u"Основной цвет\tСиний-Голубой\n"

        expected = Row.create_with_defaults(category=u"Отделочные и строительные материалы",
                                            name=u"Обои декоративные Rasch, Kids & Teens, 740080 (0,53x10м)",
                                            price_final=123.,
                                            price_for_uc=123.,
                                            params=expected_params_string,
                                            source=Row.MARKET_SOURCE, availability_by_default=True)
        actual = parse(json_with_params)
        self.assertEqual(expected.__dict__, actual)

    def test_build_shop_parsing_config(self):
        shop_config = build_shop_parsing_config("https://www.ozon.ru/context/detail/id/153010964/")
        self.assertEqual(REGEXP_AVAILABLE["ozon.ru"], shop_config.regex_available)
        self.assertEqual(REGEXP_DISCOUNTED["ozon.ru"], shop_config.regex_discounted)

        shop_config = build_shop_parsing_config("https://www.detmir.ru/product/index/id/3128637/?layout=exclude:")
        self.assertTrue(shop_config.regex_available is None)
        self.assertTrue(shop_config.regex_discounted is None)

    def test_availability_parsing_available(self):
        result = parse(json_available, "https://www.220-volt.ru/catalog-320286/")
        self.assertEqual(result["available"], False)
        self.assertEqual(result["not_available"], False)
        self.assertEqual(result["discounted"], False)
        self.assertEqual(result["active"], False)

        result = parse(json_not_available, "https://www.mvideo.ru/products/chehol-dlya-sotovogo-telefona-50127835")
        self.assertEqual(result["available"], True)
        self.assertEqual(result["not_available"], True)
        self.assertEqual(result["discounted"], False)
        self.assertEqual(result["active"], False)

        result = parse(json_discounted, "https://www.mvideo.ru/products/chehol-dlya-sotovogo-telefona-50127835")
        self.assertEqual(result["available"], True)
        self.assertEqual(result["not_available"], True)
        self.assertEqual(result["discounted"], True)
        self.assertEqual(result["active"], False)
