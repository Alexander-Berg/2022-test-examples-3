import pytest

import crypta.cm.services.db_stats.lib as db_stats


ARRAY = [0, 1, 2, 20, 50, 100, 500, 999999]


def test_upper_bound_for_rounded_float():
    assert 0 == db_stats.upper_bound_for_rounded_float(ARRAY, -1)
    assert 1 == db_stats.upper_bound_for_rounded_float(ARRAY, 0)
    assert 2 == db_stats.upper_bound_for_rounded_float(ARRAY, 1)
    assert 20 == db_stats.upper_bound_for_rounded_float(ARRAY, 2)

    assert 20 == db_stats.upper_bound_for_rounded_float(ARRAY, 19)
    assert 50 == db_stats.upper_bound_for_rounded_float(ARRAY, 20)

    assert 100 == db_stats.upper_bound_for_rounded_float(ARRAY, 99)
    assert 500 == db_stats.upper_bound_for_rounded_float(ARRAY, 100)

    assert 999999 == db_stats.upper_bound_for_rounded_float(ARRAY, 500)
    assert 999999 == db_stats.upper_bound_for_rounded_float(ARRAY, 999998)


@pytest.mark.xfail(raises=AssertionError)
def test_upper_bound_for_rounded_float_overflow():
    db_stats.upper_bound_for_rounded_float(ARRAY, 999999)
