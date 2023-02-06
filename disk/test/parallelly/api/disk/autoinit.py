# -*- coding: utf-8 -*-
from test.parallelly.api.case.unlimited_autoupload import MOBILE_APP_ID
from test.parallelly.api.disk.base import DiskApiTestCase
from test.base_suit import UserTestCaseMixin
from mpfs.common.static import tags

class AutoUserInitExternalTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_version = 'v1'
    api_mode = tags.platform.EXTERNAL

    def setup_method(self, method):
        super(AutoUserInitExternalTestCase, self).setup_method(method)
        resp = self.json_ok('user_check', {'uid': self.uid})
        self.assertEqual(int(resp['need_init']), 1)

    def test_autoinit_on_set_unlimited_autoupload(self):
        with self.specified_client(id=MOBILE_APP_ID, uid=self.uid):
            resp = self.client.request('PUT', 'case/disk/unlimited-autoupload/set-state',
                                       data={'activate': True})
        self.assertEqual(resp.status_code, 200)


class AutoUserInitTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_version = 'v1'
    api_mode = tags.platform.INTERNAL

    def setup_method(self, method):
        super(AutoUserInitTestCase, self).setup_method(method)
        resp = self.json_ok('user_check', {'uid': self.uid})
        self.assertEqual(int(resp['need_init']), 1)

    def test_autoinit_error_on_get_disk_resources(self):
        some_unexistent_uid = '52304857203487520834750289745028'
        resp = self.client.get('disk/resources',
                               query={'path': '/'},
                               uid=some_unexistent_uid)
        self.assertEqual(resp.status_code, 401)

    def test_autoinit_on_get_disk_resources(self):
        resp = self.client.get('disk/resources', query={'path': '/'}, uid=self.uid)
        self.assertEqual(resp.status_code, 200)

    def test_autoinit_on_put_disk_resources(self):
        resp = self.client.put('disk/resources', query={'path': '/test_folder'}, uid=self.uid)
        self.assertEqual(resp.status_code, 201)

    def test_autoinit_on_delete_disk_resources(self):
        resp = self.client.delete('disk/resources', query={'path': '/test_folder'}, uid=self.uid)
        self.assertEqual(resp.status_code, 404)

    def test_autoinit_on_post_disk_resources_copy(self):
        resp = self.client.post('disk/resources/copy',
                                query={'path': '/test_folder', 'from': '/src_folder'},
                                uid=self.uid)
        self.assertEqual(resp.status_code, 404)

    def test_autoinit_on_post_disk_resources_move(self):
        resp = self.client.post('disk/resources/move',
                                query={'path': '/test_folder', 'from': '/src_folder'},
                                uid=self.uid)
        self.assertEqual(resp.status_code, 404)

    def test_autoinit_on_get_disk_resources_upload(self):
        resp = self.client.get('disk/resources/upload',
                               query={'path': '/test_file.ext'},
                               uid=self.uid)
        self.assertEqual(resp.status_code, 200)

    def test_autoinit_on_get_disk_resources_download(self):
        resp = self.client.get('disk/resources/download',
                               query={'path': '/test_file.ext'},
                               uid=self.uid)
        self.assertEqual(resp.status_code, 404)

    def test_autoinit_on_get_disk_operations(self):
        resp = self.client.get('disk/operations', query={'id': 'asdf'}, uid=self.uid)
        self.assertEqual(resp.status_code, 404)
