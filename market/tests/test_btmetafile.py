#!/usr/bin/python
# -*- coding: utf-8 -*-

import unittest

from lib.btmetafile import load_comment


class Test(unittest.TestCase):
    def test(self):
        self.assertEqual(load_comment('{"dist": "a", "version": "b"}'), ('a', 'b', None))


if __name__ == '__main__':
    unittest.main()
