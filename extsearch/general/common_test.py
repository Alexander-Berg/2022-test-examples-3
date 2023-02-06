import pytest

from common import *


def test_dict_to_list():
    tests = [
        ({2: 'a', 3: 'b', 0: 's', 1: 'd'}, ['s', 'd', 'a', 'b']),
        ({0: 'x'}, ['x']),
        ({}, [])
    ]

    for dct, ans in tests:
        assert dict_to_list(dct) == ans


def test_shuffle_groups():
    def shuffler_a(lst):
        lst[:] = lst[1:] + lst[:1]

    tests = [
        ([('a', 0), ('b', 10), ('a', 1), ('c', 20)], lambda x: x[0], shuffler_a,
         [('a', 1), ('b', 10), ('a', 0), ('c', 20)]),
        ([('a', 0), ('b', 10), ('a', 1), ('b', 20), ('b', 30)], lambda x: x[0], shuffler_a,
         [('a', 1), ('b', 20), ('a', 0), ('b', 30), ('b', 10)])
    ]

    for lst, key, shuffler, ans in tests:
        lst_copy = lst[:]
        shuffle_groups(lst_copy, key, shuffler)
        assert lst_copy == ans
