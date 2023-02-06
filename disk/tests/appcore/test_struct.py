# encoding: UTF-8

import unittest

from hamcrest import *

from appcore.struct import (
    coalesce,
    maybe_list,
    expect_type,
)


class CoalesceTestCase(unittest.TestCase):
    def test_no_args(self):
        value = coalesce()

        assert_that(
            value,
            is_(None),
        )

    def test_all_args_are_false(self):
        false_values = [None, '', 0, False, {}, (), set(), []]

        for false_value in false_values:
            value = coalesce(false_value)

            assert_that(
                value,
                is_(false_value),
            )

        value = coalesce(*false_values)

        assert_that(
            value,
            is_(false_values[-1]),
        )

    def test_with_true_args(self):
        false_values = [None, '', 0, False, {}, (), set(), []]
        true_values = ['a', 1, True, {'a': 1}, (1,), {1}, [1]]

        for true_value in true_values:
            value = coalesce(true_value)

            assert_that(
                value,
                is_(true_value)
            )

            values = list(false_values) + [true_value]
            value = coalesce(*values)

            assert_that(
                value,
                is_(true_value)
            )

        values = false_values + true_values
        value = coalesce(*values)

        assert_that(
            value,
            is_(true_values[0]),
        )


class MaybeListTestCase(unittest.TestCase):
    def test_non_list(self):
        non_list_values = [None, 1, '2', True, {}, (), set()]

        for non_list_value in non_list_values:
            value = maybe_list(non_list_value)

            assert_that(
                value,
                all_of(
                    instance_of(list),
                    has_length(1),
                    has_item(is_(non_list_value)),
                ),
            )

    def test_list(self):
        list_value = [1, 'a']

        value = maybe_list(list_value)

        assert_that(
            value,
            all_of(
                instance_of(list),
                equal_to(list_value),
                is_(list_value),
            ),
        )


class ExpectTypeTestCase(unittest.TestCase):
    def test_expect_type(self):
        type_map = {
            int: 1,
            long: 1L,
            float: 0.,
            str: 'abc',
            list: [],
        }

        for t, v in type_map.items():
            value = expect_type(v, t)

            assert_that(
                value,
                is_(v),
            )

        for t1, v1 in type_map.items():
            for t2, v2 in type_map.items():
                value = expect_type(v1, [t1, t2])

                assert_that(
                    value,
                    is_(v1),
                )

                value = expect_type(v2, [t1, t2])

                assert_that(
                    value,
                    is_(v2),
                )

    def test_raises_on_unexpected_type(self):
        assert_that(
            calling(expect_type).with_args(1, str),
            raises(ValueError, 'unexpected'),
        )
