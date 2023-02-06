# -*- coding: utf-8 -*-
import hashlib

import mock
import pytest

from mpfs.common.static import tags
from mpfs.common.util import from_json
from mpfs.core.filesystem.hardlinks.common import FileChecksums
from mpfs.core.global_gallery.logic.errors import UploadRecordNotFoundError
from test.base_suit import UserTestCaseMixin, UploadFileTestCaseMixin
from test.conftest import INIT_USER_IN_POSTGRES
from test.parallelly.api.disk.base import DiskApiTestCase


@pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='pg implementation only')
class SourceIdsHandlerTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'

    def setup_method(self, method):
        super(SourceIdsHandlerTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)

    def test_adding_source_id_proxying_ok(self):
        md5 = hashlib.md5().hexdigest()
        sha256 = hashlib.sha256().hexdigest()
        size = 111
        source_ids = ['some_source_id_1', 'some_source_id_2']
        data = {
            'md5': md5,
            'sha256': sha256,
            'size': size,
            'is_live_photo': False,
            'items': [{'source_id': x} for x in source_ids],
        }
        with self.specified_client(scopes=['cloud_api:disk.write']), \
                mock.patch('mpfs.core.global_gallery.logic.controller.GlobalGalleryController.add_source_ids_to_file', return_value=None) as endpoint_mock:
            self.client.request('PUT', 'disk/source-ids', data=data)
        assert endpoint_mock.call_args[0][1] == FileChecksums(md5, sha256, size).hid
        assert set(endpoint_mock.call_args[0][2]) == set(source_ids)
        assert endpoint_mock.call_args[1]['is_live_photo'] is False

    def test_adding_source_id_proxying_not_found(self):
        md5 = hashlib.md5().hexdigest()
        sha256 = hashlib.sha256().hexdigest()
        size = 111
        source_ids = ['some_source_id_1', 'some_source_id_2']
        data = {
            'md5': md5,
            'sha256': sha256,
            'size': size,
            'items': [{'source_id': x} for x in source_ids],
        }
        with self.specified_client(scopes=['cloud_api:disk.write']), \
                mock.patch('mpfs.core.global_gallery.logic.controller.GlobalGalleryController.add_source_ids_to_file', side_effect=UploadRecordNotFoundError()):
            r = self.client.request('PUT', 'disk/source-ids', data=data)
        assert r.status_code == 404

    def test_checking_source_id_proxing(self):
        with self.specified_client(scopes=['cloud_api:disk.read']), \
                mock.patch('mpfs.core.base.check_source_id', return_value=None):
            r = self.client.request('HEAD', 'disk/source-ids/check', query={'source_id': '111'})
        assert r.status_code == 200

    def test_checking_source_id_proxing_not_found(self):
        with self.specified_client(scopes=['cloud_api:disk.read']), \
                mock.patch('mpfs.core.base.check_source_id', side_effect=UploadRecordNotFoundError()):
            r = self.client.request('HEAD', 'disk/source-ids/check', query={'source_id': '111'})
        assert r.status_code == 404

    def test_bulk_checking_source_id_proxying(self):
        data = {
            'source_ids': [
                {'source_id': '1'},
                {'source_id': '2'},
            ]
        }

        self.upload_file(self.uid, '/disk/1.jpg', opts={'source_id': '1'})
        with self.specified_client(scopes=['cloud_api:disk.read']):
            r = self.client.request('POST', 'disk/source-ids/check', data=data)
        assert r.status_code == 200
        response = from_json(r.content)
        assert set(response.keys()) == {'items'}
        assert len(response['items']) == 2

        correct_answer = {
            '1': True,
            '2': False,
        }

        r = {}
        for x in response['items']:
            assert set(x.keys()) == {'source_id', 'found'}
            assert set(x['source_id'].keys()) == {'source_id'}
            r[x['source_id']['source_id']] = x['found']

        assert r == correct_answer

    def test_bulk_checking_source_id_returns_413_if_payload_is_to_large(self):
        data = {
            'source_ids': [
                {'source_id': '1'},
                {'source_id': '2'},
                {'source_id': '3'},
            ]
        }

        with self.specified_client(scopes=['cloud_api:disk.read']), \
                mock.patch('mpfs.platform.v1.disk.source_ids.handlers.GLOBAL_GALLERY_MAX_BULK_CHECK_SOURCE_IDS_SIZE', 2):
            r = self.client.request('POST', 'disk/source-ids/check', data=data)
        assert r.status_code == 413
