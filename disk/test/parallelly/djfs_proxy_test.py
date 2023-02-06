# -*- coding: utf-8 -*-
import mock

from test.base import DiskTestCase
from mpfs.common.static import codes
from test.helpers.stubs.services import DjfsApiMockHelper


class DiskProxyTestCase(DiskTestCase):

    def valid_response_type(self):
        return dict

    def assert_proxy_is_called(self, response):
        assert isinstance(response, dict) and not response  # if we proxy request to mock -> result is dict and empty

    def assert_proxy_is_not_called(self, response):
        assert isinstance(response, self.valid_response_type()) and response  # if we process request by mpfs -> result is valid type and not empty


class DiskProxyListTestCase(DiskProxyTestCase):

    def valid_response_type(self):
        return list


class DjfsBulkInfoProxyListTestCase(DiskProxyListTestCase):

    FILE_PATH = '/disk/file.txt'
    BULK_INFO_ENABLED = 'mpfs.frontend.api.disk.JAVA_DJFS_API_PROXY_BULK_INFO_ENABLED'

    def setup_method(self, method):
        super(DjfsBulkInfoProxyListTestCase, self).setup_method(method)

        self.upload_file(self.uid, self.FILE_PATH)
        file_info = self.json_ok('info', {'uid': self.uid, 'path': self.FILE_PATH, 'meta': 'resource_id'})
        self.RESOURCE_ID = file_info['meta']['resource_id']

    def run(self, result=None):
        with DjfsApiMockHelper.mock_request():
            super(DjfsBulkInfoProxyListTestCase, self).run(result)

    def test_bulk_info_proxy_ok(self):
        with mock.patch(self.BULK_INFO_ENABLED, True):
            response = self.json_ok('bulk_info', {'uid': self.uid}, json=[self.FILE_PATH])
            self.assert_proxy_is_called(response)

    def test_bulk_info_proxy_disabled(self):
        with mock.patch(self.BULK_INFO_ENABLED, False):
            response = self.json_ok('bulk_info', {'uid': self.uid}, json=[self.FILE_PATH])
            self.assert_proxy_is_not_called(response)

    def test_bulk_info_proxy_supported_qs_param(self):
        with mock.patch(self.BULK_INFO_ENABLED, True):
            response = self.json_ok('bulk_info', {'uid': self.uid, 'preview_crop': '1'}, json=[self.FILE_PATH])
            self.assert_proxy_is_called(response)

    def test_bulk_info_proxy_unsupported_qs_param(self):
        with mock.patch(self.BULK_INFO_ENABLED, True):
            response = self.json_ok('bulk_info', {'uid': self.uid, 'unsupported_param': '1'}, json=[self.FILE_PATH])
            self.assert_proxy_is_not_called(response)

    def test_bulk_info_proxy_supported_meta_param(self):
        with mock.patch(self.BULK_INFO_ENABLED, True):
            response = self.json_ok('bulk_info', {'uid': self.uid, 'meta': 'resource_id'}, json=[self.FILE_PATH])
            self.assert_proxy_is_called(response)

    def test_bulk_info_proxy_unsupported_meta_param(self):
        with mock.patch(self.BULK_INFO_ENABLED, True):
            response = self.json_ok('bulk_info', {'uid': self.uid, 'meta': 'something_strange'}, json=[self.FILE_PATH])
            self.assert_proxy_is_not_called(response)

    def test_bulk_info_proxy_all_meta_requested(self):
        with mock.patch(self.BULK_INFO_ENABLED, True):
            response = self.json_ok('bulk_info', {'uid': self.uid, 'meta': ''}, json=[self.FILE_PATH])
            self.assert_proxy_is_not_called(response)

    def test_bulk_info_by_resource_ids_proxy_ok(self):
        with mock.patch(self.BULK_INFO_ENABLED, True):
            response = self.json_ok('bulk_info_by_resource_ids',
                                    {'uid': self.uid}, json=[self.RESOURCE_ID])
            self.assert_proxy_is_called(response)

    def test_bulk_info_by_resource_ids_proxy_disabled(self):
        with mock.patch(self.BULK_INFO_ENABLED, False):
            response = self.json_ok('bulk_info_by_resource_ids',
                                    {'uid': self.uid}, json=[self.RESOURCE_ID])
            self.assert_proxy_is_not_called(response)

    def test_bulk_info_by_resource_ids_proxy_supported_qs_param(self):
        with mock.patch(self.BULK_INFO_ENABLED, True):
            response = self.json_ok('bulk_info_by_resource_ids',
                                    {'uid': self.uid, 'enable_service_ids': '/disk'}, json=[self.RESOURCE_ID])
            self.assert_proxy_is_called(response)

    def test_bulk_info_by_resource_ids_proxy_unsupported_qs_param(self):
        with mock.patch(self.BULK_INFO_ENABLED, True):
            response = self.json_ok('bulk_info_by_resource_ids',
                                    {'uid': self.uid, 'unsupported_param': '1'}, json=[self.RESOURCE_ID])
            self.assert_proxy_is_not_called(response)

    def test_bulk_info_by_resource_ids_proxy_supported_meta_param(self):
        with mock.patch(self.BULK_INFO_ENABLED, True):
            response = self.json_ok('bulk_info_by_resource_ids',
                                    {'uid': self.uid, 'meta': 'resource_id'}, json=[self.RESOURCE_ID])
            self.assert_proxy_is_called(response)

    def test_bulk_info_by_resource_ids_proxy_unsupported_meta_param(self):
        with mock.patch(self.BULK_INFO_ENABLED, True):
            response = self.json_ok('bulk_info_by_resource_ids',
                                    {'uid': self.uid, 'meta': 'something_strange'}, json=[self.RESOURCE_ID])
            self.assert_proxy_is_not_called(response)

    def test_bulk_info_by_resource_ids_proxy_all_meta_requested(self):
        with mock.patch(self.BULK_INFO_ENABLED, True):
            response = self.json_ok('bulk_info_by_resource_ids',
                                    {'uid': self.uid, 'meta': ''}, json=[self.RESOURCE_ID])
            self.assert_proxy_is_not_called(response)


class DjfsPublicListProxyListTestCase(DiskProxyListTestCase):

    PUBLIC_LIST_ENABLED = 'mpfs.frontend.api.disk.JAVA_DJFS_API_PROXY_PUBLIC_LIST_ENABLED'
    PUBLIC_LIST_ENABLED_UIDS = 'mpfs.frontend.api.disk.JAVA_DJFS_API_PROXY_PUBLIC_LIST_ENABLED_UIDS'

    def setup_method(self, method):
        super(DjfsPublicListProxyListTestCase, self).setup_method(method)

        folder_path = '/disk/folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path})
        set_public_response = self.json_ok('set_public', {'uid': self.uid, 'path': folder_path})
        self.PUBLIC_HASH = set_public_response['hash']

    def run(self, result=None):
        with DjfsApiMockHelper.mock_request():
            super(DjfsPublicListProxyListTestCase, self).run(result)

    def test_proxy_ok(self):
        with mock.patch(self.PUBLIC_LIST_ENABLED, True):
            response = self.json_ok('public_list', {'uid': self.uid, 'private_hash': self.PUBLIC_HASH})
            self.assert_proxy_is_called(response)

    def test_proxy_disabled(self):
        with mock.patch(self.PUBLIC_LIST_ENABLED, False):
            response = self.json_ok('public_list', {'uid': self.uid, 'private_hash': self.PUBLIC_HASH})
            self.assert_proxy_is_not_called(response)

    def test_without_uid(self):
        with mock.patch(self.PUBLIC_LIST_ENABLED, False), mock.patch(self.PUBLIC_LIST_ENABLED_UIDS, [self.uid]):
            response = self.json_ok('public_list', {'private_hash': self.PUBLIC_HASH})
            self.assert_proxy_is_not_called(response)

    def test_supported_qs_parameter(self):
        with mock.patch(self.PUBLIC_LIST_ENABLED, True):
            response = self.json_ok('public_list',
                                    {'uid': self.uid, 'private_hash': self.PUBLIC_HASH, 'preview_crop': '1'})
            self.assert_proxy_is_called(response)

    def test_unsupported_qs_paratemer(self):
        with mock.patch(self.PUBLIC_LIST_ENABLED, True):
            response = self.json_ok('public_list',
                                    {'uid': self.uid, 'private_hash': self.PUBLIC_HASH, 'unsupported_smth': '1'})
            self.assert_proxy_is_not_called(response)

    def test_supported_meta_parameter(self):
        with mock.patch(self.PUBLIC_LIST_ENABLED, True):
            response = self.json_ok('public_list',
                                    {'uid': self.uid, 'private_hash': self.PUBLIC_HASH, 'meta': 'size'})
            self.assert_proxy_is_called(response)

    def test_unsupported_meta_parameter(self):
        with mock.patch(self.PUBLIC_LIST_ENABLED, True):
            response = self.json_ok('public_list',
                                    {'uid': self.uid, 'private_hash': self.PUBLIC_HASH, 'meta': 'whatsup_man'})
            self.assert_proxy_is_not_called(response)

    def test_empty_meta(self):
        with mock.patch(self.PUBLIC_LIST_ENABLED, True):
            response = self.json_ok('public_list',
                                    {'uid': self.uid, 'private_hash': self.PUBLIC_HASH, 'meta': ''})
            self.assert_proxy_is_not_called(response)

    def test_overdraft_restriction(self):
        with mock.patch(self.PUBLIC_LIST_ENABLED, True), \
             DjfsApiMockHelper.mock_request(error_code=codes.OVERDRAFT_USER_PUBLIC_LINK_IS_DISABLED, status_code=404):
            self.json_error(
                'public_list',
                {'uid': self.uid, 'private_hash': self.PUBLIC_HASH},
                code=codes.OVERDRAFT_USER_PUBLIC_LINK_IS_DISABLED,
                title='Overdraft user public link not allowed',
                status=404,
            )


class DjfsPublicInfoProxyTestCase(DiskProxyTestCase):

    PUBLIC_INFO_ENABLED = 'mpfs.frontend.api.disk.JAVA_DJFS_API_PROXY_PUBLIC_INFO_ENABLED'
    PUBLIC_INFO_ENABLED_UIDS = 'mpfs.frontend.api.disk.JAVA_DJFS_API_PROXY_PUBLIC_INFO_ENABLED_UIDS'

    def setup_method(self, method):
        super(DjfsPublicInfoProxyTestCase, self).setup_method(method)

        folder_path = '/disk/folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path})
        set_public_response = self.json_ok('set_public', {'uid': self.uid, 'path': folder_path})
        self.PUBLIC_HASH = set_public_response['hash']

    def run(self, result=None):
        with DjfsApiMockHelper.mock_request():
            super(DjfsPublicInfoProxyTestCase, self).run(result)

    def test_proxy_ok(self):
        with mock.patch(self.PUBLIC_INFO_ENABLED, True):
            response = self.json_ok('public_info', {'uid': self.uid, 'private_hash': self.PUBLIC_HASH})
            self.assert_proxy_is_called(response)

    def test_proxy_disabled(self):
        with mock.patch(self.PUBLIC_INFO_ENABLED, False):
            response = self.json_ok('public_info', {'uid': self.uid, 'private_hash': self.PUBLIC_HASH})
            self.assert_proxy_is_not_called(response)

    def test_without_uid(self):
        with mock.patch(self.PUBLIC_INFO_ENABLED, False), mock.patch(self.PUBLIC_INFO_ENABLED_UIDS, [self.uid]):
            response = self.json_ok('public_info', {'private_hash': self.PUBLIC_HASH})
            self.assert_proxy_is_not_called(response)

    def test_supported_qs_parameter(self):
        with mock.patch(self.PUBLIC_INFO_ENABLED, True):
            response = self.json_ok('public_info',
                                    {'uid': self.uid, 'private_hash': self.PUBLIC_HASH, 'preview_crop': '1'})
            self.assert_proxy_is_called(response)

    def test_unsupported_qs_paratemer(self):
        with mock.patch(self.PUBLIC_INFO_ENABLED, True):
            response = self.json_ok('public_info',
                                    {'uid': self.uid, 'private_hash': self.PUBLIC_HASH, 'unsupported_smth': '1'})
            self.assert_proxy_is_not_called(response)

    def test_supported_meta_parameter(self):
        with mock.patch(self.PUBLIC_INFO_ENABLED, True):
            response = self.json_ok('public_info',
                                    {'uid': self.uid, 'private_hash': self.PUBLIC_HASH, 'meta': 'size'})
            self.assert_proxy_is_called(response)

    def test_unsupported_meta_parameter(self):
        with mock.patch(self.PUBLIC_INFO_ENABLED, True):
            response = self.json_ok('public_info',
                                    {'uid': self.uid, 'private_hash': self.PUBLIC_HASH, 'meta': 'whatsup_man'})
            self.assert_proxy_is_not_called(response)

    def test_empty_meta(self):
        with mock.patch(self.PUBLIC_INFO_ENABLED, True):
            response = self.json_ok('public_info',
                                    {'uid': self.uid, 'private_hash': self.PUBLIC_HASH, 'meta': ''})
            self.assert_proxy_is_not_called(response)

    def test_overdraft_restriction(self):
        with mock.patch(self.PUBLIC_INFO_ENABLED, True), \
             DjfsApiMockHelper.mock_request(error_code=codes.OVERDRAFT_USER_PUBLIC_LINK_IS_DISABLED, status_code=404):
            self.json_error(
                'public_info',
                {'uid': self.uid, 'private_hash': self.PUBLIC_HASH},
                code=codes.OVERDRAFT_USER_PUBLIC_LINK_IS_DISABLED,
                title='Overdraft user public link not allowed',
                status=404,
            )


class DjfsInfoProxyTestCase(DiskProxyTestCase):
    INFO_ENABLED = 'mpfs.frontend.api.disk.JAVA_DJFS_API_PROXY_INFO_ENABLED'
    INFO_SUPPORTED_META = 'mpfs.frontend.api.disk.JAVA_DJFS_API_PROXY_INFO_SUPPORTED_META'
    folder_path='/disk/folder'

    def setup_method(self, method):
        super(DjfsInfoProxyTestCase, self).setup_method(method)
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.folder_path})

    def run(self, result=None):
        with DjfsApiMockHelper.mock_request():
            super(DjfsInfoProxyTestCase, self).run(result)

    def test_proxy_disabled(self):
        with mock.patch(self.INFO_ENABLED, False):
            response = self.json_ok('info', {'uid': self.uid, 'path': self.folder_path, 'tld': 'ru'})
            self.assert_proxy_is_not_called(response)

    def test_proxy_enabled_and_not_acceptable_meta(self):
        with mock.patch(self.INFO_ENABLED, True):
            response = self.json_ok('info', {'uid': self.uid, 'path': self.folder_path, 'tld': 'ru', 'meta': 'fs_symbolic_link'})
            self.assert_proxy_is_not_called(response)

    def test_proxy_enabled_and_acceptable_meta(self):
        with mock.patch(self.INFO_ENABLED, True):
            response = self.json_ok('info', {'uid': self.uid, 'path': self.folder_path, 'tld': 'ru', 'meta': 'file_id'})
            self.assert_proxy_is_called(response)

    def test_proxy_enabled_and_not_empty_meta(self):
        with mock.patch(self.INFO_ENABLED, True):
            response = self.json_ok('info', {'uid': self.uid, 'path': self.folder_path, 'tld': 'ru', 'meta': ''})
            self.assert_proxy_is_not_called(response)

    def test_proxy_enabled_and_without_meta(self):
        with mock.patch(self.INFO_ENABLED, True):
            response = self.json_ok('info', {'uid': self.uid, 'path': self.folder_path, 'tld': 'ru'})
            self.assert_proxy_is_called(response)

    def test_proxy_enabled_and_not_acceptable_params(self):
        with mock.patch(self.INFO_ENABLED, True):
            response = self.json_ok('info', {'uid': self.uid, 'path': self.folder_path, 'tld': 'ru', 'hello': '1'})
            self.assert_proxy_is_not_called(response)

    def test_proxy_enabled_and_acceptable_meta_and_not_supported_path(self):
        with mock.patch(self.INFO_ENABLED, True):
            self.json_ok('mkdir', {'uid': self.uid, 'path': '/settings/test'})
            response = self.json_ok('info', {'uid': self.uid, 'path': '/settings/test', 'tld': 'ru', 'meta': 'file_id'})
            self.assert_proxy_is_not_called(response)


class DjfsDefaultFoldersProxyTestCase(DiskProxyTestCase):
    DEFAULT_FOLDERS_ENABLED = 'mpfs.frontend.api.disk.JAVA_DJFS_API_PROXY_DEFAULT_FOLDERS_ENABLED'
    DEFAULT_FOLDERS_YCRID_PREFIXES = 'mpfs.frontend.api.disk.JAVA_DJFS_API_PROXY_DEFAULT_FOLDERS_YCRID_PREFIXES'

    def setup_method(self, method):
        super(DjfsDefaultFoldersProxyTestCase, self).setup_method(method)

    def run(self, result=None):
        with DjfsApiMockHelper.mock_request():
            super(DjfsDefaultFoldersProxyTestCase, self).run(result)

    def test_proxy_disabled(self):
        with mock.patch(self.DEFAULT_FOLDERS_ENABLED, False):
            response = self.json_ok('default_folders', {'uid': self.uid})
            self.assert_proxy_is_not_called(response)

    def test_proxy_enabled_and_not_acceptable_ycrid(self):
        with mock.patch(self.DEFAULT_FOLDERS_ENABLED, True), mock.patch(self.DEFAULT_FOLDERS_YCRID_PREFIXES, ['web-']):
            response = self.json_ok('default_folders', {'uid': self.uid}, headers={'Yandex-Cloud-Request-ID': 'rest-fake-ycrid'})
            self.assert_proxy_is_not_called(response)

    def test_proxy_enabled_and_acceptable_ycrid(self):
        with mock.patch(self.DEFAULT_FOLDERS_ENABLED, True), mock.patch(self.DEFAULT_FOLDERS_YCRID_PREFIXES, ['web-']):
            response = self.json_ok('default_folders', {'uid': self.uid}, headers={'Yandex-Cloud-Request-ID': 'web-fake-ycrid'})
            self.assert_proxy_is_called(response)

