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
from mpfs.platform.v1.disk.permissions import WebDavPermission
from mpfs.common.static import tags
from mpfs.config import settings


class MPFSFile(object):
    def __init__(self, name, size=123):
        self.mpfs_path = '/disk/%s' % name
        self.platform_path = 'disk:/%s' % name
        self.size = size


class DocsFilesTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'GET'

    file_supported_with_conversion = MPFSFile('duck.doc')
    supported_file = MPFSFile('duck.docx')
    unsupported_file_by_extension = MPFSFile('duck.exe')
    pdf_file = MPFSFile('duck.pdf')
    unsupported_file_by_size = MPFSFile('duck.xlsx',
                                        settings.office['only_office']['size_limits']['Excel']['edit'] + 10 * KB)

    @classmethod
    def setup_class(cls):
        super(DocsFilesTestCase, cls).setup_class()
        with open('fixtures/xml/discovery.xml') as fd:
            discovery_response_xml = fd.read()
        with mock.patch('mpfs.core.services.discovery_service.DiscoveryService.open_url',
                               return_value=discovery_response_xml):
            DiscoveryService().ensure_cache()

    def setup_method(self, method):
        super(DocsFilesTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)
        for file in (self.file_supported_with_conversion,
                     self.supported_file,
                     self.unsupported_file_by_extension,
                     self.unsupported_file_by_size,
                     self.pdf_file):
            self.upload_file(self.uid, file.mpfs_path, file_data={'size': file.size})

    @parameterized.expand([
        ('supported', supported_file, None, 'disk.yandex.ru'),
        ('supported_with_conversion', file_supported_with_conversion, None, 'disk.yandex.ru'),
        ('tld_passed', file_supported_with_conversion, 'com', 'disk.yandex.com'),
        ('unsupported_tld_passed', file_supported_with_conversion, 'pt', 'disk.yandex.ru'),
    ])
    def test_file_urls_positive_cases(self, case_name, file, tld, expected_hostname):
        query = {}
        if tld is not None:
            query['tld'] = tld

        with self.specified_client(scopes=WebDavPermission.scopes),\
            mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True),\
            mock.patch('mpfs.core.user.common.CommonUser.get_only_office_enabled', return_value=True):

            resource_id = self.json_ok('info', {'uid': self.uid, 'path': file.mpfs_path, 'meta': 'resource_id'})['meta']['resource_id']
            resp = self.client.request(self.method, 'disk/docs/resources/%s/' % resource_id, query=query)

        result = json.loads(resp.content)
        assert 'view_url' in result
        assert 'edit_url' in result
        assert expected_hostname in result['edit_url']

    @parameterized.expand([
        ('by_size', unsupported_file_by_size, 200),
        ('by_extension', unsupported_file_by_extension, 415),
        ('pdf_file', pdf_file, 200)
    ])
    def test_unsupported_files_for_edit(self, case_name, file, status_code):
        with self.specified_client(scopes=WebDavPermission.scopes),\
            mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True),\
            mock.patch('mpfs.core.user.common.CommonUser.get_only_office_enabled', return_value=True):

            resource_id = self.json_ok('info', {'uid': self.uid, 'path': file.mpfs_path, 'meta': 'resource_id'})['meta']['resource_id']
            resp = self.client.request(self.method, 'disk/docs/resources/%s/' % resource_id, query={})

        assert resp.status_code == status_code
        if resp.status_code == 200:
            result = json.loads(resp.content)
            assert 'view_url' in result
            assert 'edit_url' not in result

    def test_file_filters(self):
        with self.specified_client(scopes=WebDavPermission.scopes):
            resp = self.client.request(self.method, 'disk/docs/filters/')
            result = json.loads(resp.content)
            assert result
