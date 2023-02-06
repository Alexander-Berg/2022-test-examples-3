# -*- coding: utf-8 -*-

import mock
import urllib2
import StringIO
from nose_parameterized import parameterized
from test.base import DiskTestCase
from test.helpers.stubs.services import VideoStreamingStub


class VideoStreamingTestCase(DiskTestCase):
    """Тестируем получение ссылок на новый видео стриминг"""
    IP = '198.168.1.1'
    CLIENT_ID = '12345'

    def test_video_streams(self):
        file_path = '/disk/1.avi'
        self.upload_file(self.uid, file_path)
        file_mid = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': 'file_mid'})['meta']['file_mid']
        with VideoStreamingStub() as stub:
            resp = self.json_ok('video_streams', {'uid': self.uid, 'path': file_path, 'user_ip': self.IP, 'client_id': self.CLIENT_ID})
            assert resp == VideoStreamingStub._video_info_common_response
            stub.get_video_info.assert_called_with(self.uid, file_mid, use_http=False, user_ip=self.IP, is_public=False, client_id=self.CLIENT_ID)
            self.json_ok('video_streams', {'uid': self.uid, 'path': file_path, 'use_http': 1})
            stub.get_video_info.assert_called_with(self.uid, file_mid, use_http=True, user_ip='', is_public=False, client_id='')

    def test_public_video_streams(self):
        file_path = '/disk/1.avi'
        self.upload_file(self.uid, file_path)
        private_hash = self.json_ok('set_public', {'uid': self.uid, 'path': file_path, 'meta': 'file_mid'})['hash']
        file_mid = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': 'file_mid'})['meta']['file_mid']
        with VideoStreamingStub() as stub:
            resp = self.json_ok('public_video_streams', {'private_hash': private_hash, 'uid': self.uid, 'user_ip': self.IP, 'client_id': self.CLIENT_ID})
            assert resp == VideoStreamingStub._video_info_common_response
            stub.get_video_info.assert_called_with(self.uid, file_mid, use_http=False, user_ip=self.IP, consumer_uid=self.uid, is_public=True, client_id=self.CLIENT_ID)
            self.json_ok('public_video_streams', {'private_hash': private_hash, 'use_http': 1})
            stub.get_video_info.assert_called_with(self.uid, file_mid, use_http=True, user_ip='', consumer_uid='', is_public=True, client_id='')

    @parameterized.expand([(422, 'UNPROCESSABLE ENTITY', 422),
                           (500, 'INTERNAL SERVER ERROR', 503)])
    def test_video_streaming_video_streams_response_codes(self, video_streaming_status, status_desc, mpfs_status):
        file_path = '/disk/1.avi'
        self.upload_file(self.uid, file_path)
        fd = StringIO.StringIO()
        side_effect = urllib2.HTTPError('', video_streaming_status, status_desc,
                                        {'Yandex-cloud-request-id': 'rest-xxxx-test', 'X-request-attempt': '0'}, fd)
        with mock.patch('urllib2.urlopen', side_effect=side_effect):
            self.json_error('video_streams', {'uid': self.uid, 'path': file_path,
                                              'user_ip': self.IP, 'client_id': self.CLIENT_ID}, status=mpfs_status)

    @parameterized.expand([(422, 'UNPROCESSABLE ENTITY', 422),
                           (500, 'INTERNAL SERVER ERROR', 503)])
    def test_video_streaming_public_video_streams_response_codes(self, video_streaming_status, status_desc, mpfs_status):
        file_path = '/disk/1.avi'
        self.upload_file(self.uid, file_path)
        private_hash = self.json_ok('set_public', {'uid': self.uid, 'path': file_path, 'meta': 'file_mid'})['hash']
        fd = StringIO.StringIO()
        side_effect = urllib2.HTTPError('', video_streaming_status, status_desc,
                                        {'Yandex-cloud-request-id': 'rest-xxxx-test', 'X-request-attempt': '0'}, fd)
        with mock.patch('urllib2.urlopen', side_effect=side_effect):
            self.json_error('public_video_streams', {'private_hash': private_hash, 'uid': self.uid, 'user_ip': self.IP,
                                                     'client_id': self.CLIENT_ID}, status=mpfs_status)
