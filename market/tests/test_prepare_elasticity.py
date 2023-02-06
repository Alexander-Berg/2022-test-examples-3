# coding=utf-8
import pytest


from market.dynamic_pricing.buybox.nirvana.prepare_elasticity_report.executables.python.prepare_elasticity import (
    interpolate_linear,
    compress_elasticity_interpolate,
    clean_elasticity,
    check_elasticity_correctness,
    calc_demand,
    ElasticityCheckStatus
)

from test_data import (
    msku_561001011_elasticity_09_28
)


def test_interpolate_linear():
    assert interpolate_linear(x1=0, x2=3, y1=0, y2=3, x=1.5) == 1.5
    assert interpolate_linear(x1=0, x2=3, y1=0, y2=3, x=3) == 3
    assert interpolate_linear(x1=0, x2=3, y1=0, y2=3, x=0) == 0


# TODO: check with statuses
def test_check_if_elasticity_is_correct():
    # not correct - min price is too low less than max gmv
    price_demand = [(1, 8), (2, 9), (3, 5), (4, 3), (5, 2), (6, 1), (7, 1), (8, 2)]
    assert check_elasticity_correctness(price_demand, 1) == ElasticityCheckStatus.MIN_PRICE_LESS_MAX_GMV_PRICE

    # not correct - min price is too high
    price_demand = [(2, 9), (3, 5), (4, 3), (5, 2), (6, 1), (7, 1), (8, 1)]
    assert check_elasticity_correctness(price_demand, 9) == ElasticityCheckStatus.MIN_PRICE_OUTSIDE_OF_RANGE_RIGHT

    # not correct - in gmv tail, gmv is rising
    price_demand = [(2, 9), (3, 5), (4, 3), (5, 2), (6, 1), (7, 1), (7.5, 1), (8, 1), (8.5, 1), (9, 1)]
    assert check_elasticity_correctness(price_demand, 8) == ElasticityCheckStatus.GMV_RAISING_IN_RANGE

    # too low demand
    price_demand = [(2, 9), (3, 5), (4, 3), (5, 2), (6, 0.3), (7, 0.1), (8, 0.1), (9, 0.1), (10, 0.1)]
    assert check_elasticity_correctness(price_demand, 7, min_demand=0.1) == ElasticityCheckStatus.MIN_DEMAND

    # correct case
    price_demand = [(2, 0.5), (3, 0.2), (4, 0.1), (5, 0.08), (6, 0.06), (7, 0.05), (8, 0.03)]
    assert check_elasticity_correctness(price_demand, 5) == ElasticityCheckStatus.CORRECT

    # correct case
    price_demand = [(2, 9), (3, 5), (4, 3), (5, 2), (6, 1), (7, 1), (8, 0.6), (9, 0.5), (10, 0.1), (11, 0.08), (12, 0.07)]
    assert check_elasticity_correctness(price_demand, 5) == ElasticityCheckStatus.CORRECT


def test_correct_elasticity_real_data():
    assert check_elasticity_correctness(msku_561001011_elasticity_09_28, 2500) == ElasticityCheckStatus.MIN_PRICE_LESS_MAX_GMV_PRICE
    assert check_elasticity_correctness(msku_561001011_elasticity_09_28, 5700) == ElasticityCheckStatus.GMV_RAISING_IN_RANGE
    assert check_elasticity_correctness(msku_561001011_elasticity_09_28, 5700, min_demand=1200) == ElasticityCheckStatus.MIN_DEMAND


def test_price_demand_validation():
    # not correct - not sorted by price
    price_demand = [(1, 5), (2, 9), (3, 5), (5, 3), (4, 2), (6, 1), (7, 1), (8, 2)]
    with pytest.raises(ValueError):
        assert not check_elasticity_correctness(price_demand, 1)

    # not correct - not sorted by price
    price_demand = [(1, 5), (2, 9), (3, 5), (3, 3), (4, 2), (6, 1), (7, 1), (8, 2)]
    with pytest.raises(ValueError):
        assert not check_elasticity_correctness(price_demand, 1)

    # not correct - negative price
    price_demand = [(1, 5), (2, 9), (3, 5), (3, 3), (4, 2), (6, 1), (7, 1), (8, 2)]
    with pytest.raises(ValueError):
        assert not check_elasticity_correctness(price_demand, 1)


def test_calculate_demand():
    price_demand = [(1, 5), (2, 9), (3, 5), (4, 3), (5, 2), (6, 1), (7, 1), (8, 3)]

    # price less than min
    assert calc_demand(0.5, price_demand) == 5

    # price more than max
    assert calc_demand(9, price_demand) == 0

    # ordinal cases
    assert calc_demand(2.5, price_demand) == 7

    # ordinal cases
    assert calc_demand(3, price_demand) == 5

    # ordinal cases
    assert calc_demand(1, price_demand) == 5

    # ordinal cases
    assert calc_demand(8, price_demand) == 3


def test_compress_elasticity_interpolate():
    price_demand = [(1, 5), (2, 9), (3, 5), (4, 3), (5, 2), (6, 1), (7, 1), (8, 3)]
    with pytest.raises(ValueError):
        compress_elasticity_interpolate(price_demand, msku_min_price=None, n=10)
    with pytest.raises(ValueError):
        compress_elasticity_interpolate(price_demand, msku_min_price=-1, n=10)

    # empty elasticity
    assert compress_elasticity_interpolate([], 40) == []

    # too low elasticity values
    assert compress_elasticity_interpolate([(10, 50), (20, 90), (30, 50)], 40) == []

    price_demand = [(10, 50), (20, 90), (30, 50), (40, 30), (50, 20), (60, 10), (70, 4), (80, 5), (90, 2), (100, 1)]

    # too low elasticity values in price range
    assert compress_elasticity_interpolate(price_demand, msku_min_price=40, n=3) == []

    assert compress_elasticity_interpolate(price_demand, msku_min_price=50, n=3) == [(30, 50), (40, 30), (50, 20)]


def test_clean_elasticity():
    price_demand = [(1, 5), (2, 9), (3, 5), (4, 3), (5, 2), (6, 1), (7, 0.4), (8, 0.5)]
    result = clean_elasticity(price_demand, msku_min_price=5, min_price_left_border=0.4, min_price_right_border=0.2)
    assert result == [(3, 5), (4, 3), (5, 2), (6, 1)]

    result = clean_elasticity(price_demand, msku_min_price=5, min_price_left_border=0.1, min_price_right_border=0.1)
    assert len(result) == 0

    # change lower gmv values to the left from max_gmv to max_gmv
    elasticity = clean_elasticity(price_demand, msku_min_price=5, min_price_left_border=1, min_price_right_border=0.2)
    assert elasticity == [(1, 18), (2, 9), (3, 5), (4, 3), (5, 2), (6, 1)]
