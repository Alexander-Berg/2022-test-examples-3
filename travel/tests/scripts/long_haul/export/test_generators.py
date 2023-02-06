# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from contextlib import contextmanager

import mock
import pytest

from common.models.transport import TransportType
from common.tester.factories import create_thread, create_route, create_region, create_station, create_company
from travel.rasp.rasp_scripts.scripts.long_haul.export import generators
from travel.rasp.rasp_scripts.scripts.long_haul.export.generators import RoutesGenerator, ThreadsGenerator, L10nGenerator


@contextmanager
def mock_generators_globals():
    old_route_id_list = generators.route_id_list
    old_thread_id_list = generators.thread_id_list
    old_station_id_set = generators.station_id_set
    old_white_region_ids = generators.white_region_ids

    try:
        generators.route_id_list = set()
        generators.thread_id_list = set()
        generators.station_id_set = set()
        generators.white_region_ids = []
        yield
    finally:
        generators.route_id_list = old_route_id_list
        generators.thread_id_list = old_thread_id_list
        generators.station_id_set = old_station_id_set
        generators.white_region_ids = old_white_region_ids


@pytest.mark.dbuser
class TestRoutesGenerator(object):

    def test_skip_companies(self):
        region = create_region()
        station_from = create_station(region=region)
        station_to = create_station(region=region)

        bad_company = create_company(id=42)

        create_thread_sub = create_thread.mutate(
            t_type='suburban', schedule_v1=[
                [None, 0, station_from],
                [10, None, station_to],
            ],
        )

        route1 = create_route()
        route2 = create_route()
        bad_route = create_route()

        create_thread_sub(route=route1, company=create_company(), ordinal_number=1)
        create_thread_sub(route=route2, company=create_company())
        create_thread_sub(route=bad_route, company=bad_company)
        create_thread_sub(route=route1, company=bad_company, ordinal_number=2)

        with mock_generators_globals(), \
             mock.patch.object(generators, 'get_white_region_ids', mock.Mock(return_value=[region.id])), \
             mock.patch.object(generators, 'SKIP_COMPANIES', [bad_company.id]):

            routes_ids = [r.id for r in RoutesGenerator().generate(None)]

            expected_routes = {route1.id, route2.id}
            assert len(routes_ids) == len(expected_routes)
            assert set(routes_ids) == expected_routes


@pytest.mark.dbuser
class TestThreadsGenerator(object):

    def test_get_threads(self):
        bad_company = create_company(id=42)

        route1 = create_route()
        route2 = create_route()
        bad_route = create_route()

        thread1 = create_thread(route=route1, company=create_company(), ordinal_number=1)
        thread2 = create_thread(route=route2, company=create_company())
        create_thread(route=bad_route, company=bad_company)
        create_thread(route=route1, company=bad_company, ordinal_number=2)

        with mock_generators_globals(), \
             mock.patch.object(generators, 'SKIP_COMPANIES', [bad_company.id]):

            generators.route_id_list = [route1.id, route2.id, bad_route.id]

            expected_thread_ids = {thread1.id, thread2.id}
            thread_ids = [t.id for t in ThreadsGenerator().get_threads()]

            assert len(thread_ids) == len(expected_thread_ids)
            assert set(thread_ids) == expected_thread_ids


@pytest.mark.dbuser
class TestL10nGenerator(object):
    def test_generate(self):
        stations_data = create_station(title='какое-то название', id=875)
        airport_station = create_station(title='какое-то название', t_type=TransportType.PLANE_ID, id=975)

        with mock_generators_globals():
            generators.station_id_set = {stations_data.id, airport_station.id}
            stations_data = list(L10nGenerator().generate(None))

            assert stations_data == [
                ('stop',
                 'station__lh_875',
                 'ru',
                 'nominative',
                 'какое-то название'),
                ('stop',
                 'station__lh_875',
                 'ru',
                 '',
                 'какое-то название'),
                ('stop',
                 'station__lh_975',
                 'ru',
                 'nominative',
                 'Аэропорт какое-то название'),
                ('stop',
                 'station__lh_975',
                 'ru',
                 '',
                 'Аэропорт какое-то название')
            ]
