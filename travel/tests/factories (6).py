# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from common.tester.factories import create_settlement
from travel.rasp.morda_backend.morda_backend.search.search.serialization.request_serialization import SearchContext


def create_search_context(**kwargs):
    data = {
        'when': None,
        'nearest': False,
        'transport_type': 'train',
        'timezones': [],
        'national_version': 'ru'
    }
    data.update(kwargs)
    for key in ('point_from', 'point_to'):
        if key not in data:
            data[key] = create_settlement()
    return SearchContext(**data)
