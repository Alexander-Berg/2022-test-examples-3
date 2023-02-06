# -*- coding: utf-8 -*-
import mock

from mpfs.common.static import codes

from test.helpers.stubs.services import DjfsApiMockHelper
from test.parallelly.json_api.base import CommonJsonApiTestCase


class MkdirProxyTestCase(CommonJsonApiTestCase):
    def test_mkdir_json_ok(self):
        with mock.patch('mpfs.frontend.api.disk.JAVA_DJFS_API_PROXY_MKDIR_ENABLED', True), \
                DjfsApiMockHelper.mock_request():
            response = self.json_ok('mkdir', {'uid': 'not_relevant', 'path': 'not_relevant'})
            assert response == {}

    def test_mkdir_with_force_0_json_ok(self):
        with mock.patch('mpfs.frontend.api.disk.JAVA_DJFS_API_PROXY_MKDIR_ENABLED', True), \
                DjfsApiMockHelper.mock_request():
            response = self.json_ok('mkdir', {'uid': 'not_relevant', 'path': 'not_relevant', 'force': '0'})
            assert response == {}

    def test_mkdir_json_error(self):
        with mock.patch('mpfs.frontend.api.disk.JAVA_DJFS_API_PROXY_MKDIR_ENABLED', True), \
                DjfsApiMockHelper.mock_request(error_code=codes.MKDIR_PERMISSION_DENIED):
            self.json_error('mkdir', {'uid': 'not_relevant', 'path': 'not_relevant'}, code=codes.MKDIR_PERMISSION_DENIED)

    def test_mkdir_json_error_additional_error_data(self):
        with mock.patch('mpfs.frontend.api.disk.JAVA_DJFS_API_PROXY_MKDIR_ENABLED', True), \
                DjfsApiMockHelper.mock_request(error_code=codes.MKDIR_EXISTS, additional_error_data={'some': 'data'}):
            self.json_error('mkdir', {'uid': 'not_relevant', 'path': 'not_relevant'}, code=codes.MKDIR_EXISTS,
                            data={'some': 'data'})

    def test_mkdir_mail_error(self):
        self.mail_error('mkdir', {'uid': self.uid, 'path': '/disk/q'}, code=codes.METHOD_NOT_IMPLEMENTED)

    def test_mkdir_desktop_ok(self):
        with mock.patch('mpfs.frontend.api.disk.JAVA_DJFS_API_PROXY_MKDIR_ENABLED', True), \
                DjfsApiMockHelper.mock_request():
            response = self.desktop('mkdir', {'uid': 'not_relevant', 'path': 'not_relevant'})
            assert response is None

    def test_enabled_uids_setting(self):
        with mock.patch('mpfs.frontend.api.disk.JAVA_DJFS_API_PROXY_MKDIR_ENABLED', False), \
             mock.patch('mpfs.frontend.api.disk.JAVA_DJFS_API_PROXY_MKDIR_ENABLED_UIDS', ['14']), \
                DjfsApiMockHelper.mock_request(error_code=codes.MKDIR_EXISTS):
            # this request gets proxied and falls with mocked error
            self.json_error('mkdir', {'uid': '14', 'path': 'not_relevant'}, code=codes.MKDIR_EXISTS)
            # this request does not get proxied and falls with actual error
            self.json_error('mkdir', {'uid': '1', 'path': 'not_relevant'}, code=codes.PATH_ERROR)

    def test_mkdir_depth_limit_exceeded(self):
        with mock.patch('mpfs.frontend.api.disk.JAVA_DJFS_API_PROXY_MKDIR_ENABLED', True), \
             DjfsApiMockHelper.mock_request(error_code=codes.FOLDER_TOO_DEEP, status_code=403):
            self.json_error(
                'mkdir', {'uid': 'not_relevant', 'path': 'not_relevant'},
                code=codes.FOLDER_TOO_DEEP, title='Folder depth limit exceeded', status=403,
            )
