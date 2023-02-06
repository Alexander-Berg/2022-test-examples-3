# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import collections
import unittest

from mpfs.common.util import merge2


class Merge2TestCase(unittest.TestCase):
    """Набор тестов для функции `mpfs.common.util.merge2`."""

    def test_ascending(self):
        g = merge2(([1, 3], iter([2, 4]), [5, 8], [6, 7], (0,), {9, 10}))
        assert tuple(g) == (0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

    def test_descending(self):
        g = merge2(([3], (1,), iter([4, 2]), [8, 5], [7, 6], (10, 9, 0)), reverse=True)
        assert tuple(g) == (10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0)

    def test_key_ascending(self):
        O = collections.namedtuple('O', 'key')
        g = merge2(
            ([O(1), O(3)], iter([O(2), O(4)]), [O(5), O(8)], [O(6), O(7)], (O(0),), (O(9), O(10))),
            key=lambda o: o.key
        )
        assert tuple(o.key for o in tuple(g)) == (0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

    def test_key_desending(self):
        O = collections.namedtuple('O', 'key')
        g = merge2(([O(3)], (O(1),), iter([O(4), O(2)]), [O(8), O(5)], [O(7), O(6)], (O(10), O(9), O(0))), reverse=True)
        assert tuple(o.key for o in tuple(g)) == (10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0)

