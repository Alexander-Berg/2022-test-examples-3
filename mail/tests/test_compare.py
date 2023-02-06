# coding: utf-8

import pytest

from ora2pg.compare import is_seq, AreEqual

parametrize = pytest.mark.parametrize


@parametrize('seq', [
    [1, 2],
    (1, 2),
])
def test_is_seq_for_seqs(seq):
    assert is_seq(seq)


def test_generator_is_seq():
    assert is_seq((x for x in range(5)))


@parametrize('test_str', [
    'abc',
    u'def',
])
def test_str_is_not_seq(test_str):
    assert not is_seq(test_str)


class SObject(object):
    def __init__(self, **kwargs):
        self.__dict__.update(kwargs)

    def as_dict(self):
        return self.__dict__


def are_equal(l, r, volatile_fields=None):
    return AreEqual(
        sorter=sorted,
        volatile_getter=lambda o: set(volatile_fields or [])
    )(l, r, name='test')


class TestAreEqual(object):
    # pylint: disable=R0201
    def test_numbers_are_equal(self):
        assert are_equal(
            10, 10
        )

    def test_numbers_not_equal(self):
        assert not are_equal(10, 20)

    def test_equal_seq(self):
        assert are_equal([1, 2, 3], [3, 2, 1])

    def test_not_equal_seqs_with_differenct_elemnts_count(self):
        assert not are_equal([1], [2, 3])

    def test_equal_serializable(self):
        assert are_equal(
            SObject(
                foo='bar'
            ),
            SObject(
                foo='bar'
            )
        )

    def test_not_equal_serializable(self):
        assert not are_equal(
            SObject(
                foo='bar'
            ),
            SObject(
                foo='BAR'
            )
        )

    def test_serializable_with_diff_in_volatile(self):
        assert are_equal(
            SObject(
                foo='bar',
                baz='WAT',
            ),
            SObject(
                foo='bar',
                baz='MAN!'
            ),
            ['baz']
        )

    def test_serializable_with_missed_attribute(self):
        assert not are_equal(
            SObject(
                foo='bar',
                baz='wat'
            ),
            SObject(
                foo='bar'
            )
        )

    def test_nested_serializable(self):
        assert are_equal(
            SObject(
                foo='bar',
                baz=SObject(
                    wat='man'
                ),
            ),
            SObject(
                foo='bar',
                baz=SObject(
                    wat='man'
                ),
            )
        )

    def test_raises_when_all_fields_are_volatile(self):
        with pytest.raises(AssertionError):
            are_equal(
                SObject(foo='bar'),
                SObject(foo='bar'),
                ['foo']
            )

    def test_raises_when_try_compare_serializable_and_not(self):
        with pytest.raises(AssertionError):
            are_equal(
                SObject(foo='bar'),
                42
            )
