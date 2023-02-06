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
        .add_row(group_id=0, msku=1, ssku="10", high_price=8000, current_price=8000, low_price=8000, purchase_price=6000, rule_name=BoundsRuleName.deadstock)
    ctx.demand_input \
        .add_row(0, 8000, 0.25)
    ctx.config

    with ctx:
        yield ctx


# проверка дневного расчета
def test_basic_daily(application, yt_context):
    prices_path, margins_path, _, metrics_path, _ = application.run(yt_context)

    # check that table with prices and corresponding link are created
    assert yt_context.client.exists(yt_context.root + "/" + prices_path)
    assert yt_context.client.get(yt_context.root + "/prices/latest/@path") == yt_context.root + "/" + prices_path
    # check prices data
    prices = yt_context.read_whole(prices_path)
    assert len(prices) == 1
    assert prices[0] == {
        "high_price": 8000.0,
        "msku": 1L,
        "ssku": "10",
        "low_price": 8000.0,
        "new_price": 8000.0,
        "current_price": 8000.0,
        "group_id": 0,
        "demand": 0,
        "is_price_random": False,
        "raw_low_price": 8000.0,
        "raw_high_price": 8000.0,
        "buy_price": 6000.0,
        "rule_name": "Автораспродажа",
    }

    # check that table with margins and corresponding link are created
    assert yt_context.client.exists(yt_context.root + "/" + margins_path)
    assert yt_context.client.get(yt_context.root + "/margins/latest/@path") == yt_context.root + "/" + margins_path
    # check margins data
    margins = yt_context.read_whole(margins_path)
    assert len(margins) == 1
    assert margins[0] == {"group_id": 0, "margin": 0.0}

    # check that table with metrics and corresponding link are created
    assert yt_context.client.exists(yt_context.root + "/" + metrics_path)
    assert yt_context.client.get(yt_context.root + "/metrics/latest/@path") == yt_context.root + "/" + metrics_path
    # check metrics data
    metrics = yt_context.read_whole(metrics_path)
    assert len(metrics) == 1
    assert metrics[0]["group_id"] == 0
    assert metrics[0]["gmv"] == 0.0
    assert metrics[0]["profit"] == 0
    assert metrics[0]["margin"] == 0
