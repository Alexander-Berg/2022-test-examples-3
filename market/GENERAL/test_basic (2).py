from core.forecaster import Forecaster
from core.resources import Resources, Sales, OnePInput, BlueOffer
from core.testenv import testenv

import pytest
from datetime import datetime, timedelta


@pytest.fixture
def forecaster(testenv):
    return Forecaster(testenv)


@pytest.fixture(scope="module")
def resources(testenv):
    rsc = Resources(testenv)

    for msku in range(1, 6):
        start_date = datetime(year=2018, month=1, day=1)
        rsc.sales += [
            Sales(msku_id=msku, date=(start_date + timedelta(days=d)), source_id=1, sales=4) for d in range(0, 100)
        ]
        rsc.one_p_input += [
            OnePInput(msku_id=msku, date="2018-01-01", old_price=1000, new_price=1000)
        ]
        rsc.blue_offers += [  # msku, supplier, type, price
            BlueOffer(msku, 465852, 1, 1000)
        ]

    rsc.create_all()
    return rsc


def test_basic(resources, forecaster):
    forecast = forecaster.run_with_resources(resources, "forecast.json", start_date="2018-01-01", duration=1)
    expectations = [
        {"date": "2018-01-01", "market_sku": 1, "price": 1000, "source_id": 1, "supplier_id": 465852,
         "price_type": "currentPrice", "value": 3.8, "variance": 0, "warehouse_id": 145, "warehouse_name": "Moscow"},
        {"date": "2018-01-01", "market_sku": 1, "price": 1000, "source_id": 1, "supplier_id": 465852,
         "price_type": "currentPrice", "value": 0.2, "variance": 0, "warehouse_id": 147,
         "warehouse_name": "Rostov-on-Don"},
        {"date": "2018-01-01", "market_sku": 2, "price": 1000, "source_id": 1, "supplier_id": 465852,
         "price_type": "currentPrice", "value": 3.8, "variance": 0, "warehouse_id": 145, "warehouse_name": "Moscow"},
        {"date": "2018-01-01", "market_sku": 2, "price": 1000, "source_id": 1, "supplier_id": 465852,
         "price_type": "currentPrice", "value": 0.2, "variance": 0, "warehouse_id": 147,
         "warehouse_name": "Rostov-on-Don"},
        {"date": "2018-01-01", "market_sku": 3, "price": 1000, "source_id": 1, "supplier_id": 465852,
         "price_type": "currentPrice", "value": 3.8, "variance": 0, "warehouse_id": 145, "warehouse_name": "Moscow"},
        {"date": "2018-01-01", "market_sku": 3, "price": 1000, "source_id": 1, "supplier_id": 465852,
         "price_type": "currentPrice", "value": 0.2, "variance": 0, "warehouse_id": 147,
         "warehouse_name": "Rostov-on-Don"},
        {"date": "2018-01-01", "market_sku": 4, "price": 1000, "source_id": 1, "supplier_id": 465852,
         "price_type": "currentPrice", "value": 3.8, "variance": 0, "warehouse_id": 145, "warehouse_name": "Moscow"},
        {"date": "2018-01-01", "market_sku": 4, "price": 1000, "source_id": 1, "supplier_id": 465852,
         "price_type": "currentPrice", "value": 0.2, "variance": 0, "warehouse_id": 147,
         "warehouse_name": "Rostov-on-Don"},
        {"date": "2018-01-01", "market_sku": 5, "price": 1000, "source_id": 1, "supplier_id": 465852,
         "price_type": "currentPrice", "value": 3.8, "variance": 0, "warehouse_id": 145, "warehouse_name": "Moscow"},
        {"date": "2018-01-01", "market_sku": 5, "price": 1000, "source_id": 1, "supplier_id": 465852,
         "price_type": "currentPrice", "value": 0.2, "variance": 0, "warehouse_id": 147,
         "warehouse_name": "Rostov-on-Don"}
    ]

    for expected in expectations:
        assert expected in forecast

