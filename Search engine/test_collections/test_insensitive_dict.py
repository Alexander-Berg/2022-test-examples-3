# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from search.martylib.collections import InsensitiveDict
from search.martylib.test_utils import TestCase


class TestInsensitiveDict(TestCase):
    def test_lower(self):
        d = InsensitiveDict()
        d['foo'] = 'bar'
        self.assertEqual(d['FOO'], 'bar')

    def test_underscore(self):
        d = InsensitiveDict()
        d['f_o_o'] = 'bar'
        self.assertEqual(d['f-o-o'], 'bar')

    def test_non_string_keys(self):
        d = InsensitiveDict()
        d[('foo', 'bar')] = 'baz'
        self.assertEqual(d[('foo', 'bar')], 'baz')
        self.assertFalse(('FOO', 'BAR') in d)

    def test_from_dict(self):
        d = InsensitiveDict.from_dict({'foo': 1})
        self.assertEqual(d['FOO'], 1)

    def test_contains(self):
        d = InsensitiveDict.from_dict({'foo': 1})
        self.assertTrue('FOO' in d)
