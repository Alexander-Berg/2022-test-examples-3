# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest
from django.utils import translation
from rest_framework import serializers

from common.models.geo import Station
from common.tester.factories import create_region, create_settlement
from travel.rasp.wizards.wizard_lib.serialization.point import dump_point, parse_point, parse_point_key
from travel.rasp.wizards.wizard_lib.region_capital_provider import region_capital_provider


@translation.override('ru')
def test_dump_point():
    assert dump_point(None) is None
    assert dump_point(Station(id=123, popular_title_ru='Народное название')) == {
        'key': 's123',
        'title': 'Народное название'
    }


@pytest.mark.dbuser
def test_parse_point_capital_fallback():
    point = create_settlement(id=1, _geo_id=2, title="Point")
    krasnodar_krai = create_region(id=10995, title="Region")  # Краснодарский край
    sochi = create_settlement(
        id=10,
        region_id=krasnodar_krai.id,
        hidden=False,
        title="Sochi",
    )  # Сочи

    region_capital_provider.build_cache()
    region_capital_provider.find_region_capital(
        geo_id=10994,  # Красная поляна
    )

    assert parse_point({'point_key': 'c1'}, 'point_key', 'settlement_geoid') == point
    assert parse_point({'settlement_geoid': '2'}, 'point_key', 'settlement_geoid') == point
    assert parse_point(
        query_params={'settlement_geoid': '10994'},
        point_key_name='point_key',
        settlement_geoid_name='settlement_geoid',
    ) == sochi


@pytest.mark.dbuser
@pytest.mark.parametrize('value, expected', (
    (' ', 'invalid'),
    ('1', 'invalid'),
    ('x', 'invalid'),
    ('x1', 'invalid'),
    ('s1', 'unknown'),
    ('c1', 'unknown'),
))
def test_parse_point_parsing_validation_of_point_key(value, expected):
    create_settlement(id=1, _geo_id=1, hidden=True)

    with pytest.raises(serializers.ValidationError) as excinfo:
        parse_point({'point_key': value}, 'point_key', 'settlement_geoid')
    assert excinfo.value.detail == ['{} point key point_key={!r}'.format(expected, value)]


@pytest.mark.dbuser
@pytest.mark.parametrize('value, expected', (
    (' ', 'invalid'),
    ('x', 'invalid'),
    ('1', 'unknown'),
))
def test_parse_point_parsing_validation_of_geoid(value, expected):
    create_settlement(id=1, _geo_id=1, hidden=True)

    with pytest.raises(serializers.ValidationError) as excinfo:
        parse_point({'settlement_geoid': value}, 'point_key', 'settlement_geoid')
    assert excinfo.value.detail == ['{} geoid settlement_geoid={!r}'.format(expected, value)]


@pytest.mark.dbuser
def test_parse_point_ambigous_validation():
    create_settlement(id=1, _geo_id=2)

    with pytest.raises(serializers.ValidationError) as excinfo:
        parse_point({}, 'point_key', 'settlement_geoid')
    assert excinfo.value.detail == ['one of arguments have to be specified: point_key or settlement_geoid']

    with pytest.raises(serializers.ValidationError) as excinfo:
        parse_point({'point_key': 'c1', 'settlement_geoid': '2'}, 'point_key', 'settlement_geoid')
    assert excinfo.value.detail == ['one of arguments have to be specified: point_key or settlement_geoid']


def test_parse_point_allow_blank():
    assert parse_point({}, 'point_key', 'settlement_geoid', allow_blank=True) is None

    with pytest.raises(serializers.ValidationError):
        parse_point({}, 'point_key', 'settlement_geoid', allow_blank=False)


@pytest.mark.dbuser
def test_parse_point_key():
    point = create_settlement(id=1)

    assert parse_point_key({'point_key': 'c1'}, 'point_key') == point


def test_parse_point_key_allow_blank():
    assert parse_point_key({}, 'point_key', allow_blank=True) is None

    with pytest.raises(serializers.ValidationError):
        parse_point_key({}, 'point_key', allow_blank=False)
