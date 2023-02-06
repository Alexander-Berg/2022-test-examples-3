import pytest

from crypta.dmp.common.metrics import common


def test_calc_matching_rate():
    assert 0.5 == pytest.approx(common.calc_matching_rate(5, 10))


def test_calc_matching_rate_zero_sizes():
    assert 0 == common.calc_matching_rate(0, 0)
