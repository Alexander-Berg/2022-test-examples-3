# -*- coding: utf-8 -*-
import mock
import urlparse

from test.parallelly.api.disk.base import DiskApiTestCase
from mpfs.common.errors import DataApiBadResponse
from mpfs.common.static import tags
from mpfs.common.util import to_json
from mpfs.core.services.data_api_service import DataApiService


class ImportYTObjectTestCase(DiskApiTestCase):
    api_version = 'v1'
    api_mode = tags.platform.INTERNAL

    def setup_method(self, method):
        super(ImportYTObjectTestCase, self).setup_method(method)

        self.wrong_ypath_response = {'error': {'name': 'inaccessible-ypath'}}
        client_token = 'afb136c45e245fba963ddb13ae6f3c68c2ef48a2bd123'
        self.headers = {'Authorization': 'ClientToken token=%s;' % client_token}

    def test_proxy_start_yt_table_import(self):
        with self.specified_client(scopes=['cloud_api.import.yt:generic.test.resource.path.write']):
            with mock.patch.object(DataApiService, 'open_url', return_value=(200, '', {})) as open_url_mock:
                self.client.post('case/personality/test/resource/path/import/yt?yt_path=test_yt_path',
                                 headers=self.headers)
                requestsed_url = open_url_mock.call_args[0][0]
                qs_params = dict(urlparse.parse_qsl(urlparse.urlsplit(requestsed_url).query))
                assert qs_params == {'typeName': 'test/resource/path', 'ypath': 'test_yt_path'}

    def test_proxy_start_yt_table_import_with_id(self):
        with self.specified_client(scopes=['cloud_api.import.yt:generic.test.resource.path.write']):
            with mock.patch.object(DataApiService, 'open_url', return_value=(200, '', {})) as open_url_mock:
                self.client.put('case/personality/test/resource/path/import/yt/test_id?yt_path=test_yt_path',
                                headers=self.headers)
                requestsed_url = open_url_mock.call_args[0][0]
                qs_params = dict(urlparse.parse_qsl(urlparse.urlsplit(requestsed_url).query))
                assert qs_params == {'typeName': 'test/resource/path', 'ypath': 'test_yt_path', 'importId': 'test_id'}

    def test_proxy_check_yt_table_import_status(self):
        with self.specified_client(scopes=['cloud_api.import.yt:generic.test.resource.path.read']):
            with mock.patch.object(DataApiService, 'open_url', return_value=(200, '', {})) as open_url_mock:
                self.client.get('case/personality/test/resource/path/import/yt/test_id?', headers=self.headers)
                requestsed_url = open_url_mock.call_args[0][0]
                qs_params = dict(urlparse.parse_qsl(urlparse.urlsplit(requestsed_url).query))
                assert qs_params == {'typeName': 'test/resource/path'}
                assert '/api/generic_data/import/test_id/counters' in urlparse.urlsplit(requestsed_url).path

    def test_forward_error_yt_table_not_found(self):
        with self.specified_client(scopes=['cloud_api.import.yt:generic.test.resource.path.write']):
            with mock.patch.object(
                    DataApiService, 'open_url',
                    side_effect=DataApiBadResponse(data={'text': to_json(self.wrong_ypath_response), 'code': 400})):
                r = self.client.post('case/personality/test/resource/path/import/yt?yt_path=test_yt_path',
                                     headers=self.headers)
                assert r.status_code == 404
