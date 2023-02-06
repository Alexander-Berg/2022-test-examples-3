from core.application import Application
from core.testenv import testenv
from core.yt_context import YtContext
from core.utils import contains
import os, json, logging
import pytest


# Fix some problems with precision
def convert_output_float_to_int(output):
    result = []
    for d in output:
        obj = {}
        for k, v in d.iteritems():
            if type(v) == float:
                obj[k] = int(v)
            else:
                obj[k] = v
        result.append(obj)
    return result


@pytest.fixture
def application(testenv):
    return Application(testenv)


@pytest.fixture
def yt_context(testenv):
    ctx = YtContext(testenv)
    ctx.erp_input \
        .add_row(
            group_id=0,
            msku=1,
            ssku="10",
            purchase_price=1000,
            current_price=1000,
            low_price=2000,
            high_price=2000
        ) \
        .add_row(
            group_id=0,
            msku=2,
            ssku="20",
            purchase_price=1000,
            current_price=1000,
            low_price=100,
            high_price=100
        ) \
        .add_row(
            group_id=0,
            msku=3,
            ssku="30",
            purchase_price=1000,
            current_price=700,
            low_price=None,
            high_price=None
        ) \
        .add_row(
            group_id=1,
            msku=10,
            ssku="100",
            purchase_price=700,
            current_price=1050,
            low_price=500,
            high_price=2000
        ) \
        .add_row(
            group_id=1,
            msku=11,
            ssku="110",
            purchase_price=500,
            current_price=1100,
            low_price=500,
            high_price=2000
        ) \
        .add_row(
            group_id=1,
            msku=12,
            ssku="120",
            purchase_price=1000,
            current_price=1000,
            low_price=2000,
            high_price=2000
        ) \
        .add_row(
            group_id=1,
            msku=13,
            ssku="130",
            purchase_price=1000,
            current_price=1000,
            low_price=100,
            high_price=100
        ) \
        .add_row(
            group_id=2,
            msku=20,
            ssku="200",
            purchase_price=700,
            current_price=1050,
            low_price=500,
            high_price=2000
        ) \
        .add_row(
            group_id=2,
            msku=21,
            ssku="210",
            purchase_price=500,
            current_price=1100,
            low_price=500,
            high_price=2000
        ) \
        .add_row(
            group_id=2,
            msku=22,
            ssku="220",
            purchase_price=1000,
            current_price=1000,
            low_price=2000,
            high_price=2000
        ) \
        .add_row(
            group_id=2,
            msku=23,
            ssku="230",
            purchase_price=1000,
            current_price=1000,
            low_price=100,
            high_price=100
        ) \
        .add_row(
            group_id=3,
            msku=30,
            ssku="300",
            purchase_price=1000,
            current_price=1100,
            low_price=500,
            high_price=2000
        ) \
        .add_row(
            group_id=3,
            msku=31,
            ssku="310",
            purchase_price=950,
            current_price=1200,
            low_price=500,
            high_price=2000
        ) \
        .add_row(
            group_id=3,
            msku=32,
            ssku="320",
            purchase_price=1000,
            current_price=1000,
            low_price=2000,
            high_price=2000
        ) \
        .add_row(
            group_id=3,
            msku=33,
            ssku="330",
            purchase_price=1000,
            current_price=1000,
            low_price=100,
            high_price=100
        )
    ctx.demand_input \
        .add_row(1, 1000, 0.25) \
        .add_row(2, 1000, 0.25) \
        .add_row(3, 1000, 0.25) \
        .add_row(10, 1000, 0.25) \
        .add_row(11, 1000, 0.25) \
        .add_row(12, 1000, 0.25) \
        .add_row(13, 1000, 0.25) \
        .add_row(20, 1000, 0.25) \
        .add_row(21, 1000, 0.25) \
        .add_row(22, 1000, 0.25) \
        .add_row(23, 1000, 0.25) \
        .add_row(30, 1000, 0.25) \
        .add_row(31, 1000, 0.25) \
        .add_row(32, 1000, 0.25) \
        .add_row(33, 1000, 0.25)
    ctx.config \
        .add_row(
            0, 0,
            "trivial", {},
            "margin.v1", {
                "max_delta": 0.05,
                "max_lower_price_delta": 0.3,
                "target_margin_threshold": 0.0001,
            },
            "dummy", {}
        ) \
        .add_row(
            1, 0,
            "trivial", {},
            "margin.v1", {
                "max_delta": 0.05,
                "max_lower_price_delta": 0.3,
                "target_margin_threshold": 0.0001,
                "price_bound_calculator": "markup"
            },
            "dummy", {}
        ) \
        .add_row(
            2, 0,
            "trivial", {},
            "margin.v1", {
                "max_delta": 0.05,
                "max_lower_price_delta": 0.3,
                "target_margin_threshold": 0.0001,
                "price_bound_calculator": "expanded"
            },
            "dummy", {}
        ) \
        .add_row(
            3, 0,
            "trivial", {},
            "margin.v1", {
                "max_delta": 0.05,
                "max_lower_price_delta": 0.3,
                "target_margin": 0,
                "max_expand_delta": 0.1,
                "target_margin_threshold": 0.0001,
                "price_bound_calculator": "margin"
            },
            "dummy", {}
        )

    with ctx:
        yield ctx

def test_limits_by_price(application, yt_context):
    prices_path, _, _, _ = application.get_tables()
    application.run(yt_context)

    prices = yt_context.read_whole(prices_path)
    # Check if low-high is too high
    assert contains(prices, {
        "msku": 1,
        "low_price": 2000.0,
        "high_price": 2000.0,
        "new_price": 2000.0,
    })

    # Check if low-high is too low
    assert contains(prices, {
        "msku": 2,
        "low_price": 100.0,
        "high_price": 100.0,
        "new_price": 100.0,
    })

    # Check that "purchase - X%" works
    assert contains(prices, {
        "msku": 3,
        "low_price": 700.0,
        "high_price": 735.0,
        "new_price": 700.0,
    })

def test_limits_by_markup(application, yt_context):
    prices_path, _, _, _ = application.get_tables()
    application.run(yt_context)

    prices = yt_context.read_whole(prices_path)

    prices = convert_output_float_to_int(prices)

    assert contains(prices, {
        "msku": 10,
        "low_price": 1015.0,
        "high_price": 1085.0,
        "new_price": 1050.0
    })

    # Changing couln't be more than 50% low
    assert contains(prices, {
        "msku": 11,
        "low_price": 1075.0,
        "high_price": 1125.0,
        "new_price": 1100.0
    })

    # Check if low-high is too high
    assert contains(prices, {
        "msku": 12,
        "low_price": 2000.0,
        "high_price": 2000.0,
        "new_price": 2000.0,
    })

    # Check if low-high is too low
    assert contains(prices, {
        "msku": 13,
        "low_price": 100.0,
        "high_price": 100.0,
        "new_price": 100.0,
    })

def test_limits_by_expansion(application, yt_context):
    prices_path, _, _, _ = application.get_tables()
    application.run(yt_context)

    prices = yt_context.read_whole(prices_path)

    # Check markup base case
    assert contains(prices, {
        "msku": 20,
        "low_price": 997.0,
        "high_price": 1102.0,
        "new_price": 1000.0
    })

    # Changing couldn't be more than 50% low
    assert contains(prices, {
        "msku": 21,
        "low_price": 1045.0,
        "high_price": 1155.0,
        "new_price": 1100.0
    })

    # Check if low-high is too high
    assert contains(prices, {
        "msku": 22,
        "low_price": 2000.0,
        "high_price": 2000.0,
        "new_price": 2000.0,
    })

    # Check if low-high is too low
    assert contains(prices, {
        "msku": 23,
        "low_price": 100.0,
        "high_price": 100.0,
        "new_price": 100.0,
    })

def test_limits_by_margin(application, yt_context):
    prices_path, _, _, _ = application.get_tables()
    application.run(yt_context)

    prices = yt_context.read_whole(prices_path)

    # Check margin base case (expand lower bound)
    assert contains(prices, {
        "msku": 30,
        "low_price": 1000.0,
        "high_price": 1155.0,
        "new_price": 1000.0
    })

    # Changing couldn't be more max expand delta (10%)
    assert contains(prices, {
        "msku": 31,
        "low_price": 1080.0,
        "high_price": 1260.0,
        "new_price": 1200.0
    })

    # Check if low-high is too high
    assert contains(prices, {
        "msku": 32,
        "low_price": 2000.0,
        "high_price": 2000.0,
        "new_price": 2000.0,
    })

    # Check if low-high is too low
    assert contains(prices, {
        "msku": 33,
        "low_price": 100.0,
        "high_price": 100.0,
        "new_price": 100.0,
    })

