# -*- coding: utf-8 -*-
import pytest
import mock

from test.base import DiskTestCase

import mpfs.engine.process
from mpfs.core.metastorage.control import disk
from mpfs.common.util import from_json
from mpfs.metastorage.mongo.collections.all_user_data import AllUserDataCollection, DBResultRecord
from test.conftest import REAL_MONGO

with open('fixtures/json/file.json') as fix_file:
    fixtures = from_json(fix_file.read())


class TestMetaStorage(DiskTestCase):
    @pytest.mark.skipif(True, reason="https://st.yandex-team.ru/CHEMODAN-33420")
    def test_all(self):
        disk.make_folder(fixtures['uid'], fixtures['key_dir_parent'], fixtures['dir_value'])

        response = disk.folder_content(fixtures['uid'], fixtures['key_dir_parent'])
        self.assertEqual(len(response.value), 0)

        disk.make_folder(fixtures['uid'], fixtures['key_dir_child'], fixtures['dir_value'])
        response = disk.folder_content(fixtures['uid'], fixtures['key_dir_parent'])
        self.assertEqual(len(response.value), 1)

        disk.put(fixtures['uid'], fixtures['ne_key'], fixtures['file_value'])

        response = disk.show(fixtures['uid'], fixtures['ne_key'])
        self.assertEqual(response.value.key, fixtures['ne_key'])
        self.assertEqual(response.value.data['size'], fixtures['file_value']['size'])

        result = disk.remove(fixtures['uid'], fixtures['ne_key'])
        self.assertNotEqual(result.value, None)
        result = disk.remove(fixtures['uid'], fixtures['key_dir_child'])
        self.assertNotEqual(result.value, None)

        disk.put(fixtures['uid'], fixtures['ne_key'], fixtures['file_value'])
        response = disk.show(fixtures['uid'], fixtures['ne_key'])
        self.assertNotEqual(response.value, None)
        self.assertNotEqual(response.value.data, {})
        disk.remove(fixtures['uid'], fixtures['ne_key'])
        mpfs.engine.process.usrctl().remove(fixtures['uid'])


class AllUserDataCollectionTestCase(DiskTestCase):
    PG_SHARDS = {'1', '2'}
    DATA_COLLECTIONS = {
        'user_data',
        'trash',
        'attach_data',
        'narod_data',
        'hidden_data',
        'misc_data',
        'photounlim_data',
        'notes_data',
        'additional_data',
        'client_data'
    }

    def get_all_shards(self):
        return self.PG_SHARDS

    def test_get_all_shards(self):
        res = AllUserDataCollection()._get_all_shards()
        assert isinstance(res, list)
        assert set(res) == self.get_all_shards()

    def test_get_uid_shard(self):
        res = AllUserDataCollection()._get_uid_shard(self.uid)
        assert isinstance(res, basestring)
        assert res in self.get_all_shards()

    def test_find(self):
        test_key = '/disk/test_dir'
        self.json_ok('mkdir', {'uid': self.uid, 'path': test_key})

        uids_keys = set()
        for db_result in AllUserDataCollection().find():
            assert isinstance(db_result, DBResultRecord)
            assert isinstance(db_result.record, dict)
            assert db_result.shard_name in self.get_all_shards()
            assert db_result.collection_name in self.DATA_COLLECTIONS
            doc = db_result.record
            uids_keys.add((doc['uid'], doc['key']))
        assert (self.uid, test_key) in uids_keys

        uids_keys = set()
        result = list(AllUserDataCollection().find({'uid': self.uid, 'key': test_key}))
        assert self.uid == result[0].record['uid']
        assert test_key == result[0].record['key']

    def test_count(self):
        for db_result in AllUserDataCollection().count():
            assert isinstance(db_result, DBResultRecord)
            assert isinstance(db_result.record, (int, long))
            assert db_result.shard_name in self.get_all_shards()
            assert db_result.collection_name in self.DATA_COLLECTIONS

        result = sum(r.record for r in AllUserDataCollection().count({'uid': self.uid}))
        assert result > 0

    def test_find_one(self):
        result = AllUserDataCollection().find_one({'uid': self.uid, 'key': '/'})
        assert isinstance(result, DBResultRecord)

    def test_find_one_on_uid_shard(self):
        result = AllUserDataCollection().find_one_on_uid_shard(self.uid, {'uid': self.uid, 'key': '/'})
        assert isinstance(result, DBResultRecord)
