# -*- coding: utf-8 -*-

"""
Набор тестов на хэндлеры, которые унаследованы от AsyncOperationHandler.
"""
import os
import urlparse

from test.parallelly.api.disk.base import DiskApiTestCase
from test.base_suit import UploadFileTestCaseMixin, UserTestCaseMixin
from mpfs.common.static import tags
from mpfs.common.util import from_json

from mpfs.platform.v1.disk.permissions import WebDavPermission, DiskWritePermission


def check_response_contains_correct_href_status_code(response, href_path, status_code):
    assert response.status_code == status_code
    data = from_json(response.content)
    assert 'href' in data
    href = data['href']
    parsed_href = urlparse.urlparse(href)
    assert parsed_href.path.startswith(href_path)


def check_response_contains_resource_href(response, status_code=202):
    check_response_contains_correct_href_status_code(response, '/v1/disk/resources', status_code)


def check_response_contains_async_operation_href(response, status_code=202):
    check_response_contains_correct_href_status_code(response, '/v1/disk/operations/', status_code)


class DeleteResourcesHandlerTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'DELETE'
    url = 'disk/resources'

    def setup_method(self, method):
        super(DeleteResourcesHandlerTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=True)

    def test_force_async_true_get_parameter(self):
        """
        Проверить что GET-параметр `force_async` принудительно сводит выполнение
        к асинхронной операции.
        """
        uid = self.uid
        self.json_ok('mkdir', {
            'path': '/disk/empty_folder',
            'uid': uid
        })
        with self.specified_client(scopes=WebDavPermission.scopes, uid=uid):
            response = self.client.request(
                self.method, self.url, query={
                    'path': 'empty_folder',
                    'force_async': 'true'
                }
            )
            check_response_contains_async_operation_href(response)

    def test_force_async_false_get_parameter(self):
        uid = self.uid
        self.json_ok('mkdir', {
            'path': '/disk/empty_folder',
            'uid': uid
        })
        with self.specified_client(scopes=WebDavPermission.scopes, uid=uid):
            response = self.client.request(
                self.method, self.url, query={
                    'path': 'empty_folder'
                }
            )
            assert response.status_code == 204


class CopyResourceHandlerTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'POST'
    url = 'disk/resources/copy'

    def setup_method(self, method):
        super(CopyResourceHandlerTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=True)

    def test_force_async_true_get_parameter(self):
        """
        Проверить что GET-параметр `force_async` принудительно сводит выполнение
        к асинхронной операции.
        """
        uid = self.uid
        self.json_ok('mkdir', {
            'path': '/disk/empty_folder_2',
            'uid': uid
        })
        with self.specified_client(scopes=WebDavPermission.scopes, uid=uid):
            response = self.client.request(
                self.method, self.url, query={
                    'from': 'empty_folder_2',
                    'path': 'copied_empty_folder_2',
                    'force_async': 'true'
                }
            )
            check_response_contains_async_operation_href(response)

    def test_force_async_false_get_parameter(self):
        uid = self.uid
        self.json_ok('mkdir', {
            'path': '/disk/empty_folder',
            'uid': uid
        })
        with self.specified_client(scopes=WebDavPermission.scopes, uid=uid):
            response = self.client.request(
                self.method, self.url, query={
                    'from': 'empty_folder',
                    'path': 'copied_empty_folder'
                }
            )
            assert response.status_code == 201


class MoveResourceHandlerTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'POST'
    url = 'disk/resources/move'

    def setup_method(self, method):
        super(MoveResourceHandlerTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=True)

    def test_force_async_true_get_parameter(self):
        """
        Проверить что GET-параметр `force_async` принудительно сводит выполнение
        к асинхронной операции.
        """
        uid = self.uid
        self.json_ok('mkdir', {
            'path': '/disk/empty_folder_2',
            'uid': uid
        })
        with self.specified_client(scopes=WebDavPermission.scopes, uid=uid):
            response = self.client.request(
                self.method, self.url, query={
                    'from': 'empty_folder_2',
                    'path': 'moved_empty_folder_2',
                    'force_async': 'true'
                }
            )
            check_response_contains_async_operation_href(response)

    def test_force_async_false_get_parameter(self):
        uid = self.uid
        self.json_ok('mkdir', {
            'path': '/disk/empty_folder',
            'uid': uid
        })
        with self.specified_client(scopes=WebDavPermission.scopes, uid=uid):
            response = self.client.request(
                self.method, self.url, query={
                    'from': 'empty_folder',
                    'path': 'moved_empty_folder'
                }
            )
            assert response.status_code == 201


class SaveToDiskPublicResourceHandlerTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'POST'
    url = 'disk/public/resources/save-to-disk'

    def setup_method(self, method):
        super(SaveToDiskPublicResourceHandlerTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=True)

    def test_force_async_true_get_parameter(self):
        """
        Проверить что GET-параметр `force_async` принудительно сводит выполнение
        к асинхронной операции.
        """
        uid = self.uid
        self.upload_file(uid, '/disk/test2.jpg')
        result = self.json_ok('set_public', {
            'path': '/disk/test2.jpg',
            'uid': uid
        })
        public_hash = result['hash']
        with self.specified_client(scopes=DiskWritePermission.scopes, uid=uid):
            response = self.client.request(
                self.method, self.url, query={
                    'public_key': public_hash,
                    'force_async': 'true'
                }
            )
            check_response_contains_async_operation_href(response)

    def test_force_async_false_get_parameter(self):
        uid = self.uid
        self.upload_file(uid, '/disk/test.jpg')
        result = self.json_ok('set_public', {
            'path': '/disk/test.jpg',
            'uid': uid
        })
        public_hash = result['hash']
        with self.specified_client(scopes=DiskWritePermission.scopes, uid=uid):
            response = self.client.request(
                self.method, self.url, query={
                    'public_key': public_hash,
                }
            )
            check_response_contains_resource_href(response, status_code=201)


class RestoreFromTrashHandlerTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'PUT'
    url = 'disk/trash/resources/restore'

    def setup_method(self, method):
        super(RestoreFromTrashHandlerTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=True)

    def test_force_async_true_get_parameter(self):
        """
        Проверить что GET-параметр `force_async` принудительно сводит выполнение
        к асинхронной операции.
        """
        uid = self.uid
        self.json_ok('mkdir', {
            'path': '/disk/empty_folder_2',
            'uid': uid
        })
        trash_path = self.json_ok('trash_append', {
            'path': '/disk/empty_folder_2',
            'uid': uid
        })['this']['id']
        with self.specified_client(scopes=WebDavPermission.scopes, uid=uid):
            response = self.client.request(
                self.method, self.url, query={
                    'path': os.path.basename(trash_path),
                    'name': 'restored_folder_2',
                    'force_async': 'true'
                }
            )
            check_response_contains_async_operation_href(response)

    def test_force_async_false_get_parameter(self):
        uid = self.uid
        self.json_ok('mkdir', {
            'path': '/disk/empty_folder',
            'uid': uid
        })
        trash_path = self.json_ok('trash_append', {
            'path': '/disk/empty_folder',
            'uid': uid
        })['this']['id']
        with self.specified_client(scopes=WebDavPermission.scopes, uid=uid):
            response = self.client.request(
                self.method, self.url, query={
                    'path': os.path.basename(trash_path),
                    'name': 'restored_folder'
                }
            )
            assert response.status_code == 201


class ClearTrashHandlerTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'DELETE'
    url = 'disk/trash/resources'

    def setup_method(self, method):
        super(ClearTrashHandlerTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=True)

    def test_force_async_true_get_parameter(self):
        """
        Проверить что GET-параметр `force_async` принудительно сводит выполнение
        к асинхронной операции.
        """
        uid = self.uid
        self.json_ok('mkdir', {
            'path': '/disk/empty_folder',
            'uid': uid
        })
        trash_path = self.json_ok('trash_append', {
            'path': '/disk/empty_folder',
            'uid': uid
        })['this']['id']
        with self.specified_client(scopes=WebDavPermission.scopes, uid=uid):
            response = self.client.request(
                self.method, self.url, query={
                    'path': os.path.basename(trash_path),
                    'force_async': 'true'
                }
            )
            check_response_contains_async_operation_href(response)

    def test_force_async_false_get_parameter(self):
        uid = self.uid
        self.json_ok('mkdir', {
            'path': '/disk/empty_folder',
            'uid': uid
        })
        trash_path = self.json_ok('trash_append', {
            'path': '/disk/empty_folder',
            'uid': uid
        })['this']['id']
        with self.specified_client(scopes=WebDavPermission.scopes, uid=uid):
            response = self.client.request(
                self.method, self.url, query={
                    'path': os.path.basename(trash_path),
                }
            )
            assert response.status_code == 204
