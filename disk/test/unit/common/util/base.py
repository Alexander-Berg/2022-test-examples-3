# -*- coding: utf-8 -*-
import itertools

from hamcrest import assert_that, calling, raises
from mock import MagicMock, patch
from nose_parameterized import parameterized
import unittest

from mpfs.common.util import chunks2, chunks, _check_percentage_value, filter_uid_by_percentage, filter_value_by_percentage, SuppressAndLogExceptions, grouped_chunks


class ChunksTestCase(unittest.TestCase):
    """Набор тестов для функций `chunks` и `chunks2`"""

    both = parameterized.expand([
        (chunks,),
        (chunks2,),
    ])

    @both
    def test_common(self, test_func):
        assert list(test_func(range(5))) == [[0, 1], [2, 3], [4]]

    @both
    def test_empty_list(self, test_func):
        assert list(test_func([])) == []

    @both
    def test_chunk_size(self, test_func):
        assert list(test_func(range(3), chunk_size=1)) == [[0], [1], [2]]
        assert list(test_func(range(3), chunk_size=2)) == [[0, 1], [2]]
        assert list(test_func(range(3), chunk_size=3)) == [[0, 1, 2]]
        assert list(test_func(range(3), chunk_size=4)) == [[0, 1, 2]]

    def test_iteration_obj(self):
        assert list(chunks2(xrange(5))) == [[0, 1], [2, 3], [4]]
        assert list(chunks2(iter(range(5)))) == [[0, 1], [2, 3], [4]]


class GroupedChunksTestCase(unittest.TestCase):

    def test_common(self):
        func_result = list(grouped_chunks(['aa', 'bb', 'cc', 'ddd', 'eee'], len, chunk_size=2))
        assert func_result == [(2, ['aa', 'bb']), (3, ['ddd', 'eee']), (2, ['cc'])]

    def test_chunk_size(self):
        func_result = list(grouped_chunks(['aa', 'bb', 'cc', 'ddd', 'eee'], len, chunk_size=1))
        assert func_result == [(2, ['aa']), (2, ['bb']), (2, ['cc']), (3, ['ddd']), (3, ['eee'])]

        func_result = list(grouped_chunks(['aa', 'bb', 'cc', 'ddd', 'eee'], len, chunk_size=3))
        assert func_result == [(2, ['aa', 'bb', 'cc']), (3, ['ddd', 'eee'])]

    def test_iterable_obj(self):
        def generator():
            for s in ('aa', 'bb', 'cc', 'ddd', 'eee'):
                yield s
        func_result = list(grouped_chunks(generator(), len, chunk_size=2))
        assert func_result == [(2, ['aa', 'bb']), (3, ['ddd', 'eee']), (2, ['cc'])]

    def test_empty_list(self):
        assert list(grouped_chunks([], len)) == []

    def test_all_values_are_present_in_chunks(self):
        before_values = list(itertools.product('abcdef', repeat=2))
        after_values = []
        for key, chunk in grouped_chunks(before_values, len):
            after_values.extend(chunk)
        before_values.sort()
        after_values.sort()
        assert after_values == before_values


class CheckPercentageTestCase(unittest.TestCase):

    def test_wrong_type(self):
        assert_that(calling(_check_percentage_value).with_args(10.5), raises(TypeError))

    @parameterized.expand([
        (101,),
        (-1,)
    ])
    def test_wrong_value(self, percentage):
        assert_that(calling(_check_percentage_value).with_args(percentage), raises(ValueError))


class FilteringUsersByPercentageTestCase(unittest.TestCase):

    @parameterized.expand([
        ('100000000', True),
        ('100000100', True),
        ('100000019', True),
        ('100000020', False),
        ('100000099', False),
    ])
    def test_correct_filtering(self, uid, is_remained):
        percentage = 20
        assert filter_uid_by_percentage(uid, percentage) is is_remained


class FilteringStringByPercentageTestCase(unittest.TestCase):

    @parameterized.expand([
        ('abc', False),
        ('test_string', True),
        (u'ыфа%9A', True),
        (123, False),
        ({1: 2}, False),
        ([1, 2, 3], False),
    ])
    def test_correct_filtering(self, value, is_remained):
        percentage = 20
        assert filter_value_by_percentage(value, percentage) is is_remained


class SuppressAndLogExceptionsTestCase(unittest.TestCase):
    def setUp(self):
        self.logger = MagicMock()
        self.logger.warning = MagicMock()
        self.patch_suppress_enabled = patch.dict('mpfs.config.settings.feature_toggles',
                                                 {'suppress_exception_block_enabled': True})
        self.patch_suppress_enabled.start()

    def tearDown(self):
        self.patch_suppress_enabled.stop()

    def raise_key_error(self):
        {}['missed_key']

    def test_doesnt_suppress_not_listed_exception(self):
        with SuppressAndLogExceptions(self.logger, TypeError):
            assert_that(calling(self.raise_key_error), raises(KeyError))
            self.logger.warning.assert_not_called()

    def test_suppress_exception(self):
        with SuppressAndLogExceptions(self.logger, KeyError):
            self.raise_key_error()
            self.logger.warning.assert_called()

    def test_suppress_listed_exception(self):
        with SuppressAndLogExceptions(self.logger, TypeError, KeyError):
            self.raise_key_error()
            self.logger.warning.assert_called()

    def test_suppress_listed_super_class_exception(self):
        with SuppressAndLogExceptions(self.logger, Exception):
            self.raise_key_error()
            self.logger.warning.assert_called()
