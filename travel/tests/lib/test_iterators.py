# -*- coding: utf-8 -*-

from travel.avia.admin.tests.lib.unittests.testcase import TestCase

from travel.avia.admin.lib.iterators import chunker


class ChunkerTest(TestCase):
    def testSimple(self):
        self.assertListEqual(
            [
                ['a', 'a', 'a'],
                ['b', 'b', 'b']
            ], list(chunker('aaabbb', 3)))

    def testNotFull(self):
        self.assertListEqual(
            [
                ['a', 'a', 'a'],
                ['b', 'b', 'b'],
                ['c']
            ], list(chunker('aaabbbc', 3)))
