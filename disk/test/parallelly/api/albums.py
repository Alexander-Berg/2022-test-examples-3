# -*- coding: utf-8 -*-
from mock import mock
from nose_parameterized import parameterized

from mpfs.platform.v1.disk.permissions import WebDavPermission
from test.parallelly.api.disk.base import DiskApiTestCase

from mpfs.common.static import tags


class AlbumsDeltasTestCase(DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'albums/deltas'

    @parameterized.expand([
        ('single', 'personal', '&type=personal'),
        ('multiple', 'personal,geo', '&type=personal,geo'),
    ])
    def test_type_param_passing(self, case_name, passed_types, expected_types):
        params = '?base_revision=1&type=%s' % passed_types

        with self.specified_client(scopes=WebDavPermission.scopes), \
                mock.patch('mpfs.core.services.common_service.Service.open_url') as patched_open_url:
            self.client.request(self.method, self.url + params)

            actual_url = patched_open_url.call_args[0][0]
            assert expected_types in actual_url

    def test_400_with_wrong_type(self):
        params = '?base_revision=1&type=geo,animals'

        with self.specified_client(scopes=WebDavPermission.scopes):
            resp = self.client.request(self.method, self.url + params)

            assert resp.status_code == 400


    def test_no_type_param_passing(self):
        params = '?base_revision=1'

        with self.specified_client(scopes=WebDavPermission.scopes), \
             mock.patch('mpfs.core.services.common_service.Service.open_url') as patched_open_url:
            self.client.request(self.method, self.url + params)

            actual_url = patched_open_url.call_args[0][0]
            assert 'type=' not in actual_url
