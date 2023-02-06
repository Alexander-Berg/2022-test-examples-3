# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date

import pytest
from hamcrest import assert_that, contains_inanyorder, has_entries

from common.data_api.platforms.client import PlatformsClient, get_dynamic_platform_collection
from common.data_api.platforms.serialization import PlatformData, PlatformKey, PlatformRecord
from common.tester.utils.replace_setting import replace_dynamic_setting


@pytest.mark.mongouser
def test_platforms_client_find():
    platforms_coll = get_dynamic_platform_collection()
    platforms_coll.insert_many([
        {
            'date': '2019-01-10',
            'station_id': 120,
            'train_number': '1000',
            'departure_platform': 'путь0'
        },
        {
            'date': '2019-01-11',
            'station_id': 121,
            'train_number': '1001',
            'departure_platform': 'путь1'
        },
        {
            'date': '2019-01-12',
            'station_id': 122,
            'train_number': '1002',
            'departure_platform': 'путь2',
            'arrival_platform': 'путь2а'
        },

    ])

    keys = [
        PlatformKey(date=date(2019, 1, 10), station_id=120, train_number='1000'),
        PlatformKey(date=date(2019, 1, 12), station_id=122, train_number='1002'),
        PlatformKey(date=date(2019, 1, 11), station_id=121, train_number='9999'),
    ]
    client = PlatformsClient(platforms_coll)
    result = client.find_platforms(keys)

    assert_that(result, has_entries({
        keys[0]: PlatformData(departure_platform='путь0'),
        keys[1]: PlatformData(departure_platform='путь2', arrival_platform='путь2а')
    }))

    with replace_dynamic_setting('DYNAMIC_PLATFORMS_ENABLED', False):
        result = client.find_platforms(keys)
        assert result == {}


@pytest.mark.mongouser
def test_platforms_client_update():
    platforms_coll = get_dynamic_platform_collection()
    client = PlatformsClient(platforms_coll)

    client.update([
        PlatformRecord(
            key=PlatformKey(date=date(2019, 1, 10), station_id=120, train_number='1000'),
            data=PlatformData(departure_platform='путь0')
        ),
        PlatformRecord(
            key=PlatformKey(date=date(2019, 1, 11), station_id=121, train_number='1001'),
            data=PlatformData(arrival_platform='путь1', departure_platform='путь1а')
        ),
    ])

    docs = [doc for doc in platforms_coll.find({})]
    assert_that(docs, contains_inanyorder(
        has_entries({
            'station_id': 120,
            'train_number': '1000',
            'date': '2019-01-10',
            'departure_platform': 'путь0',
        }),
        has_entries({
            'station_id': 121,
            'train_number': '1001',
            'date': '2019-01-11',
            'arrival_platform': 'путь1',
            'departure_platform': 'путь1а',
        }),
    ))
