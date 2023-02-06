from market.dynamic_pricing.pricing.common.checks.solomon_sender.price_report_checker import GroupInfo
from market.dynamic_pricing.pricing.library.errors import PriceCalcErrors
from market.dynamic_pricing.pricing.library.types import PriceGroupType
import pytest
from contextlib import contextmanager


@pytest.fixture
def group_info():
    return GroupInfo(2000)


def test_errors_crossed(group_info):
    errors = [
        PriceCalcErrors.PRICE_BELOW_LOWER_BOUND_CROSSED.error_code,
        PriceCalcErrors.PRICE_CANNOT_BE_CALCULATED.error_code,
        PriceCalcErrors.PRICE_ABOVE_UPPER_BOUND_CROSSED.error_code
    ]
    group_info.check_ssku_errors(errors, PriceGroupType.DCO_CROSSED)
    assert 2 == sum(v > 0 for k, v in group_info.metrics.items())


def test_errors_sell(group_info):
    errors = [
        PriceCalcErrors.PRICE_BELOW_LOWER_BOUND_CROSSED.error_code,
        PriceCalcErrors.PRICE_CANNOT_BE_CALCULATED.error_code,
        PriceCalcErrors.PRICE_ABOVE_UPPER_BOUND_CROSSED.error_code
    ]
    group_info.check_ssku_errors(errors, PriceGroupType.DCO)
    assert 1 == sum(v > 0 for k, v in group_info.metrics.items())


def test_errors_empty(group_info):
    errors = []
    group_info.check_ssku_errors(errors, PriceGroupType.DEADSTOCK)
    assert 0 == sum(v > 0 for k, v in group_info.metrics.items())
