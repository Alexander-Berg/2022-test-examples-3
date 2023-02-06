# -*- coding: utf-8 -*-
from test.base import DiskTestCase
import mpfs.core.services.kladun_service as kladun


class TestKladun(DiskTestCase):
    def test_upload_url(self):
        uploader = kladun.UploadToDisk()
        d = {'uid': self.uid,
             'path': '/disk/test',
             'oid': '',
             'file-id': 1,
             'service': 'disk'}
        upload_url, status_url = uploader.post_request(d)
        self.assertNotEqual(upload_url, None)
        self.assertNotEqual(status_url, None)

    def test_status(self):
        uploader = kladun.UploadToDisk()
        d = {'uid': self.uid,
             'path': '/disk/test',
             'oid': '',
             'file-id': 1,
             'service': 'disk'}
        upload_url, status_url = uploader.post_request(d)
        self.assertNotEqual(upload_url, None)
        self.assertNotEqual(status_url, None)
        result = uploader.status(status_url)
        self.assertNotEqual(result, None)
