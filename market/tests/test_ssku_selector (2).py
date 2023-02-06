from core.application import Application
from core.testenv import testenv
from core.yt_context import YtContext
from core.tables import PriceGroups
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
        .add_row(group_id=0, msku=1, ssku="10", high_price=9000, current_price=8000, low_price=7000, purchase_price=8000,
                price_group=PriceGroups.dco) \
        .add_row(group_id=0, msku=1, ssku="11", high_price=8000, current_price=8000, low_price=8000, purchase_price=8000,
                price_group=PriceGroups.dco) \
        .add_row(group_id=1, msku=2, ssku="20", high_price=7000, current_price=9000, low_price=7000, purchase_price=7000,
                price_group=PriceGroups.fix) \
        .add_row(group_id=1, msku=2, ssku="21", high_price=9000, current_price=9000, low_price=9000, purchase_price=9000,
                price_group=PriceGroups.rrc) \
        .add_row(group_id=1, msku=2, ssku="22", high_price=8000, current_price=9000, low_price=8000, purchase_price=8000,
                price_group=PriceGroups.dco)
    ctx.demand_input \
        .add_row(1, 8000, 0.25) \
        .add_row(2, 9000, 0.25)
    margin_v1_config = {
        "max_delta": 0.15,
        "max_lower_price_delta": 0.3,
        "target_margin": 0,
        "max_expand_delta": 0.1,
        "target_margin_threshold": 0.0001,
        "price_bound_calculator": "margin"
    }
    ctx.config \
        .add_row(0, 0, "trivial", {}, "margin.v1", margin_v1_config, "dummy", {}) \
        .add_row(1, 0, "trivial", {}, "margin.v1", margin_v1_config, "dummy", {})

    with ctx:
        yield ctx


def test_basics(application, yt_context):
    prices_path, margins_path, _, _ = application.get_tables()
    application.run(yt_context)

    # check that table with prices and corresponding link are created
    assert yt_context.client.exists(yt_context.root + "/" + prices_path)
    assert yt_context.client.get(yt_context.root + "/prices/latest/@path") == yt_context.root + "/" + prices_path

    # check that table with margins and corresponding link are created
    assert yt_context.client.exists(yt_context.root + "/" + margins_path)
    assert yt_context.client.get(yt_context.root + "/margins/latest/@path") == yt_context.root + "/" + margins_path

    # check margins data
    margins = yt_context.read_whole(margins_path)
    assert len(margins) == 2

    prices = yt_context.read_whole(prices_path)
    assert len(prices) == 5


def test_two_autostrategy_ssku(application, yt_context):
    prices_path, margins_path, _, _ = application.get_tables()
    application.run(yt_context)

    margins = yt_context.read_whole(margins_path)
    assert margins[0] == {"group_id": 0, "margin": 0.0}

    prices = yt_context.read_whole(prices_path)
    assert prices[0]["msku"] == prices[1]["msku"]
    assert prices[0]["new_price"] == prices[1]["new_price"]
    assert prices[0] == {
        "high_price": 9000.0,
        "msku": 1L,
        "ssku": "10",
        "low_price": 7000.0,
        "new_price": 8000.0,
        "current_price": 8000.0,
        "group_id": 0L,
        "demand": 0.25,
        "is_price_random": False,
        "raw_low_price": 7000.0,
        "raw_high_price": 9000.0,
        "buy_price": 8000.0,
        "price_group_type": PriceGroups.dco.name,
        "price_group_priority": PriceGroups.dco.priority,
        "base_rule_name": PriceGroups.dco.base_rule_name,
        "is_deadstock": False,
        "is_promo": False,
        "is_kvi": False,
        "stock": 1000000,
    }


def test_fix_rrc_autostrategy_ssku(application, yt_context):
    prices_path, margins_path, _, _ = application.get_tables()
    application.run(yt_context)

    margins = yt_context.read_whole(margins_path)
    assert margins[1] == {"group_id": 1, "margin": 0.1111111111111111}

    prices = yt_context.read_whole(prices_path)
    assert prices[2]["msku"] == prices[3]["msku"] and prices[3]["msku"] == prices[4]["msku"]
    assert prices[2]["new_price"] == 9000
    assert prices[3]["new_price"] == 9000
    assert prices[4]["new_price"] == 8000
    assert prices[2] == {
        "high_price": 9000.0,
        "msku": 2L,
        "ssku": "20",
        "low_price": 9000.0,
        "new_price": 9000.0,
        "current_price": 9000.0,
        "group_id": 1L,
        "demand": 0.25,
        "is_price_random": False,
        "raw_low_price": 7000.0,
        "raw_high_price": 7000.0,
        "buy_price": 7000.0,
        "price_group_type": PriceGroups.fix.name,
        "price_group_priority": PriceGroups.fix.priority,
        "base_rule_name": PriceGroups.fix.base_rule_name,
        "is_deadstock": False,
        "is_promo": False,
        "is_kvi": False,
        "stock": 1000000,
    }
    assert prices[3] == {
        "high_price": 9000.0,
        "msku": 2L,
        "ssku": "21",
        "low_price": 9000.0,
        "new_price": 9000.0,
        "current_price": 9000.0,
        "group_id": 1L,
        "demand": 0.25,
        "is_price_random": False,
        "raw_low_price": 9000.0,
        "raw_high_price": 9000.0,
        "buy_price": 9000.0,
        "price_group_type": PriceGroups.rrc.name,
        "price_group_priority": PriceGroups.rrc.priority,
        "base_rule_name": PriceGroups.fix.base_rule_name,
        "is_deadstock": False,
        "is_promo": False,
        "is_kvi": False,
        "stock": 1000000,
    }
