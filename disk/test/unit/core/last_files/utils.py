# -*- coding: utf-8 -*-
import datetime

from uuid import uuid4
from test.unit.base import NoDBTestCase

from mpfs.core.last_files.logic import SharedLastFilesProcessor
from mpfs.core.last_files.dao.cache import LastFilesCacheDAOItem


class LastFilesCacheDiffTest(NoDBTestCase):
    def assert_equal(self, a_item, b_item):
        assert a_item.id == b_item.id
        assert a_item.uid == b_item.uid
        assert a_item.owner_uid == b_item.owner_uid
        assert a_item.gid == b_item.gid
        assert a_item.creation_time == b_item.creation_time
        assert a_item.file_date_modified == b_item.file_date_modified
        assert a_item.file_id == b_item.file_id

    def setup_method(self, method):
        self.cache = [LastFilesCacheDAOItem(), LastFilesCacheDAOItem()]
        creation_time = datetime.datetime.now() - datetime.timedelta(minutes=1)
        self.gid = uuid4().hex
        self.uid = "1"
        for i, cache_item in enumerate(self.cache):
            cache_item.id = uuid4().hex
            cache_item.uid = self.uid
            cache_item.owner_uid = self.uid
            cache_item.gid = self.gid
            cache_item.creation_time = creation_time
            cache_item.file_date_modified = creation_time
            cache_item.file_id = str(i) * 64

    def test_diff_caches_for_gid_uid_all_new(self):
        items_to_insert, items_to_drop = SharedLastFilesProcessor.diff_caches_for_gid_uid(self.cache, [])
        assert len(items_to_insert) == len(self.cache)
        assert items_to_drop == []

    def test_diff_caches_for_gid_uid_nothing_new(self):
        items_to_insert, items_to_drop = SharedLastFilesProcessor.diff_caches_for_gid_uid([], self.cache)
        assert items_to_insert == []
        assert len(items_to_drop) == len(self.cache)

    def test_diff_caches_for_gid_uid_modified_recently(self):
        old = self.cache
        new = [i.copy() for i in self.cache]
        new[0].file_date_modified = datetime.datetime.now()
        items_to_insert, items_to_drop = SharedLastFilesProcessor.diff_caches_for_gid_uid(new, old)
        self.assert_equal(old[1], new[1])
        assert len(items_to_insert) == 1
        assert len(items_to_drop) == 1
        self.assert_equal(items_to_drop[0], old[0])
        self.assert_equal(items_to_insert[0], new[0])

    def test_diff_caches_for_gid_uid_modified_only_one(self):
        old = self.cache
        new = self.cache[0].copy()
        new.file_date_modified = datetime.datetime.now()
        items_to_insert, items_to_drop = SharedLastFilesProcessor.diff_caches_for_gid_uid([new], old)
        assert len(items_to_insert) == 1
        assert len(items_to_drop) == 2
        self.assert_equal(items_to_insert[0], new)
