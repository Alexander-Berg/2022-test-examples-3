# -*- coding: utf-8 -*-

import unittest

from market.idx.snippets.src import feed


class UrlFixerTest(unittest.TestCase):
    def setUp(self):
        self.urlfixer = feed.UrlFixer()

    def test(self):
        fix = self.urlfixer.query_remove_marks
        self.assertEqual(fix(''), '')
        self.assertEqual(fix('a'), 'a')
        self.assertEqual(fix('a&b'), 'a&b')
        self.assertEqual(fix('a=1&b=2'), 'a=1&b=2')
        self.assertEqual(fix('a=1&b'), 'a=1&b')
        self.assertEqual(fix('from='), '')
        self.assertEqual(fix('from=12'), '')
        self.assertEqual(fix('from=12&a=1'), 'a=1')
        self.assertEqual(fix('from=12&a=1&b=2'), 'a=1&b=2')
        self.assertEqual(fix('from=12&partner=12&a=1'), 'a=1')
