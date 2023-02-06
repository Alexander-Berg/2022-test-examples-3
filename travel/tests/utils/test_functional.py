# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import mock

from travel.rasp.wizards.wizard_lib.utils.functional import compose, tuplify


def test_compose():
    func_1 = mock.Mock(return_value=mock.sentinel.func_1_result)
    func_2 = mock.Mock(return_value=mock.sentinel.func_2_result)
    func_3 = mock.Mock(return_value=mock.sentinel.func_3_result)

    assert compose(func_1, func_2, func_3)(mock.sentinel.dummy_value) == mock.sentinel.func_3_result
    func_1.assert_called_once_with(mock.sentinel.dummy_value)
    func_2.assert_called_once_with(mock.sentinel.func_1_result)
    func_3.assert_called_once_with(mock.sentinel.func_2_result)


def test_tuplify():
    @tuplify
    def dummy_func(l):
        for i in l:
            yield 'odd' if i % 2 else 'even'

    assert dummy_func([1, 2, 3]) == ('odd', 'even', 'odd')
