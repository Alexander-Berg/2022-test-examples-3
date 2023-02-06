# -*- coding: utf-8 -*-
import datetime

import pytest
import mock
from test.base import DiskTestCase, time_machine
from mpfs.dao.session import Session
from test.helpers.stubs.services import SearchIndexerStub
from test.helpers.stubs.manager import StubsManager


class SearchFaceClusterizeTestCase(DiskTestCase):
    """
    При проставленном флаге user_index.faces_indexing_state = reindexed (то есть по умолчанию)
    мы в колбеках в поиск добавляем QS параметр clusterize_face=true
    """
    stubs_manager = StubsManager(class_stubs=set(StubsManager.DEFAULT_CLASS_STUBS) - {SearchIndexerStub})

    def set_faces_indexing_state(self, uid, state):
        session = Session.create_from_uid(uid)
        session.execute(
            "UPDATE disk.user_index SET faces_indexing_state = :state where uid = :uid;",
            uid=self.uid,
            state=state
        )

    def assert_has_flag(self, search_mock):
        assert search_mock.call_count >= 1
        for call in search_mock.call_args_list:
            args, kwargs = call
            search_call_url = args[0]
            assert '&clusterize_face=true' in search_call_url

    def test_store_has_clusterize_face(self):
        with mock.patch('mpfs.core.services.index_service.SearchIndexer.open_url') as open_url_mock:
            self.upload_file(self.uid, '/disk/1.jpg')
            assert open_url_mock.call_count == 1
            args, kwargs = open_url_mock.call_args
            search_call_url = args[0]
            assert '&clusterize_face=true' in search_call_url

    def test_store(self):
        with mock.patch('mpfs.core.services.index_service.SearchIndexer.open_url') as open_url_mock:
            self.upload_file(self.uid, '/disk/file.jpg')
            self.assert_has_flag(open_url_mock)

    def test_public_copy(self):
        with mock.patch('mpfs.core.services.index_service.SearchIndexer.open_url') as open_url_mock:
            self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/d1'})
            self.upload_file(self.uid, '/disk/d1/1.jpg')
            self.upload_file(self.uid, '/disk/d1/2.jpg')
            hsh = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/d1'})['hash']

        with mock.patch('mpfs.core.services.index_service.SearchIndexer.open_url') as open_url_mock:
            self.json_ok('async_public_copy', {'uid': self.uid, 'private_hash': hsh})
            self.assert_has_flag(open_url_mock)

    def test_trash_append(self):
        with mock.patch('mpfs.core.services.index_service.SearchIndexer.open_url') as open_url_mock:
            self.upload_file(self.uid, '/disk/1.jpg')

        with mock.patch('mpfs.core.services.index_service.SearchIndexer.open_url') as open_url_mock:
            self.json_ok('async_trash_append', {'uid': self.uid, 'path': '/disk/1.jpg'})
            self.assert_has_flag(open_url_mock)

    def test_trash_restore(self):
        with mock.patch('mpfs.core.services.index_service.SearchIndexer.open_url') as open_url_mock:
            self.upload_file(self.uid, '/disk/1.jpg')
            self.json_ok('async_trash_append', {'uid': self.uid, 'path': '/disk/1.jpg'})

        with mock.patch('mpfs.core.services.index_service.SearchIndexer.open_url') as open_url_mock:
            trash = self.json_ok('list', {'uid': self.uid, 'path': '/trash'})[1:]
            assert trash[0]['name'] == '1.jpg'
            self.json_ok('async_trash_restore', {'uid': self.uid, 'path': trash[0]['path']})
            self.assert_has_flag(open_url_mock)
