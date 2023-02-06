# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import date

import hamcrest
import pytest
from rest_framework import serializers

from common.tester.factories import create_station, create_transport_subtype
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_setting
from travel.rasp.wizards.suburban_wizard_api.lib.tests_utils import make_station_query
from travel.rasp.wizards.suburban_wizard_api.lib.serialization.proto_station import dump_station_response, load_query
from travel.rasp.wizards.suburban_wizard_api.lib.station.suburban_directions_cache import ARRIVAL_DIRECTION
from travel.rasp.wizards.wizard_lib.cache import Translations
from travel.rasp.wizards.wizard_lib.experiment_flags import ExperimentFlag
from travel.rasp.wizards.wizard_lib.protobuf_models.station_direction_pb2 import TStationDirection
from travel.rasp.wizards.wizard_lib.protobuf_models.station_response_pb2 import TStationResponse
from travel.rasp.wizards.wizard_lib.protobuf_models.thread_pb2 import TThread
from travel.rasp.wizards.wizard_lib.tests_utils import make_dummy_raw_segment


pytestmark = pytest.mark.dbuser


@replace_now('2000-01-01')
def test_load_query():
    station = create_station()

    assert load_query({
        'station_id': station.id
    }) == make_station_query(
        station=station
    )

    assert load_query({
        'station_id': station.id, 'event_date': '2000-02-01', 'lang': 'uk', 'tld': 'ua', 'exp_flags': 'RASPWIZARDS-600'
    }) == make_station_query(
        station=station,
        event_date=date(2000, 2, 1),
        language='uk',
        tld='ua',
        experiment_flags=frozenset([ExperimentFlag.SUBURBAN_STATION_HACKED_FUTURE])
    )


@pytest.mark.dbuser
def test_load_query_station_id_validation():
    with pytest.raises(serializers.ValidationError) as excinfo:
        load_query({})
    assert excinfo.value.detail == ['station_id is required']

    with pytest.raises(serializers.ValidationError) as excinfo:
        load_query({'station_id': '123'})
    assert excinfo.value.detail == ['unknown station_id']


@replace_now('2000-01-01')
@replace_setting('MORDA_HOST_BY_TLD', {'ru': 'rasp.yandex.ru'})
@replace_setting('TOUCH_HOST_BY_TLD', {'ru': 't.rasp.yandex.ru'})
def test_dump_station_response():
    departure_station = create_station()
    arrival_station = create_station()
    transport_subtype = create_transport_subtype(t_type='suburban')
    dummy_segment = make_dummy_raw_segment(departure_station, arrival_station, transport_subtype.id)
    dummy_segment.thread.express_type = 'express'
    dummy_segment.event_stop.platform = 'platform_text'
    dummy_segment.event_stop.stops_text = Translations(**{lang: 'stops_text' for lang in Translations._fields})
    query = make_station_query(arrival_station)
    result = dump_station_response([(ARRIVAL_DIRECTION, 10, [dummy_segment])], query)
    expected_thread_url = (
        'https://{{}}/thread/some_thread_uid/?departure=2000-01-01&station_from={}&station_to={}'
        .format(departure_station.id, arrival_station.id)
    )

    assert isinstance(result, TStationResponse)
    hamcrest.assert_that(result, hamcrest.has_properties(
        Directions=hamcrest.contains(
            hamcrest.has_properties(
                SuburbanCode='arrival',
                Segments=hamcrest.contains(
                    hamcrest.has_properties(
                        ArrivalStationId=arrival_station.id,
                        DepartureStationId=departure_station.id,
                        ArrivalTimestamp=hamcrest.has_properties(Timestamp=946771200, UtcOffset=10800),
                        DepartureTimestamp=hamcrest.has_properties(Timestamp=946684800, UtcOffset=10800),
                        TrainPlatform='platform_text',
                        TrainStops='stops_text',
                        Thread=hamcrest.has_properties(
                            Number='some_number',
                            StartDate=730120,
                            Title='some_title',
                            TransportSubtypeId=transport_subtype.id,
                            ExpressType=TThread.EXPRESS
                        ),
                        Urls=hamcrest.has_properties(
                            Desktop=expected_thread_url.format('rasp.yandex.ru'),
                            Mobile=expected_thread_url.format('t.rasp.yandex.ru'),
                        )
                    ),
                ),
                Total=10,
                Type=TStationDirection.ARRIVAL,
            )
        )
    ))


def test_dump_station_response_segments_limit():
    departure_station = create_station()
    arrival_station = create_station()
    raw_directions = [(ARRIVAL_DIRECTION, 10, [make_dummy_raw_segment(departure_station, arrival_station)]*5)]
    query = make_station_query(arrival_station)

    hamcrest.assert_that(
        dump_station_response(raw_directions, query),
        hamcrest.has_properties(Directions=hamcrest.contains(hamcrest.has_properties(Segments=hamcrest.has_length(5))))
    )

    hamcrest.assert_that(
        dump_station_response(raw_directions, query, segments_limit=3),
        hamcrest.has_properties(Directions=hamcrest.contains(hamcrest.has_properties(Segments=hamcrest.has_length(3))))
    )
