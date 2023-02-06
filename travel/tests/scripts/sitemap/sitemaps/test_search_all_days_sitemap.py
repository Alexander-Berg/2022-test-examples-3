# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from django.contrib.sites.models import Site
from hamcrest import assert_that, contains_inanyorder

from common.data_api.baris.test_helpers import mock_baris_response
from common.models.transport import TransportType, TransportSubtype
from common.models.geo import Station2Settlement
from common.tester.testcase import TestCase
from common.tester.factories import create_settlement, create_thread, create_station, create_transport_subtype
from common.tester.utils.django_cache import clear_cache_until_switch
from route_search.models import ZNodeRoute2

from travel.rasp.admin.scripts.sitemap.sitemaps.search import SearchAllDaysSitemap


pytestmark = [pytest.mark.dbuser]


class TestSearchAllDaysSitemap(TestCase):
    @staticmethod
    def _create_thread(station_from, station_to, t_type=TransportType.TRAIN_ID, t_subtype=None):
        return create_thread(
            __={'calculate_noderoute': True},
            t_type=t_type,
            schedule_v1=[
                [None, 0, station_from],
                [10, None, station_to],
            ],
            t_subtype=t_subtype
        )

    @staticmethod
    def _run_test(expected_urls):
        sitemap = SearchAllDaysSitemap(use_cache=False)
        urls = sitemap.get_urls(site=Site(domain='rasp.yandex.ru'))

        actual_urls = [url['location'] for url in urls]
        assert_that(actual_urls, contains_inanyorder(*expected_urls))

    def test_plane_records(self):
        """
        Карта для поисков самолетами из БАРиС
        """
        city_1 = create_settlement(id=1001, slug='city1')
        city_2 = create_settlement(id=2001, slug='city2')
        create_station(id=101, slug='station11', settlement=city_1, t_type=TransportType.PLANE_ID)
        create_station(id=102, slug='station12', settlement=city_1, t_type=TransportType.PLANE_ID)
        create_station(id=201, slug='station21', settlement=city_2, t_type=TransportType.PLANE_ID)
        station_22 = create_station(id=202, slug='station22', t_type=TransportType.PLANE_ID)
        Station2Settlement.objects.create(station=station_22, settlement=city_2)
        create_station(id=301, slug='station31', t_type=TransportType.PLANE_ID)
        with mock_baris_response({
            "flights": [
                {'departureStation': 101, 'arrivalStation': 201, 'flightsCount': 1, 'totalFlightsCount': 1},
                {'departureStation': 102, 'arrivalStation': 201, 'flightsCount': 1, 'totalFlightsCount': 1},
                {'departureStation': 202, 'arrivalStation': 101, 'flightsCount': 1, 'totalFlightsCount': 1},
                {'departureStation': 101, 'arrivalStation': 301, 'flightsCount': 1, 'totalFlightsCount': 1},
                {'departureStation': 201, 'arrivalStation': 301, 'flightsCount': 1, 'totalFlightsCount': 1},
                {'departureStation': 301, 'arrivalStation': 202, 'flightsCount': 1, 'totalFlightsCount': 1},

                {'departureStation': 101, 'arrivalStation': 999, 'flightsCount': 1, 'totalFlightsCount': 1},
                {'departureStation': 998, 'arrivalStation': 101, 'flightsCount': 1, 'totalFlightsCount': 1},
                {'departureStation': 101, 'arrivalStation': 102, 'flightsCount': 1, 'totalFlightsCount': 1},
            ]}
        ):
            self._run_test([
                'https://rasp.yandex.ru/plane/city1--city2',
                'https://rasp.yandex.ru/plane/city2--city1',
                'https://rasp.yandex.ru/plane/city1--station21',
                'https://rasp.yandex.ru/plane/city1--station31',
                'https://rasp.yandex.ru/plane/city2--station11',
                'https://rasp.yandex.ru/plane/city2--station31',
                'https://rasp.yandex.ru/plane/station11--city2',
                'https://rasp.yandex.ru/plane/station12--city2',
                'https://rasp.yandex.ru/plane/station22--city1',
                'https://rasp.yandex.ru/plane/station31--city2',
            ])

    def test_bus_records(self):
        city1 = create_settlement(slug='city1')
        city2 = create_settlement(slug='city2')
        station1 = create_station(slug='station1', settlement=city1)
        station2 = create_station(slug='station2', settlement=city2)
        self._create_thread(station1, station2, TransportType.BUS_ID)
        self._create_thread(station1, station1, TransportType.BUS_ID)

        city3 = create_settlement(slug='city3')
        city4 = create_settlement(slug='city4')
        station31 = create_station(slug='station31', settlement=city3)
        station32 = create_station(slug='station32', settlement=city3)
        station41 = create_station(slug='station41', settlement=city4)
        station42 = create_station(slug='station42', settlement=city4)
        self._create_thread(station31, station41, TransportType.BUS_ID)
        self._create_thread(station32, station41, TransportType.BUS_ID)
        self._create_thread(station31, station42, TransportType.BUS_ID)
        for z in ZNodeRoute2.objects.filter(station_from_id=station31.id):
            z.good_for_start = False
            z.save()
        for z in ZNodeRoute2.objects.filter(station_to_id=station41.id):
            z.good_for_finish = False
            z.save()

        city6 = create_settlement(slug='city6')
        station5 = create_station(slug='station5')
        station6 = create_station(slug='station6', settlement=city6)
        station7 = create_station(slug='station7')
        self._create_thread(station5, station6, TransportType.BUS_ID)
        self._create_thread(station6, station7, TransportType.BUS_ID)

        station8 = create_station(slug='station8')
        station9 = create_station(slug='station9')
        self._create_thread(station8, station9, TransportType.BUS_ID)

        with mock_baris_response({'flights': []}):
            self._run_test([
                'https://rasp.yandex.ru/bus/station1--city2',
                'https://rasp.yandex.ru/bus/city1--station2',
                'https://rasp.yandex.ru/bus/city1--city2',

                'https://rasp.yandex.ru/bus/station31--city4',
                'https://rasp.yandex.ru/bus/station32--city4',
                'https://rasp.yandex.ru/bus/city3--station41',
                'https://rasp.yandex.ru/bus/city3--station42',
                'https://rasp.yandex.ru/bus/city3--city4',

                'https://rasp.yandex.ru/bus/station5--city6',
                'https://rasp.yandex.ru/bus/city6--station7',
            ])

    def test_train_records(self):
        city1 = create_settlement(slug='city1')
        city2 = create_settlement(slug='city2')
        station1 = create_station(slug='station1', settlement=city1)
        station2 = create_station(slug='station2', settlement=city2)
        self._create_thread(station1, station2)
        self._create_thread(station1, station1)

        city3 = create_settlement(slug='city3')
        city4 = create_settlement(slug='city4')
        station31 = create_station(slug='station31', settlement=city3)
        station32 = create_station(slug='station32', settlement=city3)
        station41 = create_station(slug='station41', settlement=city4)
        station42 = create_station(slug='station42', settlement=city4)
        self._create_thread(station31, station41)
        self._create_thread(station32, station41)
        self._create_thread(station31, station42)

        city6 = create_settlement(slug='city6')
        station5 = create_station(slug='station5')
        station6 = create_station(slug='station6', settlement=city6)
        station7 = create_station(slug='station7')
        self._create_thread(station5, station6)
        self._create_thread(station6, station7)

        station8 = create_station(slug='station8')
        station9 = create_station(slug='station9')
        self._create_thread(station8, station9)

        citya = create_settlement(slug='citya')
        cityb = create_settlement(slug='cityb')
        stationa = create_station(slug='stationa', settlement=citya)
        stationb1 = create_station(slug='stationb1', settlement=cityb)
        stationb2 = create_station(slug='stationb2', settlement=cityb)
        self._create_thread(stationa, stationb1)
        self._create_thread(stationa, stationb2)

        with mock_baris_response({'flights': []}):
            self._run_test([
                'https://rasp.yandex.ru/train/station1--station2',

                'https://rasp.yandex.ru/train/station31--station41',
                'https://rasp.yandex.ru/train/station32--station41',
                'https://rasp.yandex.ru/train/station31--station42',
                'https://rasp.yandex.ru/train/station31--city4',
                'https://rasp.yandex.ru/train/city3--station41',
                'https://rasp.yandex.ru/train/city3--city4',

                'https://rasp.yandex.ru/train/station5--station6',
                'https://rasp.yandex.ru/train/station6--station7',

                'https://rasp.yandex.ru/train/station8--station9',

                'https://rasp.yandex.ru/train/stationa--stationb1',
                'https://rasp.yandex.ru/train/stationa--stationb2',
                'https://rasp.yandex.ru/train/stationa--cityb',

                'https://rasp.yandex.ru/train/city1--city2',
                'https://rasp.yandex.ru/train/citya--cityb'
            ])

    def test_train_and_suburban_records(self):
        city1 = create_settlement(slug='city1')
        city2 = create_settlement(slug='city2')
        station11 = create_station(slug='station11', settlement=city1)
        station12 = create_station(slug='station12', settlement=city1)
        station21 = create_station(slug='station21', settlement=city2)
        station22 = create_station(slug='station22', settlement=city2)
        self._create_thread(station11, station21)
        self._create_thread(station11, station22)
        self._create_thread(station12, station21)
        self._create_thread(station12, station22)
        self._create_thread(station11, station21, TransportType.SUBURBAN_ID)

        city3 = create_settlement(slug='city3')
        city4 = create_settlement(slug='city4')
        city5 = create_settlement(slug='city5')
        city6 = create_settlement(slug='city6')

        station31 = create_station(slug='station31', settlement=city3)
        station32 = create_station(slug='station32', settlement=city3)
        station4 = create_station(slug='station4', settlement=city4)
        station5 = create_station(slug='station5', settlement=city5)
        station61 = create_station(slug='station61', settlement=city6)
        station62 = create_station(slug='station62', settlement=city6)

        self._create_thread(station31, station4)
        self._create_thread(station32, station4)
        self._create_thread(station4, station5)
        self._create_thread(station5, station61)
        self._create_thread(station5, station62)
        self._create_thread(station31, station4, TransportType.SUBURBAN_ID)
        self._create_thread(station32, station4, TransportType.SUBURBAN_ID)
        self._create_thread(station4, station5, TransportType.SUBURBAN_ID)
        self._create_thread(station5, station61, TransportType.SUBURBAN_ID)
        self._create_thread(station5, station62, TransportType.SUBURBAN_ID)

        with mock_baris_response({'flights': []}):
            self._run_test([
                'https://rasp.yandex.ru/train/station11--station21',
                'https://rasp.yandex.ru/train/station12--station21',
                'https://rasp.yandex.ru/train/station11--station22',
                'https://rasp.yandex.ru/train/station12--station22',
                'https://rasp.yandex.ru/train/station11--city2',
                'https://rasp.yandex.ru/train/station12--city2',
                'https://rasp.yandex.ru/train/city1--station21',
                'https://rasp.yandex.ru/train/city1--station22',
                'https://rasp.yandex.ru/train/city1--city2',
                'https://rasp.yandex.ru/suburban/station11--station21/today',
                'https://rasp.yandex.ru/all-transport/city1--city2',

                'https://rasp.yandex.ru/train/station31--station4',
                'https://rasp.yandex.ru/train/station32--station4',
                'https://rasp.yandex.ru/train/city3--station4',
                'https://rasp.yandex.ru/train/station4--station5',
                'https://rasp.yandex.ru/train/station5--station61',
                'https://rasp.yandex.ru/train/station5--station62',
                'https://rasp.yandex.ru/train/station5--city6',
                'https://rasp.yandex.ru/suburban/station31--station4/today',
                'https://rasp.yandex.ru/suburban/station32--station4/today',
                'https://rasp.yandex.ru/suburban/city3--station4/today',
                'https://rasp.yandex.ru/suburban/station4--station5/today',
                'https://rasp.yandex.ru/suburban/station5--station61/today',
                'https://rasp.yandex.ru/suburban/station5--station62/today',
                'https://rasp.yandex.ru/suburban/station5--city6/today',
                'https://rasp.yandex.ru/all-transport/city3--station4',
                'https://rasp.yandex.ru/all-transport/station4--station5',
                'https://rasp.yandex.ru/all-transport/station5--city6',

                'https://rasp.yandex.ru/train/city3--city4',
                'https://rasp.yandex.ru/train/city4--city5',
                'https://rasp.yandex.ru/train/city5--city6'
            ])

    def test_train_suburban_t_subtypes(self):
        city1 = create_settlement(slug='city1')
        city2 = create_settlement(slug='city2')
        station11 = create_station(slug='station11', settlement=city1)
        station21 = create_station(slug='station21', settlement=city2)
        station22 = create_station(slug='station22', settlement=city2)
        t_subtype = create_transport_subtype(
            t_type=TransportSubtype.SUBURBAN_ID,
            code='last',
            use_in_suburban_search=True
        )
        self._create_thread(station11, station21, t_subtype=t_subtype)
        self._create_thread(station11, station22)

        test_result = [
            'https://rasp.yandex.ru/train/station11--station21',
            'https://rasp.yandex.ru/suburban/station11--station21/today',

            'https://rasp.yandex.ru/train/station11--station22',
            'https://rasp.yandex.ru/train/station11--city2',
            'https://rasp.yandex.ru/all-transport/station11--city2',

            'https://rasp.yandex.ru/train/city1--city2'
        ]

        clear_cache_until_switch()
        with mock_baris_response({'flights': []}):
            self._run_test(test_result)

        self._create_thread(station11, station21, TransportType.SUBURBAN_ID)
        with mock_baris_response({'flights': []}):
            self._run_test(test_result)

    def test_train_city(self):
        city1 = create_settlement(slug='city1')
        city2 = create_settlement(slug='city2')
        station11 = create_station(slug='station11', settlement=city1)
        station12 = create_station(slug='station12', settlement=city1)
        station21 = create_station(slug='station21', settlement=city2)
        station22 = create_station(slug='station22', settlement=city2)

        self._create_thread(station11, station21)
        self._create_thread(station11, station22)

        with mock_baris_response({'flights': []}):
            self._run_test([
            'https://rasp.yandex.ru/train/station11--station21',
            'https://rasp.yandex.ru/train/station11--station22',

            'https://rasp.yandex.ru/train/station11--city2',

            'https://rasp.yandex.ru/train/city1--city2'
            ])

            # Проверяем, что не будет дубля city1--city2
            self._create_thread(station12, station22)
            self._run_test([
                'https://rasp.yandex.ru/train/station11--station21',
                'https://rasp.yandex.ru/train/station11--station22',
                'https://rasp.yandex.ru/train/station12--station22',

                'https://rasp.yandex.ru/train/city1--station22',
                'https://rasp.yandex.ru/train/station11--city2',

                'https://rasp.yandex.ru/train/city1--city2'
            ])

    def test_train_city_same_title(self):
        city1 = create_settlement(slug='city1')
        city2 = create_settlement(slug='city2')
        station11 = create_station(slug='station11', settlement=city1, title=city1.title)
        station21 = create_station(slug='station21', settlement=city2)
        station22 = create_station(slug='station22', settlement=city2)

        self._create_thread(station11, station21)

        with mock_baris_response({'flights': []}):
            self._run_test([
                'https://rasp.yandex.ru/train/station11--station21',

                'https://rasp.yandex.ru/train/city1--city2'
            ])

            station21.title = city2.title
            station21.save()
            self._run_test([
                'https://rasp.yandex.ru/train/station11--station21',
            ])

            self._create_thread(station11, station22)
            self._run_test([
                'https://rasp.yandex.ru/train/station11--station21',
                'https://rasp.yandex.ru/train/station11--station22',

                'https://rasp.yandex.ru/train/station11--city2',

                'https://rasp.yandex.ru/train/city1--city2'
            ])

    def test_all_types_records(self):
        city1 = create_settlement(slug='city1')
        city2 = create_settlement(slug='city2')
        create_station(id=101, slug='station1', settlement=city1, t_type=TransportType.PLANE_ID)
        create_station(id=201, slug='station2', settlement=city2, t_type=TransportType.PLANE_ID)

        city3 = create_settlement(slug='city3')
        city4 = create_settlement(slug='city4')
        station3 = create_station(slug='station3', settlement=city3, t_type=TransportType.TRAIN_ID)
        station4 = create_station(slug='station4', settlement=city4, t_type=TransportType.TRAIN_ID)
        self._create_thread(station3, station4)

        city5 = create_settlement(slug='city5')
        city6 = create_settlement(slug='city6')
        create_station(id=501, slug='station51', settlement=city5, t_type=TransportType.PLANE_ID)
        station52 = create_station(slug='station52', settlement=city5, t_type=TransportType.TRAIN_ID)
        create_station(id=601, slug='station61', settlement=city6, t_type=TransportType.PLANE_ID)
        station62 = create_station(slug='station62', settlement=city6, t_type=TransportType.TRAIN_ID)
        self._create_thread(station52, station62)

        with mock_baris_response({"flights": [
               {'departureStation': 101, 'arrivalStation': 201, 'flightsCount': 1, 'totalFlightsCount': 1},
               {'departureStation': 501, 'arrivalStation': 601, 'flightsCount': 1, 'totalFlightsCount': 1},
           ]}
        ):
            self._run_test([
                'https://rasp.yandex.ru/plane/station1--city2',
                'https://rasp.yandex.ru/plane/city1--station2',
                'https://rasp.yandex.ru/plane/city1--city2',

                'https://rasp.yandex.ru/train/station3--station4',

                'https://rasp.yandex.ru/plane/station51--city6',
                'https://rasp.yandex.ru/plane/city5--station61',
                'https://rasp.yandex.ru/plane/city5--city6',
                'https://rasp.yandex.ru/train/station52--station62',
                'https://rasp.yandex.ru/all-transport/city5--city6',

                'https://rasp.yandex.ru/train/city3--city4',
                'https://rasp.yandex.ru/train/city5--city6'
            ])

    def test_good_for_start_finish_records(self):
        city1 = create_settlement(slug='city1')
        city2 = create_settlement(slug='city2')
        city3 = create_settlement(slug='city3')
        city5 = create_settlement(slug='city5')
        station11 = create_station(slug='station11', settlement=city1, majority=1)
        station12 = create_station(slug='station12', settlement=city1, majority=2)
        station2 = create_station(slug='station2', settlement=city2)
        station31 = create_station(slug='station31', settlement=city3)
        station32 = create_station(slug='station32', settlement=city3)
        station4 = create_station(slug='station4')
        station51 = create_station(slug='station51', settlement=city5, majority=1)
        station52 = create_station(slug='station52', settlement=city5, majority=2)

        create_thread(__={'calculate_noderoute': True}, t_type=TransportType.TRAIN_ID,
            schedule_v1=[
                [None, 0, station11],
                [10, 11, station12],
                [20, 21, station2],
                [30, 31, station31],
                [40, 41, station4],
                [50, 51, station51],
                [60, None, station52]
            ]
        )
        create_thread(__={'calculate_noderoute': True}, t_type=TransportType.TRAIN_ID,
            schedule_v1=[
                [None, 0, station11],
                [10, 11, station12],
                [30, 32, station32],
                [50, 51, station51],
                [60, None, station52]
            ]
        )
        create_thread(__={'calculate_noderoute': True}, t_type=TransportType.SUBURBAN_ID,
            schedule_v1=[
                [None, 0, station11],
                [10, 11, station12],
                [30, 32, station32],
                [50, 51, station51],
                [60, None, station52]
            ]
        )
        for z in ZNodeRoute2.objects.filter(station_from_id=station12.id):
            z.good_for_start = False
            z.save()
        for z in ZNodeRoute2.objects.filter(station_to_id=station52.id):
            z.good_for_finish = False
            z.save()

        with mock_baris_response({'flights': []}):
            self._run_test([
                'https://rasp.yandex.ru/train/station11--station12',
                'https://rasp.yandex.ru/train/station11--station2',
                'https://rasp.yandex.ru/train/station11--station31',
                'https://rasp.yandex.ru/train/station11--station32',
                'https://rasp.yandex.ru/train/station11--station4',
                'https://rasp.yandex.ru/train/station11--station51',
                'https://rasp.yandex.ru/train/station11--station52',
                'https://rasp.yandex.ru/train/station12--station2',
                'https://rasp.yandex.ru/train/station12--station31',
                'https://rasp.yandex.ru/train/station12--station32',
                'https://rasp.yandex.ru/train/station12--station4',
                'https://rasp.yandex.ru/train/station12--station51',
                'https://rasp.yandex.ru/train/station12--station52',
                'https://rasp.yandex.ru/train/station2--station31',
                'https://rasp.yandex.ru/train/station2--station4',
                'https://rasp.yandex.ru/train/station2--station51',
                'https://rasp.yandex.ru/train/station2--station52',
                'https://rasp.yandex.ru/train/station31--station4',
                'https://rasp.yandex.ru/train/station31--station51',
                'https://rasp.yandex.ru/train/station31--station52',
                'https://rasp.yandex.ru/train/station32--station51',
                'https://rasp.yandex.ru/train/station32--station52',
                'https://rasp.yandex.ru/train/station4--station51',
                'https://rasp.yandex.ru/train/station4--station52',
                'https://rasp.yandex.ru/train/station51--station52',

                'https://rasp.yandex.ru/train/station11--city3',
                'https://rasp.yandex.ru/train/station12--city3',
                'https://rasp.yandex.ru/train/city3--station51',
                'https://rasp.yandex.ru/train/city3--station52',

                'https://rasp.yandex.ru/suburban/station11--station12/today',
                'https://rasp.yandex.ru/suburban/station11--station32/today',
                'https://rasp.yandex.ru/suburban/station11--station51/today',
                'https://rasp.yandex.ru/suburban/station11--station52/today',
                'https://rasp.yandex.ru/suburban/station12--station32/today',
                'https://rasp.yandex.ru/suburban/station12--station51/today',
                'https://rasp.yandex.ru/suburban/station12--station52/today',
                'https://rasp.yandex.ru/suburban/station32--station51/today',
                'https://rasp.yandex.ru/suburban/station32--station52/today',
                'https://rasp.yandex.ru/suburban/station51--station52/today',

                'https://rasp.yandex.ru/all-transport/station11--city3',
                'https://rasp.yandex.ru/all-transport/city3--station51',
                'https://rasp.yandex.ru/all-transport/station11--station51',

                'https://rasp.yandex.ru/train/city1--city2',
                'https://rasp.yandex.ru/train/city1--city3',
                'https://rasp.yandex.ru/train/city1--city5',
                'https://rasp.yandex.ru/train/city2--city3',
                'https://rasp.yandex.ru/train/city2--city5',
                'https://rasp.yandex.ru/train/city3--city5'
            ])

    def test_ignore_local_routes_records(self):
        """
        Карту сайта создаем только для маршрутов из города в себя же
        """
        from_station = create_station(slug='from_station', settlement=create_settlement(slug='from_city'))
        to_station = create_station(slug='to_station', settlement=from_station.settlement)

        self._create_thread(from_station, to_station, TransportType.BUS_ID)

        with mock_baris_response({'flights': []}):
            self._run_test([])
