# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from django.db import connection
from django.test.utils import CaptureQueriesContext

from travel.rasp.library.python.common23.models.core.geo.replace_exception import ReplaceException
from travel.rasp.library.python.common23.tester.factories import create_settlement, create_station


@pytest.mark.dbuser
class TestReplaceException(object):
    def test_using_precache(self):
        settlement = create_settlement()

        with CaptureQueriesContext(connection) as queries:
            with ReplaceException.using_precache():
                with ReplaceException.using_precache():
                    ReplaceException.check_points(settlement, settlement)
                    assert len(queries) == 1

                ReplaceException.check_points(settlement, settlement)
                assert len(queries) == 1

            ReplaceException.check_points(settlement, settlement)
            assert len(queries) == 2

    def test_check_points_settlements(self):
        city_from = create_settlement()
        city_to = create_settlement()

        assert not ReplaceException.check_points(city_from, city_to)

        ReplaceException.objects.create(city_from=city_from, city_to=city_to)

        assert ReplaceException.check_points(city_from, city_to)
        assert not ReplaceException.check_points(create_settlement(), city_to)

    def test_check_points_settlement_station(self):
        city_from = create_settlement()
        station_to = create_station()

        assert not ReplaceException.check_points(city_from, station_to)

        ReplaceException.objects.create(city_from=city_from, station_to=station_to)

        assert ReplaceException.check_points(city_from, station_to)
        assert not ReplaceException.check_points(create_settlement(), station_to)
