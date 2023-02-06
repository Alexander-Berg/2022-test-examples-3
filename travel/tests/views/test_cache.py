# encoding: utf-8

import pytest
from django.db import connection
from django.test.utils import CaptureQueriesContext

from common.tester.factories import create_settlement, create_station
from geosearch.views.cache import ExternalDirectionCache, SuburbanZoneCache, StationCache

pytestmark = pytest.mark.dbuser('module')


class TestExternalDirectionCache(object):
    def _making_queries(self):
        station = create_station()
        settlement = create_settlement()

        with CaptureQueriesContext(connection) as queries:
            ExternalDirectionCache.markers_by_stations([station])
            ExternalDirectionCache.markers_by_stations_ids([station.id])
            list(ExternalDirectionCache.by_station(station))
            list(ExternalDirectionCache.by_settlement(settlement))

            return bool(len(queries))

    def test_using_precache(self):
        with ExternalDirectionCache.using_precache():
            with ExternalDirectionCache.using_precache():  # using_precache() can be nested
                assert not self._making_queries()
            assert not self._making_queries()
        assert self._making_queries()


class TestSuburbanZoneCache(object):
    def _making_queries(self):
        with CaptureQueriesContext(connection) as queries:
            SuburbanZoneCache.by_settlement_id(12345)

            return bool(len(queries))

    def test_using_precache(self):
        with SuburbanZoneCache.using_precache():
            with SuburbanZoneCache.using_precache():  # using_precache() can be nested
                assert not self._making_queries()
            assert not self._making_queries()
        assert self._making_queries()


class TestStationCache(object):
    def _making_queries(self):
        settlement = create_settlement()

        with CaptureQueriesContext(connection) as queries:
            list(StationCache.by_settlement(settlement))

            return bool(len(queries))

    def test_using_precache(self):
        with StationCache.using_precache():
            with StationCache.using_precache():  # using_precache() can be nested
                assert not self._making_queries()
            assert not self._making_queries()
        assert self._making_queries()
