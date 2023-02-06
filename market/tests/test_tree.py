# -*- coding: utf-8 -*-

import unittest

from guruindexer.tree import Node, make_tree, add_ancestors, remove_ancestors


class Test(unittest.TestCase):
    def setUp(self):
        nodes = [Node(0, None), Node(1, 0), Node(2, 0), Node(3, 1)]
        self._tree = make_tree(nodes)

    def test_1(self):
        self.assertEqual(self._tree.root().id, 0)
        self.assertEqual(len(self._tree.nodes()), 4)

    def test_2(self):
        self.assertEqual(sorted(remove_ancestors([1, 2, 3], self._tree)), [2, 3])
        self.assertEqual(sorted(add_ancestors([2, 3], self._tree)), [0, 1, 2, 3])
