# -*- coding: utf-8 -*-
import mock
from nose_parameterized import parameterized

from test.parallelly.api.disk.base import DiskApiTestCase
from test.base_suit import UserTestCaseMixin
from mpfs.common.static import tags
from mpfs.common.util import from_json

REPORT_TYPE_CHOICES = (
    'download_file_404',
    'hash_conflict',
)


class ReportRestoreHandlerTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_version = 'v1'
    api_mode = tags.platform.INTERNAL

    def setup_method(self, method):
        super(ReportRestoreHandlerTestCase, self).setup_method(method)
        self.create_user(uid=self.uid, locale='ru')
        resp = self.client.put('disk/resources', query={'path': '/RestoreFile'}, uid=self.uid)
        self.assertEqual(resp.status_code, 201)
        resp = self.client.get('disk/resources', query={'path': '/RestoreFile'}, uid=self.uid)
        self.assertEqual(resp.status_code, 200)
        self.resource = from_json(resp.content)
        resp = self.client.get('disk/resources', query={'path': '/'}, uid=self.uid)
        self.assertEqual(resp.status_code, 200)
        self.disk_revision = from_json(resp.content)['revision']

    @parameterized.expand(REPORT_TYPE_CHOICES)
    def test_restore_report(self, report_type):
        with self.specified_client(scopes=['yadisk:all']):
            resp = self.client.post('disk/restore/report/%s' % report_type,
                                    query={
                                        'path': self.resource['path'],
                                        'resource_id': self.resource['resource_id'],
                                        'file_revision': self.resource['revision'],
                                        'disk_revision': self.disk_revision,
                                    })
            assert resp.status_code == 204

    @parameterized.expand(REPORT_TYPE_CHOICES)
    def test_restore_report_local_hashes(self, report_type):
        with self.specified_client(scopes=['yadisk:all']):
            resp = self.client.post('disk/restore/report/%s' % report_type,
                                    query={
                                        'path': self.resource['path'],
                                        'resource_id': self.resource['resource_id'],
                                        'file_revision': self.resource['revision'],
                                        'disk_revision': self.disk_revision,
                                        'local_md5': '00000000000000000000000000000000',
                                    })
            assert resp.status_code == 204

            resp = self.client.post(
                'disk/restore/report/%s' % report_type,
                query={
                    'path': self.resource['path'],
                    'resource_id': self.resource['resource_id'],
                    'file_revision': self.resource['revision'],
                    'disk_revision': self.disk_revision,
                    'local_sha256': '0000000000000000000000000000000000000000000000000000000000000000',
                })
            assert resp.status_code == 204

    @parameterized.expand(REPORT_TYPE_CHOICES)
    def test_permissions(self, report_type):
        query = {
            'path': self.resource['path'],
            'resource_id': self.resource['resource_id'],
            'file_revision': self.resource['revision'],
            'disk_revision': self.disk_revision
        }

        scopes_to_status = (
            ([], 403),
            (['cloud_api:disk.read'], 403),
            (['cloud_api:disk.app_folder'], 403),
            (['cloud_api:disk.app_folder', 'cloud_api:disk.read'], 403),
            (['yadisk:all'], 204),
        )
        with mock.patch(
                'mpfs.platform.permissions.BasePlatformPermission.is_legacy_internal_auth',
                return_value=False), \
             mock.patch(
                 'mpfs.platform.permissions.BasePlatformPermission.is_conductor_auth_fallback_mode',
                 return_value=False):
            self._permissions_test(scopes_to_status, 'POST', 'disk/restore/report/%s' % report_type, query=query)


class RestoreUploadHandlerTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_version = 'v1'
    api_mode = tags.platform.INTERNAL

    def test_common(self):
        with mock.patch('mpfs.core.base.restore_file', return_value={
            'upload_url': 'http://localhost/fake_upload_url',
            'oid': '123',
            'type': 'store',
            'at_version': 1234
        }) as mpfs_restore_stub:
            with self.specified_client(scopes=['yadisk:all'], uid=self.uid):
                resp = self.client.put('disk/restore/upload',
                                       query={'md5': 'fake_md5',
                                              'sha256': 'fake_sha256',
                                              'size': 321})
                assert resp.status == 200
                json_resp = from_json(resp.content)
                assert 'upload_link' in json_resp
                assert 'href' in json_resp['upload_link']
                assert 'method' in json_resp['upload_link']

                mpfs_restore_stub.assert_called_once()
                mpfs_req = mpfs_restore_stub.call_args[0][0]
                assert mpfs_req.uid == self.uid
                assert mpfs_req.md5 == 'fake_md5'
                assert mpfs_req.sha256 == 'fake_sha256'
                assert mpfs_req.size == 321
