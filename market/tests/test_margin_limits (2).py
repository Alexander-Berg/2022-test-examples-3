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
        .add_row(group_id=0, msku=1, ssku="10", high_price=11000, current_price=8000, low_price=4000, purchase_price=8000,
                stock=2, is_promo=True) \
        .add_row(group_id=0, msku=2, ssku="20", high_price=11000, current_price=8000, low_price=4000, purchase_price=8500,
                stock=2, is_promo=False) \
        .add_row(group_id=1, msku=3, ssku="30", high_price=11000, current_price=8000, low_price=4000, purchase_price=8000,
                stock=2, is_promo=True) \
        .add_row(group_id=1, msku=4, ssku="40", high_price=11000, current_price=8000, low_price=4000, purchase_price=8500,
                stock=2, is_promo=False) \
        .add_row(group_id=2, msku=5, ssku="50", high_price=11000, current_price=8000, low_price=4000, purchase_price=8000,
                stock=2, is_promo=True) \
        .add_row(group_id=2, msku=6, ssku="60", high_price=11000, current_price=8000, low_price=4000, purchase_price=8500,
                stock=2, is_promo=False)

    ctx.demand_input \
        .add_row(1, 8000, 2.5) \
        .add_row(1, 8500, 2) \
        .add_row(2, 8000, 2.5) \
        .add_row(2, 8500, 2) \
        .add_row(3, 8000, 2.5) \
        .add_row(3, 8500, 2) \
        .add_row(4, 8000, 2.5) \
        .add_row(4, 8500, 2) \
        .add_row(5, 8000, 2.5) \
        .add_row(5, 8500, 2) \
        .add_row(6, 8000, 2.5) \
        .add_row(6, 8500, 2)
    ctx.config \
        .add_row(
            0, -0.06,
            "trivial", {},
            "margin.v1", {
                "max_delta": 0.1,
                "max_lower_price_delta": 0.3,
                "target_margin_threshold": 0.005,
            },
            "dummy", {}
        ) \
        .add_row(
            1, -0.06,
            "trivial", {},
            "margin.v1", {
                "max_delta": 0.1,
                "max_lower_price_delta": 0.3,
                "target_margin_threshold": 0.005,
                "use_sku_with_promo": True
            },
            "dummy", {}
        ) \
        .add_row(
            2, -0.06,
            "trivial", {},
            "margin.v1", {
                "max_delta": 0.1,
                "max_lower_price_delta": 0.3,
                "target_margin_threshold": 0.005,
                "use_sku_with_promo": False
            },
            "dummy", {}
        )

    with ctx:
        yield ctx


def test_margins(application, yt_context):
    _, margins_path, _, _ = application.get_tables()
    application.run(yt_context)

    # check that table with margins and corresponding link are created
    assert yt_context.client.exists(yt_context.root + "/" + margins_path)
    assert yt_context.client.get(yt_context.root + "/margins/latest/@path") == yt_context.root + "/" + margins_path

    # check margins data
    margins = yt_context.read_whole(margins_path)
    assert len(margins) == 3
    assert margins[0] == {"group_id": 0, "margin": -0.03125}
    assert margins[1] == {"group_id": 1, "margin": -0.03125}
    assert margins[2] == {"group_id": 2, "margin": -0.06}
