# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json


def set_suburban_selling_response(httpretty, selling_tariffs, keys):
    selling_partners = [
        {
            'code': 'cppk',
            'provider': 'movista',
            'ogrn': 111,
            'title': 'ЦППК',
            'address': 'дом',
            'work_time': 'всегда'
        },
        {
            'code': 'aeroexpress',
            'provider': 'aeroexpress',
            'ogrn': 222,
            'title': 'Аэроэкспресс',
            'address': 'улица',
            'work_time': 'иногда'
        },
    ]

    httpretty.register_uri(
        httpretty.POST, 'https://sellingurl.net/get_tariffs',
        status=200, content_type='application/json',
        body=json.dumps({
            'result': {
                'selling_tariffs': selling_tariffs,
                'keys': keys,
                'selling_partners': selling_partners
            },
            'errors': [],
        })
    )
