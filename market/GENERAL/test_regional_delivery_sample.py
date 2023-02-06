# coding: utf-8

import os
import pytest
import yatest

import market.proto.delivery.delivery_calc.delivery_calc_pb2 as DeliveryCalc
from market.idx.offers.yatf.resources.offers_indexer.regional_delivery import RegionalDelivery
from market.idx.yatf.utils.mmap.regional_delivery_writer import RegionalDeliveryBuilder


@pytest.fixture
def resource():
    builder = RegionalDeliveryBuilder()
    builder.add_option_group(1, 1, [[1, 1, 18, 1000], [2, 2, 19, 2000]])
    builder.add_bucket(0, 123, DeliveryCalc.REGULAR_PROGRAM, 1 , 2)
    builder.add_region_to_bucket(0, 2, 1)
    return RegionalDelivery(builder)


def test_regional_delivery(resource):
    resource.write(yatest.common.test_output_path())
    assert os.path.exists(resource.path)
    assert resource.path.endswith(resource.filename)
