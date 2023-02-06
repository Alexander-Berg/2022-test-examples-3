from core.application import Application
from core.testenv import testenv
from core.yt_context import YtContext
from core.tables import BoundsRuleName
import os, json, logging
from datetime import datetime, timedelta
import pytest


@pytest.fixture
def application(testenv):
    return Application(testenv)


@pytest.fixture
def yt_context(testenv):
    ctx = YtContext(testenv)
    ctx.erp_input \
        .add_row(group_id=1, msku=1, ssku="30", high_price=9000, current_price=7000, low_price=8000, purchase_price=7000,
                rule_name=BoundsRuleName.other, stock=1) \
        .add_row(group_id=1, msku=1, ssku="31", high_price=7000, current_price=7000, low_price=7000, purchase_price=7000,
                rule_name=BoundsRuleName.fix, stock=1)
    ctx.demand_input \
        .add_row(1, 7000, 1) \
        .add_row(1, 8000, 1)
    margin_v1_config = {
        "max_delta": 0.15,
        "max_lower_price_delta": 0.3,
        "target_margin": 0,
        "max_expand_delta": 0.1,
        "target_margin_threshold": 0.0001,
        "price_bound_calculator": "margin"
    }
    ctx.config \
        .add_row(1, 0, "trivial", {}, "margin.v1", margin_v1_config, "dummy", {})

    with ctx:
        yield ctx


def test_fix_and_autostrategy_ssku_with_remain_stock(application, yt_context):
    prices_path, _, _, metrics_path, _ = application.run(yt_context)

    metrics = yt_context.read_whole(metrics_path)
    assert len(metrics) == 2
    assert metrics[1]["group_id"] == 1
    assert metrics[1]["gmv"] == 15000.0
    assert metrics[1]["profit"] == 1000.0
    assert metrics[1]["margin"] == 0.06666666666666667

    prices = yt_context.read_whole(prices_path)
    prices_auto = prices[0]
    prices_fix = prices[1]

    assert prices_fix["msku"] == prices_auto["msku"]
    assert prices_fix["ssku"] == "31"
    assert prices_fix["rule_name"] == BoundsRuleName.fix
    assert prices_auto["ssku"] == "30"
    assert prices_auto["rule_name"] == BoundsRuleName.other

    assert prices_fix["new_price"] == 7000
    assert prices_auto["new_price"] == 8000
    assert prices_fix == {
        "high_price": 7000.0,
        "msku": 1L,
        "ssku": "31",
        "low_price": 7000.0,
        "new_price": 7000.0,
        "current_price": 7000.0,
        "group_id": 1,
        "demand": 1,
        "is_price_random": False,
        "raw_low_price": 7000.0,
        "raw_high_price": 7000.0,
        "buy_price": 7000.0,
        "rule_name": "ФиксЦен"
    }
    assert prices_auto == {
        "high_price": 7000.0*1.15,
        "msku": 1L,
        "ssku": "30",
        "low_price": 8000.0,
        "new_price": 8000.0,
        "current_price": 7000.0,
        "group_id": 1L,
        "demand": 1.0,
        "is_price_random": False,
        "raw_low_price": 8000.0,
        "raw_high_price": 9000.0,
        "buy_price": 7000.0,
        "rule_name": "Другое"
    }
