from core.forecaster import Forecaster
from core.resources import Resources, Sales, OnePInput, BlueOffer
from core.testenv import testenv

import os
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
        rsc.blue_offers += [  # necessary to produce output sorted by MSKU
            BlueOffer(msku=msku, supplier_id=1, supplier_type=1, price=1000)
        ]

    rsc.create_all()
    return rsc


@pytest.fixture(scope="module")
def yt_client():
    from yt.wrapper import YtClient

    client = YtClient(proxy=os.environ["YT_PROXY"])
    yield client


def test_uploading_to_yt(resources, forecaster, yt_client):
    forecaster.run_with_resources(resources, "//forecast", start_date="2018-01-01", duration=1, upload_to_yt=True)

    forecast = []
    for row in yt_client.read_table("//forecast", format="json"):
        forecast.append(row)

    # check row count
    assert len(forecast) == 10  # 10 = 1 (day) * 5 (MSKU) * 2 (warehouses)

    # check msku order
    assert forecast[0]["market_sku"] == 1
    assert forecast[2]["market_sku"] == 2
    assert forecast[4]["market_sku"] == 3
    assert forecast[6]["market_sku"] == 4
    assert forecast[8]["market_sku"] == 5

