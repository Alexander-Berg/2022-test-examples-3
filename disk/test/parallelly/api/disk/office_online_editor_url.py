# -*- coding: utf-8 -*-
import json

from mock import mock
from nose_parameterized import parameterized

from mpfs.core.services.discovery_service import DiscoveryService
from test.base_suit import (
    UploadFileTestCaseMixin,
    UserTestCaseMixin,
)
from test.helpers.size_units import KB
from test.parallelly.api.disk.base import DiskApiTestCase
from mpfs.common.static import tags
from mpfs.config import settings


class MPFSFile(object):
    def __init__(self, name, size=123):
        self.mpfs_path = '/disk/%s' % name
        self.platform_path = 'disk:/%s' % name
        self.size = size


class OnlineEditorURLTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'disk/resources/online-editor'

    file_supported_with_conversion = MPFSFile('enot.doc')
    supported_file = MPFSFile('enot.docx')
    unsupported_file_by_extension = MPFSFile('enot.rtf')
    unsupported_file_by_size = MPFSFile('enot.xlsx', settings.office['size_limits']['Excel']['edit'] + 10*KB)

    allowed_client_id = settings.platform['disk_apps_ids'][0]

    @classmethod
    def setup_class(cls):
        super(OnlineEditorURLTestCase, cls).setup_class()
        with open('fixtures/xml/discovery.xml') as fd:
            discovery_response_xml = fd.read()
        with mock.patch('mpfs.core.services.discovery_service.DiscoveryService.open_url',
                               return_value=discovery_response_xml):
            DiscoveryService().ensure_cache()

    def setup_method(self, method):
        super(OnlineEditorURLTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)
        for file in (self.file_supported_with_conversion,
                     self.supported_file,
                     self.unsupported_file_by_extension,
                     self.unsupported_file_by_size):
            self.upload_file(self.uid, file.mpfs_path, file_data={'size': file.size})

    @parameterized.expand([
        ('supported', supported_file.platform_path, None, 'disk.yandex.ru'),
        ('supported_with_conversion', file_supported_with_conversion.platform_path, None, 'disk.yandex.ru'),
        ('tld_passed', file_supported_with_conversion.platform_path, 'com', 'disk.yandex.com'),
        ('unsupported_tld_passed', file_supported_with_conversion.platform_path, 'pt', 'disk.yandex.ru'),
    ])
    def test_positive_cases(self, case_name, path, tld, expected_hostname):
        query = {'path': path}
        if tld is not None:
            query['tld'] = tld

        with self.specified_client(id=self.allowed_client_id):
            resp = self.client.request(self.method, self.url,
                                       query=query)

        result = json.loads(resp.content)

        assert 'edit_url' in result
        assert expected_hostname in result['edit_url']


    @parameterized.expand([
        ('by_size', unsupported_file_by_size.platform_path),
        ('by_extension', unsupported_file_by_extension.platform_path),
    ])
    def test_unsupported_files(self, case_name, path):
        with self.specified_client(id=self.allowed_client_id):
            resp = self.client.request(self.method, self.url,
                                       query={'path': path})

        assert resp.status_code == 415

    @mock.patch('mpfs.core.office.util.FEATURE_TOGGLES_ONLYOFFICE_EDITOR_FOR_USERS_WITHOUT_EDITOR_ENABLED', False)
    def test_user_with_disabled_editor(self):
        with self.specified_client(id=self.allowed_client_id), \
                mock.patch('mpfs.core.office.logic.microsoft.MicrosoftEditor.is_user_allowed', return_value=False):
            resp = self.client.request(self.method, self.url,
                                       query={'path': self.supported_file.platform_path})

        assert resp.status_code == 403

    def test_not_found(self):
        with self.specified_client(id=self.allowed_client_id):
            resp = self.client.request(self.method, self.url,
                                       query={'path': u'disk:/Диплом 2003.docx'})

        assert resp.status_code == 404
