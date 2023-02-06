# -*- coding: utf-8 -*-
from test.base_suit import UploadFileTestCaseMixin, UserTestCaseMixin
from test.helpers.stubs.services import DjfsApiMockHelper
from test.parallelly.api.disk.base import DiskApiTestCase
from mpfs.common.static import tags
from mpfs.common.util import from_json, to_json
from mpfs.platform.v1.notes.permissions import NotesReadPermission, NotesWritePermission


class NotesResourcesTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'disk/notes/%s'
    file_path = '/notes/test1.jpg'
    file_data = {'mimetype': 'image/jpg'}

    def setup_method(self, method):
        super(NotesResourcesTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)
        self.upload_file(self.uid, self.file_path, file_data=self.file_data)
        self.res_info = self.json_ok('info', {'uid': self.uid, 'path': self.file_path, 'meta': ''})

    def test_resource_info(self):
        resource_id = self.res_info['meta']['resource_id']

        with self.specified_client(scopes=NotesReadPermission.scopes + NotesWritePermission.scopes, uid=self.uid), \
             DjfsApiMockHelper.mock_request(status_code=200, content=to_json([self.res_info])):
            response = self.client.request('GET', self.url % resource_id)
            assert response.status_code == 200
            data = from_json(response.content)
            assert 'created' in data
            assert 'md5' in data
            assert 'media_type' in data and data['media_type'] == 'image'
            assert 'mime_type' in data and data['mime_type'] == 'image/jpg'
            assert 'modified' in data
            assert 'name' in data and data['name'] == 'test1.jpg'
            assert 'path' in data and data['path'] == 'notes:/test1.jpg'
            assert 'preview' in data
            assert 'revision' in data
            assert 'sha256' in data
            assert 'size' in data
            assert 'type' in data and data['type'] == 'file'
            assert 'antivirus_status' in data
            assert data['antivirus_status'] == 'clean'

    def test_permissions(self):
        response = self.json_ok('info', {'uid': self.uid, 'path': self.file_path, 'meta': ''})
        resource_id = response['meta']['resource_id']
        scopes_to_status = (
            ([], 403),
            (['cloud_api:disk.read'], 403),
            (['cloud_api:disk.write'], 403),
            (['cloud_api.notes:read', 'cloud_api.notes:write'], 200),
            (['yadisk:all'], 200),
        )
        with DjfsApiMockHelper.mock_request(status_code=200, content=to_json([self.res_info])):
            self._permissions_test(scopes_to_status, self.method, self.url % resource_id)
