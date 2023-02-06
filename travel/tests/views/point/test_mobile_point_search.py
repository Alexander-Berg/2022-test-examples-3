# -*- coding: utf-8 -*-

import pytest

from travel.avia.library.python.geosearch.views.point import MobilePointSearch
from travel.avia.library.python.tester.factories import create_station, create_settlement


@pytest.mark.dbuser
def test_find_settlement_by_iata():
    """
    FIXME: Почему станция не находится и какая логика ее отсекает не понятно
    """
    create_station(__={'codes': {'IATA': u'RRR'}})
    settlement = create_settlement(iata=u'RRR')

    results = MobilePointSearch().find_points(u'RrR')

    assert results == [settlement]


@pytest.mark.dbuser
def test_find_station_by_iata():
    station = create_station(__={'codes': {'IATA': u'RRR'}})

    results = MobilePointSearch().find_points(u'RrR')

    assert results == [station]
