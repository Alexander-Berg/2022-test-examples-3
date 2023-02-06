# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import datetime

import pytz
from django.test.client import Client, RequestFactory
from iso8601 import parse_date

from common.utils.date import MSK_TZ


test_server_time_dt = datetime(2001, 2, 2, 12, 13, 14)

MOCK_TRAIN_RESPONSE = {
    'segments': [
        {
            'key': 'train 119A 20180910_12',
            'tariffs': {
                'classes': {
                    'sitting': {
                        'seats': 150,
                        'price': {
                            'currency': 'RUB',
                            'value': 59.31
                        },
                        'trainOrderUrl': '/order/?coachType=sitting',
                    },
                    'suite': {
                        'seats': 40,
                        'price': {
                            'currency': 'RUB',
                            'value': 10000.4
                        },
                        'trainOrderUrl': '/order/?coachType=suite',
                    }
                }
            }
        },
        {
            'key': 'train 222Я 20180910_14',
            'tariffs': {
                'classes': {
                    'sitting': {
                        'seats': 10,
                        'price': {
                            'currency': 'RUB',
                            'value': 1500
                        },
                        'trainOrderUrl': '/order/?coachType=sitting',
                    }
                }
            }
        }
    ],
    'querying': False
}


def check_server_time(msk_dt, response_data):
    iso_dt = response_data['date_time']['server_time']
    utc_dt = parse_date(iso_dt)
    assert utc_dt == MSK_TZ.localize(msk_dt).astimezone(pytz.utc)


def check_segment_departure_utc(departure_time, response_data):
    segment = response_data['days'][0]['segments'][0]
    iso_dt = segment['departure']['time_utc']
    utc_dt = parse_date(iso_dt)
    msk_dt_departure = MSK_TZ.localize(datetime.combine(test_server_time_dt.date(), departure_time))
    assert utc_dt == msk_dt_departure.astimezone(pytz.utc)
    assert iso_dt == '2001-02-01T21:30:00+00:00'


def api_get_json(view, params=None, method='get', **kwargs):
    status_code = kwargs.pop('response_status_code', 200)
    params = params if params is not None else {}
    headers = kwargs if kwargs else {}

    if method == 'get':
        response = Client().get(view, params, **headers)
    elif method == 'post':
        response = Client().post(view, params, **headers)
    else:
        raise ValueError

    assert response.status_code == status_code
    return json.loads(response.content)


def create_request(url='', attribs=None, headers=None):
    attribs = attribs if attribs else {}
    attribs.setdefault('tld', 'ru')

    request = RequestFactory().get(url)
    for atrr_name, attr_value in attribs.items():
        setattr(request, atrr_name, attr_value)

    if headers:
        for key, value in headers.items():
            # Django конвертит все http-хедеры в вид HTTP_SOME_HEADER и кладет в META
            key = key.replace('-', '_').upper()
            request.META['HTTP_' + key] = value

    return request
