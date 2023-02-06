# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from travel.rasp.wizards.suburban_wizard_api.lib.serialization.proto_station import StationQuery, dump_station_response
from travel.rasp.wizards.suburban_wizard_api.lib.station.suburban_directions_cache import ARRIVAL_DIRECTION


def make_station_query(station, experiment_flags=frozenset(), language='ru', **kwargs):
    kwargs.update(
        station=station,
        experiment_flags=experiment_flags,
        language=language
    )
    return StationQuery(*(kwargs.get(field) for field in StationQuery._fields))


def make_station_response_body(station, segments):
    query = make_station_query(station)
    return dump_station_response(((ARRIVAL_DIRECTION, 10, segments),), query).SerializeToString()
