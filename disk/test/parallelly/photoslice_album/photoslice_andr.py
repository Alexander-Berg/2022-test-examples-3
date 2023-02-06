# -*- coding: utf-8 -*-
import pytest
from parameterized import parameterized

from mpfs.core.photoslice.albums.static import PhotosliceAlbumTypeConst
from test.conftest import INIT_USER_IN_POSTGRES
from test.parallelly.photoslice_album.base import PhotosliceAlbumBaseTestCase, UploadTypes


@pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Tests for PG only')
class PhotosliceAndrAlbumTestCase(PhotosliceAlbumBaseTestCase):
    def setup_method(self, method):
        super(PhotosliceAndrAlbumTestCase, self).setup_method(method)
        self.user_agent = self.andr_user_agent

    @parameterized.expand([
        ({'device_original_path': '/u0/folder/screenshot/1.jpg'}, UploadTypes.upload_common,),
        ({'device_original_path': '/u0/folder/SCREENSHOT/1.jpg'}, UploadTypes.upload_common,),
        ({'device_original_path': '/u0/folder/Screenshot/1.jpg'}, UploadTypes.upload_common,),
        ({'device_original_path': '/u0/folder/123/testscreenshottest.jpg'}, UploadTypes.upload_common,),
        ({'device_original_path': '/u0/folder/123/testSCREENSHOTtest.jpg'}, UploadTypes.upload_common,),
        ({'device_original_path': '/u0/folder/123/testScreenshottest.jpg'}, UploadTypes.upload_common,),
        ({'device_original_path': '/u0/folder/screenshot/1.jpg'}, UploadTypes.upload_hardlink_on_store,),
        ({'device_original_path': '/u0/folder/SCREENSHOT/1.jpg'}, UploadTypes.upload_hardlink_on_store,),
        ({'device_original_path': '/u0/folder/Screenshot/1.jpg'}, UploadTypes.upload_hardlink_on_store,),
        ({'device_original_path': '/u0/folder/123/testscreenshottest.jpg'}, UploadTypes.upload_hardlink_on_store,),
        ({'device_original_path': '/u0/folder/123/testSCREENSHOTtest.jpg'}, UploadTypes.upload_hardlink_on_store,),
        ({'device_original_path': '/u0/folder/123/testScreenshottest.jpg'}, UploadTypes.upload_hardlink_on_store,),
        ({'device_original_path': '/u0/folder/screenshot/1.jpg'}, UploadTypes.upload_hardlink_on_callback,),
        ({'device_original_path': '/u0/folder/SCREENSHOT/1.jpg'}, UploadTypes.upload_hardlink_on_callback,),
        ({'device_original_path': '/u0/folder/Screenshot/1.jpg'}, UploadTypes.upload_hardlink_on_callback,),
        ({'device_original_path': '/u0/folder/123/testscreenshottest.jpg'}, UploadTypes.upload_hardlink_on_callback,),
        ({'device_original_path': '/u0/folder/123/testSCREENSHOTtest.jpg'}, UploadTypes.upload_hardlink_on_callback,),
        ({'device_original_path': '/u0/folder/123/testScreenshottest.jpg'}, UploadTypes.upload_hardlink_on_callback,),
        ({'device_original_path': '/u0/folder/screenshot/1.jpg', 'etime': '123456789'}, UploadTypes.upload_common,),
        ({'device_original_path': '/u0/folder/screenshot/1.jpg', 'etime': '123456789'},
         UploadTypes.upload_hardlink_on_store,),
        ({'device_original_path': '/u0/folder/screenshot/1.jpg', 'etime': '123456789'},
         UploadTypes.upload_hardlink_on_callback,),
    ])
    def test_upload_screenshot(self, opts, upload_type):
        path = '/disk/1.jpg'
        self._upload_by_type(
            upload_type, self.uid, path,
            media_type='image', opts=opts, headers={'user-agent': self.user_agent})
        res = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
        assert res['meta'].get('photoslice_album_type') == unicode(PhotosliceAlbumTypeConst.SCREENSHOTS)

    @parameterized.expand([
        ({'device_original_path': '/DCIM/Selfie/Meow.jpg', 'etime': '123456789'}, UploadTypes.upload_common,),
        ({'device_original_path': '/DCIM/100ANDRO/nya.jpg', 'etime': '123456789'}, UploadTypes.upload_common,),
        ({'device_original_path': '/DCIM/Camera/FoxAndRaccoon.jpg', 'etime': '123456789'}, UploadTypes.upload_hardlink_on_store,),
        ({'device_original_path': '/DCIM/OpenCamera/FoxAndRaccoon.jpg', 'etime': '123456789'}, UploadTypes.upload_hardlink_on_store,),
        ({'device_original_path': '/DCIM/selfie/1.jpg', 'etime': '123456789'}, UploadTypes.upload_hardlink_on_callback,),
        ({'device_original_path': '/DCIM/oPeNCaMeRa/1.jpg', 'etime': '123456789'}, UploadTypes.upload_hardlink_on_callback,),
    ])
    def test_upload_camera_photo(self, opts, upload_type):
        path = '/disk/1.jpg'
        self._upload_by_type(
            upload_type, self.uid, path,
            media_type='image', opts=opts, headers={'user-agent': self.user_agent})
        res = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
        assert res['meta'].get('photoslice_album_type') == unicode(PhotosliceAlbumTypeConst.CAMERA)

    @parameterized.expand([
        ({'mtime': '1234'}, UploadTypes.upload_common,),
        ({}, UploadTypes.upload_common,),
        ({'device_original_path': '/u0/folder/screamshot/1.jpg'}, UploadTypes.upload_common,),
        ({'screenshot': '1'}, UploadTypes.upload_common,),
        ({'mtime': '1234'}, UploadTypes.upload_hardlink_on_store,),
        ({}, UploadTypes.upload_hardlink_on_store,),
        ({'device_original_path': '/u0/folder/screamshot/1.jpg'}, UploadTypes.upload_hardlink_on_store,),
        ({'screenshot': '1'}, UploadTypes.upload_hardlink_on_store,),
        ({'mtime': '1234'}, UploadTypes.upload_hardlink_on_callback,),
        ({}, UploadTypes.upload_hardlink_on_callback,),
        ({'device_original_path': '/u0/folder/screamshot/1.jpg'}, UploadTypes.upload_hardlink_on_callback,),
        ({'screenshot': '1'}, UploadTypes.upload_hardlink_on_callback,),
    ])
    def test_other_photos_from_new_andr_arent_in_albums(self, opts, upload_type):
        path = '/disk/1.jpg'
        self._upload_by_type(
            upload_type, self.uid, path,
            media_type='image', opts=opts, headers={'user-agent': self.user_agent})
        res = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
        assert res['meta'].get('photoslice_album_type') is None

    @parameterized.expand([
        ({'device_original_path': '/DCIM/Camera/1.mov'}, UploadTypes.upload_hardlink_on_store,),
        ({'device_original_path': '/DCIM/Camera/enoty/1.mov'}, UploadTypes.upload_common,),
    ])
    def test_upload_camera_video(self, opts, upload_type):
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
        path = '/photostream/enoty.mov'
        mpfs_path = '/photounlim/enoty.mov'
        self._upload_by_type(
            upload_type, self.uid, path,
            media_type='video', opts=opts, headers={'user-agent': self.user_agent,
                                                    'Yandex-Cloud-Request-ID': 'andr-test'})
        res = self.json_ok('info', {'uid': self.uid, 'path': mpfs_path, 'meta': ''})
        assert res['meta'].get('photoslice_album_type') == unicode(PhotosliceAlbumTypeConst.CAMERA)

    @parameterized.expand([
        ({'mtime': '1234'}, UploadTypes.upload_common,),
        ({}, UploadTypes.upload_common,),
        ({'device_original_path': '/u0/folder/screenshot/1.mov'}, UploadTypes.upload_common,),
        ({'screenshot': '1'}, UploadTypes.upload_common,),
        ({'device_original_path': '/u0/folder/canera/1.mov'}, UploadTypes.upload_common,),
        ({'mtime': '1234'}, UploadTypes.upload_hardlink_on_store,),
        ({}, UploadTypes.upload_hardlink_on_store,),
        ({'device_original_path': '/u0/folder/screenshot/1.mov'}, UploadTypes.upload_hardlink_on_store,),
        ({'screenshot': '1'}, UploadTypes.upload_hardlink_on_store,),
        ({'device_original_path': '/u0/folder/canera/1.mov'}, UploadTypes.upload_hardlink_on_store,),
        ({'mtime': '1234'}, UploadTypes.upload_hardlink_on_callback,),
        ({}, UploadTypes.upload_hardlink_on_callback,),
        ({'device_original_path': '/u0/folder/screenshot/1.mov'}, UploadTypes.upload_hardlink_on_callback,),
        ({'screenshot': '1'}, UploadTypes.upload_hardlink_on_callback,),
        ({'device_original_path': '/u0/folder/canera/1.mov'}, UploadTypes.upload_hardlink_on_callback,),
    ])
    def test_other_videos_from_new_andr_arent_in_albums(self, opts, upload_type):
        path = '/disk/1.mov'
        self._upload_by_type(
            upload_type, self.uid, path,
            media_type='video', opts=opts, headers={'user-agent': self.user_agent})
        res = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
        assert res['meta'].get('photoslice_album_type') is None
