# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from django.core.exceptions import ObjectDoesNotExist

from travel.rasp.library.python.common23.models.core.geo.country import Country
from travel.rasp.library.python.common23.tester.factories import (
    create_region, create_station, create_settlement, create_country
)

from travel.rasp.library.python.common23.tester.testcase import TestCase
from travel.rasp.library.python.common23.models.core.geo.point import Point


class TestRegionKey(TestCase):
    def test_get_region_by_key(self):
        region = create_region(pk=10, title=u'ААА', country=Country.RUSSIA_ID)
        assert region == Point.get_by_key('r10')

    def test_get_region_key(self):
        assert create_region(pk=10, title=u'ААА', country=Country.RUSSIA_ID).point_key == 'r10'


@pytest.mark.dbuser
def test_station_key():
    station = create_station()
    assert station.point_key == 's{}'.format(station.pk)
    assert station == Point.get_by_key('s{}'.format(station.pk))

    hidden_station = create_station(hidden=True)
    assert hidden_station == Point.get_by_key(hidden_station.point_key, use_hidden_manager=False)

    with pytest.raises(ObjectDoesNotExist):
        assert hidden_station == Point.get_by_key(hidden_station.point_key)


@pytest.mark.dbuser
def test_settlement_key():
    settlement = create_settlement()
    assert settlement.point_key == 'c{}'.format(settlement.pk)
    assert settlement == Point.get_by_key('c{}'.format(settlement.pk))

    hidden_settlement = create_station(hidden=True)
    assert hidden_settlement == Point.get_by_key(hidden_settlement.point_key, use_hidden_manager=False)

    with pytest.raises(ObjectDoesNotExist):
        assert hidden_settlement == Point.get_by_key(hidden_settlement.point_key)


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
