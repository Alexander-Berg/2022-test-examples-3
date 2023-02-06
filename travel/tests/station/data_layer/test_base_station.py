# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from hamcrest import has_entries, assert_that, contains_inanyorder

from common.apps.info_center.models import Info
from common.models.factories import create_info
from travel.rasp.library.python.common23.tester.factories import create_external_direction, create_external_direction_marker
from common.models.geo import Country, StationType
from common.models.teasers import Teaser
from common.models.transport import TransportType
from common.tester.factories import (create_station, create_thread, create_settlement, create_way_to_airport,
                                     create_station_phone)

from travel.rasp.morda_backend.morda_backend.station.data_layer.page_type import get_station_by_id
from travel.rasp.morda_backend.morda_backend.station.data_layer.base_station import BaseStationForPage
from travel.rasp.morda_backend.morda_backend.station.data_layer.station import StationPageType


pytestmark = [pytest.mark.dbuser, pytest.mark.mongouser]
create_thread = create_thread.mutate(__={'calculate_noderoute': True})
create_station = create_station.mutate(country=Country.RUSSIA_ID)


def test_base_station_for_page():
    settlement = create_settlement(id=22, slug='city', title_ru='Город', title_ru_genitive='Города')
    station = create_station(
        id=401,
        t_type=TransportType.TRAIN_ID,
        station_type=StationType.TRAIN_STATION_ID,
        type_choices='train',
        title_ru='Станция',
        popular_title_ru_genitive='Станции',
        address_ru='Адрес',
        near_metro='Метро',
        longitude=66.6666,
        latitude=55.5555,
        settlement=settlement
    )

    station2 = create_station(t_type=TransportType.TRAIN_ID)
    create_thread(t_type=TransportType.TRAIN_ID, schedule_v1=[[None, 0, station], [100, None, station2]])

    create_station_phone(station=station, phone='+7 800 555 35 35')
    create_station_phone(station=station, phone='+7 800 555 36 36')

    station = get_station_by_id(401)
    page_type = StationPageType(station, None)
    st_for_page = BaseStationForPage(page_type)
    page_type = st_for_page.page_type

    assert page_type.page_type == 'train'
    assert page_type.subtypes == ['train']
    assert page_type.main_subtype == 'train'
    assert page_type.current_subtype == 'train'

    assert_that(st_for_page.station_properties, has_entries({
        'id': 401,
        'title': 'Станция',
        'title_genitive': 'Станции',
        'full_title': 'вокзал Станция',
        'full_title_genitive': 'вокзала Станция',
        'full_title_dative': 'вокзалу Станция',
        'has_popular_title': False,
        'address': 'Адрес',
        'subway': 'м. Метро',
        'longitude': 66.6666,
        'latitude': 55.5555,
        'phones': contains_inanyorder('+7 800 555 35 35', '+7 800 555 36 36'),
        'settlement': has_entries({
            'id': 22,
            'slug': 'city',
            'title': 'Город',
            'title_genitive': 'Города'
        })
    }))

    assert st_for_page.threads == []
    assert st_for_page.schedule_routes == []
    assert st_for_page.teasers == {}

    create_station(
        id=403, slug='station3',
        t_type=TransportType.TRAIN_ID,
        station_type=StationType.TRAIN_STATION_ID,
        popular_title_ru='Самый-самый вокзал',
        popular_title_ru_genitive='Самого-самого вокзала',
    )
    station = get_station_by_id(403)
    page_type = StationPageType(station, None)
    st_for_page = BaseStationForPage(page_type)

    assert_that(st_for_page.station_properties, has_entries({
        'full_title': 'Самый-самый вокзал',
        'full_title_genitive': 'Самого-самого вокзала',
        'full_title_dative': 'станции Самый-самый вокзал',
        'has_popular_title': True,
    }))

    create_station(
        id=404, slug='station4',
        t_type=TransportType.TRAIN_ID,
        station_type=StationType.TRAIN_STATION_ID,
        near_metro=None,
        popular_title_ru='Самый-самый вокзал',
        popular_title_ru_genitive='',
    )
    create_way_to_airport(
        station_id=404,
        title_ru='аэроэкспресс', way_type='aeroexpress',
        from_station_id=404, to_station_id=403
    )

    station = get_station_by_id(404)
    page_type = StationPageType(station, None)
    st_for_page = BaseStationForPage(page_type)

    assert_that(st_for_page.station_properties, has_entries({
        'subway': None,
        'address': '',
        'way_to_airport': has_entries({
            'way_type': 'aeroexpress',
            'link_title': 'аэроэкспресс',
            'from_point_key': 's404',
            'from_point_slug': 'station4',
            'to_point_key': 's403',
            'to_point_slug': 'station3',
        }),
        'full_title': 'Самый-самый вокзал',
        'full_title_genitive': 'станции Самый-самый вокзал',
        'full_title_dative': 'станции Самый-самый вокзал',
        'has_popular_title': True,
    }))
    assert 'settlement' not in st_for_page.station_properties


def _make_stations_for_teasers():
    station1 = create_station(
        id=501,
        t_type=TransportType.TRAIN_ID,
        type_choices='train,suburban,tablo'
    )

    station2 = create_station(
        id=502,
        t_type=TransportType.TRAIN_ID,
        type_choices='train,suburban,tablo'
    )
    ext_direction = create_external_direction()
    create_external_direction_marker(external_direction=ext_direction, station=station2)

    station3 = create_station(
        id=503,
        t_type=TransportType.TRAIN_ID,
        type_choices='train,suburban,tablo'
    )

    create_thread(
        t_type=TransportType.SUBURBAN_ID,
        schedule_v1=[[None, 0, station1], [50, 60, station2], [100, None, station3]]
    )

    return station1, ext_direction


@pytest.mark.parametrize('mode', [m[0] for m in Teaser.MODES])
def test_desktop_teasers(mode):
    station1, ext_direction = _make_stations_for_teasers()

    create_info(
        id=1001,
        title='Тизер по станции',
        text='Тизер по станции. Длинный текст',
        text_short=None,
        url='www.station.ru',
        info_type=mode,
        importance=10,
        services=[Info.Service.WEB],
        stations=[station1]
    )

    create_info(
        id=1002,
        title='Тизер по направлению',
        text='Тизер по направлению. Длинный текст',
        text_short=None,
        url='www.direction.ru',
        info_type=mode,
        importance=20,
        services=[Info.Service.WEB],
        external_directions=[ext_direction]
    )

    station = get_station_by_id(501)
    st_for_page = BaseStationForPage(StationPageType(station, None))
    st_for_page.make_teasers(False)

    assert len(st_for_page.teasers) == 1
    assert mode in st_for_page.teasers
    teaser = st_for_page.teasers[mode]

    assert teaser.id == 1001
    assert teaser.importance == 10
    assert teaser.title == 'Тизер по станции'
    assert teaser.content == 'Тизер по станции. Длинный текст'
    assert teaser.url == 'www.station.ru'
    assert teaser.mobile_content is None

    station = get_station_by_id(502)
    st_for_page = BaseStationForPage(StationPageType(station, None))
    st_for_page.make_teasers(False)

    assert len(st_for_page.teasers) == 1
    assert mode in st_for_page.teasers
    teaser = st_for_page.teasers[mode]

    assert teaser.id == 1002
    assert teaser.importance == 20
    assert teaser.title == 'Тизер по направлению'
    assert teaser.content == 'Тизер по направлению. Длинный текст'
    assert teaser.url == 'www.direction.ru'
    assert teaser.mobile_content is None

    station = get_station_by_id(503)
    st_for_page = BaseStationForPage(StationPageType(station, None))
    st_for_page.make_teasers(False)

    assert len(st_for_page.teasers) == 0


def test_mobile_teasers():
    station1, ext_direction = _make_stations_for_teasers()

    create_info(
        id=1001,
        title='Тизер по станции',
        text='Тизер по станции. Длинный текст',
        text_short='mobile_content',
        url='www.station.ru',
        info_type='ahtung',
        importance=10,
        services=[Info.Service.WEB],
        stations=[station1]
    )

    create_info(
        id=1011,
        text_short='mobile_content',
        info_type='normal',
        importance=100,
        services=[Info.Service.WEB],
        stations=[station1]
    )

    create_info(
        id=1002,
        title='Тизер по направлению',
        text='Тизер по направлению. Длинный текст',
        text_short='mobile_content',
        url='www.direction.ru',
        info_type='ahtung',
        importance=20,
        services=[Info.Service.WEB],
        external_directions=[ext_direction]
    )

    create_info(
        id=1012,
        text_short='mobile_content',
        info_type='special',
        importance=200,
        services=[Info.Service.WEB],
        external_directions=[ext_direction]
    )

    station = get_station_by_id(501)
    st_for_page = BaseStationForPage(StationPageType(station, None))
    st_for_page.make_teasers(True)

    assert len(st_for_page.teasers) == 1
    assert 'ahtung' in st_for_page.teasers
    teaser = st_for_page.teasers['ahtung']

    assert teaser.id == 1001
    assert teaser.importance == 10
    assert teaser.title == 'Тизер по станции'
    assert teaser.content == 'Тизер по станции. Длинный текст'
    assert teaser.url == 'www.station.ru'
    assert teaser.mobile_content == 'mobile_content'

    station = get_station_by_id(502)
    st_for_page = BaseStationForPage(StationPageType(station, None))
    st_for_page.make_teasers(True)

    assert len(st_for_page.teasers) == 1
    assert 'ahtung' in st_for_page.teasers
    teaser = st_for_page.teasers['ahtung']

    assert teaser.id == 1002
    assert teaser.importance == 20
    assert teaser.title == 'Тизер по направлению'
    assert teaser.content == 'Тизер по направлению. Длинный текст'
    assert teaser.url == 'www.direction.ru'
    assert teaser.mobile_content == 'mobile_content'

    station = get_station_by_id(503)
    st_for_page = BaseStationForPage(StationPageType(station, None))
    st_for_page.make_teasers(True)

    assert len(st_for_page.teasers) == 0
