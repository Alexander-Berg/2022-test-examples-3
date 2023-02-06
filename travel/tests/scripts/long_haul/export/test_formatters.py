# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from common.models.transport import TransportType
from common.tester.factories import create_station
from travel.rasp.rasp_scripts.scripts.long_haul.export.formatters import get_station_title, StopsFormatter


@pytest.mark.dbuser
def test_get_station_title():
    station = create_station(title='какое-то название')
    airport_station = create_station(title='какое-то название', t_type=TransportType.PLANE_ID)

    assert get_station_title(station) == 'какое-то название'
    assert get_station_title(airport_station) == 'Аэропорт какое-то название'


@pytest.mark.dbuser
def test_stops_formatter():
    formatter = StopsFormatter()
    station = create_station(title='какое-то название', id=875)
    assert formatter.format(station) == (
        'station__lh_875',
        None,
        'какое-то название',
        'stop',
        1,
        1
    )

    airport_station = create_station(title='какое-то название', t_type=TransportType.PLANE_ID, id=975)
    assert formatter.format(airport_station) == (
        'station__lh_975',
        None,
        'Аэропорт какое-то название',
        'stop',
        1,
        1
    )
