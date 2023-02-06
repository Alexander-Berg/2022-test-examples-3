# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import date

import pytest
from django.utils.datastructures import MultiValueDict

from common.tester.factories import create_station
from common.tester.utils.datetime import replace_now
from travel.rasp.wizards.proxy_api.lib.station.models import PlaneStationQuery, SuburbanStationQuery
from travel.rasp.wizards.proxy_api.lib.station.serialization import load_plane_station_query, load_suburban_station_query
from travel.rasp.wizards.wizard_lib.experiment_flags import ExperimentFlag
from travel.rasp.wizards.wizard_lib.station.direction_type import DirectionType

pytestmark = pytest.mark.dbuser


@replace_now('2000-01-01')
def test_load_plane_station_query():
    station = create_station()

    assert load_plane_station_query(
        station,
        frozenset([ExperimentFlag.DUMMY_FLAG]),
        MultiValueDict({'date': ['2000-01-01'], 'lang': ['uk'], 'tld': ['com']})
    ) == PlaneStationQuery(
        station=station,
        event_date=date(2000, 1, 1),
        direction_type=None,
        language='uk',
        experiment_flags=frozenset([ExperimentFlag.DUMMY_FLAG]),
        tld='com'
    )


def test_load_plane_station_query_direction_type():
    station = create_station()

    assert load_plane_station_query(
        station,
        frozenset(),
        MultiValueDict({'query': ['not_direction_type']})
    ).direction_type is None

    assert load_plane_station_query(
        station,
        frozenset(),
        MultiValueDict({'query': ['not_direction_type', 'departure']})
    ).direction_type is DirectionType.DEPARTURE


@replace_now('2000-01-01')
def test_load_suburban_station_query():
    station = create_station()

    assert load_suburban_station_query(
        station,
        frozenset([ExperimentFlag.DUMMY_FLAG]),
        MultiValueDict({'date': ['2000-01-01'], 'lang': ['uk'], 'tld': ['com']})
    ) == SuburbanStationQuery(
        station=station,
        event_date=date(2000, 1, 1),
        language='uk',
        experiment_flags=frozenset([ExperimentFlag.DUMMY_FLAG]),
        tld='com'
    )
