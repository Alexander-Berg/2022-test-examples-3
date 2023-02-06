# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from builtins import next
from builtins import str
import pytest
from hamcrest import assert_that, contains_inanyorder

from travel.rasp.library.python.common23.utils.iterrecipes import unique_everseen, unique_justseen, pairwise, product_by_key, chunker


def test_pairwise():
    input_sequence = [1, 2, 3]
    output_iter = pairwise(input_sequence)

    assert list(next(output_iter)) == [1, 2]
    assert list(next(output_iter)) == [2, 3]

    with pytest.raises(StopIteration):
        next(output_iter)


def test_unique_everseen():
    assert list(unique_everseen('AAAABBBCCDAABBB')) == ['A', 'B', 'C', 'D']
    assert list(unique_everseen('ABBCcAD', key=str.lower)) == ['A', 'B', 'C', 'D']


def test_unique_justseen():
    assert list(unique_justseen('AAAABBBCCDAABBB')) == ['A', 'B', 'C', 'D', 'A', 'B']
    assert list(unique_justseen('ABBCcAD', key=str.lower)) == ['A', 'B', 'C', 'A', 'D']


def test_product_by_key():
    assert_that(
        product_by_key({'foo': [1, 2], 'bar': ['a', 'b']}),
        contains_inanyorder(
            {'foo': 1, 'bar': 'a'},
            {'foo': 1, 'bar': 'b'},
            {'foo': 2, 'bar': 'a'},
            {'foo': 2, 'bar': 'b'}
        )
    )


def test_chunker_simple():
    assert list(chunker('aaabbb', 3)) == [
        ['a', 'a', 'a'],
        ['b', 'b', 'b']
    ]


def test_chunker_not_full():
    assert list(chunker('aaabbbc', 3)) == [
        ['a', 'a', 'a'],
        ['b', 'b', 'b'],
        ['c']
    ]
