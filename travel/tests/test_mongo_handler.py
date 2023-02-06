# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pymongo
import pytest
from calendar import timegm
from datetime import datetime
from hamcrest import assert_that, has_entries, starts_with

from travel.rasp.library.python.common23.db.mongo import database
from travel.rasp.library.python.common23.logging.mongo.mongo_handler import (
    MongoHandler, DangerousIndexesConfig
)
from travel.rasp.library.python.common23.tester.utils.logging import _create_log_record


def test_mongo_handler_format():
    utc_dt = datetime(2017, 10, 10, 10, 10, 10, 123456)
    handler = MongoHandler()
    log_record = _create_log_record()
    log_record.context = {'foo': object(), 'bar': 2}
    log_record.created = timegm(utc_dt.timetuple()) + utc_dt.microsecond / 1000000
    result = handler.format(log_record)
    assert_that(result, has_entries(
        message='',
        created_utc='2017-10-10T10:10:10.123456Z',
        context=has_entries(
            foo=starts_with('<object object'),
            bar='2'
        )
    ))


@pytest.mark.mongouser
class TestMongoHandler(object):
    def test_mongo_handler_emit(self):
        handler = MongoHandler()
        handler.emit(_create_log_record(msg='foo'))
        assert handler.collection.count() == 1
        assert_that(handler.collection.find_one(), has_entries(
            message='foo',
            levelname='DEBUG'
        ))

    _logging_config = {
        'version': 1,
        'handlers': {
            'mongo_handler_no_index': {
                'class': 'travel.rasp.library.python.common23.logging.mongo.mongo_handler.MongoHandler',
                'dbalias': 'default',
                'collection_name': 'tmp_log_collection_no_index',
                'level': 'DEBUG',
            },
            'mongo_handler': {
                'class': 'travel.rasp.library.python.common23.logging.mongo.mongo_handler.MongoHandler',
                'dbalias': 'default',
                'collection_name': 'tmp_log_collection_index',
                'level': 'DEBUG',
                'indexes': [pymongo.IndexModel('a', background=True),
                            pymongo.IndexModel([('b', pymongo.DESCENDING)], name='named_b', background=True)]
            }
        }
    }

    def teardown_method(self, method):
        database.tmp_log_collection_index.drop_indexes()
        database.tmp_log_collection_no_index.drop_indexes()

    def test_ensure_indexes_empty_collection(self):
        MongoHandler.ensure_indexes_from_config(self._logging_config)
        assert 'tmp_log_collection_no_index' not in database.list_collection_names()
        assert 'tmp_log_collection_index' in database.list_collection_names()
        assert len(database.tmp_log_collection_index.index_information()) == 3  # with object id index

    def test_ensure_indexes_on_existed_collection(self):
        database.tmp_log_collection_no_index.insert_one({'a': 1, 'b': 2})
        database.tmp_log_collection_index.insert_one({'a': 1, 'b': 2})
        assert len(database.tmp_log_collection_no_index.index_information()) == 1  # object id index
        assert len(database.tmp_log_collection_index.index_information()) == 1

        MongoHandler.ensure_indexes_from_config(self._logging_config)
        assert len(database.tmp_log_collection_no_index.index_information()) == 1
        assert len(database.tmp_log_collection_index.index_information()) == 3

    def test_drop_stale_indexes(self):
        database.tmp_log_collection_index.insert_one({'a': 1, 'b': 2})
        name_of_c_index = database.tmp_log_collection_index.create_index('c')
        database.tmp_log_collection_no_index.insert_one({'a': 1, 'b': 2})
        database.tmp_log_collection_no_index.create_index('c')
        assert len(database.tmp_log_collection_no_index.index_information()) == 2
        assert len(database.tmp_log_collection_index.index_information()) == 2
        assert name_of_c_index in database.tmp_log_collection_index.index_information()

        MongoHandler.ensure_indexes_from_config(self._logging_config)
        assert len(database.tmp_log_collection_no_index.index_information()) == 1
        assert len(database.tmp_log_collection_index.index_information()) == 3
        assert name_of_c_index not in database.tmp_log_collection_index.index_information()

    def test_no_background_index(self):
        with pytest.raises(DangerousIndexesConfig):
            MongoHandler.ensure_indexes_from_config({
                'version': 1,
                'handlers': {
                    'mongo_handler': {
                        'class': 'travel.rasp.library.python.common23.logging.mongo.mongo_handler.MongoHandler',
                        'dbalias': 'default',
                        'collection_name': 'tmp_log_collection_index',
                        'level': 'DEBUG',
                        'indexes': [pymongo.IndexModel('a')]
                    }
                }
            })
