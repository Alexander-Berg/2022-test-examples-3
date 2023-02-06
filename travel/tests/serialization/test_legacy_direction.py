# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest
from django.utils.datastructures import MultiValueDict
from rest_framework import serializers

from common.models.geo import Country, Region
from common.tester.factories import create_region, create_settlement, create_station
from travel.rasp.wizards.train_wizard_api.serialization.legacy_direction import load_query

pytestmark = pytest.mark.dbuser


@pytest.mark.dbuser
@pytest.mark.parametrize('departure_point_factory', (create_settlement, create_station))
@pytest.mark.parametrize('departure_country, expected_valid_departure', (
    (Country.RUSSIA_ID, True), ({}, False), (None, False),
))
@pytest.mark.parametrize('arrival_point_factory', (create_settlement, create_station))
@pytest.mark.parametrize('arrival_country, expected_valid_arrival', (
    (Country.RUSSIA_ID, True), ({}, False), (None, False),
))
def test_load_query_russia_points(
    departure_point_factory,
    departure_country,
    expected_valid_departure,
    arrival_point_factory,
    arrival_country,
    expected_valid_arrival
):
    departure_point = departure_point_factory(country=departure_country)
    arrival_point = arrival_point_factory(country=arrival_country)

    if expected_valid_departure and expected_valid_arrival:
        query = load_query(MultiValueDict({
            'departure_point_key': [departure_point.point_key],
            'arrival_point_key': [arrival_point.point_key]
        }))
        assert query.departure_point == departure_point
        assert query.arrival_point == arrival_point
    else:
        with pytest.raises(serializers.ValidationError) as excinfo:
            load_query(MultiValueDict({
                'departure_point_key': [departure_point.point_key],
                'arrival_point_key': [arrival_point.point_key]
            }))
        assert excinfo.value.detail == ['direction points should be within Russia']


@pytest.mark.parametrize('departure_point_factory', (create_settlement, create_station))
@pytest.mark.parametrize('departure_region, departure_inside_kaliningrad', (
    (Region.KALININGRAD_REGION_ID, True), ({}, False), (None, False),
))
@pytest.mark.parametrize('arrival_point_factory', (create_settlement, create_station))
@pytest.mark.parametrize('arrival_region, arrival_inside_kaliningrad', (
    (Region.KALININGRAD_REGION_ID, True), ({}, False), (None, False),
))
def test_load_query_kaliningrad_points(
    departure_point_factory,
    departure_region,
    departure_inside_kaliningrad,
    arrival_point_factory,
    arrival_region,
    arrival_inside_kaliningrad
):
    create_region(id=Region.KALININGRAD_REGION_ID)
    departure_point = departure_point_factory(country=Country.RUSSIA_ID, region=departure_region)
    arrival_point = arrival_point_factory(country=Country.RUSSIA_ID, region=arrival_region)

    if departure_inside_kaliningrad == arrival_inside_kaliningrad:
        query = load_query(MultiValueDict({
            'departure_point_key': [departure_point.point_key],
            'arrival_point_key': [arrival_point.point_key]
        }))
        assert query.departure_point == departure_point
        assert query.arrival_point == arrival_point
    else:
        with pytest.raises(serializers.ValidationError) as excinfo:
            load_query(MultiValueDict({
                'departure_point_key': [departure_point.point_key],
                'arrival_point_key': [arrival_point.point_key]
            }))
        assert excinfo.value.detail == ['crossing of Kaliningrad region border is not alowed']
