# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import httpretty
from hamcrest import assert_that, has_entries, has_item

from travel.rasp.library.python.api_clients.dzv import DzvClient


@httpretty.activate
def test_dzv_client_rasp():
    url = 'https://url/yandex'
    key = 'secretkey'
    term_id = 22

    content = """
    {
        "response":{
            "arrival":[
                {
                    "TrainNumber":"1",
                    "StartStation":"Станция0",
                    "EndStation":"Станция1",
                    "EvTrackNumber":"2",
                    "EvSndTime":"2019-01-02 03:04:05",
                    "TrainType":"0"
                }
            ],
            "departure":[]
        },
        "error":""
    }"""
    httpretty.register_uri(httpretty.GET, url, status=200, body=content)

    client = DzvClient(url, key, timeout=1)
    platforms_data = client.get_platforms(term_id)

    assert_that(httpretty.last_request().querystring, has_entries({
        'method': ['rasp'],
        'term_id': [str(term_id)],
        'key': [key],
    }))
    assert_that(platforms_data, has_entries({
        'arrival': has_item(
            has_entries({
                'train_number': '1',
                'start_station': 'Станция0',
                'end_station': 'Станция1',
                'track_number': '2',
                'event_dt': datetime(2019, 1, 2, 3, 4, 5),
                'train_type': 0
            })
        ),
        'departure': []
    }))
