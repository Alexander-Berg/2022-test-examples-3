# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from hamcrest import assert_that, has_entries, has_items

from common.tester.factories import create_settlement, create_station, create_thread
from common.models.transport import TransportType
from travel.rasp.trains.scripts.generate_canonical.generate_canonical import Runner


pytestmark = [pytest.mark.dbuser]


def create_train_thread(station_from, station_to):
    return create_thread(
        __={'calculate_noderoute': True},
        t_type=TransportType.TRAIN_ID,
        schedule_v1=[
            [None, 0, station_from],
            [10, None, station_to],
        ]
    )


def test_canonical():
    settlement_1 = create_settlement(slug='settlement_1', id=111, title_ru='sett_1')
    settlement_2 = create_settlement(slug='settlement_2', id=222)
    settlement_3 = create_settlement(slug='settlement_3', id=333)
    station_1 = create_station(slug='station_1', settlement=settlement_1, id=111, title_ru='st_1', popular_title_ru='pop_st_1')
    station_2 = create_station(slug='station_2', settlement=settlement_2, id=222)
    station_3_1 = create_station(slug='station_3_1', settlement=settlement_3, id=3331)
    station_3_2 = create_station(slug='station_3_2', settlement=settlement_3, id=3332, title_ru='st_3_2', popular_title_ru='pop_st_3_2')

    create_train_thread(station_1, station_2)
    create_train_thread(station_1, station_3_1)
    create_train_thread(station_1, station_3_2)

    runner = Runner('', '')
    result = runner.generate_canonical_by_search()

    assert_that(result, has_entries({
        ('c111', 's222'): ('c111', 'c222'),
        ('c111', 'c222'): ('c111', 'c222'),
        ('s111', 's222'): ('c111', 'c222'),
        ('s111', 'c222'): ('c111', 'c222'),

        ('s111', 's3332'): ('c111', 's3332'),
        ('s111', 's3331'): ('c111', 's3331'),
        ('c111', 's3331'): ('c111', 's3331'),
        ('c111', 's3332'): ('c111', 's3332'),
        ('s111', 'c333'): ('c111', 'c333'),
        ('c111', 'c333'): ('c111', 'c333')
    }))

    point_by_key = runner.get_point_by_key()
    prepared_result = runner.prepare_dicts(result, point_by_key)
    assert len(prepared_result) == len(result)
    assert_that(prepared_result, has_items(
        has_entries({
            'from_slug': 'station_1',
            'to_slug': 'station_3_2',
            'canonical_from_slug': 'settlement_1',
            'canonical_to_slug': 'station_3_2',
            'canonical_from_title': 'sett_1',
            'canonical_to_title': 'st_3_2',
            'canonical_from_popular_title': 'sett_1',
            'canonical_to_popular_title': 'pop_st_3_2'
        }),
        has_entries({
            'from_slug': 'station_1',
            'to_slug': 'settlement_2',
            'canonical_from_slug': 'settlement_1',
            'canonical_to_slug': 'settlement_2'
        })
    ))


def test_canonical_empty_settlement():
    settlement_1 = create_settlement(id=555)
    station_empty_1_1 = create_station(id=111)
    station_empty_1_2 = create_station(id=112)
    station_1 = create_station(settlement=settlement_1, id=555)

    settlement_2 = create_settlement(id=666)
    station_empty_2_1 = create_station(id=221)
    station_2 = create_station(settlement=settlement_2, id=666)

    create_train_thread(station_empty_1_1, station_1)
    create_train_thread(station_empty_1_2, station_1)
    create_train_thread(station_2, station_empty_2_1)

    runner = Runner('', '')
    result = runner.generate_canonical_by_search()

    assert_that(result, has_entries({
        ('s111', 's555'): ('s111', 'c555'),
        ('s111', 'c555'): ('s111', 'c555'),

        ('s112', 's555'): ('s112', 'c555'),
        ('s112', 'c555'): ('s112', 'c555'),

        ('s666', 's221'): ('c666', 's221'),
        ('c666', 's221'): ('c666', 's221'),
    }))


def test_canonical_fully_connected():
    settlement_1 = create_settlement(id=111)
    settlement_2 = create_settlement(id=222)
    station_1_1 = create_station(settlement=settlement_1, id=1111)
    station_1_2 = create_station(settlement=settlement_1, id=1112)
    station_2_1 = create_station(settlement=settlement_2, id=2221)
    station_2_2 = create_station(settlement=settlement_2, id=2222)

    create_train_thread(station_1_1, station_2_1)
    create_train_thread(station_1_2, station_2_2)

    runner = Runner('', '')
    result = runner.generate_canonical_by_search()
    assert_that(result, has_entries({
        ('c111', 'c222'): ('c111', 'c222'),

        ('s1111', 's2221'): ('s1111', 'c222'),
        ('s1111', 'c222'): ('s1111', 'c222'),
        ('c111', 's2221'): ('c111', 's2221'),

        ('s1112', 's2222'): ('s1112', 'c222'),
        ('s1112', 'c222'): ('s1112', 'c222'),
        ('c111', 's2222'): ('c111', 's2222'),
    }))

    create_train_thread(station_1_2, station_2_1)

    result = runner.generate_canonical_by_search()
    assert_that(result, has_entries({
        ('c111', 'c222'): ('c111', 'c222'),

        ('s1111', 's2221'): ('s1111', 'c222'),
        ('s1111', 'c222'): ('s1111', 'c222'),
        ('c111', 's2221'): ('c111', 's2221'),

        ('s1112', 's2222'): ('c111', 's2222'),
        ('s1112', 'c222'): ('s1112', 'c222'),
        ('c111', 's2222'): ('c111', 's2222'),

        ('s1112', 's2221'): ('s1112', 's2221')
    }))

    create_train_thread(station_1_1, station_2_2)

    result = runner.generate_canonical_by_search()
    assert_that(result, has_entries({
        ('c111', 'c222'): ('c111', 'c222'),

        ('s1111', 's2221'): ('s1111', 's2221'),
        ('s1111', 'c222'): ('s1111', 'c222'),
        ('c111', 's2221'): ('c111', 's2221'),

        ('s1112', 's2222'): ('s1112', 's2222'),
        ('s1112', 'c222'): ('s1112', 'c222'),
        ('c111', 's2222'): ('c111', 's2222'),

        ('s1112', 's2221'): ('s1112', 's2221'),

        ('s1111', 's2222'): ('s1111', 's2222')
    }))
