# -*- coding: utf-8 -*-

import datetime
import time
import unittest

from nose_parameterized import parameterized

from mpfs.metastorage.mongo.util import compress_data
from mpfs.dao.base import BaseDAOItem, DAOItemField
from mpfs.dao.fields import DateTimeField, StringArrayField, FieldValidationError, DateTimeDeltaField
from mpfs.core.user.dao.user import UserDAOItem


class DAOItemsTestCase(unittest.TestCase):
    def test_dao_object(self):
        class TestDAOClass(BaseDAOItem):
            field1 = DAOItemField(mongo_path='field1', pg_path=None)
            field2 = DAOItemField(mongo_path='data.field2', pg_path=None)
            field3 = DAOItemField(mongo_path='zdata.field3', pg_path=None)
            field4 = DAOItemField(mongo_path='zdata.meta.field4', pg_path=None)

        instance = TestDAOClass.create_from_mongo_dict({
            'field1': 1,
            'data': {
                'field2': 'a',
            },
            'zdata': compress_data({
                'field3': 'abc',
                'meta': {
                    'field4': 777,
                }
            })
        })

        assert instance.field1 == 1
        assert instance.field2 == 'a'
        assert instance.field3 == 'abc'
        assert instance.field4 == 777

    def test_stid_filter_getter(self):
        class StidsListParser(object):
            def __init__(self, stid_type):
                self.stid_type = stid_type

            def parse(self, stids):
                """
                Костыль для доставания нужного стида из списка.
                """
                for s in stids:
                    if s['type'] == self.stid_type:
                        return s['stid']
                raise LookupError()

            def format(self, stid):
                """
                Костыль для формирования списка из стида.
                """
                return [{
                    'type': self.stid_type,
                    'stid': stid
                }]

        class TestDAOClass(BaseDAOItem):
            file_stid = DAOItemField(mongo_path='data.stids', mongo_item_parser=StidsListParser('file_mid'),
                                     pg_path=None)
            preview_stid = DAOItemField(mongo_path='data.stids', mongo_item_parser=StidsListParser('pmid'),
                                        pg_path=None)
            super_stid = DAOItemField(mongo_path='data.stids', mongo_item_parser=StidsListParser('not-existing-stid'),
                                      pg_path=None)

        instance = TestDAOClass.create_from_mongo_dict({
            'data': {
                'stids': [
                    {'stid': 'yadisk-stid-1', 'type': 'file_mid'},
                    {'stid': 'yadisk-stid-2', 'type': 'pmid'},
                ]
            },
        })

        assert instance.file_stid == 'yadisk-stid-1'
        assert instance.preview_stid == 'yadisk-stid-2'

        try:
            _ = instance.super_stid
        except LookupError:
            pass
        else:
            self.assertTrue(False, 'LookupException expected.')


class DAOItemFieldsTestCase(unittest.TestCase):
    def test_datetime_field(self):
        field = DateTimeField(mongo_path=None, pg_path=None)

        timestamp = int(time.time())
        date = field.from_mongo(timestamp)
        assert isinstance(date, datetime.datetime)

        assert field.to_mongo(date) == timestamp

    @parameterized.expand([
        (3600, 1),
        (-3600, -1),
    ])
    def test_datetime_delta_field(self, seconds, hours):
        field = DateTimeDeltaField(mongo_path=None, pg_path=None)

        timestamp = seconds
        date = field.from_mongo(timestamp)
        assert isinstance(date, datetime.timedelta)
        assert date == datetime.timedelta(hours=hours)

        assert field.to_mongo(date) == timestamp


class StringArrayFieldTestCase(unittest.TestCase):
    class TestDAOItem(BaseDAOItem):
        collections = StringArrayField(
            mongo_path='collections', pg_path=None)

    @staticmethod
    def test_basic():
        test_value = ['foo', 'bar']
        field = StringArrayField(mongo_path=None, pg_path=None)
        assert test_value == field.from_mongo(test_value)
        assert test_value == field.to_mongo(test_value)
        assert test_value == field.from_postgres(test_value)
        assert test_value == field.to_postgres(test_value)

    def test_wrong_value_type_failed(self):
        test_value = ('foo', 'bar')
        field = StringArrayField(mongo_path=None, pg_path=None)
        with self.assertRaises(FieldValidationError):
            test_value == field.from_mongo(test_value)


class UserDAOItemTestCase(unittest.TestCase):
    USER_DOCUMENT = {u'reg_time': 1486038065, u'locale': u'ru',
                     u'shard_key': 39991, u'version': 1486038066694991L,
                     u'collections': [u'user_data', u'disk_info', u'trash',
                                      u'hidden_data', u'link_data'],
                     u'_id': u'128280859', u'type': u'standart'}

    def test_create_from_mongo_dict(self):
        instance = UserDAOItem.create_from_mongo_dict(self.USER_DOCUMENT)

        assert instance.uid == self.USER_DOCUMENT[u'_id']
        assert instance.version == self.USER_DOCUMENT[u'version']
        assert instance.blocked is False
        assert instance.deleted is None
        assert instance.user_type is self.USER_DOCUMENT[u'type']
        assert isinstance(instance.reg_time, datetime.datetime)
        assert instance.locale is self.USER_DOCUMENT[u'locale']
        assert instance.shard_key is self.USER_DOCUMENT[u'shard_key']
        assert instance.b2b_key is None
        assert instance.collections == self.USER_DOCUMENT[u'collections']

    def test_convert_to_mongo_representation(self):
        instance = UserDAOItem.create_from_mongo_dict(self.USER_DOCUMENT)
        assert instance.get_mongo_representation() == self.USER_DOCUMENT

    def test_convert_to_postgres_representation(self):
        instance = UserDAOItem.create_from_mongo_dict(self.USER_DOCUMENT)
        not_raised = False
        try:
            instance.get_postgres_representation()
            not_raised = True
        except Exception:
            pass
        assert not_raised
