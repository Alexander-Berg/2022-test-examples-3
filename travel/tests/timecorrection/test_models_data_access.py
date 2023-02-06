# -*- coding: utf-8 -*-

import mock
import pytest

from travel.rasp.admin.timecorrection.data_downloaders import MapsDataDownloader
from travel.rasp.admin.timecorrection.models import PathSpan, PathSpanDataCache, ThreadCalcDataCache, MapData
from travel.rasp.admin.timecorrection.models_data_access import (PathSpanProxy, PathSpanDataCacheProxy, ThreadCalcDataCacheProxy,
                                               PathSpanDataPreparer)

from tester.factories import create_station, create_country, create_rtstation, create_thread


class TestPathSpanProxy:
    @pytest.mark.dbuser
    def test_get_pathspan(self):
        """Проверка создания записи PathSpan"""
        some_country = create_country()
        station_a = create_station(longitude=78.5666, latitude=55.734071, country=some_country)
        station_b = create_station(longitude=37.592780, latitude=1111.56, country=some_country)

        pathtime_proxy = PathSpanProxy()

        with mock.patch.object(MapsDataDownloader, 'get_data', return_value=None):
            # проверка отсутсвия объекта
            assert not PathSpan.objects.filter(station_from=station_a, station_to=station_b).exists()

            # проверка создания объекта
            pathtime_proxy.get_pathspan(station_a, station_b)
            assert PathSpan.objects.filter(station_from=station_a, station_to=station_b).exists()

            # проверка на отсутствие дублирования
            pathtime_proxy.get_pathspan(station_a, station_b)
            assert 1 == PathSpan.objects.filter(station_from=station_a, station_to=station_b).count()


class TestPathSpanDataPreparer:
    @pytest.mark.parametrize('map_data, distance, duration', [
        [None, 21.76, 25.9],
        [MapData(distance=0.0, time=0.0), 21.76, 25.9],
        [MapData(distance=25, time=40), 25.0, 40.0],
        [MapData(distance=4623.76, time=500), 4623.76, 500],
    ])
    @pytest.mark.dbuser
    def test_call(self, map_data, distance, duration):
        """проверка метода __call__"""
        some_country = create_country()
        station_a = create_station(longitude=78.5666, latitude=55.734071, country=some_country)
        station_b = create_station(longitude=78.8777, latitude=55.75, country=some_country)
        assert distance, duration == PathSpanDataPreparer()(station_a, station_b, map_data=map_data)

    @pytest.mark.dbuser
    def test_distance_km_with_route_deviation(self):
        """Проверка расчета расстояния по координатам"""
        some_country = create_country()
        station_a = create_station(longitude=78.5666, latitude=55.734071, country=some_country)
        station_b = create_station(longitude=78.8777, latitude=55.75, country=some_country)
        assert 21.76 == round(PathSpanDataPreparer.distance_km_with_route_deviation(station_a, station_b), 2)


class TestPathSpanDataCacheProxy:
    @pytest.mark.dbuser
    def test_get_path_span_cache(self):
        """Проверка создания PathSpanDataCache"""
        thread = create_thread()
        some_country = create_country()
        station_a = create_station(longitude=78.5666, latitude=55.734071, country=some_country)
        station_b = create_station(longitude=37.592780, latitude=1111.56, country=some_country)
        path_span = PathSpan.objects.create(station_from=station_a, station_to=station_b,
                                            is_one_country_path=True, duration=100, distance=200)
        rts_a = create_rtstation(thread=thread, station=station_a, tz_arrival=None, tz_departure=0)
        rts_b = create_rtstation(thread=thread, station=station_b, tz_arrival=100, tz_departure=None)

        path_span_cache = PathSpanDataCacheProxy()
        with mock.patch.object(PathSpanProxy, 'get_pathspan', return_value=path_span):
            # проверка отсутсвия объекта
            assert not PathSpanDataCache.objects.filter(rtstation_from=rts_a, rtstation_to=rts_b).exists()

            # проверка создания объекта
            path_span_cache.get_path_span_cache(rts_a, rts_b)
            assert PathSpanDataCache.objects.filter(rtstation_from=rts_a, rtstation_to=rts_b).exists()

            # проверка на отсутствие дублирования
            path_span_cache.get_path_span_cache(rts_a, rts_b)
            assert 1 == PathSpanDataCache.objects.filter(rtstation_from=rts_a, rtstation_to=rts_b).count()


class TestThreadCalcDataCacheProxy:
    @pytest.mark.dbuser
    def test_get_thread_data_cache(self):
        """Проверка создания PathSpanDataCache"""
        thread = create_thread()
        thread_data_cache = ThreadCalcDataCacheProxy()
        with mock.patch.object(PathSpanDataCacheProxy, 'get_path_span_cache', return_value=None):
            # проверка отсутсвия объекта
            assert not ThreadCalcDataCache.objects.filter(thread=thread).exists()

            # проверка создания объекта
            thread_data_cache.get_thread_data_cache(thread)
            assert ThreadCalcDataCache.objects.filter(thread=thread).exists()

            # проверка на отсутствие дублирования
            thread_data_cache.get_thread_data_cache(thread)
            assert 1 == ThreadCalcDataCache.objects.filter(thread=thread).count()
