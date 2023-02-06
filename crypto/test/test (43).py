# -*- coding: utf-8 -*-

import unittest

from crypta.profile.lib.segments import condition_helpers


class TestConditions(unittest.TestCase):
    def test_normalize_int_list_with_comments(self):
        values = [
            '12345',
            '12456 ',
            '125454 # comment',
            '1235654#comment',
            '1235655 #comment',
            'jfdfjdk',
            'some random # text',
        ]

        self.assertEquals(
            condition_helpers.normalize_int_list_with_comments(values),
            [12345, 12456, 125454, 1235654, 1235655],
        )

    def test_normalize_bundle_id(self):
        self.assertIsNone(condition_helpers.normalize_bundle_id(None))
        self.assertIsNone(condition_helpers.normalize_bundle_id(123))
        self.assertIsNone(condition_helpers.normalize_bundle_id(lambda x: x / 2))
        self.assertEquals(condition_helpers.normalize_bundle_id('com.example.app'), 'com.example.app')
        self.assertEquals(condition_helpers.normalize_bundle_id('com.ExaMPle.aPp'), 'com.example.app')

    def test_normalize_counters_and_goals(self):
        values = [
            '123:456',
            '123:456 # comment',
            '123:456 #comment',
            '123:456#comment',
            '123:456 #comment #more words',

            '1234',
            '1234 # comment',
            '1234 #comment',
            '1234#comment',

            '1234:fjhg #comment',
        ]

        self.assertEquals(
            condition_helpers.normalize_counters_and_goals(values),
            [
                condition_helpers.CounterAndGoalCondition(123, 456),
                condition_helpers.CounterAndGoalCondition(123, 456),
                condition_helpers.CounterAndGoalCondition(123, 456),
                condition_helpers.CounterAndGoalCondition(123, 456),
                condition_helpers.CounterAndGoalCondition(123, 456),

                condition_helpers.CounterAndGoalCondition(1234, None),
                condition_helpers.CounterAndGoalCondition(1234, None),
                condition_helpers.CounterAndGoalCondition(1234, None),
                condition_helpers.CounterAndGoalCondition(1234, None),
            ],
        )

    def test_split_url(self):
        values = [
            'utkonos.ru/cat/5707',
            'beru.ru/catalog/lakomstva-dlia-koshek/77671',
            'petshop.ru/catalog/cats/holistic?filter=1&gl=2',

            'vseigru.net/igru-dlya-malchikov/26779-igra-russkij-voditel.html',
            'vseigru.net/igru-dlya-malchikov/24758-igra-druzya-razbojniki-2.html',

            'r01.ru/domain/whois/instruments/converter.php',

            u'дивное.рф/a/b',
            u'дивное.рф',
            u'дивное.рф/',
            u'дивное.рф/прекрасные/диваны',

            'example.com/a/b/c/',

            '/a/b/c//',

            r'regexp:nalog.ru/rn\d+/yul',
            r'regexp:navigator.smbn.ru/st/\d+',
        ]

        self.assertEquals(
            condition_helpers.split_urls(values),
            [
                condition_helpers.UrlCondition('utkonos.ru', ur'utkonos\\.ru/cat/5707.*'),
                condition_helpers.UrlCondition('beru.ru', ur'beru\\.ru/catalog/lakomstva-dlia-koshek/77671.*'),
                condition_helpers.UrlCondition('petshop.ru', ur'petshop\\.ru/catalog/cats/holistic.*'),

                condition_helpers.UrlCondition('vseigru.net', ur'vseigru\\.net/igru-dlya-malchikov/26779-igra-russkij-voditel\\.html.*'),
                condition_helpers.UrlCondition('vseigru.net',
                                               ur'vseigru\\.net/igru-dlya-malchikov/24758-igra-druzya-razbojniki-2\\.html.*'),

                condition_helpers.UrlCondition('r01.ru', ur'r01\\.ru/domain/whois/instruments/converter\\.php.*'),

                condition_helpers.UrlCondition('xn--b1adek0ag.xn--p1ai', ur'xn--b1adek0ag\\.xn--p1ai/a/b.*'),
                condition_helpers.UrlCondition('xn--b1adek0ag.xn--p1ai', ur'xn--b1adek0ag\\.xn--p1ai/.*'),
                condition_helpers.UrlCondition('xn--b1adek0ag.xn--p1ai', ur'xn--b1adek0ag\\.xn--p1ai/.*'),
                condition_helpers.UrlCondition(
                    'xn--b1adek0ag.xn--p1ai',
                    ur'xn--b1adek0ag\\.xn--p1ai/%D0%BF%D1%80%D0%B5%D0%BA%D1%80%D0%B0%D1%81%D0%BD%D1%8B%D0%B5/%D0%B4%D0%B8%D0%B2%D0%B0%D0%BD%D1%8B.*'
                ),

                condition_helpers.UrlCondition('example.com', ur'example\\.com/a/b/c.*'),

                condition_helpers.UrlCondition('nalog.ru', ur'nalog\\.ru/rn\\d+/yul'),
                condition_helpers.UrlCondition('navigator.smbn.ru', ur'navigator\\.smbn\\.ru/st/\\d+'),
            ],
        )

    def test_normalize_hosts(self):
        values = [
            'utkonos.ru',
            u'дивное.рф',
            'example.com/a/b/c',
            'example.com/'
            '/a/b/c//',
        ]
        self.assertEquals(
            condition_helpers.normalize_hosts(values),
            [
                'utkonos.ru',
                'xn--b1adek0ag.xn--p1ai',
                'example.com',
                'example.com',
            ],
        )
