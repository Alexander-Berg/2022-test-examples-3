# -*- coding=utf-8 -*-

import pytest

from travel.avia.library.python.common.models.geo import Point
from travel.avia.library.python.tester.factories import (
    create_region, create_station, create_settlement, create_country
)
from travel.avia.library.python.tester.testcase import TestCase


class TestRegionKey(TestCase):
    def test_get_region_by_key(self):
        country = create_country()
        region = create_region(pk=10, title=u'ААА', country=country)
        assert region == Point.get_by_key('r10')

    def test_get_region_key(self):
        country = create_country()
        assert create_region(
            pk=10, title=u'ААА', country=country
        ).point_key == 'r10'


@pytest.mark.dbuser
def test_station_key():
    station = create_station()
    assert station.point_key == 's{}'.format(station.pk)
    assert station == Point.get_by_key('s{}'.format(station.pk))


@pytest.mark.dbuser
def test_settlement_key():
    settlement = create_settlement()
    assert settlement.point_key == 'c{}'.format(settlement.pk)
    assert settlement == Point.get_by_key('c{}'.format(settlement.pk))


@pytest.mark.dbuser
def test_country_key():
    country = create_country()
    assert country.point_key == 'l{}'.format(country.pk)
    assert country == Point.get_by_key('l{}'.format(country.pk))


@pytest.mark.dbuser
def test_point_type():
    station = create_station()
    settlement = create_settlement()
    country = create_country()
    region = create_region()
    some_point = Point()
    assert station.type == 'station'
    assert settlement.type == 'settlement'
    assert country.type == 'country'
    assert region.type == 'region'
    with pytest.raises(NotImplementedError):
        some_point.type


@pytest.mark.dbuser
def test_is_station():
    assert create_station().is_station
    assert not create_settlement().is_station
    assert not create_country().is_station
    assert not create_region().is_station
