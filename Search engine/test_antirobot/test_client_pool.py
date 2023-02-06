# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import pytest

from search.martylib.antirobot.client_pool import location_getter, Location


class TestLocationGetter(object):
    @pytest.mark.parametrize('endpoint,location', (
        ('sas1-0000.search.yandex.net', Location.sas),
        ('man1-0000.search.yandex.net', Location.man),
        ('vla1-0000.search.yandex.net', Location.vla),
        ('guardian.search.yandex.net', None),
    ))
    def test_parse_location(self, endpoint, location):
        assert location_getter(endpoint, 'any') == location
