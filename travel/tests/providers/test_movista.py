# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from travel.rasp.library.python.api_clients.travel_api.providers.movista import (
    WicketDeviceType, WicketTypeCode, MovistaServiceBookData
)


def test_movista_service_book_data():
    book_data = MovistaServiceBookData(
        date='2021-02-02',
        station_from_express_id=111,
        station_to_express_id=222,
        fare_id=333,
        wicket_device=WicketDeviceType('MID2Tutorial', WicketTypeCode.TURNSTILE)
    )

    assert book_data.request_json == {
        'date': '2021-02-02',
        'station_from_express_id': 111,
        'station_to_express_id': 222,
        'fare_id': 333,
        'wicket': {
            'type': WicketTypeCode.TURNSTILE,
            'device_type': 'MID2Tutorial'
        }
    }
