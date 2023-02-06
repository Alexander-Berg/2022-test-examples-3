# -*- coding: utf-8 -*-
import hamcrest
from travel.cpa.data_processing.lib.order_processor import BusforBusesProcessor, BoyBusesProcessor
from travel.cpa.data_processing.lib.order_data_model import BusesOrderWithEncodedLabel

from data_provider import OrderProcessorDataProvider


ORDER_PROCESSOR_TESTS = [
    (BusforBusesProcessor, BusesOrderWithEncodedLabel, 'buses_busfor'),
    (BoyBusesProcessor, BusesOrderWithEncodedLabel, 'buses_boy'),
]


def test_order_processor():
    data_provider = OrderProcessorDataProvider()
    for order_processor_cls, order_cls, fn_key in ORDER_PROCESSOR_TESTS:
        for snapshots, expected_order in data_provider.get_data(order_cls, fn_key):
            order_processor = order_processor_cls(None)
            order = order_processor.process(snapshots)

            hamcrest.assert_that(
                order.as_dict(),
                hamcrest.has_entries(expected_order)
            )
