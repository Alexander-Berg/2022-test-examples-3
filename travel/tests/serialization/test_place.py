# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest

from travel.rasp.wizards.wizard_lib.serialization.place import dump_places


@pytest.mark.parametrize('price, expected', (
    (None, True, None),
    ([{'count': 1}], True, True),
    ([{'count': 1}], False, False),
    ([{'count': 0}, {'count': 0}], True, None),
))
def test_dump_places_electronic_ticket(places, electronic_ticket, expected):
    segment = {'updated_at': '2000-01-01', 'places': places, 'electronic_ticket': electronic_ticket}

    if expected is None:
        assert 'electronic_ticket' not in dump_places(segment)
    else:
        assert dump_places(segment)['electronic_ticket'] == expected