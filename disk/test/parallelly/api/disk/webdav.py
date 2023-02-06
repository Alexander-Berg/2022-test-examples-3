# -*- coding: utf-8 -*-
from test.parallelly.api.disk.base import DiskApiTestCase
from test.base_suit import UserTestCaseMixin
from mpfs.common.static import tags


class WebDavScopeTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'

    def test_webdav_scope(self):
        tests = (
            ('GET', 'disk', {}, 200),
            ('GET', 'disk/resources', {'path': '/'}, 200),
            ('PUT', 'disk/resources', {'path': '/dir'}, 201),
            ('DELETE', 'disk/resources', {'path': '/dir'}, 204),
            ('PUT', 'disk/resources', {'path': '/dir'}, 201),
            ('POST', 'disk/resources/copy', {'from': '/dir', 'path': '/moved'}, 201),
            ('GET', 'disk/resources/download', {'path': '/moved'}, 200),
            ('GET', 'disk/resources/files', {}, 403),
            ('GET', 'disk/resources/last-uploaded', {}, 403),
            ('POST', 'disk/resources/move', {'from': '/moved', 'path': '/tmp'}, 201),
            ('GET', 'disk/resources/public', {}, 200),
            ('PUT', 'disk/resources/publish', {'path': '/tmp'}, 200),
            ('PUT', 'disk/resources/unpublish', {'path': '/tmp'}, 200),
            ('GET', 'disk/resources/upload', {'path': '/1.txt'}, 200),
        )

        for method, url, query, status in tests:
            with self.specified_client(scopes=['yadisk:all']):
                resp = self.client.request(method, url,
                                           query=query,
                                           uid=self.uid,
                                           ip='6.6.6.6')
                assert resp.status == status, '%s %s %s != %s' % (method, url, resp.status, status)
