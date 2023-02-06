# encoding: utf-8

import unittest
from logbroker_client_common.parsers import TSKV


class TestTSKVParser(unittest.TestCase):
    def setUp(self):
        self.data_ok = 'tskv	k=v	spam=eggs	foo=bar	'
        self.data_junk = 'junk	junk	boohaha='
        self.data_bogus = 'tskv	junk	boohaha'
        self.data_longjunk = open("/bin/sh").read()
        self.data_cyrillic = 'tskv\tmessage=У нас есть кириллица в логах\t'
        self.instance = TSKV()

    def test_genuine_data(self):
        expected = {'k': 'v', 'spam': 'eggs', 'foo': 'bar'}
        self.assertEqual(expected, self.instance.to_dict(self.data_ok))

    def test_malformed_data(self):
        self.assertEqual(self.instance.to_dict(self.data_bogus), {})

    def test_junk_data(self):
        self.assertEqual(None, self.instance.to_dict(self.data_junk))

    def test_long_binary_junk_data(self):
        self.assertRaises(ValueError, self.instance.to_dict, self.data_longjunk)

    def test_cyrillic(self):
        expected = {'message': u'У нас есть кириллица в логах'}
        print(self.data_cyrillic)
        self.assertEqual(expected, self.instance.to_dict(self.data_cyrillic))
