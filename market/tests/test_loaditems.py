# coding: utf-8

import pytest
from hamcrest import assert_that, is_, equal_to

from market.pylibrary.stock_storage import (
    ShopSkuDoc,
    loaditems,
)

import yatest.common


@pytest.fixture()
def sku_path():
    return yatest.common.source_path(
        'market/pylibrary/stock_storage/tests/data/sku.pbsn'
    )


def test_ok(sku_path):
    actual = loaditems(sku_path)
    assert_that(len(actual), is_(equal_to(1)))
    expected = [
        ShopSkuDoc(
            shop_id=111,
            ssku_id='5e8q9ags39f44d9jc4ro',
            warehouse_id=111,
            available_count=0,

            height_cm=2.0,
            length_cm=1.0,
            width_cm=123.01,
            size_cm=(2.0, 1.0, 123.01),

            weight_kg=100000000000.0,
            density_kg_m3=406471018616372.6,
        ),
    ]
    assert_that(actual, is_(equal_to(expected)))
