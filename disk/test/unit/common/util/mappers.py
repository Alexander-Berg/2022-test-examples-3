# -*- coding: utf-8 -*-

import unittest
from mpfs.common.util.mappers import ListMapper, EasyMapper


class ListMapperTestCase(unittest.TestCase):
    def test_common(self):
        lm = ListMapper([1, 2, 3, 4, 5])
        assert lm.map([3, 5, 2, 55]) == [None, 2, 3, None, 5]
        assert lm.map([2]) == [None, 2, None, None, None]

        lm = ListMapper([1, 4])
        lm.map([{'key': 4, 'cool': 'very'}], key_getter=lambda i: i['key']) == [None, {'key': 4, 'cool': 'very'}]


class EasyMapperTestCase(unittest.TestCase):
    def test_map_unmap(self):
        em = EasyMapper([
            ('a', 1),
            ('a', 2),
            ('b', 3),
        ])
        assert em.map('a') == 1
        assert em.map('b') == 3
        assert em.map('c') == 'c'
        assert em.unmap(1) == 'a'
        assert em.unmap(2) == 'a'
        assert em.unmap(3) == 'b'
        assert em.unmap(4) == 4

    def test_map_unmap_iterable(self):
        em = EasyMapper([
            ('a', 1),
            ('a', 2),
            ('b', 3),
        ])
        assert list(em.map_iterable(['a', 'f', 'b', 'b'])) == [1, 'f', 3, 3]
        src = ['a', 'f', 'b', 'b']
        assert list(em.unmap_iterable(em.map_iterable(src))) == src

    def test_map_unmap_dict(self):
        em = EasyMapper([
            ('a', 1),
            ('a', 2),
            ('b', 3),
        ])
        src = {
            'a': [],
            'f': (),
            'b': {}
        }
        assert em.unmap_dict_keys(em.map_dict_keys(src)) == src
        assert em.map_dict_keys({'a': 1, 'b': 2, 'm': 'm'}) == {1: 1, 3: 2, 'm': 'm'}
