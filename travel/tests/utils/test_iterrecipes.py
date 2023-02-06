# -*- coding: utf-8 -*-

import pytest

from travel.avia.library.python.common.utils.iterrecipes import unique_everseen, unique_justseen, pairwise


def test_pairwise():
    input_sequence = [1, 2, 3]
    output_iter = pairwise(input_sequence)

    assert list(output_iter.next()) == [1, 2]
    assert list(output_iter.next()) == [2, 3]

    with pytest.raises(StopIteration):
        output_iter.next()


def test_unique_everseen():
    assert list(unique_everseen('AAAABBBCCDAABBB')) == ['A', 'B', 'C', 'D']
    assert list(unique_everseen('ABBCcAD', key=str.lower)) == ['A', 'B', 'C', 'D']


def test_unique_justseen():
    assert list(unique_justseen('AAAABBBCCDAABBB')) == ['A', 'B', 'C', 'D', 'A', 'B']
    assert list(unique_justseen('ABBCcAD', key=str.lower)) == ['A', 'B', 'C', 'A', 'D']
