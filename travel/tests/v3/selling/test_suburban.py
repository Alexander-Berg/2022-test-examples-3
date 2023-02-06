# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import pytest
from hamcrest import assert_that, has_properties, has_entries, contains_inanyorder

from common.models.schedule import Company
from common.models.tariffs import (
    AeroexTariff, TariffType, TariffTypeCode, SuburbanSellingFlow, SuburbanSellingBarcodePreset
)
from common.tester.factories import create_rthread_segment
from common.tester.utils.replace_setting import replace_setting
from common.utils.date import MSK_TZ

from travel.rasp.export.export.v3.selling.suburban import (
    get_suburban_selling_tariffs, get_tariff_key, add_suburban_selling_tariffs, TariffKey,
    _get_tariff_by_ids, SELLING_V3
)

from travel.rasp.export.tests.v3.factories import create_thread, create_station
from travel.rasp.export.tests.v3.selling.factories import set_suburban_selling_response


pytestmark = [pytest.mark.dbuser]


def _set_selling_response(httpretty):
    selling_tariffs = [
        {
            'provider': 'movista',
            'tariffs': [
                {
                    'id': 1, 'price': 56.12,
                    'valid_from': '2020-10-24T00:00:00+03:00',
                    'valid_until': '2020-10-25T03:00:00+03:00'
                },
                {
                    'id': 2, 'price': 55.12,
                    'valid_from': '2020-10-25T00:00:00+03:00',
                    'valid_until': '2020-10-26T03:00:00+03:00'
                }
            ]
        },
        {
            'provider': 'aeroexpress',
            'tariffs': [
                {
                    'id': 3, 'menu_id': 80,
                    'valid_from': '2020-10-23T00:00:00+03:00',
                    'valid_until': '2020-11-24T00:00:00+03:00'
                },
                {
                    'id': 4, 'menu_id': 82,
                    'valid_from': '2020-10-23T00:00:00+03:00',
                    'valid_until': '2020-11-24T00:00:00+03:00'
                }
            ]
        },
        {
            'provider': 'something',
            'tariffs': [
                {
                    'id': 5, 'price': 42.42,
                    'valid_from': '2020-10-23T00:00:00+03:00',
                    'valid_until': '2020-11-24T00:00:00+03:00'
                }
            ]
        },
    ]

    keys = [
        {
            'key': {
                'date': '2020-10-24',
                'station_from': 42,
                'station_to': 43,
                'company': Company.CPPK_ID,
                'tariff_type': 'etrain',
            },
            'provider': 'movista',
            'tariff_ids': [1]
        },
        {
            'key': {
                'date': '2020-10-25',
                'station_from': 42,
                'station_to': 43,
                'company': Company.CPPK_ID,
                'tariff_type': 'etrain',
            },
            'provider': 'movista',
            'tariff_ids': [2]
        },
        {
            'key': {
                'date': '2020-10-24',
                'station_from': 43,
                'station_to': 44,
                'company': Company.AEROEXPRESS_ID,
                'tariff_type': 'aeroexpress',
            },
            'provider': 'aeroexpress',
            'tariff_ids': [3, 4]
        },
        {
            'key': {
                'date': '2020-10-24',
                'station_from': 43,
                'station_to': 44,
                'company': 1234,
                'tariff_type': 'unknown',
            },
            'provider': 'unknown',
            'tariff_ids': [5]
        }
    ]

    set_suburban_selling_response(httpretty, selling_tariffs, keys)


def test_get_tariff_key():
    thread = create_thread(company=Company(id=Company.CPPK_ID))
    segment = create_rthread_segment(
        station_from=create_station(id=42),
        station_to=create_station(id=43),
        thread=thread,
        departure=MSK_TZ.localize(datetime(2020, 10, 24)),
    )

    tariff = AeroexTariff(type=TariffType.objects.get(code=TariffTypeCode.USUAL))
    segment.base_tariff = tariff

    segment_key = get_tariff_key(segment)

    assert isinstance(segment_key, TariffKey)
    assert_that(segment_key, has_properties(
        date='2020-10-24',
        station_from=42,
        station_to=43,
        tariff_type=TariffTypeCode.USUAL,
        company=Company.CPPK_ID
    ))

    segment.thread.company = Company(id=42)
    assert get_tariff_key(segment) is None

    segment.thread.company = Company(id=Company.CPPK_ID)
    delattr(segment, 'base_tariff')
    assert get_tariff_key(segment) is None


@replace_setting('SUBURBAN_SELLING_URL', 'https://sellingurl.net')
def test_get_suburban_selling_tariffs(httpretty):
    _set_selling_response(httpretty)

    key1 = TariffKey(
        date='2020-10-24',
        station_from=42,
        station_to=43,
        company=Company.CPPK_ID,
        tariff_type=TariffTypeCode.USUAL,
    )
    key2 = TariffKey(
        date='2020-10-25',
        station_from=42,
        station_to=43,
        company=Company.CPPK_ID,
        tariff_type=TariffTypeCode.USUAL,
    )
    key3 = TariffKey(
        date='2020-10-24',
        station_from=43,
        station_to=44,
        company=Company.AEROEXPRESS_ID,
        tariff_type=TariffTypeCode.AEROEXPRESS,
    )

    selling_tariffs, result_keys, selling_partners = get_suburban_selling_tariffs(
        tariff_keys=[key1, key2, key3],
        selling_flows=[SuburbanSellingFlow.AEROEXPRESS, SuburbanSellingFlow.VALIDATOR],
        selling_barcode_presets=[SuburbanSellingBarcodePreset.PDF417_CPPK]
    )

    assert_that(selling_tariffs, contains_inanyorder(
        has_entries({
            'provider': 'movista',
            'tariffs': [
                {
                    'id': 1, 'price': 56.12,
                    'valid_from': '2020-10-24T00:00:00+03:00',
                    'valid_until': '2020-10-25T03:00:00+03:00'
                },
                {
                    'id': 2, 'price': 55.12,
                    'valid_from': '2020-10-25T00:00:00+03:00',
                    'valid_until': '2020-10-26T03:00:00+03:00'
                }
            ]
        }),
        has_entries({
            'provider': 'aeroexpress',
            'tariffs': contains_inanyorder(
                {
                    'id': 3, 'menu_id': 80,
                    'valid_from': '2020-10-23T00:00:00+03:00',
                    'valid_until': '2020-11-24T00:00:00+03:00'
                },
                {
                    'id': 4, 'menu_id': 82,
                    'valid_from': '2020-10-23T00:00:00+03:00',
                    'valid_until': '2020-11-24T00:00:00+03:00'
                }
            )
        }),
        has_entries({
            'provider': 'something',
            'tariffs': contains_inanyorder(has_entries({'id': 5, 'price': 42.42}))
        })

    ))

    assert_that(result_keys, contains_inanyorder(
        has_entries({
            'tariff_ids': [1],
            'key': has_entries({'company': Company.CPPK_ID})
        }),
        has_entries({
            'tariff_ids': [2],
            'key': has_entries({'company': Company.CPPK_ID})
        }),
        has_entries({
            'tariff_ids': contains_inanyorder(3, 4),
            'key': has_entries({'company': Company.AEROEXPRESS_ID})
        }),
        has_entries({
            'tariff_ids': [5],
            'key': has_entries({'company': 1234})
        })
    ))

    assert_that(selling_partners, contains_inanyorder(
        has_entries({
            'provider': 'movista',
            'title': 'ЦППК'
        }),
        has_entries({
            'provider': 'aeroexpress',
            'title': 'Аэроэкспресс',
        })
    ))


def test_get_tariff_by_ids():
    selling_tariffs = [
        {
            'provider': 'movista',
            'tariffs': [
                {'id': 1, 'price': 10},
                {'id': 2, 'price': 20}
            ]
        },
        {
            'provider': 'aeroexpress',
            'tariffs': [
                {'id': 3, 'price': 30},
                {'id': 4, 'price': 40}
            ]
        }
    ]

    tariff_by_ids = _get_tariff_by_ids(selling_tariffs)

    assert_that(tariff_by_ids, has_entries({
        1: {'provider': 'movista', 'id': 1, 'price': 10},
        2: {'provider': 'movista', 'id': 2, 'price': 20},
        3: {'provider': 'aeroexpress', 'id': 3, 'price': 30},
        4: {'provider': 'aeroexpress', 'id': 4, 'price': 40},
    }))


def _create_map():
    station1 = create_station(id=42)
    station2 = create_station(id=43)
    station3 = create_station(id=44)

    thread0 = create_thread(company=Company(id=Company.CPPK_ID), number='100')
    segment0 = create_rthread_segment(
        thread=thread0, station_from=station1, station_to=station2,
        departure=MSK_TZ.localize(datetime(2020, 10, 24)),
    )
    segment0.base_tariff = AeroexTariff(type=TariffType.objects.get(code=TariffTypeCode.USUAL))

    thread1 = create_thread(company=Company(id=Company.CPPK_ID), number='101')
    segment1 = create_rthread_segment(
        thread=thread1, station_from=station1, station_to=station2,
        departure=MSK_TZ.localize(datetime(2020, 10, 25, 1, 0, 0)),
    )
    segment1.base_tariff = AeroexTariff(type=TariffType.objects.get(code=TariffTypeCode.USUAL))

    thread2 = create_thread(company=Company(id=Company.AEROEXPRESS_ID), number='102')
    segment2 = create_rthread_segment(
        thread=thread2, station_from=station2, station_to=station3,
        departure=MSK_TZ.localize(datetime(2020, 10, 24)),
    )
    segment2.base_tariff = AeroexTariff(type=TariffType.objects.get(code=TariffTypeCode.AEROEXPRESS))

    segment3 = create_rthread_segment(
        thread=thread2, station_from=station2, station_to=station3,
        departure=MSK_TZ.localize(datetime(2020, 10, 24)),
    )

    return [segment0, segment1, segment2, segment3]


@replace_setting('SUBURBAN_SELLING_URL', 'https://sellingurl.net')
def test_add_suburban_selling_tariffs(httpretty):
    _set_selling_response(httpretty)
    segments = _create_map()

    search_result = {}
    add_suburban_selling_tariffs(
        segments, segments, search_result, selling_version=SELLING_V3,
        selling_flows=[SuburbanSellingFlow.AEROEXPRESS, SuburbanSellingFlow.VALIDATOR]
    )

    assert_that(search_result, has_entries({
        'selling_partners': contains_inanyorder(
            has_entries({
                'code': 'cppk',
                'provider': 'movista',
                'ogrn': 111,
                'title': 'ЦППК',
                'address': 'дом',
                'work_time': 'всегда'
            }),
            has_entries({
                'code': 'aeroexpress',
                'provider': 'aeroexpress',
                'ogrn': 222,
                'title': 'Аэроэкспресс',
                'address': 'улица',
                'work_time': 'иногда'
            })
        )
    }))

    assert 'selling_tariffs' in search_result
    assert_that(search_result['selling_tariffs'], contains_inanyorder(
        has_entries({'provider': 'movista', 'id': 1, 'price': 56.12}),
        has_entries({'provider': 'movista', 'id': 2, 'price': 55.12}),
        has_entries({'provider': 'aeroexpress', 'id': 3, 'menu_id': 80}),
        has_entries({'provider': 'aeroexpress', 'id': 4, 'menu_id': 82}),
        has_entries({'provider': 'something', 'id': 5, 'price': 42.42}),
    ))

    assert segments[0].selling_tariffs_ids == [1]
    assert_that(segments[1].selling_tariffs_ids, contains_inanyorder(1, 2))
    assert_that(segments[2].selling_tariffs_ids, contains_inanyorder(3, 4))
    assert segments[3].selling_tariffs_ids == []
