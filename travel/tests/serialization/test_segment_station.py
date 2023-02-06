# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from hamcrest import has_entry, assert_that

from common.models.geo import Settlement, Country
from common.tester.factories import create_station, create_settlement
from travel.rasp.morda_backend.morda_backend.serialization.segment_station import SegmentStationSchema


pytestmark = pytest.mark.dbuser


def test_segment_station_schema():
    station = create_station(id=20, title='Название', popular_title='Популярное название', t_type='bus',
                             type_choices='schedule', settlement=Settlement.MOSCOW_ID, country=Country.RUSSIA_ID)
    station.codes = {'express': '222'}

    result = {
        'id': 20,
        'title': 'Название',
        'popularTitle': 'Популярное название',
        'settlementId': Settlement.MOSCOW_ID,
        'timezone': 'Europe/Moscow',
        'pageType': 'bus',
        'mainSubtype': 'schedule',
        'country': {'code': 'RU', 'id': 225},
        'codes': {'express': '222'}
    }

    station_data = SegmentStationSchema().dump(station)[0]
    assert station_data == result


@pytest.mark.parametrize('station_zone, settlement_zone, result', {
    ('Asia/Omsk', 'Europe/Oslo', 'Europe/Oslo'),
    ('Asia/Omsk', 'Asia/Omsk', 'Asia/Omsk'),
})
def test_timezone_dump(station_zone, settlement_zone, result):
    station = create_station(settlement=create_settlement(time_zone=settlement_zone), time_zone=station_zone)
    assert_that(SegmentStationSchema().dump(station)[0], has_entry('timezone', result))
