from core.application import Application
from core.testenv import testenv
from core.yt_context import YtContext
import os, json, logging
import pytest


@pytest.fixture
def application(testenv):
    return Application(testenv)


@pytest.fixture
def yt_context(testenv):
    ctx = YtContext(testenv)
    ctx.erp_input \
        .add_row(group_id=0, msku=1, ssku="10", high_price=11000, current_price=8000, low_price=14000, purchase_price=8000) \
        .add_row(group_id=0, msku=2, ssku="20", high_price=None, current_price=8000, low_price=None, purchase_price=8000) \
        .add_row(group_id=0, msku=3, ssku="30", high_price=8000, current_price=8000, low_price=8000, purchase_price=8000, cost=80000)
    ctx.demand_input \
        .add_row(1, 8000, 0.25) \
        .add_row(2, 8000, 0.25) \
        .add_row(3, 8000, 0.25)
    ctx.config \
        .add_row(
            0, 0,
            "trivial", {},
            "margin.v1", {
                "max_delta": 0.05,
                "max_lower_price_delta": 0.3,
                "target_margin_threshold": 0.0001,
            },
            "combined", {
                "checkers": [
                    {
                        "name": "wrong_bounds_sku",
                        "config": {
                            "ignore_fixed": False
                        }
                    },
                    {
                        "name": "empty_bounds_sku",
                        "config": {
                            "ignore_fixed": False
                        }
                    },
                    {
                        "name": "cost_purchase_warning",
                        "config": {
                            "threshold": 5.0,
                            "ignore_fixed": False
                        }
                    }
                ]
            }
        )

    with ctx:
        yield ctx

def test_yt_tables(application, yt_context):
    _, _, check_path, _, _ = application.run(yt_context)

     # check that table with checks and corresponding link are created
    assert yt_context.client.exists(yt_context.root + "/" + check_path)
    assert yt_context.client.get(yt_context.root + "/check/latest/@path") == yt_context.root + "/" + check_path
    # checker data
    checks = yt_context.read_whole(check_path)
    assert len(checks) == 3


def test_wrong_bounds(application, yt_context):
    _, _, check_path, _, _ = application.run(yt_context)
    checks = yt_context.read_whole(check_path)
    assert checks[0] == {
        "msku": 1,
        "ssku": "10",
        "error_code": 5,
        "group_id": 0,
        "message": "Low price is above high price (14000 > 11000)",
        "notification": "Полученная цена меньше глобальной нижней границы, установлена цена = нижней границе"
    }


def test_empty_bounds(application, yt_context):
    _, _, check_path, _, _ = application.run(yt_context)
    checks = yt_context.read_whole(check_path)
    assert checks[1] == {
        "msku": 2,
        "ssku": "20",
        "error_code": 9,
        "group_id": 0,
        "message": "Low price or high price are empty",
    }


def test_cost_purchase_warning(application, yt_context):
    _, _, check_path, _, _ = application.run(yt_context)
    checks = yt_context.read_whole(check_path)
    assert checks[2] == {
        "msku": 3,
        "ssku": "30",
        "error_code": 10,
        "group_id": 0,
        "message": "Purchase price is much lower or greater than cost price (PurchasePrice = 8000; CostPrice = 80000)",
    }

