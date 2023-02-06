# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from common.models.transport import TransportType
from common.tester.factories import create_thread, create_station, create_country

from travel.rasp.morda_backend.morda_backend.station.data_layer.page_type import StationPageType, get_station_by_id


pytestmark = [pytest.mark.dbuser]
create_thread = create_thread.mutate(__={'calculate_noderoute': True})


def _check_page_type(station_id, page_sub_type, type, subtypes, main_subtype, current_subtype, t_type_code):
    station = get_station_by_id(station_id)
    page_type = StationPageType(station, page_sub_type)
    assert page_type.page_type == type
    assert page_type.subtypes == subtypes
    assert page_type.main_subtype == main_subtype
    assert page_type.current_subtype == current_subtype
    assert page_type.t_type_code == t_type_code


def test_station_page_type_no_choices():
    create_station(
        id=204, t_type=TransportType.TRAIN_ID, type_choices=''
    )
    _check_page_type(204, 'train', 'train', [], None, None, None)
    create_station(
        id=205, t_type=TransportType.PLANE_ID, type_choices=''
    )
    _check_page_type(205, 'plane', 'plane', [], None, None, None)


def _make_and_check_station(station_t_type, type_choices, page_subtype,
                            type, subtypes, main_subtype, current_subtype, t_type_code):
    station1 = create_station(t_type=station_t_type, type_choices=type_choices)
    station2 = create_station(t_type=station_t_type)
    if station_t_type != TransportType.PLANE_ID:
        create_thread(t_type=station_t_type, schedule_v1=[[None, 0, station1], [100, None, station2]])
    _check_page_type(station1.id, page_subtype, type, subtypes, main_subtype, current_subtype, t_type_code)


def test_station_page_type():
    _make_and_check_station(
        TransportType.TRAIN_ID, type_choices='train', page_subtype='train',
        type='train', subtypes=['train'], main_subtype='train', current_subtype='train', t_type_code='train'
    )
    _make_and_check_station(
        TransportType.TRAIN_ID, type_choices='train', page_subtype=None,
        type='train', subtypes=['train'], main_subtype='train', current_subtype='train', t_type_code='train'
    )
    _make_and_check_station(
        TransportType.TRAIN_ID, type_choices='train', page_subtype='suburban',
        type='train', subtypes=['train'], main_subtype='train', current_subtype=None, t_type_code=None
    )
    _make_and_check_station(
        TransportType.TRAIN_ID, type_choices='train', page_subtype='bus',
        type='train', subtypes=['train'], main_subtype='train', current_subtype=None, t_type_code=None
    )

    _make_and_check_station(
        TransportType.TRAIN_ID, type_choices='tablo,train,suburban', page_subtype='train',
        type='train', subtypes=['train', 'suburban', 'tablo'], main_subtype='train',
        current_subtype='train', t_type_code='train'
    )
    _make_and_check_station(
        TransportType.TRAIN_ID, type_choices='tablo,train,suburban', page_subtype='suburban',
        type='train', subtypes=['train', 'suburban', 'tablo'], main_subtype='train',
        current_subtype='suburban', t_type_code='suburban'
    )
    _make_and_check_station(
        TransportType.TRAIN_ID, type_choices='tablo,train,suburban', page_subtype=None,
        type='train', subtypes=['train', 'suburban', 'tablo'], main_subtype='train',
        current_subtype='train', t_type_code='train'
    )
    _make_and_check_station(
        TransportType.TRAIN_ID, type_choices='tablo,train,suburban', page_subtype='bus',
        type='train', subtypes=['train', 'suburban', 'tablo'], main_subtype='train',
        current_subtype=None, t_type_code=None
    )

    _make_and_check_station(
        TransportType.TRAIN_ID, type_choices='tablo,suburban', page_subtype='train',
        type='train', subtypes=['suburban'], main_subtype='suburban',
        current_subtype=None, t_type_code=None
    )
    _make_and_check_station(
        TransportType.TRAIN_ID, type_choices='tablo,suburban', page_subtype='suburban',
        type='train', subtypes=['suburban'], main_subtype='suburban',
        current_subtype='suburban', t_type_code='suburban'
    )
    _make_and_check_station(
        TransportType.TRAIN_ID, type_choices='tablo,suburban', page_subtype=None,
        type='train', subtypes=['suburban'], main_subtype='suburban',
        current_subtype='suburban', t_type_code='suburban'
    )
    _make_and_check_station(
        TransportType.TRAIN_ID, type_choices='tablo,suburban', page_subtype='bus',
        type='train', subtypes=['suburban'], main_subtype='suburban',
        current_subtype=None, t_type_code=None
    )

    _make_and_check_station(
        TransportType.PLANE_ID, type_choices='tablo', page_subtype='plane',
        type='plane', subtypes=['plane'], main_subtype='plane',
        current_subtype='plane', t_type_code='plane'
    )
    _make_and_check_station(
        TransportType.PLANE_ID, type_choices='tablo', page_subtype=None,
        type='plane', subtypes=['plane'], main_subtype='plane',
        current_subtype='plane', t_type_code='plane'
    )
    _make_and_check_station(
        TransportType.PLANE_ID, type_choices='tablo', page_subtype='bus',
        type='plane', subtypes=['plane'], main_subtype='plane',
        current_subtype=None, t_type_code=None
    )

    _make_and_check_station(
        TransportType.PLANE_ID, type_choices='tablo,suburban', page_subtype='plane',
        type='plane', subtypes=['plane'], main_subtype='plane',
        current_subtype='plane', t_type_code='plane'
    )
    _make_and_check_station(
        TransportType.PLANE_ID, type_choices='tablo,suburban', page_subtype=None,
        type='plane', subtypes=['plane'], main_subtype='plane',
        current_subtype='plane', t_type_code='plane'
    )
    _make_and_check_station(
        TransportType.PLANE_ID, type_choices='tablo,suburban', page_subtype='suburban',
        type='plane', subtypes=['plane'], main_subtype='plane',
        current_subtype=None, t_type_code=None
    )
    _make_and_check_station(
        TransportType.PLANE_ID, type_choices='tablo,suburban', page_subtype='bus',
        type='plane', subtypes=['plane'], main_subtype='plane',
        current_subtype=None, t_type_code=None
    )

    _make_and_check_station(
        TransportType.BUS_ID, type_choices='schedule', page_subtype='schedule',
        type='bus', subtypes=['schedule'], main_subtype='schedule',
        current_subtype='schedule', t_type_code='bus'
    )
    _make_and_check_station(
        TransportType.BUS_ID, type_choices='schedule', page_subtype=None,
        type='bus', subtypes=['schedule'], main_subtype='schedule',
        current_subtype='schedule', t_type_code='bus'
    )
    _make_and_check_station(
        TransportType.BUS_ID, type_choices='schedule', page_subtype='train',
        type='bus', subtypes=['schedule'], main_subtype='schedule',
        current_subtype=None, t_type_code=None
    )

    _make_and_check_station(
        TransportType.WATER_ID, type_choices='schedule', page_subtype='schedule',
        type='water', subtypes=['schedule'], main_subtype='schedule',
        current_subtype='schedule', t_type_code='water'
    )
    _make_and_check_station(
        TransportType.WATER_ID, type_choices='schedule', page_subtype=None,
        type='water', subtypes=['schedule'], main_subtype='schedule',
        current_subtype='schedule', t_type_code='water'
    )
    _make_and_check_station(
        TransportType.WATER_ID, type_choices='schedule', page_subtype='train',
        type='water', subtypes=['schedule'], main_subtype='schedule',
        current_subtype=None, t_type_code=None
    )
    _make_and_check_station(
        TransportType.WATER_ID, type_choices='schedule', page_subtype='bus',
        type='water', subtypes=['schedule'], main_subtype='schedule',
        current_subtype=None, t_type_code=None
    )


def test_foreign_station():
    country = create_country(id=6666)
    station_train = create_station(t_type=TransportType.TRAIN_ID, country=country)
    station = get_station_by_id(station_train.id)
    page_type = StationPageType(station, 'train')
    assert page_type.not_enough_info is True

    station_plane = create_station(t_type=TransportType.PLANE_ID, country=country)
    station = get_station_by_id(station_plane.id)
    page_type = StationPageType(station, 'plane')
    assert page_type.not_enough_info is False
