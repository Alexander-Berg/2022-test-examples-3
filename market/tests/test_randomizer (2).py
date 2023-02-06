from core.application import Application
from core.testenv import testenv
from core.yt_context import YtContext
import os, json, logging
from datetime import datetime
import pytest


@pytest.fixture
def application(testenv):
    # fix date for random tests
    return Application(testenv, date="2019-12-11")


@pytest.fixture
def yt_context(testenv):
    ctx = YtContext(testenv)
    ctx.erp_input \
        .add_row(group_id=0, msku=1, ssku="10", high_price=11000, current_price=8000, low_price=4000, purchase_price=8000) \
        .add_row(group_id=0, msku=2, ssku="20", high_price=11000, current_price=8000, low_price=4000, purchase_price=8000) \
        .add_row(group_id=0, msku=3, ssku="30", high_price=11000, current_price=8000, low_price=4000, purchase_price=8000) \
        .add_row(group_id=0, msku=4, ssku="40", high_price=11000, current_price=8000, low_price=4000, purchase_price=8000) \
        .add_row(group_id=0, msku=5, ssku="50", high_price=11000, current_price=8000, low_price=4000, purchase_price=8000) \
        .add_row(group_id=0, msku=6, ssku="60", high_price=11000, current_price=8000, low_price=4000, purchase_price=8000) \
        .add_row(group_id=1, msku=11, ssku="110", high_price=11000, current_price=8000, low_price=4000, purchase_price=8000) \
        .add_row(group_id=1, msku=12, ssku="120", high_price=11000, current_price=8000, low_price=4000, purchase_price=8000)
    ctx.demand_input \
        .add_row(1, 8000, 0.25) \
        .add_row(2, 8000, 0.25) \
        .add_row(3, 8000, 0.25) \
        .add_row(4, 8000, 0.25) \
        .add_row(5, 8000, 0.25) \
        .add_row(6, 8000, 0.25) \
        .add_row(11, 8000, 0.25) \
        .add_row(12, 8000, 0.25)
    ctx.config \
        .add_row(
            0, 0,
            "trivial", {},
            "margin.v1", {
                "max_delta": 0.05,
                "max_lower_price_delta": 0.3,
                "target_margin_threshold": 0.0001,
                "random_sample_rate": 0.5
            },
            "dummy", {}
        ) \
        .add_row(
            1, 0,
            "trivial", {},
            "margin.v1", {
                "max_delta": 0.05,
                "max_lower_price_delta": 0.3,
                "target_margin_threshold": 0.0001
            },
            "dummy", {}
        )

    with ctx:
        yield ctx


# проверка фиксированности рандома в течение дня
# рандомизация поменялась из-за отключения дополнительного расчёта Лимитированного стока
def test_price_randomizer(application, yt_context):
    expected_result = [
        { 'group_id': 0, 'msku': 1, 'is_price_random': True, 'new_price': 7821.0 },
        { 'group_id': 0, 'msku': 2, 'is_price_random': True, 'new_price': 8267.0 },
        { 'group_id': 0, 'msku': 3, 'is_price_random': False, 'new_price': 8000.0 },
        { 'group_id': 0, 'msku': 4, 'is_price_random': True, 'new_price': 8275.0 },
        { 'group_id': 0, 'msku': 5, 'is_price_random': False, 'new_price': 8000.0 },
        { 'group_id': 0, 'msku': 6, 'is_price_random': False, 'new_price': 8000.0 },
        { 'group_id': 1, 'msku': 11, 'is_price_random': False, 'new_price': 8000.0 },
        { 'group_id': 1, 'msku': 12, 'is_price_random': False, 'new_price': 8000.0 }
    ]

    # repeat to check randomness
    for _ in range(0, 5, 1):
        prices_path, margins_path, _, _ = application.get_tables()
        application.run(yt_context)

        # check that table with prices and corresponding link are created
        assert yt_context.client.exists(yt_context.root + "/" + prices_path)
        assert yt_context.client.get(yt_context.root + "/prices/latest/@path") == yt_context.root + "/" + prices_path
        # check prices data
        prices = yt_context.read_whole(prices_path)

        # check randomized prices
        for i in range(0, 8):
            assert prices[i]['msku'] == expected_result[i]['msku']
            assert prices[i]['is_price_random'] == expected_result[i]['is_price_random']
            assert prices[i]['new_price'] == expected_result[i]['new_price']

