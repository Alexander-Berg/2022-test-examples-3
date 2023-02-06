# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from hamcrest import assert_that, contains, has_entries, anything

from travel.rasp.library.python.api_clients.travel_api.create_order_info import (
    TravelApiCreateOrderInfo, SuburbanService, SuburbanServiceBookData
)


class TestBookData(SuburbanServiceBookData):
    def to_json(self):
        return {'book': 'data'}


def test_travel_api_create_order_info():
    user_info = TravelApiCreateOrderInfo(
        phone='8-888',
        email='mail@mail.yadex',
        geo_id=1000,
        ip='1.2.3.4',
        label='service_label',
        suburban_services=[
            SuburbanService(
                provider='provider',
                station_from_id=100,
                station_to_id=200,
                price=300.30,
                carrier_partner='carrier',
                book_data=TestBookData(),
                test_context_token='test_token'
            )
        ],
        payment_test_context_token='payment_token'
    )

    assert_that(user_info.request_json, has_entries({
        'deduplication_key': anything(),
        'label': 'service_label',
        'contact_info': has_entries({
            'phone': '8-888',
            'email': 'mail@mail.yadex',
        }),
        'user_info': has_entries({
            'ip': '1.2.3.4',
            'geo_id': 1000,
        }),
        'order_history': [],
        'suburban_services': contains(
            has_entries({
                'provider': 'provider',
                'station_from_id': 100,
                'station_to_id': 200,
                'price': 300.30,
                'carrier_partner': 'carrier',
                'provider_book_data': {'book': 'data'},
                'test_context_token': 'test_token',
            })
        ),
        'payment_test_context_token': 'payment_token'
    }))
