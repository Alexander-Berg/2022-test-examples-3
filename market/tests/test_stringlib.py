# -*- coding: utf-8 -*-

import unittest

from market.idx.snippets.src.stringlib import remove_multispaces, truncate
from market.idx.snippets.src.shopsdat2 import toutf8


class TestStringlib(unittest.TestCase):
    def test_remove_multispaces(self):
        self.assertEqual(remove_multispaces('1 2\n3\r4\r\n5 \n6\t7'), '1 2 3 4 5 6 7')

    def test_truncate(self):
        s = u'привет медвед'

        self.assertEqual(truncate(s, 1), u'п')
        self.assertEqual(truncate(s, 2), u'пр')
        self.assertEqual(truncate(s, 3), u'при')
        self.assertEqual(truncate(s, 4), u'прив')
        self.assertEqual(truncate(s, 5), u'приве')

        for i in range(6, 13):
            self.assertEqual(truncate(s, i), u'привет')

        self.assertEqual(truncate(s, 13), s)
        self.assertEqual(truncate(s, 14), s)

        self.assertEqual(truncate(u'1 2 3', 3), '1 2')

    def test_toutf8(self):
        self.assertEqual(toutf8(None), None)
        self.assertEqual(toutf8('foo'), 'foo')
        self.assertEqual(toutf8(u'мир'), 'мир')
