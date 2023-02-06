from core.application import Application
from core.testenv import testenv
from core.yt_context import YtContext
from core import utils, tables
import os, json, logging
import pytest


def add_msku_data(yt_context):
    # msku 1
    yt_context.erp_input \
        .add_row(group_id=0, msku=1, ssku="10", high_price=1000, current_price=900, low_price=800, purchase_price=700)
    yt_context.demand_input \
        .add_row(sku=1, price_variant=900 * 0.98, demand_mean=0.4) \
        .add_row(sku=1, price_variant=900 * 0.99, demand_mean=0.35) \
        .add_row(sku=1, price_variant=900,        demand_mean=0.3) \
        .add_row(sku=1, price_variant=900 * 1.01, demand_mean=0.25) \
        .add_row(sku=1, price_variant=900 * 1.02, demand_mean=0.2)

    # msku 2
    yt_context.erp_input \
        .add_row(group_id=0, msku=2, ssku="20", high_price=2000, current_price=1800, low_price=1600, purchase_price=1400)
    yt_context.demand_input \
        .add_row(sku=2, price_variant=1800 * 0.98, demand_mean=0.5) \
        .add_row(sku=2, price_variant=1800 * 0.99, demand_mean=0.45) \
        .add_row(sku=2, price_variant=1800,        demand_mean=0.4) \
        .add_row(sku=2, price_variant=1800 * 1.01, demand_mean=0.1) \
        .add_row(sku=2, price_variant=1800 * 1.02, demand_mean=0.05)


def add_config(yt_context, date, group_id=0, margin=0.3):
    yt_context.config.add_row(
        group_id=group_id,
        margin=margin,
        margin_adj_alg="monthly_correction",
        margin_adj_config={
            "history_table": yt_context.history_table.path,
            "date": date
        },
        pricing_alg="margin.v1",
        pricing_config={
            "max_delta": 0.15,
            "max_lower_price_delta": 0.3,
            "target_margin_threshold": 0.0001,
        },
        checker_alg="dummy",
        checker_config={},
    )


@pytest.fixture(scope="module")
def application(testenv):
    return Application(testenv)


@pytest.fixture(scope="function")
def yt_context(testenv):
    ctx = YtContext(testenv)
    add_msku_data(ctx)
    return ctx


def test_too_high_margin(application, yt_context):
    add_config(yt_context, margin=0.3, date="2019-01-15")
    for date in utils.date_generator("2019-01-01", "2019-01-15"):
        yt_context.history_table.add_row(
            group_id=0,
            date=date,
            offer_price=1000,
            is_exp=1,
            item_count=1,
            loss=800,
            gmv_with_coupons=1000,
            sku=1,
            purchase_price=800,
            coupons=0
        )

    with yt_context:
        prices_path, margins_path, _, metrics_path, _ = application.run(yt_context)

        # Mt = 0.3  - target margin
        # Mr = 0.2  - actual margin since the month begin
        # R = DaysPassed/DaysRemained = 14/16 = 0.875
        # adjusted margin = Mt * (1 + R) - Mr * R = 0.3 * (1 + 0.875) - 0.2 * 0.875 ~ 0.3875
        # after availability check = 0.2374727669
        margins = yt_context.read_whole(margins_path)
        assert margins[0]["group_id"] == 0
        assert margins[0]["margin"] == pytest.approx(0.2374727669, 1e-4)

        # Margin still can't be reached
        prices = yt_context.read_whole(prices_path)
        logging.info(prices)
        assert {
            "msku": 1,
            "ssku": "10",
            "high_price": 1000.0,
            "low_price": 800.0,
            "new_price": 882.0,
            "current_price": 900.0,
            "group_id": 0,
            "demand": 0.4,
            "is_price_random": False,
            "raw_low_price": 800.0,
            "raw_high_price": 1000.0,
            "buy_price": 700.0,
            "rule_name": "Другое",
        } in prices
        assert {
            "msku": 2,
            "ssku": "20",
            "high_price": 2000.0,
            "low_price": 1600.0,
            "new_price": 1782.0,
            "current_price": 1800.0,
            "group_id": 0,
            "demand": 0.45,
            "is_price_random": False,
            "raw_low_price": 1600.0,
            "raw_high_price": 2000.0,
            "buy_price": 1400.0,
            "rule_name": "Другое",
        } in prices

        # Check metrics
        metrics = yt_context.read_whole(metrics_path)
        assert metrics[0]["group_id"] == 0
        assert metrics[0]["gmv"] == 1154.7
        assert metrics[0]["profit"] ==  pytest.approx(244.7, 1e-4)
        assert metrics[0]["margin"] == pytest.approx(0.2119, 1e-4)
