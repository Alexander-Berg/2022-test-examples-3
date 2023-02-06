from core.application import Application
from core.testenv import testenv
from core.yt_context import YtContext
from core.tables import PriceGroups
from core import utils
import os, json, logging
import pytest


def add_msku_data(yt_context):
    # msku 1
    yt_context.erp_input \
        .add_row(group_id=0, msku=1, ssku="10", high_price=2000, current_price=900, low_price=500, purchase_price=700)
    yt_context.demand_input \
        .add_row(sku=1, price_variant=900 * 0.98, demand_mean=0.4) \
        .add_row(sku=1, price_variant=900 * 0.99, demand_mean=0.35) \
        .add_row(sku=1, price_variant=900,        demand_mean=0.3) \
        .add_row(sku=1, price_variant=900 * 1.01, demand_mean=0.25) \
        .add_row(sku=1, price_variant=900 * 1.02, demand_mean=0.2)

    # msku 2
    yt_context.erp_input \
        .add_row(group_id=0, msku=2, ssku="20", high_price=3000, current_price=1800, low_price=600, purchase_price=1400)
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
        margin_adj_alg="bias",
        margin_adj_config={
            "default_bias": 0.02,
            "max_abs_bias": 0.02,
            "overcome_margin": 0.005,
            "min_margin": -0.07,
            "max_margin": 0.3,
            "window_size": 28,
            "history_table": yt_context.history_table.path,
            "est_history_table": yt_context.est_history_table.path,
            "consider_coupons": False
        },
        pricing_alg="margin.v1",
        pricing_config={
            "price_bound_calculator": "margin",
            "max_delta": 0.03,
            "max_lower_price_delta": 0.2,
            "max_expand_delta": 0.1,
            "target_margin_threshold": 0.005,
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
    add_config(yt_context, margin=0.21, date="2019-01-15")
    coeff = 0
    for date in utils.date_generator("2019-01-01", "2019-01-15"):
        yt_context.history_table.add_row(
            group_id=0,
            date=date,
            offer_price=1000 + coeff,
            is_exp=1,
            item_count=1,
            loss=800 + coeff,
            gmv_with_coupons=1000 + coeff,
            sku=1,
            purchase_price=800 + coeff,
            coupons=0
        )
        yt_context.history_table.add_row(
            group_id=0,
            date=date,
            offer_price=1900,
            is_exp=1,
            item_count=1,
            loss=1500,
            gmv_with_coupons=1900,
            sku=2,
            purchase_price=1500,
            coupons=0
        )
        yt_context.est_history_table.add_row(
            group_id=0,
            date=date,
            is_exp=1,
            loss=2200 + coeff,
            gmv=2820 + coeff
        )
        coeff += 10

    yt_context.margins_table.add_row(
        group_id=0,
        margin=0.21
    )

    with yt_context:
        prices_path, margins_path, _, _ = application.get_tables()
        application.run(yt_context)

        # Mt = 0.21  - target margin
        # Mreal = 0.202361  - real historical margin (14 days estimated)
        # Mest = 0.2149  - optimizer expected margin (14 days estimated)
        # DefaultBias = 0.02 - default bias value for short history
        # MaxAbsBias = 0.02 - maximum absolute bias value
        # OvercomeMargin = 0.005 - value for target margin overcomig (KPI)
        # MinMargin = -0.07 - minimum margin value
        # MaxMargin = 0.3 - maximum margin value
        # adjusted margin = Max(Min(Mt + Max(Min(Bias, -MaxAbsBias), MaxAbsBias) + OvercomeMargin), MaxMargin), MinMargin)
        margins = yt_context.read_whole(margins_path)
        assert margins[0]["group_id"] == 0
        assert margins[0]["margin"] == pytest.approx(0.206349, 1e-4)

        # Margin still can't be reached - just set max allowed prices
        prices = yt_context.read_whole(prices_path)
        assert {
            "msku": 1,
            "ssku": "10",
            "high_price": 927.0,
            "low_price": 873.0,
            "new_price": 882.0,
            "current_price": 900.0,
            "group_id": 0,
            "demand": 0.4,
            "is_price_random": False,
            "raw_low_price": 500.0,
            "raw_high_price": 2000.0,
            "buy_price": 700.0,
            "price_group_type": PriceGroups.dco.name,
            "price_group_priority": PriceGroups.dco.priority,
            "base_rule_name": PriceGroups.dco.base_rule_name,
            "is_deadstock": False,
            "is_promo": False,
            "is_kvi": False,
            "stock": 1000000,
        } in prices
        assert {
            "msku": 2,
            "ssku": "20",
            "high_price": 1854.0,
            "low_price": 1746.0,
            "new_price": 1764.0,
            "current_price": 1800.0,
            "group_id": 0,
            "demand": 0.5,
            "is_price_random": False,
            "raw_low_price": 600.0,
            "raw_high_price": 3000.0,
            "buy_price": 1400.0,
            "price_group_type": PriceGroups.dco.name,
            "price_group_priority": PriceGroups.dco.priority,
            "base_rule_name": PriceGroups.dco.base_rule_name,
            "is_deadstock": False,
            "is_promo": False,
            "is_kvi": False,
            "stock": 1000000,
        } in prices
