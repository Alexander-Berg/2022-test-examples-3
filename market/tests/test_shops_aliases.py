#!/usr/bin/python
# -*- coding: utf-8 -*-

import unittest

from market.pylibrary.shopsutil import shops_aliases
from market.pylibrary.shopsutil.transliterate import u


class TestShopsAliasesUtils(unittest.TestCase):
    def test_normalize_spaces(self):
        self.assertEqual('a b', shops_aliases.normalize_spaces(' a  b  '))

    def test_remove_phrase(self):
        tests = [
            # one word phrase
            (u('ооо эдам'), u('ооо'), u('эдам')),
            (u('эльдорадо онлайн'), u('онлайн'), u('эльдорадо')),
            (u('дочки и сыночки'), u('и'), u('дочки сыночки')),

            # several occurences
            (u('сайт мастер сайт потолков сайт'), u('сайт'), u('мастер потолков')),

            # several words phrase
            (u('торговый дом царь'), u('торговый дом'), u('царь')),
            (u('суперский мебельный центр'), u('мебельный центр'), u('суперский')),
            (u('мистер удалить это джинс'), u('удалить это'), u('мистер джинс')),
        ]

        for initial_phrase, phrase_to_remove, result in tests:
            self.assertEqual(result, shops_aliases.remove_phrase(initial_phrase, phrase_to_remove))

    def test_word_count(self):
        self.assertEqual(1, shops_aliases.word_count('abc'))
        self.assertEqual(2, shops_aliases.word_count('abc def'))
        self.assertEqual(3, shops_aliases.word_count(' abc  def   ghi    '))

    def test_domain_levels_count(self):
        self.assertEqual(1, shops_aliases.domain_levels_count('com'))
        self.assertEqual(2, shops_aliases.domain_levels_count('екатеринбург.рф'))
        self.assertEqual(3, shops_aliases.domain_levels_count('ekb.apteka.ru'))

    def test_url_without_extension(self):
        self.assertEqual('not_url', shops_aliases.url_without_extension('not_url'))
        self.assertEqual('apteka', shops_aliases.url_without_extension('apteka.ru'))
        self.assertEqual('veliki.com', shops_aliases.url_without_extension('veliki.com.ua'))

    def test_is_like_good_url(self):
        self.assertTrue(shops_aliases.is_like_good_url('mvideo.ru'))
        self.assertTrue(shops_aliases.is_like_good_url('аптека.ру'))

        self.assertFalse(shops_aliases.is_like_good_url('not_url'))
        self.assertFalse(shops_aliases.is_like_good_url('not url'))
        self.assertFalse(shops_aliases.is_like_good_url('spb.esky.ru'))
        self.assertFalse(shops_aliases.is_like_good_url('abc.def.ghi.com'))

    def test_get_common_word_prefix(self):
        tests = [
            # equal phrases
            ('ab', 'ab', 'ab'),
            ('ab cd', 'ab cd', 'ab cd'),

            # common prefix doesn't equal one of phrases
            ('ab cd', 'ab ef', 'ab'),

            # one phrase is a prefix of another
            ('ab', 'ab cd', 'ab'),
            ('ab cd', 'ab', 'ab'),

            # tests other delimeters
            ('d&g', 'd&q', 'd'),
            ('d:g', 'd-g', 'd'),

            # no common prefix
            ('ab', 'cd', ''),
            ('abc', 'abd', ''),
            ('ab', 'abc', ''),
        ]

        for phrase1, phrase2, result in tests:
            self.assertEqual(result, shops_aliases.get_common_word_prefix(phrase1, phrase2))

    def test_normalize_name(self):
        shopwords = [u('фирменный')]
        tests = {
            'muZZDvor': 'muzzdvor',
            'юлмарт | ulmart.ru': u('юлмарт'),
            'юлмарт (ulmart.ru)': u('юлмарт'),
            '"100 #диванов & you!"': u('100 диванов & you'),
            'keramice  knife': 'keramice knife',
            'www.knopka.by': 'knopka.by',
            'фирменный microsoft': 'microsoft',
        }

        for shopname, result in tests.items():
            self.assertEqual(result, shops_aliases.normalize_name(shopname, shopwords))


class TestShopsAliases(unittest.TestCase):
    def do_test(self, test_data):
        shops_info = []
        expected = {}
        for shop_id, (name, url, cluster_id, expected_aliases) in enumerate(test_data):
            shops_info.append(shops_aliases.ShopInfo(shop_id=shop_id, shop_name=name, url_for_log=url, shop_cluster_id=cluster_id))
            expected[shop_id] = expected_aliases

        shopwords = [u('фирменный'), u('магазин')]
        shops_aliases_storage = shops_aliases.ShopsAliasesStorage(shops_info, shopwords)
        all_shops_ids = shops_aliases_storage.get_all_shops_ids()

        for shop_id in all_shops_ids:
            expected_aliases = expected[shop_id]
            actual_aliases = shops_aliases_storage.get_shop(shop_id).generate_aliases().get_list()

            self.assertEqual(len(expected_aliases), len(actual_aliases))

            for (actual_alias, (expected_alias_string, expected_alias_type)) in zip(actual_aliases, expected_aliases):
                self.assertEqual(expected_alias_string, actual_alias.alias_string)
                self.assertEqual(expected_alias_type, actual_alias.alias_type)

    def test_single_shop(self):
        self.do_test([
            ('всё для сварки', 'svarka.com.ua', None, [
                (u('всё для сварки'), 'NAME'),
                (u('svarka.com.ua'), 'URL'),
            ])
        ])

    def test_single_shop_with_short_name(self):
        self.do_test([
            ('super сварка', 'svarka.com.ru', None, [
                (u('super сварка'), 'NAME'),
                (u('svarka.com.ru'), 'URL'),
                (u('super svarka'), 'FROM_NAME'),
                (u('супер сварка'), 'FROM_NAME'),
            ])
        ])

    def test_single_shop_with_good_url(self):
        self.do_test([
            ('всё для сварки', u('суперsvarka.рф'), None, [
                (u('всё для сварки'), 'NAME'),
                (u('суперsvarka.рф'), 'URL'),
                (u('supersvarka.rf'), 'FROM_URL'),
                (u('суперсварка.рф'), 'FROM_URL'),
                (u('суперsvarka'), 'FROM_URL'),
                (u('supersvarka'), 'FROM_URL'),
                (u('суперсварка'), 'FROM_URL'),
            ])
        ])

    def test_single_shop_with_name_like_good_url(self):
        self.do_test([
            ('superсварка.pro', 'svarka.com.ru', None, [
                (u('superсварка.pro'), 'NAME'),
                (u('svarka.com.ru'), 'URL'),
                (u('supersvarka.pro'), 'FROM_NAME'),
                (u('суперсварка.про'), 'FROM_NAME'),
                (u('superсварка'), 'FROM_NAME'),
                (u('supersvarka'), 'FROM_NAME'),
                (u('суперсварка'), 'FROM_NAME'),
            ])
        ])

    def test_name_cluster(self):
        self.do_test([
            ('супер пупер днс северодвинск', 'dnsseverodvinsk.com.ua', 1, [
                (u('супер пупер днс'), 'NAME'),
                (u('dnsseverodvinsk.com.ua'), 'URL'),
            ]),
            ('супер пупер днс тула', 'dnsseverodvinsk.com.ru', 1, [
                (u('супер пупер днс'), 'NAME'),
                (u('dnsseverodvinsk.com.ru'), 'URL'),
            ]),
            # shop with another clister_id
            ('супер пупер днс москва', 'dnsseverodvinsk.com.kz', 2, [
                (u('супер пупер днс москва'), 'NAME'),
                (u('dnsseverodvinsk.com.kz'), 'URL'),
            ]),
            # shop without cluster_id
            ('супер пупер днс калуга', 'dnsseverodvinsk.com.by', None, [
                (u('супер пупер днс калуга'), 'NAME'),
                (u('dnsseverodvinsk.com.by'), 'URL'),
            ]),
        ])

    def test_name_cluster_with_too_short_common_prefix(self):
        self.do_test([
            ('тц мисс европа', 'europe.com.ru', 1, [
                (u('тц мисс европа'), 'NAME'),
                (u('europe.com.ru'), 'URL'),
            ]),
            ('тц миссис азия', 'asia.com.kz', 1, [
                (u('тц миссис азия'), 'NAME'),
                (u('asia.com.kz'), 'URL'),
            ]),
        ])

    def test_url_cluster(self):
        self.do_test([
            ('просто длинное название', 'ekb.super.shop.ru', 1, [
                (u('просто длинное название'), 'NAME'),
                (u('super.shop.ru'), 'URL'),
            ]),
            ('просто длинное название', 'msk.super.shop.ru', None, [
                (u('просто длинное название'), 'NAME'),
                (u('super.shop.ru'), 'URL'),
            ]),
        ])

    def test_url_cluster_with_too_short_common_domain(self):
        self.do_test([
            ('просто длинное название', 'ekb.ru', None, [
                (u('просто длинное название'), 'NAME'),
                (u('ekb.ru'), 'URL'),
                (u('екб.ру'), 'FROM_URL'),
                (u('ekb'), 'FROM_URL'),
                (u('екб'), 'FROM_URL'),
            ]),
            ('просто длинное название', 'msk.ru', None, [
                (u('просто длинное название'), 'NAME'),
                (u('msk.ru'), 'URL'),
                (u('мск.ру'), 'FROM_URL'),
                (u('msk'), 'FROM_URL'),
                (u('мск'), 'FROM_URL'),
            ]),
        ])

    def test_empty_name_and_url(self):
        self.do_test([
            # 'фирменный' and 'магазин' are shopwords so they are cut from name
            # name becomes empty so there is not NAME-alias
            ('фирменный магазин', 'xperia.sony.com', None, [
                (u('xperia.sony.com'), 'URL'),
            ]),
            # url is empty so there is not URL-alias
            ('магаз без урла', '', None, [
                (u('магаз без урла'), 'NAME'),
            ]),
        ])

    def test_equal_name_and_url(self):
        self.do_test([
            ('ozon.ru', 'ozon.ru', None, [
                (u('ozon.ru'), 'URL'),
                (u('озон.ру'), 'FROM_NAME'),
                (u('ozon'), 'FROM_NAME'),
                (u('озон'), 'FROM_NAME'),
            ])
        ])


if __name__ == '__main__':
    unittest.main()
