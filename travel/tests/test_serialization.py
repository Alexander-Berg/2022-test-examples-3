# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

from hamcrest import assert_that, contains, has_entries

from travel.rasp.library.python.api_clients.dzv.serialization import DzvRaspResponseSchema


def test_dzv_rasp_response_schema():
    content = """
    {
        "response":{
            "arrival":[
                {
                    "TrainNumber":"837",
                    "StartStation":"Иваново",
                    "EndStation":"Москва",
                    "EvTrackNumber":"",
                    "EvSndTime":"2019-05-08 17:24:00",
                    "TrainType":"2"
                },
                {
                    "TrainNumber":"6540",
                    "StartStation":"Москва",
                    "EndStation":"Железнодорожная",
                    "EvTrackNumber":"6т",
                    "EvSndTime":"2019-05-08 16:13:00",
                    "TrainType":"0"
                }
            ],
            "departure":[
                {
                    "TrainNumber":"073",
                    "StartStation":"Москва",
                    "EndStation":"Кривой Рог Днепропетровск",
                    "EvTrackNumber":"0",
                    "EvSndTime":"2019-05-08 15:00:00",
                    "TrainType":"1"
                },
                {
                    "TrainNumber":"7151",
                    "StartStation":"Москва",
                    "EndStation":"Серпухов",
                    "EvTrackNumber":"4",
                    "EvSndTime":"2019-05-09 09:19:00",
                    "TrainType":"1"
                }
            ]
        },
        "error":""
    }"""

    parsed, errors = DzvRaspResponseSchema().loads(content)

    assert not errors
    assert_that(parsed, has_entries({
        'response': has_entries({
            'arrival': contains(
                has_entries({
                    'train_number': '837',
                    'start_station': 'Иваново',
                    'end_station': 'Москва',
                    'track_number': '',
                    'event_dt': datetime(2019, 5, 8, 17, 24, 0),
                    'train_type': 2
                }),
                has_entries({
                    'train_number': '6540',
                    'start_station': 'Москва',
                    'end_station': 'Железнодорожная',
                    'track_number': '6т',
                    'event_dt': datetime(2019, 5, 8, 16, 13, 0),
                    'train_type': 0
                })
            ),
            'departure': contains(
                has_entries({
                    'train_number': '073',
                    'start_station': 'Москва',
                    'end_station': 'Кривой Рог Днепропетровск',
                    'track_number': '0',
                    'event_dt': datetime(2019, 5, 8, 15, 0, 0),
                    'train_type': 1
                }),
                has_entries({
                    'train_number': '7151',
                    'start_station': 'Москва',
                    'end_station': 'Серпухов',
                    'track_number': '4',
                    'event_dt': datetime(2019, 5, 9, 9, 19, 0),
                    'train_type': 1
                })
            )
        }),
        'error': None
    }))


def test_dzv_rasp_response_error_schema():
    content = '{"response":null,"error":403,"error_description":"Forbidden"}'

    parsed, errors = DzvRaspResponseSchema().loads(content)

    assert not errors
    assert_that(parsed, has_entries({
        'response': None,
        'error': 403,
        'error_description': 'Forbidden'
    }))
