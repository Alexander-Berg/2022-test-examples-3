# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date

import pytest

from common.models.transport import TransportType
from common.models.geo import StationMajority, Station2Settlement, Country
from common.tester.factories import create_station, create_company, create_transport_model, create_settlement
from common.tester.utils.datetime import replace_now
from common.data_api.baris.helpers import (
    BarisData, get_plane_stations_ids, make_pseudo_uid_for_baris_flight, BarisPseudoUid, BarisMasksProcessor
)
from common.data_api.baris.service import BarisResponse


pytestmark = [pytest.mark.dbuser]
create_station = create_station.mutate(t_type=TransportType.PLANE_ID)


def test_baris_tablo_data():
    response = BarisResponse(None, set(), set(), set())
    data = BarisData(response)

    assert len(data.stations_by_ids) == 0
    assert len(data.companies_by_ids) == 0
    assert len(data.transport_models_by_ids) == 0

    create_station(id=101, t_type=TransportType.PLANE_ID)
    create_station(id=102, t_type=TransportType.PLANE_ID)
    create_company(id=301)
    create_company(id=302)
    create_transport_model(id=201)
    create_transport_model(id=202)

    response = BarisResponse(None, {101, 102}, {301, 302}, {201, 202})
    data = BarisData(response)

    assert len(data.stations_by_ids) == 2
    assert len(data.companies_by_ids) == 2
    assert 101 in data.stations_by_ids
    assert data.stations_by_ids[101].id == 101
    assert 102 in data.stations_by_ids
    assert data.stations_by_ids[102].id == 102
    assert 301 in data.companies_by_ids
    assert data.companies_by_ids[301].id == 301
    assert 302 in data.companies_by_ids
    assert data.companies_by_ids[302].id == 302
    assert 201 in data.transport_models_by_ids
    assert data.transport_models_by_ids[201].id == 201
    assert 202 in data.transport_models_by_ids
    assert data.transport_models_by_ids[202].id == 202


def test_get_plane_stations_ids():
    settlement = create_settlement(country=Country.RUSSIA_ID)
    station = create_station(
        id=101, settlement=settlement, majority=StationMajority.IN_TABLO_ID,
        t_type=TransportType.PLANE_ID, type_choices='tablo'
    )
    create_station(
        id=102, settlement=settlement, majority=StationMajority.NOT_IN_TABLO_ID,
        t_type=TransportType.PLANE_ID, type_choices='tablo'
    )
    station2 = create_station(
        id=103, majority=StationMajority.IN_TABLO_ID,
        t_type=TransportType.PLANE_ID, type_choices='tablo'
    )
    Station2Settlement.objects.create(station=station2, settlement=settlement)
    create_station(
        id=104, settlement=settlement, majority=StationMajority.IN_TABLO_ID,
        t_type=TransportType.TRAIN_ID, type_choices='train'
    )
    station3 = create_station(
        id=105, majority=StationMajority.IN_TABLO_ID,
        t_type=TransportType.TRAIN_ID, type_choices='train'
    )

    assert get_plane_stations_ids(station) == [101]
    assert get_plane_stations_ids(settlement) == [101, 103]
    assert get_plane_stations_ids(station3) == []


@replace_now('2020-04-01')
def test_baris_masks_processor():
    masks = [
        {
            'from': '2020-04-01',
            'until': '2020-07-01',
            'on': 12
        }
    ]
    flight = {
        'masks': masks,
        'departureTime': '01:30',
        'arrivalTime': '05:00',
        'arrivalDayShift': 1
    }
    processor = BarisMasksProcessor(masks, 'Etc/GMT-3')

    assert processor.get_days_text('ru') == 'пн, вт по 30.06'

    days = processor.get_run_days()['2020']
    assert days['4'] == [0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0]
    assert days['5'] == [0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0]
    assert days['6'] == [1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1]

    nearest_departure = processor.get_nearest_datetime(flight['departureTime'], timezone='Etc/GMT-3')
    assert nearest_departure.isoformat() == '2020-04-06T01:30:00+03:00'
    nearest_arrival = processor.get_nearest_datetime(flight['arrivalTime'], flight['arrivalDayShift'], 'Etc/GMT-5')
    assert nearest_arrival.isoformat() == '2020-04-07T05:00:00+05:00'

    assert processor.get_days_text('ru', days_shift=1) == 'вт, ср с 07.04 по 01.07'

    days = processor.get_run_days(days_shift=1)['2020']
    assert days['4'] == [0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0]
    assert days['5'] == [0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0]
    assert days['6'] == [0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1]
    assert days['7'] == [1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]

    masks = [
        {
            'from': '2020-04-09',
            'until': '2020-04-16',
            'on': 4
        },
        {
            'from': '2020-04-20',
            'until': '2020-06-30',
            'on': 1234567
        },
    ]
    processor = BarisMasksProcessor(masks, 'Etc/GMT-3')

    days_text = processor.get_days_text('ru')
    assert days_text == '9, 16, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30 апреля, 1, 2, 3, 4, 5, 6, 7 мая, …'

    days = processor.get_run_days()['2020']
    assert len(days) == 3
    assert days['4'] == [0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]
    assert days['5'] == [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]
    assert days['6'] == [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]

    nearest_departure = processor.get_nearest_datetime(flight['departureTime'], timezone='Etc/GMT-3')
    assert nearest_departure.isoformat() == '2020-04-09T01:30:00+03:00'
    nearest_arrival = processor.get_nearest_datetime(flight['arrivalTime'], flight['arrivalDayShift'], 'Etc/GMT-5')
    assert nearest_arrival.isoformat() == '2020-04-10T05:00:00+05:00'

    masks = [
        {
            'from': '2020-04-11',
            'until': '2020-06-15',
            'on': 6
        },
        {
            'from': '2020-06-29',
            'until': '2020-08-16',
            'on': 6
        }
    ]
    processor = BarisMasksProcessor(masks, 'Etc/GMT-3')

    days_text = processor.get_days_text('ru')
    assert days_text == 'сб с 11.04 по 15.08, кроме 20.06, 27.06'

    days = processor.get_run_days()['2020']
    assert len(days) == 5
    assert days['4'] == [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0]
    assert days['5'] == [0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0]
    assert days['6'] == [0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
    assert days['7'] == [0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0]
    assert days['8'] == [1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]

    nearest_departure = processor.get_nearest_datetime(flight['departureTime'], timezone='Etc/GMT-3')
    assert nearest_departure.isoformat() == '2020-04-11T01:30:00+03:00'
    nearest_arrival = processor.get_nearest_datetime(flight['arrivalTime'], flight['arrivalDayShift'], 'Etc/GMT-5')
    assert nearest_arrival.isoformat() == '2020-04-12T05:00:00+05:00'

    masks = [
        {
            'from': '2020-04-01',
            'until': '2020-06-30',
            'on': 1234567
        }
    ]
    processor = BarisMasksProcessor(masks, 'Etc/GMT-3')

    assert processor.get_days_text('ru') == 'ежедневно по 30.06'

    days = processor.get_run_days()['2020']
    assert len(days) == 3
    assert days['4'] == [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]
    assert days['5'] == [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]
    assert days['6'] == [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]

    nearest_departure = processor.get_nearest_datetime(flight['departureTime'], timezone='Etc/GMT-3')
    assert nearest_departure.isoformat() == '2020-04-01T01:30:00+03:00'
    nearest_arrival = processor.get_nearest_datetime(flight['arrivalTime'], flight['arrivalDayShift'], 'Etc/GMT-5')
    assert nearest_arrival.isoformat() == '2020-04-02T05:00:00+05:00'


def test_make_pseudo_uid_for_baris_flight():
    assert make_pseudo_uid_for_baris_flight('SU 123', 400, date(2020, 8, 18)) == 'SU-123_200818_c400_12'
    assert make_pseudo_uid_for_baris_flight('АЛ 321', 500, date(2020, 9, 19)) == 'RUSAL-321_200919_c500_12'
    assert make_pseudo_uid_for_baris_flight('ЮЙ 321Б', 500, date(2020, 10, 20)) == 'RUSQUJ-321B_201020_c500_12'
    assert make_pseudo_uid_for_baris_flight('ЯШ 321', 500, date(2020, 11, 21)) == 'RUSQAQH-321_201121_c500_12'


def test_baris_pseudo_uid():
    pseudo_uid = BarisPseudoUid('SU-1_200818_c1_12')
    assert pseudo_uid.is_baris_flight is True
    assert pseudo_uid.flight_number == 'SU-1'
    assert pseudo_uid.last_date == date(2020, 8, 18)

    pseudo_uid = BarisPseudoUid('US-1567_20200919_c11_12')
    assert pseudo_uid.is_baris_flight is True
    assert pseudo_uid.flight_number == 'US-1567'
    assert pseudo_uid.last_date == date(2020, 9, 19)

    pseudo_uid = BarisPseudoUid('RUSAB-111_201020_c111_12')
    assert pseudo_uid.is_baris_flight is True
    assert pseudo_uid.flight_number == 'АБ-111'
    assert pseudo_uid.last_date == date(2020, 10, 20)

    pseudo_uid = BarisPseudoUid('RUSAB-111A_201121_c112_12')
    assert pseudo_uid.is_baris_flight is True
    assert pseudo_uid.flight_number == 'АБ-111А'
    assert pseudo_uid.last_date == date(2020, 11, 21)

    pseudo_uid = BarisPseudoUid('RUSQAQZ-222_20201222_c113_12')
    assert pseudo_uid.is_baris_flight is True
    assert pseudo_uid.flight_number == 'ЯЖ-222'
    assert pseudo_uid.last_date == date(2020, 12, 22)

    pseudo_uid = BarisPseudoUid('SU-1_2_c1_12')
    assert pseudo_uid.is_baris_flight is False
    assert pseudo_uid.flight_number is None
    assert pseudo_uid.last_date is None

    pseudo_uid = BarisPseudoUid('155QI_11_2')
    assert pseudo_uid.is_baris_flight is False
    assert pseudo_uid.flight_number is None
    assert pseudo_uid.last_date is None

    pseudo_uid = BarisPseudoUid('7040_0_2000003_g20_4')
    assert pseudo_uid.is_baris_flight is False

    pseudo_uid = BarisPseudoUid('-1_1_c1_12')
    assert pseudo_uid.is_baris_flight is False

    pseudo_uid = BarisPseudoUid('RUS155QI_11_2')
    assert pseudo_uid.is_baris_flight is False
