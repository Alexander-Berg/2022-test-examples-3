# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from travel.cpa.lib.common import json_bytes_to_unicode


def test_json_bytes_to_unicode():
    j = {
        b'dict': {
            b'key_1': 'value_1',
            'key_2': b'value_2',
        },
        'list': ['value_1', b'value_2'],
        b'float': .5,
        'int': 1,
        b'str': b'some text'
    }
    exp = {
        'dict': {
            'key_1': 'value_1',
            'key_2': 'value_2',
        },
        'list': ['value_1', 'value_2'],
        'float': .5,
        'int': 1,
        'str': 'some text'
    }
    assert exp == json_bytes_to_unicode(j)
