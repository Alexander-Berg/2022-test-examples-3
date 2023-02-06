# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from hamcrest import assert_that, has_entries

from common.tester.factories import create_station
from travel.rasp.library.python.api_clients.travel_api.create_order_info import SuburbanServiceBookData

from travel.rasp.suburban_selling.selling.order.helpers import SuburbanServiceFactory, ForbiddenOrderError


pytestmark = [pytest.mark.dbuser]


class StubSuburbanServiceBookData(SuburbanServiceBookData):
    def to_json(self):
        return {'book': 'data'}


class StubSuburbanSellingFactory(SuburbanServiceFactory):
    def __init__(self, orders_forbidden):
        self.orders_forbidden = orders_forbidden

    def raise_if_order_forbidden(self):
        if self.orders_forbidden:
            raise ForbiddenOrderError('Orders forbidden')

    def make_book_data(self, order_data):
        return StubSuburbanServiceBookData()


def test_suburban_service_factory():
    service_factory = StubSuburbanSellingFactory(orders_forbidden=True)

    with pytest.raises(ForbiddenOrderError) as ex:
        service_factory.make_service({})
    assert ex.value.message == 'Orders forbidden'

    service_factory = StubSuburbanSellingFactory(orders_forbidden=False)
    order_data = {
        'station_from': create_station(id=101),
        'station_to': create_station(id=102),
        'provider': 'test_provider',
        'price': 103,
        'partner': 'test_partner'
    }
    service = service_factory.make_service(order_data, 'test_token')

    assert_that(service.request_json, has_entries({
        'provider': 'test_provider',
        'station_from_id': 101,
        'station_to_id': 102,
        'price': 103,
        'carrier_partner': 'test_partner',
        'provider_book_data': {'book': 'data'},
        'test_context_token': 'test_token'
    }))
