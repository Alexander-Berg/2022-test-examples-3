# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from travel.rasp.library.python.api_clients.travel_api.providers.aeroexpress import AeroexpressServiceBookData


def test_aeroexpress_service_book_data():
    book_data = AeroexpressServiceBookData(
        date='2021-02-02',
        menu_id=11,
        order_type=22
    )

    assert book_data.request_json == {
        'menu_id': 11,
        'order_type': 22,
        'date': '2021-02-02',
    }
