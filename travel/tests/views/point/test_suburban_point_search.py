# coding: utf-8
from __future__ import unicode_literals

import pytest
from django.db import connection
from django.test.utils import CaptureQueriesContext

from common.models.geo import Station
from common.models.transport import TransportType
from common.tester.factories import create_station
from geosearch.models import NameSearchIndex
from geosearch.views.point import SuburbanPointSearch


@pytest.mark.dbuser
def test_train_station_by_iata_is_found():
    station = create_station(__={'codes': {'IATA': 'RRR'}}, t_type='train')

    assert station in SuburbanPointSearch().find_points('RRR')


@pytest.mark.dbuser
def test_bus_station_by_iata_not_found():
    station = create_station(__={'codes': {'IATA': 'RRR'}}, t_type='bus')

    assert station not in SuburbanPointSearch().find_points('RRR')


@pytest.mark.dbuser
def test_precache():
    station = create_station(__={'codes': {'IATA': 'RRR'}}, t_type='train')

    with Station.objects.using_precache(), \
            Station.code_manager.using_precache(), \
            TransportType.objects.using_precache(), \
            NameSearchIndex.using_precache(), \
            CaptureQueriesContext(connection) as queries:
        assert station in SuburbanPointSearch().find_points('RRR')
        assert not len(queries)
