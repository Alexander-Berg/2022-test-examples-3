# -*- coding: utf-8 -*-
import pytest

from travel.avia.ticket_daemon_api.jsonrpc.lib.baggage import info_from_key


@pytest.mark.parametrize('key, expected', [
    ('1d1d23d', {'included': {'count': 1, 'source': 'db'},
                 'pc': {'count': 1, 'source': 'db'},
                 'wt': {'count': 23, 'source': 'db'}}),
    ('1p1d15p', {'included': {'count': 1, 'source': 'partner'},
                 'pc': {'count': 1, 'source': 'db'},
                 'wt': {'count': 15, 'source': 'partner'}}),
    ('1d2p20d', {'included': {'count': 1, 'source': 'db'},
                 'pc': {'count': 2, 'source': 'partner'},
                 'wt': {'count': 20, 'source': 'db'}}),
    ('0p0p0p', {'included': {'count': 0, 'source': 'partner'},
                'pc': {'count': 0, 'source': 'partner'},
                'wt': {'count': 0, 'source': 'partner'}}),
    ('0d0d0d', {'included': {'count': 0, 'source': 'db'},
                'pc': {'count': 0, 'source': 'db'},
                'wt': {'count': 0, 'source': 'db'}}),
    ('1pNN', {'included': {'count': 1, 'source': 'partner'},
              'pc': None,
              'wt': None}),
    ('NNN', {'included': None,
             'pc': None,
             'wt': None}),
])
def test_baggage_info_from_key(key, expected):
    assert info_from_key(key) == expected
