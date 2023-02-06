# -*- coding: utf-8 -*-

import unittest

from getter import core


class Test(unittest.TestCase):
    def test_match(self):
        names_pattern_result_list = [
            (['a/1', 'a/2'], 'a/*', ['a/1', 'a/2']),
            (['a/1', 'a/2'], 'a/.*', ['a/1', 'a/2']),
            (['foo', 'bar.txt'], '.*.txt', ['bar.txt']),
            (['1', '2'], '*', ['1', '2']),
        ]
        for names, pattern, result in names_pattern_result_list:
            self.assertEqual(core._match(names, pattern), result)


if __name__ == '__main__':
    unittest.main()
