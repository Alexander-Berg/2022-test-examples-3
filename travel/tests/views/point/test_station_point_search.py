# -*- coding: utf-8 -*-

import pytest
from hamcrest import assert_that, contains_inanyorder

from travel.avia.library.python.geosearch.views.point import StationPointSearch
from travel.avia.library.python.tester.factories import create_station, create_settlement


@pytest.mark.dbuser
def test_find_by_iata():
    station = create_station(__={'codes': {'IATA': u'RRR'}})
    settlement = create_settlement(iata=u'RRR')

    results = StationPointSearch().find_points(u'RrR')

    assert_that(results, contains_inanyorder(station, settlement))
