# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from collections import namedtuple
from contextlib import contextmanager

import hamcrest
import pytest
import ujson
from django.test.client import Client

from common.tester.factories import create_station, create_thread
from common.tester.transaction_context import transaction_fixture
from travel.rasp.wizards.suburban_wizard_api.lib.schedule_cache import schedule_cache
from travel.rasp.wizards.suburban_wizard_api.lib.station.suburban_directions_cache import SuburbanDirectionsCache
from travel.rasp.wizards.wizard_lib.protobuf_models.station_direction_pb2 import TStationDirection
from travel.rasp.wizards.wizard_lib.protobuf_models.station_response_pb2 import TStationResponse


pytestmark = pytest.mark.dbuser

_DefaultQuery = namedtuple('_DefaultQuery', 'query station')


@pytest.fixture
@transaction_fixture
def default_query(request, fixed_now):
    station = create_station(title='some_station')
    return _DefaultQuery(
        query={
            'station_id': station.id,
            'event_date': '2000-01-01'
        },
        station=station,
    )


@contextmanager
def using_precache():
    with schedule_cache.using_precache(), SuburbanDirectionsCache.using_precache():
        yield


def test_segments_not_found(default_query):
    with using_precache():
        response = Client().get('/api/proto_station/', default_query.query)
        assert response.status_code == 204

        response = Client().get('/api/proto_station/', dict(default_query.query, **{'json': 1}))
        assert response.status_code == 200
        assert response.json() == {'error': 'segments not found'}


def test_segments_found(default_query):
    station = default_query.station
    departure_subdir = u'на Юг'
    create_thread(number='some_number', t_type='suburban', schedule_v1=[
        [None, 0, station, dict(departure_subdir=departure_subdir)],
        [10, None],
    ])

    with using_precache():
        response = Client().get('/api/proto_station/', default_query.query)
        assert response.status_code == 200
        assert response['content-type'] == 'application/x-suburban-station'

        station_proto = TStationResponse()
        station_proto.ParseFromString(response.content)

        hamcrest.assert_that(station_proto, hamcrest.has_properties(
            Directions=hamcrest.contains(
                hamcrest.has_properties(
                    Type=TStationDirection.SUBDIR,
                    SuburbanCode=departure_subdir.encode('utf-8'),
                    Total=1,
                    Segments=hamcrest.contains(
                        hamcrest.has_properties(Thread=hamcrest.has_properties(Number='some_number'))
                    )
                )
            )
        ))

        response = Client().get('/api/proto_station/', dict(default_query.query, **{'json': 1}))
        hamcrest.assert_that(ujson.loads(response.content), hamcrest.has_entry(
            'Directions', hamcrest.contains(
                hamcrest.has_entries({
                    'Type': 'SUBDIR',
                    'SuburbanCode': departure_subdir,
                    'Total': 1,
                    'Segments': hamcrest.contains(
                        hamcrest.has_entry('Thread', hamcrest.has_entry('Number', 'some_number'))
                    )
                })
            )
        ))
