# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest

from common.models.transport import TransportType
from common.tester.factories import create_thread, create_station
from travel.rasp.library.python.common23.date import environment
from travel.rasp.morda_backend.morda_backend.station.data_layer.base_station import BaseStationForPage
from travel.rasp.morda_backend.morda_backend.station.data_layer.bus import BusStationForPage
from travel.rasp.morda_backend.morda_backend.station.data_layer.plane import PlaneStationForPage
from travel.rasp.morda_backend.morda_backend.station.data_layer.station import get_station
from travel.rasp.morda_backend.morda_backend.station.data_layer.suburban import SuburbanStationForPage
from travel.rasp.morda_backend.morda_backend.station.data_layer.tablo import TabloStationForPage
from travel.rasp.morda_backend.morda_backend.station.data_layer.train import TrainStationForPage
from travel.rasp.morda_backend.morda_backend.station.request_serialization import StationContext


pytestmark = [pytest.mark.dbuser]
create_thread = create_thread.mutate(__={'calculate_noderoute': True})


def _make_and_check_station_for_page(station_t_type, type_choices, subtype, station_for_page_class):
    station1 = create_station(t_type=station_t_type, type_choices=type_choices)
    station2 = create_station(t_type=station_t_type)
    create_thread(t_type=station_t_type, schedule_v1=[[None, 0, station1], [100, None, station2]])

    context = StationContext(station1.id, subtype)
    assert isinstance(get_station(context, environment.now_aware()), station_for_page_class)


def test_get_station_for_page_class():
    with mock.patch.object(BaseStationForPage, 'threads_smart_sort'):
        with mock.patch.object(TrainStationForPage, 'load_threads'):
            with mock.patch.object(SuburbanStationForPage, 'load_threads'):
                with mock.patch.object(TabloStationForPage, 'load_threads'):
                    _make_and_check_station_for_page(TransportType.TRAIN_ID, 'tablo,train', None, TrainStationForPage)
                    _make_and_check_station_for_page(TransportType.TRAIN_ID, 'tablo,train', 'train', TrainStationForPage)
                    _make_and_check_station_for_page(
                        TransportType.TRAIN_ID, 'tablo,suburban', None, SuburbanStationForPage
                    )
                    _make_and_check_station_for_page(
                        TransportType.TRAIN_ID, 'tablo,suburban', 'tablo', TabloStationForPage
                    )
                    _make_and_check_station_for_page(
                        TransportType.TRAIN_ID, 'tablo,train,suburban', 'suburban', SuburbanStationForPage
                    )
                    _make_and_check_station_for_page(
                        TransportType.TRAIN_ID, 'tablo,train,suburban', None, TrainStationForPage
                    )
                    _make_and_check_station_for_page(TransportType.TRAIN_ID, 'tablo,train,suburban', 'tablo', TabloStationForPage)
                    _make_and_check_station_for_page(TransportType.TRAIN_ID, '', None, TrainStationForPage)

        with mock.patch.object(PlaneStationForPage, 'load_threads'):
            _make_and_check_station_for_page(TransportType.PLANE_ID, 'tablo', None, PlaneStationForPage)
            _make_and_check_station_for_page(TransportType.PLANE_ID, 'tablo', 'plane', PlaneStationForPage)
            _make_and_check_station_for_page(TransportType.PLANE_ID, 'tablo,suburban', 'plane', PlaneStationForPage)
            _make_and_check_station_for_page(TransportType.PLANE_ID, 'tablo,suburban', 'suburban', PlaneStationForPage)
            _make_and_check_station_for_page(TransportType.PLANE_ID, '', None, PlaneStationForPage)

        with mock.patch.object(BusStationForPage, 'load_threads'):
            _make_and_check_station_for_page(TransportType.BUS_ID, 'schedule', None, BusStationForPage)
            _make_and_check_station_for_page(TransportType.BUS_ID, 'schedule', 'schedule', BusStationForPage)
            _make_and_check_station_for_page(TransportType.BUS_ID, '', None, BusStationForPage)
            _make_and_check_station_for_page(TransportType.WATER_ID, 'schedule', None, BusStationForPage)
            _make_and_check_station_for_page(TransportType.WATER_ID, 'schedule', 'schedule', BusStationForPage)
            _make_and_check_station_for_page(TransportType.WATER_ID, '', None, BusStationForPage)
