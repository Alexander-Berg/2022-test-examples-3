# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from travel.rasp.library.python.api_clients.travel_api.providers.im import ImServiceBookData


def test_im_service_book_data():
    book_data = ImServiceBookData(
        date='2021-02-02',
        station_from_express_id=111,
        station_to_express_id=222,
        train_number='333A',
        im_provider='P6'
    )

    assert book_data.request_json == {
        'date': '2021-02-02',
        'station_from_express_id': 111,
        'station_to_express_id': 222,
        'train_number': '333A',
        'im_provider': 'P6'
    }
