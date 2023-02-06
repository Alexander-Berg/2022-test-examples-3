# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import json

import pytest

from travel.avia.library.python.common.models.transport import TransportType
from travel.avia.library.python.common.utils.title_generator import (
    build_simple_title_common, build_default_title_common, TitleGenerator, generalize_point
)
from travel.avia.library.python.tester.factories import create_settlement, create_station, create_country, create_thread


@pytest.mark.dbuser
def tests_build_simple_title_common():
    assert json.loads(build_simple_title_common(TransportType.objects.get(pk=TransportType.PLANE_ID), [
        create_station(pk=1000), create_settlement(pk=2000)
    ])) == {
        'type': 'default',
        't_type': 'plane',
        'title_parts': ['s1000', 'c2000']
    }


def build_default_title_dict(points, intracity_path=False):
    return json.loads(build_default_title_common(
        TransportType.objects.get(pk=TransportType.PLANE_ID), points, intracity_path=intracity_path
    ))


@pytest.mark.dbuser
def tests_build_default_title_common():
    settlement_from = create_settlement(title='Settlement from')
    station_from = create_station(settlement=settlement_from, title='Station from')
    settlement_to = create_settlement(title='Settlement to')
    station_to = create_station(settlement=settlement_to, not_generalize=True, title='Station to')

    points = [station_from, station_to]

    title_dict = build_default_title_dict(points)
    assert title_dict == {
        'type': 'default',
        't_type': 'plane',
        'title_parts': [settlement_from.point_key, station_to.point_key]
    }
    assert TitleGenerator.L_title(title_dict, lang='ru') == 'Settlement from - Station to'

    title_dict = build_default_title_dict(points, intracity_path=True)
    assert title_dict['title_parts'] == [station_from.point_key, station_to.point_key]
    assert TitleGenerator.L_title(title_dict, lang='ru') == 'Station from - Station to'


@pytest.mark.dbuser
def test_generalize_point():
    settlement = create_settlement()
    station = create_station(settlement=settlement)

    assert generalize_point(station) == settlement
    assert generalize_point(station, intracity_path=True) == station
    assert generalize_point(settlement) == settlement

    station.not_generalize = True
    assert generalize_point(station) == station

    country = create_country()
    assert generalize_point(country) == country

    station_without_settlement = create_station(settlement=None)
    assert generalize_point(station_without_settlement) == station_without_settlement


@pytest.mark.dbuser
def test_title_generator_simple_case():
    station_from = create_station(settlement=create_settlement(title='Settlement from'),
                                  title='Station from')
    station_to = create_station(settlement=create_settlement(title='Settlement to'),
                                not_generalize=True, title='Station to')
    thread = create_thread(t_type=TransportType.PLANE_ID, schedule_v1=[
        [None, 0, station_from],
        [10, None, station_to],
    ])

    generator = TitleGenerator(thread)
    generator.generate()

    assert generator.title == 'Settlement from - Station to'
