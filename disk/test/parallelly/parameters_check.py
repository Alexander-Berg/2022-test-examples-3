# -*- coding: utf-8 -*-
import itertools

from parameterized import parameterized
from mpfs.common.static import codes
from test.base import DiskTestCase
from test.parallelly.api.disk.base import DiskApiTestCase


class ParametersCheck(DiskTestCase):
    @parameterized.expand(itertools.product(*
        [['path', 'src', 'dst'],
         ['async_trash_append', 'upload_file', 'not_even_a_real_method']]))
    def test_null_symbol_is_bad_request(self, parameter, method):
        filename = u'\u0000' + 'file.txt'
        self.json_error(method, {'uid': self.uid, parameter: '/disk/%s' % filename},
                        code=codes.URL_BAD_PATH,
                        status=400)


class ParametersApiCheck(DiskApiTestCase):
    api_version = 'v1'
    method = 'GET'

    @parameterized.expand(itertools.product(*
        [['path', 'src', 'dst'],
         ['resources', 'resources/download', 'public/video/streams']]))  # несколько случайных апи.
    def test_null_symbol_is_bad_request(self, parameter, method):
        filename = u'\u0000' + 'file.txt'

        with self.specified_client():
            resp = self.client.request('GET',  'disk/%s?%s=%s' % (method, parameter, filename))
            assert resp.status_code == 400
