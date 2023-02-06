# -*- coding: utf-8 -*-
import pytest
from nose_parameterized import parameterized

from mpfs.common.util import to_json
from mpfs.core.photoslice.albums.static import PhotosliceAlbumTypeConst
from test.conftest import INIT_USER_IN_POSTGRES
from test.parallelly.photoslice_album.base import UploadTypes, PhotosliceAlbumBaseTestCase


@pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Tests for PG only')
class PhotosliceIosAlbumTestCase(PhotosliceAlbumBaseTestCase):

    def setup_method(self, method):
        super(PhotosliceIosAlbumTestCase, self).setup_method(method)
        self.user_agent = self.ios_user_agent

    @parameterized.expand([
        (UploadTypes.upload_common,),
        (UploadTypes.upload_hardlink_on_store,),
        (UploadTypes.upload_hardlink_on_callback,),
    ])
    def test_upload_screenshot(self, upload_type):
        path = '/disk/1.jpg'
        opts = {'screenshot': '1'}
        self._upload_by_type(
            upload_type, self.uid, path, media_type='image', opts=opts, headers={'user-agent': self.user_agent})
        res = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
        assert res['meta'].get('photoslice_album_type') == unicode(PhotosliceAlbumTypeConst.SCREENSHOTS)

    @parameterized.expand([
        (UploadTypes.upload_common,),
        (UploadTypes.upload_hardlink_on_store,),
        (UploadTypes.upload_hardlink_on_callback,),
    ])
    def test_upload_camera_photo(self, upload_type):
        path = '/disk/1.jpg'
        opts = {'etime': '123456789'}
        self._upload_by_type(
            upload_type, self.uid, path, media_type='image', opts=opts, headers={'user-agent': self.user_agent})
        res = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
        assert res['meta'].get('photoslice_album_type') == unicode(PhotosliceAlbumTypeConst.CAMERA)

    @parameterized.expand([
        ({'mtime': '1234'}, UploadTypes.upload_common,),
        ({}, UploadTypes.upload_common,),
        ({'device_original_path': '/u0/folder/screenshot/1.jpg'}, UploadTypes.upload_common,),
        ({'screenshot': '0'}, UploadTypes.upload_common,),
        ({'mtime': '1234'}, UploadTypes.upload_hardlink_on_store,),
        ({}, UploadTypes.upload_hardlink_on_store,),
        ({'device_original_path': '/u0/folder/screenshot/1.jpg'}, UploadTypes.upload_hardlink_on_store,),
        ({'screenshot': '0'}, UploadTypes.upload_hardlink_on_store,),
        ({'mtime': '1234'}, UploadTypes.upload_hardlink_on_callback,),
        ({}, UploadTypes.upload_hardlink_on_callback,),
        ({'device_original_path': '/u0/folder/screenshot/1.jpg'}, UploadTypes.upload_hardlink_on_callback,),
        ({'screenshot': '0'}, UploadTypes.upload_hardlink_on_callback,),
    ])
    def test_other_photos_arent_in_albums(self, opts, upload_type):
        path = '/disk/1.jpg'
        self._upload_by_type(
            upload_type, self.uid, path, media_type='image', opts=opts, headers={'user-agent': self.user_agent})
        res = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
        assert res['meta'].get('photoslice_album_type') is None

    @parameterized.expand([
        ({}, UploadTypes.upload_common),
        ({'device_collections': '[]'}, UploadTypes.upload_hardlink_on_store),
        ({}, UploadTypes.upload_hardlink_on_store),
        ({}, UploadTypes.upload_hardlink_on_callback,),
    ])
    def test_upload_camera_video(self, opts, upload_type):
        path = '/disk/1.mov'
        self._upload_by_type(
            upload_type, self.uid, path, media_type='video', opts=opts, headers={'user-agent': self.user_agent})
        res = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
        assert res['meta'].get('photoslice_album_type') == unicode(PhotosliceAlbumTypeConst.CAMERA)

    @parameterized.expand([
        ({'mtime': '1234', 'device_collections': '["enoty"]'}, UploadTypes.upload_common,),
        ({'device_collections': '["enoty"]'}, UploadTypes.upload_common,),
        ({'device_original_path': '/u0/folder/screenshot/1.mov', 'device_collections': '["enoty"]'}, UploadTypes.upload_common,),
        ({'screenshot': '1', 'device_collections': '["enoty"]'}, UploadTypes.upload_common,),
        ({'device_collections': '["camera_roll", "col1", "col2"]'}, UploadTypes.upload_common,),
        ({'mtime': '1234', 'device_collections': '["enoty"]'}, UploadTypes.upload_hardlink_on_store,),
        ({'device_collections': '["enoty"]'}, UploadTypes.upload_hardlink_on_store,),
        ({'device_original_path': '/u0/folder/screenshot/1.mov', 'device_collections': '["enoty"]'}, UploadTypes.upload_hardlink_on_store,),
        ({'screenshot': '1', 'device_collections': '["enoty"]'}, UploadTypes.upload_hardlink_on_store,),
        ({'device_collections': '["camera_roll", "col1", "col2"]'}, UploadTypes.upload_hardlink_on_store,),
        ({'mtime': '1234', 'device_collections': '["enoty"]'}, UploadTypes.upload_hardlink_on_callback,),
        ({'device_collections': '["enoty"]'}, UploadTypes.upload_hardlink_on_callback,),
        ({'device_original_path': '/u0/folder/screenshot/1.mov', 'device_collections': '["enoty"]'}, UploadTypes.upload_hardlink_on_callback,),
        ({'screenshot': '1', 'device_collections': '["enoty"]'}, UploadTypes.upload_hardlink_on_callback,),
        ({'device_collections': '["camera_roll", "col1", "col2"]'}, UploadTypes.upload_hardlink_on_callback,),
    ])
    def test_other_videos_arent_in_albums(self, opts, upload_type):
        path = '/disk/1.mov'
        self._upload_by_type(
            upload_type, self.uid, path, media_type='video', opts=opts, headers={'user-agent': self.user_agent})
        res = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
        assert res['meta'].get('photoslice_album_type') is None
