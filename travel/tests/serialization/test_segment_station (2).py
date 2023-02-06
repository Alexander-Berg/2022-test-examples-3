# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from hamcrest import has_entry, assert_that

from common.models.geo import Settlement, Country
from common.tester.factories import create_station, create_settlement
from travel.rasp.train_api.serialization.segment_station import SegmentStationSchema


pytestmark = pytest.mark.dbuser


def test_segment_station_schema():
    station = create_station(id=20, title='Название', popular_title='Популярное название',
                             settlement=Settlement.MOSCOW_ID, country=Country.RUSSIA_ID)

    result = {
        'id': 20,
        'title': 'Название',
        'popularTitle': 'Популярное название',
        'settlementId': Settlement.MOSCOW_ID,
        'timezone': 'Europe/Moscow',
        'country': None
    }

    assert SegmentStationSchema().dump(station)[0] == result
    result['codes'] = {'express': '222'}
    assert SegmentStationSchema(context={'express_by_station_id_cache': {20: '222'}}).dump(station)[0] == result


def test_codes():
    station = create_station()
    assert_that(SegmentStationSchema(context={'express_by_station_id_cache': {station.id: '222'}}).dump(station)[0],
                has_entry('codes', {'express': '222'}))


@pytest.mark.parametrize('station_zone, settlement_zone, result', {
    ('Asia/Omsk', 'Europe/Oslo', 'Europe/Oslo'),
    ('Asia/Omsk', 'Asia/Omsk', 'Asia/Omsk'),
})
def test_timezone_dump(station_zone, settlement_zone, result):
    station = create_station(settlement=create_settlement(time_zone=settlement_zone), time_zone=station_zone)
    assert_that(SegmentStationSchema().dump(station)[0], has_entry('timezone', result))
