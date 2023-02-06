# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from geosearch.models import NameSearchIndex
from travel.rasp.wizards.proxy_api.lib.direction.models import DirectionQuery, Segment
from travel.rasp.wizards.proxy_api.lib.general.models import GeneralQuery
from travel.rasp.wizards.proxy_api.lib.segments.models import Thread, Urls
from travel.rasp.wizards.proxy_api.lib.station.models import PlaneStationQuery, SuburbanStationQuery
from travel.rasp.wizards.wizard_lib.serialization.limit import DEFAULT_SEGMENTS_LIMIT
from travel.rasp.wizards.wizard_lib.serialization.proto_direction import dump_segments
from travel.rasp.wizards.wizard_lib.tests_utils import utc_dt


def make_direction_query(
    departure_point=None, arrival_point=None, experiment_flags=frozenset(), language='ru', **kwargs
):
    kwargs.update(
        departure_point=departure_point,
        arrival_point=arrival_point,
        experiment_flags=experiment_flags,
        language=language,
        limit=DEFAULT_SEGMENTS_LIMIT,
    )
    return DirectionQuery(*(kwargs.get(field) for field in DirectionQuery._fields))


def make_suburban_station_query(station=None, experiment_flags=frozenset(), language='ru', **kwargs):
    kwargs.update(
        station=station,
        experiment_flags=experiment_flags,
        language=language
    )
    return SuburbanStationQuery(*(kwargs.get(field) for field in SuburbanStationQuery._fields))


def make_plane_station_query(station=None, experiment_flags=frozenset(), language='ru', **kwargs):
    kwargs.update(
        station=station,
        experiment_flags=experiment_flags,
        language=language
    )
    return PlaneStationQuery(*(kwargs.get(field) for field in PlaneStationQuery._fields))


def make_general_query(departure_settlement=None, experiment_flags=frozenset(), language='ru', **kwargs):
    kwargs.update(
        departure_settlement=departure_settlement,
        experiment_flags=experiment_flags,
        language=language
    )
    return GeneralQuery(*(kwargs.get(field) for field in GeneralQuery._fields))


def make_segment(factory=Segment, **kwargs):
    thread = kwargs.get('thread', {})
    if isinstance(thread, dict):
        kwargs['thread'] = Thread(*(thread.get(field) for field in Thread._fields))

    urls = kwargs.get('urls', dict(desktop='http://desktop', mobile='http://mobile'))
    if isinstance(urls, dict):
        kwargs['urls'] = Urls(*(urls.get(field) for field in Urls._fields))

    kwargs.setdefault('departure_local_dt', utc_dt(2000, 1, 1, 1))
    kwargs.setdefault('arrival_local_dt', utc_dt(2000, 1, 1, 12))

    return factory(*(kwargs.get(field) for field in factory._fields))


def make_direction_response_body(segments, query):
    return dump_segments(segments, query).SerializeToString()


def create_indexed_points(*point_factories):
    points = [point_factory() for point_factory in point_factories]
    if points:
        NameSearchIndex.objects.bulk_create(NameSearchIndex.get_records())
    return points
