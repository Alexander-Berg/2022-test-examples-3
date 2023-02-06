# -*- coding: utf-8 -*-
import urlparse

import mock
import pytest

from test.common.sharing import SharingWithSearchTestCase
from test.base_suit import set_up_open_url, tear_down_open_url
from test.fixtures.users import user_4

import mpfs.engine.process

from mpfs.common.util import from_json, filetypes, trace_calls
from mpfs.core.address import Address, ResourceId
from mpfs.core.services.search_service import DiskSearch
from mpfs.core.services.index_service import SearchIndexer
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase


db = CollectionRoutedDatabase()


class SearchShareTestCase(SharingWithSearchTestCase):

    uid_4 = user_4.uid

    file_data = {
        "meta": {
            "file_mid": "1000003.yadisk:89031628.249690056312488962060095667221",
            "digest_mid": "1000005.yadisk:89031628.3983296384177350807526090116783",
            "md5": "83e5cd52e94e3a41054157a6e33226f7",
            "sha256": "4355a46b19d348dc2f57c046f8ef63d4538ebb936000f3c9ee954a27460dd865",
        },
        "size": 10000,
        "mimetype": "audio/mp3",
    }

    def setup_method(self, method):
        super(SearchShareTestCase, self).setup_method(method)

        for uid in (self.uid_3, self.uid, self.uid_1, self.uid_4):
            docs = []
            for doc_id in DiskSearch().get_all_documents_for_user(uid):
                docs.append({
                    'action': 'delete',
                    "file_id": doc_id,
                    "uid": uid,
                    'version': 999999999999999999,
                    'operation': 'rm',
                })
            SearchIndexer().push_change(docs)

        for uid in (self.uid_3, self.uid_1, self.uid_4):
            self.json_ok('user_init', {'uid': uid})

        self.make_dirs()

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_search_shared_folder(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {
            'uid': self.uid_3,
            'path': '/disk',
            'amount': 100,
            'query': 'folder'
        }
        result = self.json_ok('new_search', opts)['results']
        shared_found = False
        for resource in result:
            if resource['path'] == '/disk/folder2':
                shared_found = True
        self.assertTrue(shared_found)

    def test_reindex_search_owner(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {'uid': self.uid}
        xiva_requests = set_up_open_url()
        self.service('reindex_search', opts)
        tear_down_open_url()
        opts = {
            'uid': self.uid,
            'path': '/disk',
            'meta': '',
        }
        diff_result = self.desktop('diff', opts)
        all_owner_resource_ids = {ResourceId(self.uid, x['fid']).serialize() for x in diff_result['result']}

        indexed_resource_ids = set()
        for k, v in xiva_requests.iteritems():
            if not k.startswith(self.search_url):
                continue
            qs = urlparse.parse_qs(urlparse.urlparse(k).query)
            if qs['action'][0] != 'reindex' or qs['prefix'][0] != self.uid:
                continue
            indexed_resource_ids.add(qs['resource_id'][0])
        assert indexed_resource_ids == all_owner_resource_ids

    def test_reindex_search_user(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {'uid': self.uid_3}
        xiva_requests = set_up_open_url()
        self.service('reindex_search', opts)
        tear_down_open_url()
        opts = {
            'uid': self.uid_3,
            'path': '/disk',
            'meta': '',
        }
        diff_result = self.desktop('diff', opts)
        folder_fid = self.json_ok('info', {'uid': self.uid_3, 'path': '/disk/folder2', 'meta': 'file_id'})['meta']['file_id']
        all_guest_resource_ids = ({ResourceId(self.uid, x['fid']).serialize() for x in diff_result['result'] if x['key'] != '/disk/folder2'}
                                  | {ResourceId(self.uid_3, folder_fid).serialize()})

        indexed_resource_ids = set()
        for k, v in xiva_requests.iteritems():
            if not k.startswith(self.search_url):
                continue
            qs = urlparse.parse_qs(urlparse.urlparse(k).query)
            if qs['action'][0] != 'reindex' or qs['prefix'][0] != self.uid_3:
                continue
            indexed_resource_ids.add(qs['resource_id'][0])
        assert all_guest_resource_ids == indexed_resource_ids

    def test_reindex_search_owner_with_mediatype(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        self.upload_file(self.uid, '/disk/new_folder/folder2/audio_track.mp3', file_data=self.file_data)
        self.upload_file(self.uid, '/disk/new_folder/folder2/video.vid', media_type='video')
        video_resource_id = self.json_ok('info', {'uid': self.uid, 'path': '/disk/new_folder/folder2/video.vid','meta': 'resource_id'})['meta']['resource_id']
        audio_resource_id = self.json_ok('info', {'uid': self.uid, 'path': '/disk/new_folder/folder2/audio_track.mp3','meta': 'resource_id'})['meta']['resource_id']
        all_video_resource_ids = {video_resource_id}
        all_media_resource_ids = {video_resource_id, audio_resource_id}

        opts = {'uid': self.uid, 'mediatype': 'video'}
        xiva_requests = set_up_open_url()
        self.service('reindex_search', opts)
        tear_down_open_url()

        indexed_resource_ids = set()
        for k, v in xiva_requests.iteritems():
            if not k.startswith(self.search_url):
                continue
            qs = urlparse.parse_qs(urlparse.urlparse(k).query)
            if qs['action'][0] != 'reindex' or qs['prefix'][0] != self.uid:
                continue
            indexed_resource_ids.add(qs['resource_id'][0])
        assert all_video_resource_ids == indexed_resource_ids

        # теперь тоже самое, но сдвоенный медиатип
        opts = {'uid': self.uid, 'mediatype': 'video,audio'}
        xiva_requests = set_up_open_url()
        self.service('reindex_search', opts)
        tear_down_open_url()

        indexed_resource_ids = set()
        for k, v in xiva_requests.iteritems():
            if not k.startswith(self.search_url):
                continue
            qs = urlparse.parse_qs(urlparse.urlparse(k).query)
            if qs['action'][0] != 'reindex' or qs['prefix'][0] != self.uid:
                continue
            indexed_resource_ids.add(qs['resource_id'][0])
        assert all_media_resource_ids == indexed_resource_ids

    def test_reindex_search_user_with_mediatype(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        self.upload_file(self.uid, '/disk/new_folder/folder2/audio_track.mp3', file_data=self.file_data)
        self.upload_file(self.uid, '/disk/new_folder/folder2/video.vid', media_type='video')
        video_resource_id = self.json_ok('info', {'uid': self.uid, 'path': '/disk/new_folder/folder2/video.vid', 'meta': 'resource_id'})['meta']['resource_id']
        audio_resource_id = self.json_ok('info', {'uid': self.uid, 'path': '/disk/new_folder/folder2/audio_track.mp3', 'meta': 'resource_id'})['meta']['resource_id']
        all_video_resource_ids = {video_resource_id}
        all_media_resource_ids = {video_resource_id, audio_resource_id}

        opts = {'uid': self.uid_3, 'mediatype': 'video'}
        xiva_requests = set_up_open_url()
        self.service('reindex_search', opts)
        tear_down_open_url()

        indexed_resource_ids = set()
        for k, v in xiva_requests.iteritems():
            if not k.startswith(self.search_url):
                continue
            qs = urlparse.parse_qs(urlparse.urlparse(k).query)
            if qs['action'][0] != 'reindex' or qs['prefix'][0] != self.uid_3:
                continue
            indexed_resource_ids.add(qs['resource_id'][0])
        assert all_video_resource_ids == indexed_resource_ids

        # теперь тоже самое, но сдвоенный медиатип
        opts = {'uid': self.uid_3, 'mediatype': 'video,audio'}
        xiva_requests = set_up_open_url()
        self.service('reindex_search', opts)
        tear_down_open_url()

        indexed_resource_ids = set()
        for k, v in xiva_requests.iteritems():
            if not k.startswith(self.search_url):
                continue
            qs = urlparse.parse_qs(urlparse.urlparse(k).query)
            if qs['action'][0] != 'reindex' or qs['prefix'][0] != self.uid_3:
                continue
            indexed_resource_ids.add(qs['resource_id'][0])
        assert all_media_resource_ids == indexed_resource_ids

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-34034')
    def test_stub_lost_resources(self):
        # Avoid cyclic import
        from mpfs.core.bus import Bus

        for i in xrange(5):
            file_id = Address.Make(self.uid_4, '/disk/file{}'.format(i)).id
            Bus().mkfile(self.uid_4, file_id, data=self.file_data)

        opts = {
            'uid': self.uid_4,
            'path': Address.Make(self.uid_4, '/disk').id,
            'amount': 100,
            'query': 'file'
        }

        with trace_calls(DiskSearch, 'open_url') as tracer:
            response = self.json_ok('new_search', opts)
            search_response = tracer['return_value']
            assert 'lost_results_count' not in response

        for i in xrange(5):
            file_id = Address.Make(self.uid_4, '/disk/file{}'.format(i)).id
            Bus().rm(self.uid_4, file_id)

        with mock.patch.object(DiskSearch, 'open_url') as mocked_open_url:
            mocked_open_url.return_value = search_response
            response = self.json_ok('new_search', opts)
            assert 'lost_results_count' not in response

        opts['count_lost_results'] = 1
        with mock.patch.object(DiskSearch, 'open_url') as mocked_open_url:
            mocked_open_url.return_value = search_response
            response = self.json_ok('new_search', opts)
            assert response['lost_results_count'] == 5

        opts['count_lost_results'] = 0
        with mock.patch.object(DiskSearch, 'open_url') as mocked_open_url:
            mocked_open_url.return_value = search_response
            response = self.json_ok('new_search', opts)
            assert 'lost_results_count' not in response

        opts['count_lost_results'] = 1
        response = self.json_ok('new_search', opts)
        assert response['lost_results_count'] == 0
