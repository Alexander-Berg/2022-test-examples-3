# -*- coding: utf-8 -*-
import pytest

from travel.rasp.admin.timecorrection.utils import n_wize, accumulate, undo_accumulate


def test_n_wize_pair():
    """проверка n_wize для n=2"""
    iterable = [1, 2, 3, 4]
    result = [(1, 2), (2, 3), (3, 4)]
    assert result == list(n_wize(iterable, n=2))


def test_n_wize_trio():
    """проверка n_wize для n=3"""
    iterable = [1, 2, 3, 4]
    result = [(1, 2, 3), (2, 3, 4)]
    assert result == list(n_wize(iterable, n=3))


@pytest.mark.parametrize('iterable, result', [
    [[1, 1, 1, 1], [1, 2, 3, 4]],
    [[1, 2, 3, 4], [1, 3, 6, 10]]
])
def test_accumulate(iterable, result):
    """проверка accumulate"""
    assert result == list(accumulate(iterable))


@pytest.mark.parametrize('iterable, result', [
    [[1, 2, 3, 4], [1, 1, 1, 1]],
    [[1, 3, 6, 10], [1, 2, 3, 4]]
])
def test_undo_accumulate(iterable, result):
    """проверка undo_accumulate"""
    assert result == list(undo_accumulate(iterable))
