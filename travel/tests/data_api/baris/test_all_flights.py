# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import struct
from io import BytesIO

from common.data_api.baris.all_flights import iterate_on_flights_stream
from travel.proto.dicts.avia.schedule_dump_pb2 import TFlight


def _add_point(schedule, airport_id, arrival_time, arrival_day_shift, departure_time, departure_day_shift):
    point = schedule.Route.add()
    point.AirportID = airport_id
    point.ArrivalTime = arrival_time
    point.ArrivalDayShift = arrival_day_shift
    point.DepartureTime = departure_time
    point.DepartureDayShift = departure_day_shift


def _add_mask(schedule, p_from, p_until, p_on):
    mask = schedule.Masks.add()
    mask.From = p_from
    mask.Until = p_until
    mask.On = p_on


def _flight(title, airline_id):
    flight = TFlight()
    flight.Title = title
    flight.AirlineID = airline_id
    return flight


def _make_flights():
    """
    Создание списка рейсов, каждый рейс - объект типа TFlight
    """
    flights = [_flight('SU 1', 1), _flight('SU 2', 2)]
    schedule = flights[0].Schedules.add()
    _add_point(schedule, 10, '', 0, '01:00:00', 0)
    _add_point(schedule, 20, '02:00:00', 0, '03:00:00', 1),
    _add_point(schedule, 30, '04:00:00', 2, '', 0)
    _add_mask(schedule, '2020-10-01', '2020-10-11', 12),
    _add_mask(schedule, '2020-10-21', '2020-10-31', 34)

    schedule = flights[0].Schedules.add()
    _add_point(schedule, 10, '', 0, '01:30:00', 0),
    _add_point(schedule, 20, '02:30:00', 1, '', 0),
    _add_mask(schedule, '2020-10-02', '2020-10-12', 1234567)

    schedule = flights[1].Schedules.add()
    _add_point(schedule, 40, '', 0, '11:20:00', 0),
    _add_point(schedule, 50, '12:20:00', 0, '', 0),
    _add_mask(schedule, '2020-10-03', '2020-10-13', 7)

    return flights


UINT32 = struct.Struct('<I')


def _get_all_flights_proto_stream(flights):
    """
    Упаковка рейсов в BytesIO при помощи protobuf
    """
    bytes = BytesIO(b'')
    for flight in flights:
        proto = flight.SerializeToString()
        proto_len = UINT32.pack(len(proto))
        # Пишем длину протобуфа, затем сам протобуф
        bytes.write(proto_len)
        bytes.write(proto)

    bytes.seek(0)  # Сдвигаем позицию в начало, чтобы читалось потом с начала
    return bytes


def test_all_flights_iterate():
    init_flights = _make_flights()
    stream = _get_all_flights_proto_stream(init_flights)
    flights = [flight for flight in iterate_on_flights_stream(stream)]

    assert len(flights) == len(init_flights)
    for index in range(0, len(flights)):
        assert flights[index] == init_flights[index]
