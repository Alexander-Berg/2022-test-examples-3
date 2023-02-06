# -*- coding: utf-8 -*-
import pytest

from travel.avia.ticket_daemon.ticket_daemon.lib.baggage import Baggage


@pytest.mark.parametrize('key, expected', [
    ('1d1d23d', {'included': {'count': 1, 'source': 'db'},
                 'pieces': {'count': 1, 'source': 'db'},
                 'weight': {'count': 23, 'source': 'db'}}),
    ('1p1d15p', {'included': {'count': 1, 'source': 'partner'},
                 'pieces': {'count': 1, 'source': 'db'},
                 'weight': {'count': 15, 'source': 'partner'}}),
    ('1d2p20d', {'included': {'count': 1, 'source': 'db'},
                 'pieces': {'count': 2, 'source': 'partner'},
                 'weight': {'count': 20, 'source': 'db'}}),
    ('0p0p0p', {'included': {'count': 0, 'source': 'partner'},
                'pieces': {'count': 0, 'source': 'partner'},
                'weight': {'count': 0, 'source': 'partner'}}),
    ('0d0d0d', {'included': {'count': 0, 'source': 'db'},
                'pieces': {'count': 0, 'source': 'db'},
                'weight': {'count': 0, 'source': 'db'}}),
    ('1pNN', {'included': {'count': 1, 'source': 'partner'},
              'pieces': None,
              'weight': None}),
    ('NNN', {'included': None,
             'pieces': None,
             'weight': None}),
])
def test_baggage_info_from_key(key, expected):
    assert Baggage.info_from_key(key) == expected
