# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from django.db import connection

from common.data_api.baris.test_helpers import mock_baris_response
from common.models.geo import Station
from common.models.transport import TransportType
from tester.factories import create_thread, create_station, create_route, create_rtstation

from travel.rasp.admin.scripts.www_stations import ensure_stations_visibility


@pytest.mark.dbuser
def test_ensure_stations_visibility():
    to_unhide = create_rtstation(
        thread=create_thread(
            route=create_route()
        ),
        station=create_station(hidden=True)
    )
    not_change = create_rtstation(
        thread=create_thread(
            route=create_route()
        ),
        station=create_station(hidden=False)
    )
    to_hide_1 = create_rtstation(
        thread=create_thread(
            route=create_route(hidden=True)
        ),
        station=create_station(hidden=False)
    )
    to_hide_2 = create_rtstation(
        thread=create_thread(
            route=create_route()
        ),
        station=create_station(hidden=False),
        is_technical_stop=True
    )

    unhide_airport = create_station(id=101, t_type=TransportType.PLANE_ID, hidden=True)
    hidden_airport = create_station(id=102, t_type=TransportType.PLANE_ID, hidden=True)
    not_hidden_airport = create_station(id=103, t_type=TransportType.PLANE_ID, hidden=False)
    hide_airport = create_station(id=104, t_type=TransportType.PLANE_ID, hidden=False)

    with mock_baris_response({
        "flights": [
            {'departureStation': 101, 'arrivalStation': 103, 'flightsCount': 1, 'totalFlightsCount': 1}
        ]
    }):
        cursor = connection.cursor()
        ensure_stations_visibility(cursor)

    not_hidden = set(s.id for s in Station.objects.filter(hidden=False))
    hidden = set(s.id for s in Station.objects.filter(hidden=True))

    assert to_unhide.station.id in not_hidden
    assert not_change.station.id in not_hidden
    assert to_hide_1.station.id in hidden
    assert to_hide_2.station.id in hidden

    assert unhide_airport.id in not_hidden
    assert not_hidden_airport.id in not_hidden
    assert hidden_airport.id in hidden
    assert hide_airport.id in hidden
