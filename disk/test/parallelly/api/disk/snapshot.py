# -*- coding: utf-8 -*-
import json

import mock
from hamcrest import (assert_that,
                      has_items,
                      has_entries,
                      is_not,
                      instance_of,
                      all_of,
                      has_entry,
                      has_key)
from mock import patch
from unittest import TestCase

from mpfs.common.errors import RequestsLimitExceeded429
from test.helpers.utils import disk_path_to_area_path
from test.parallelly.api.disk.base import DiskApiTestCase
from test.base_suit import UploadFileTestCaseMixin, UserTestCaseMixin
from test.parallelly.api.fixtures import snapshot_fixtures
from mpfs.common.static import tags
from mpfs.platform.v1.disk.handlers import PutSnapshotHandler
from mpfs.platform.v1.disk.serializers import (SnapshotIterationKeySerializer, SnapshotSerializer,
                                               SnapshotResourceSerializer)


class SnapshotIterationKeySerializerTestCase(TestCase):
    @staticmethod
    def test_basic():
        data = SnapshotIterationKeySerializer({'iteration_key': '123'}).data
        assert data
        assert 'iteration_key' in data

    @staticmethod
    def test_no_iteration_key():
        data = SnapshotIterationKeySerializer({}).data
        assert data == {}


class SnapshotSerializerTestCase(TestCase):
    def test_basic(self):
        required_fields = set()
        for visible_field in SnapshotResourceSerializer.visible_fields:
            if SnapshotResourceSerializer.fields[visible_field].required:
                required_fields.add(visible_field)

        data = SnapshotSerializer(snapshot_fixtures.SNAPSHOT).data
        assert data['revision']
        assert data['iteration_key']
        assert data['items']
        items = data['items']
        for item in items:
            assert_that(item.viewkeys(), has_items(*required_fields))
            # не должно быть полей, которых нет в списке видимых
            assert not item.viewkeys() - SnapshotResourceSerializer.visible_fields

        CORRECTLY_SERIALIZED_SNAPSHOT = {
            'items': [
                {
                    'md5': 'd41d8cd98f00b204e9800998ecf8427e',
                    'modified': '2015-12-22T12:01:11+00:00',
                    'path': 'disk:/path_99',
                    'resource_id': '4000756161:9f8c4e04d79caa888674eddd83fe0620b4289ca4b86700be158a9f28194bf25f',
                    'revision': 1478082183645717,
                    'sha256': 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
                    'size': 0,
                    'type': 'file'
                },
                {
                    'modified': '2015-12-22T15:20:11+00:00',
                    'path': 'disk:/dir',
                    'resource_id': '4000756161:03e8165f51dd4c6e68bbffc8edabd824f1b350caac5b042b65dc62afec98ed90',
                    'revision': 1478082143638469,
                    'type': 'dir'
                },
                {
                    'modified': '2015-05-05T02:23:31+00:00',
                    'path': 'disk:/shared_dir_rw',
                    'resource_id': '4000756161:03e8165f51dd4c6e68b1f2c8edabd824f1b350caac5b042b65dc62afec5fed90',
                    'revision': 1478082143238469,
                    'share': {'is_owned': False, 'rights': 'rw'},
                    'type': 'dir'
                },
                {
                    'modified': '2015-12-22T15:36:52+00:00',
                    'path': 'disk:/shared_dir_ro',
                    'resource_id': '4000756161:2ae8165551dd4c6e68b1f2c8edabd824f1b350caac5b042b65dc22afed5fed91',
                    'revision': 1478082143238469,
                    'share': {'is_owned': False, 'rights': 'r'},
                    'type': 'dir'
                },
                {
                    'modified': '2015-12-22T15:56:52+00:00',
                    'path': 'disk:/shared_dir_own',
                    'resource_id': '4000756161:63e8165f51dd4c6e68b1f268edabd824f1b350caac5b042b65dc62afec5fed66',
                    'revision': 1478092243842369,
                    'share': {'is_owned': True, 'rights': 'rw'},
                    'type': 'dir'
                },
                {
                    'modified': '2015-12-22T15:20:11+00:00',
                    'path': 'disk:/dir_with_symlink',
                    'resource_id': '4000756161:13e8165f51dd4c6e68bbffc8edabd824f1b350caac5b042b65dc62afec98ed11',
                    'revision': 1455082143638469,
                    'type': 'dir',
                    'discsw_symlink': '%25disk%25jntjq9ajpn7h65f3%25test508/12345',
                },
                {
                    'md5': 'd41d8cd98f00b204e9800998ecf8427e',
                    'modified': '2015-12-22T12:01:11+00:00',
                    'path': 'disk:/public_path_99',
                    'resource_id': '4000756161:8f8c4e04d79caa888674eddd83fe0620b4289ca4b86700be158a9f28194bf25f',
                    'revision': 1478082184645817,
                    'sha256': 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
                    'size': 0,
                    'type': 'file'
                }
            ],
            'iteration_key': '1478082429130762;055b2729c20b37394cb7d8eb15dbe7c2;;',
            'revision': 1478082429130762
        }
        self.assertEqual(data, CORRECTLY_SERIALIZED_SNAPSHOT)


class PutSnapshotTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'
    method = 'PUT'
    url = 'disk/snapshot'

    def setup_method(self, method):
        super(PutSnapshotTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)
        self.file_path = '/disk/test.txt'
        self.upload_file(self.uid, self.file_path)

    def test_basic(self):
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method,
                                       'disk/resources/snapshot',
                                       uid=self.uid)
            assert resp.status_code == 200
            assert json.loads(resp.content)

    def test_download_full_snapshot(self):
        import mpfs.core.snapshot.logic.snapshot  # цикл. импорт

        iterative_snapshot = []
        with patch.object(mpfs.core.snapshot.logic.snapshot, 'SNAPSHOT_CHUNK_SIZE', new=1):
            with self.specified_client(scopes=['cloud_api:disk.read']):
                resp = self.client.request(self.method, 'disk/resources/snapshot', uid=self.uid)
                assert resp.status_code == 200
                content = json.loads(resp.content)
                iteration_key = content['iteration_key']
                iterative_snapshot.extend(content['items'])
                while iteration_key:
                    resp = self.client.request(self.method, 'disk/resources/snapshot',
                                               uid=self.uid,
                                               data={'iteration_key': iteration_key})
                    assert resp.status_code == 200
                    content = json.loads(resp.content)
                    iteration_key = content['iteration_key']
                    iterative_snapshot.extend(content['items'])

        full_snapshot = []
        with patch.object(mpfs.core.snapshot.logic.snapshot, 'SNAPSHOT_CHUNK_SIZE', new=1000):
            resp = self.client.request(self.method, 'disk/resources/snapshot', uid=self.uid,
                                       data={'iteration_key': iteration_key})
            assert resp.status_code == 200
            content = json.loads(resp.content)
            full_snapshot.extend(content['items'])
        assert len(iterative_snapshot) == len(full_snapshot)
        assert sorted(iterative_snapshot) == sorted(full_snapshot)

    def test_500_on_bad_response_format_from_mpfs(self):
        """Протестировать что кидаем ошибку в случае отсутсвия полей в ответе МПФС.
        Чтобы не было ниакаих дефолтных значений.
        """

        fixtures = (
            snapshot_fixtures.NO_PATH_SNAPSHOT,
            snapshot_fixtures.NO_TYPE_SNAPSHOT,
            snapshot_fixtures.NO_MTIME_SNAPSHOT,
            snapshot_fixtures.NO_META_RESOURCE_ID_SNAPSHOT,
            snapshot_fixtures.NO_META_REVISION_SNAPSHOT,
        )
        for fixture in fixtures:
            with patch.object(PutSnapshotHandler, 'request_service', return_value=fixture):
                resp = self.client.request(self.method, 'disk/resources/snapshot', uid=self.uid, data={})
                assert resp.status_code == 500

    def test_public_field(self):
        public_dir = '/disk/public_dir'
        for dir_path in (public_dir,):
            self.json_ok('mkdir', opts={'uid': self.uid,
                                        'path': dir_path})
        file_in_public_dir = '%s/photo.jpg' % public_dir
        public_file = '/disk/public_photo.jpg'
        for file_path in (file_in_public_dir,
                          public_file):
            self.upload_file(self.uid, file_path)
        for public_path in (public_dir,
                            public_file):
            self.json_ok('set_public', opts={'uid': self.uid, 'path': public_path})
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method,
                                       'disk/resources/snapshot',
                                       uid=self.uid,
                                       query={'amount': 10})
            assert resp.status_code == 200
            content = json.loads(resp.content)
            assert_that(content['items'], has_items(has_entries(path=disk_path_to_area_path(public_dir),
                                                                public_url=instance_of(unicode)),
                                                    has_entries(path=disk_path_to_area_path(public_file),
                                                                public_url=instance_of(unicode)),
                                                    all_of(has_entry('path',
                                                                     disk_path_to_area_path(file_in_public_dir)),
                                                           is_not(has_key('public_url'))),
                                                    all_of(has_entry('path',
                                                                     disk_path_to_area_path(self.file_path)),
                                                           is_not(has_key('public_url')))))

    def test_sending_session_id_from_headers(self):
        session_id = '12435'
        headers = {'User-Agent': 'Yandex.Disk {"os":"win",'
                                 '"session_id":"%s"}' % session_id}
        with self.specified_client(scopes=['cloud_api:disk.read']), \
                mock.patch('mpfs.core.base.snapshot', return_value=None) as snapshot_mock:
            self.client.request(self.method,
                                'disk/resources/snapshot',
                                uid=self.uid,
                                headers=headers)
            assert snapshot_mock.call_args[0][0].session_id == session_id

    def test_429_on_too_many_requests(self):
        session_id = '12435'
        headers = {'User-Agent': 'Yandex.Disk {"os":"win",'
                                 '"session_id":"%s"}' % session_id}
        with self.specified_client(scopes=['cloud_api:disk.read']), \
                mock.patch('mpfs.core.base.snapshot', side_effect=RequestsLimitExceeded429()) as snapshot_mock:
            r = self.client.request(self.method,
                                    'disk/resources/snapshot',
                                    uid=self.uid,
                                    headers=headers)
            assert r.status_code == 429
