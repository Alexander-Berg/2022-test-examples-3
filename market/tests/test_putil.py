# coding: utf-8

import unittest

from market.pylibrary.putil import restore_url
from market.pylibrary.putil import punycode_url, punycode_and_quote_url


class TestUrlRestoring(unittest.TestCase):
    def test_restore_url_prefix(self):
        self.assertEqual('https://aaa.yandex.com', restore_url('https://aaa.yandex.com'))
        self.assertEqual('http://aaa.yandex.com', restore_url('http://aaa.yandex.com'))
        self.assertEqual('http://aaa.yandex.com', restore_url('aaa.yandex.com'))


def test_punycode():
    actual_expected_list = [
        ('http://hello.ru/', 'http://hello.ru/'),
        ('http://привет.ru/', 'http://xn--b1agh1afp.ru/'),
        ('/path', '/path'),
        ('http://идн-тест.яндекс.рф/mptest/фиды/кошак.jpg', 'http://xn----gtbej0a0afc.xn--d1acpjx3f.xn--p1ai/mptest/фиды/кошак.jpg'),
        # обычный урл c логином
        ('http://user@yandex.ru/', 'http://user@yandex.ru/'),
        # обычный урл c кирилическим логином
        ('http://пользователь@yandex.ru/', 'http://пользователь@yandex.ru/'),
        # обычный урл c логином и паролем
        ('http://user:pass@yandex.ru/', 'http://user:pass@yandex.ru/'),
        # урл с не латинскими символами с логином
        ('http://user@стройторг.com/pictures/6090_big.jpg', 'http://user@xn--c1alobhdcid.com/pictures/6090_big.jpg'),
        # урл с не латинскими символами с кирилическим логином
        ('http://пользователь@стройторг.com/pictures/6090_big.jpg', 'http://пользователь@xn--c1alobhdcid.com/pictures/6090_big.jpg'),
        # урл с не латинскими символами с логином и паролем
        ('http://user:pass@стройторг.com/pictures/6090_big.jpg', 'http://user:pass@xn--c1alobhdcid.com/pictures/6090_big.jpg'),
    ]
    for actual, expected in actual_expected_list:
        assert punycode_url(actual) == expected


def test_punycode_and_quote_url():
    actual_expected_list = [
        # обычный урл
        ('http://yandex.ru/', 'http://yandex.ru/'),
        # урл с не латинскими символами
        ('http://стройторг.com/pictures/6090_big.jpg', 'http://xn--c1alobhdcid.com/pictures/6090_big.jpg'),
        # урл с уже экранированным пробелом
        ('http://yandex.ru/pictures/6090%20big.jpg', 'http://yandex.ru/pictures/6090%20big.jpg'),
        # урл с path/query/fragment
        ('http://yandex.ru/path?k1=v1&k2=v2#fragment', 'http://yandex.ru/path?k1=v1&k2=v2#fragment'),
    ]
    for actual, expected in actual_expected_list:
        assert punycode_and_quote_url(actual) == expected
