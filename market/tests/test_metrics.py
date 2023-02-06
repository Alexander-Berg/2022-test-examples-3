from core.application import Application
from core.testenv import testenv
from core.yt_context import YtContext
import os, json, logging
from datetime import datetime
import pytest


@pytest.fixture
def application(testenv):
    return Application(testenv)


@pytest.fixture
def yt_context(testenv):
    ctx = YtContext(testenv)
    ctx.erp_input \
        .add_row(group_id=0, msku=1, ssku="10", high_price=11000, current_price=8000, low_price=4000, purchase_price=8000,
                is_promo=None) \
        .add_row(group_id=0, msku=2, ssku="20", high_price=11000, current_price=8000, low_price=4000, purchase_price=8000,
                is_promo=True) \
        .add_row(group_id=0, msku=3, ssku="30", high_price=11000, current_price=8000, low_price=4000, purchase_price=8000,
                is_promo=False) \
        .add_row(group_id=0, msku=4, ssku="40", high_price=8800, current_price=8000, low_price=8800, purchase_price=8000,
                is_promo=False) \
        .add_row(group_id=0, msku=5, ssku="50", high_price=4000, current_price=8000, low_price=8800, purchase_price=8000,
                is_promo=False) \
        .add_row(group_id=1, msku=6, ssku="60", high_price=11000, current_price=8000, low_price=4000, purchase_price=8000,
                is_promo=None) \
        .add_row(group_id=1, msku=7, ssku="70", high_price=11000, current_price=8000, low_price=4000, purchase_price=8000,
                is_promo=True) \
        .add_row(group_id=1, msku=8, ssku="80", high_price=11000, current_price=8000, low_price=4000, purchase_price=8000,
                is_promo=False)

    ctx.demand_input \
        .add_row(1, 8800, 0.25) \
        .add_row(2, 8800, 0.25) \
        .add_row(3, 8800, 0.25) \
        .add_row(4, 8800, 0.25) \
        .add_row(5, 8800, 0.25) \
        .add_row(6, 8800, 0.25) \
        .add_row(7, 8800, 0.25) \
        .add_row(8, 8800, 0.25) \

    ctx.config \
        .add_row(
            0, 0.1,
            "trivial", {},
            "margin.v1", {
                "max_delta": 0.2,
                "max_lower_price_delta": 0.3,
                "target_margin_threshold": 0.0001,
                "use_sku_with_promo": True
            },
            "dummy", {}
        ) \
        .add_row(
            1, 0.1,
            "trivial", {},
            "margin.v1", {
                "max_delta": 0.2,
                "max_lower_price_delta": 0.3,
                "target_margin_threshold": 0.0001,
                "use_sku_with_promo": False
            },
            "dummy", {}
        )
    with ctx:
        yield ctx

def test_metrics_filters(application, yt_context):
    _, _, _, metrics_path, _ = application.run(yt_context)

    # check that table with metrics and corresponding link are created
    assert yt_context.client.exists(yt_context.root + "/" + metrics_path)
    assert yt_context.client.get(yt_context.root + "/metrics/latest/@path") == yt_context.root + "/" + metrics_path

    # check metrics data structure
    metrics = yt_context.read_whole(metrics_path)

    expected_sku_gmv = 2200.0
    expected_sku_profit = 200.0
    expected_margin = 8000.0 / 8800.0 * 0.1
    assert len(metrics) == 2
    assert metrics[0] == {
        "gmv": expected_sku_gmv*5,
        "gmv_all": expected_sku_gmv*5,
        "gmv_deadstock": expected_sku_gmv*0,
        "gmv_equal_bounds": expected_sku_gmv*1,
        "gmv_not_deadstock": expected_sku_gmv*5,
        "gmv_not_promo": expected_sku_gmv*4,
        "gmv_promo": expected_sku_gmv*1,
        "gmv_wrong_bounds": expected_sku_gmv*1,
        "group_id": 0L,
        "margin": expected_margin,
        "margin_all": expected_margin,
        "margin_deadstock": expected_margin*0,
        "margin_equal_bounds": expected_margin,
        "margin_not_deadstock": expected_margin,
        "margin_not_promo": expected_margin,
        "margin_promo": expected_margin,
        "margin_wrong_bounds": expected_margin,
        "profit": expected_sku_profit*5,
        "profit_all": expected_sku_profit*5,
        "profit_deadstock": expected_sku_profit*0,
        "profit_equal_bounds": expected_sku_profit*1,
        "profit_not_deadstock": expected_sku_profit*5,
        "profit_not_promo": expected_sku_profit*4,
        "profit_promo": expected_sku_profit*1,
        "profit_wrong_bounds": expected_sku_profit*1,
    }

    # check that selected optimization metric
    assert metrics[1]["group_id"] == 1
    assert metrics[1]["gmv"] == metrics[1]["gmv_not_promo"]
    assert metrics[1]["gmv"] == 4400.0
