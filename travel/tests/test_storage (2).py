# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import pytest
from bson.objectid import ObjectId
from hamcrest import assert_that, has_entries, instance_of, contains

from travel.rasp.library.python.common23.dynamic_settings.core import NOTHING
from travel.rasp.library.python.common23.dynamic_settings.storages import MongoStorage, MongoStorageWithHistory
from travel.rasp.library.python.common23.tester.utils.datetime import replace_now


@pytest.mark.mongouser
def test_mongo_storage():
    storage = MongoStorage()

    assert storage.get('a') is NOTHING
    storage.set('a', 1)
    storage.set('b', 2)
    assert storage.get('a') == 1
    assert storage.get('b') == 2
    storage.set('a', 42)
    assert storage.get('a') == 42

    assert MongoStorage().get('a') == 42
    assert MongoStorage().get('b') == 2

    assert_that(storage.get_setting_info('a'), has_entries({
        '_id': instance_of(ObjectId),
        'value': 42,
        'name': 'a',
    }))


@pytest.mark.mongouser
def test_mongo_storage_with_history():
    storage = MongoStorageWithHistory()

    with replace_now('2018-11-02 13:15:01'):
        assert storage.get('a') is NOTHING
        storage.set('a', 1)
        storage.set('b', 2)
        assert storage.get('a') == 1
        assert storage.get('b') == 2

        assert_that(storage.get_setting_info('a'), has_entries({
            '_id': instance_of(ObjectId),
            'value': 1,
            'name': 'a',
            'history': contains(
                {
                    'dt': datetime(2018, 11, 2, 13, 15, 1),
                    'new_value': 1,
                    'context': {},
                }
            )
        }))

        assert_that(storage.get_setting_info('b'), has_entries({
            '_id': instance_of(ObjectId),
            'value': 2,
            'name': 'b',
            'history': contains(
                {
                    'dt': datetime(2018, 11, 2, 13, 15, 1),
                    'new_value': 2,
                    'context': {},
                }
            )
        }))

    with replace_now('2018-11-02 13:15:03'):
        storage.set_saving_context(user='admin')
        storage.set('a', 42)
        assert storage.get('a') == 42

        assert MongoStorage().get('a') == 42
        assert MongoStorage().get('b') == 2

        assert_that(storage.get_setting_info('a'), has_entries({
            '_id': instance_of(ObjectId),
            'value': 42,
            'name': 'a',
            'history': contains(
                {
                    'dt': datetime(2018, 11, 2, 13, 15, 1),
                    'new_value': 1,
                    'context': {},
                },
                {
                    'dt': datetime(2018, 11, 2, 13, 15, 3),
                    'new_value': 42,
                    'current_value': 1,
                    'context': {'user': 'admin'},
                },
            )
        }))

        assert_that(storage.get_setting_info('b'), has_entries({
            '_id': instance_of(ObjectId),
            'value': 2,
            'name': 'b',
            'history': contains(
                {
                    'dt': datetime(2018, 11, 2, 13, 15, 1),
                    'new_value': 2,
                    'context': {},
                }
            )
        }))
