# -*- coding: utf-8 -*-
from __future__ import absolute_import

from itertools import chain

import pytest

from travel.avia.backend.main.lib.covid_restrictions import _cache


def test_covid_restrictions():
    """Базовая проверка, что csv-файл читаемый и кэш наполняется"""
    try:
        region_restrictions, country_restrictions = _cache()
        fields = ('mask', 'gloves', 'passes', 'selfIsolation', 'borders', 'untilDate', 'fullText', 'closedText')
        boolean_fields = ('isClosed',)
        for id_, restriction in chain(region_restrictions.iteritems(), country_restrictions.iteritems()):
            assert isinstance(id_, int)
            for field in fields:
                assert isinstance(restriction[field], basestring)
            for field in boolean_fields:
                assert isinstance(restriction[field], bool)
    except Exception:
        pytest.fail('Unexpected Exception on parsing covid_restrictions.csv')
