# -*- coding: utf-8 -*-
import copy

import mock
import pytest
from nose_parameterized import parameterized

from mpfs.common.static import codes
from mpfs.core.photoslice.albums.static import PhotosliceAlbumTypeConst
from test.conftest import INIT_USER_IN_POSTGRES
from test.parallelly.image_size_suit import ImageSizeMixin
from test.parallelly.photoslice_album.base import UploadTypes, PhotosliceAlbumBaseTestCase


@pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Tests for PG only')
class PhotosliceIosOldAlbumTestCase(ImageSizeMixin, PhotosliceAlbumBaseTestCase):

    def setup_method(self, method):
        super(PhotosliceIosOldAlbumTestCase, self).setup_method(method)
        self.user_agent = self.old_ios_user_agent

    @parameterized.expand([
        (750, 1334,),
        (1334, 750,),
        (1242, 2208,),
    ])
    def test_upload_screenshot(self, width, height):
        path = '/disk/1.png'
        res = self.json_ok('store', {'uid': self.uid, 'path': path}, headers={'user-agent': self.user_agent})
        oid = res['oid']
        self._make_kladun_callbacks_with_image_size(self.uid, oid, width, height, 0)
        res = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
        assert res['meta'].get('photoslice_album_type') == unicode(PhotosliceAlbumTypeConst.SCREENSHOTS)

    def test_hardlink_for_screenshots_sets_as_screenshot(self):
        hardlink_file_path = '/disk/2.png'
        self.upload_file(self.uid, hardlink_file_path, media_type='image', opts={})
        self.json_ok('setprop', {'uid': self.uid, 'path': hardlink_file_path, 'photoslice_album_type': PhotosliceAlbumTypeConst.SCREENSHOTS})
        res = self.json_ok('info', {'uid': self.uid, 'path': hardlink_file_path, 'meta': ''})

        path = '/disk/1.png'
        qs_params = {
            'uid': self.uid,
            'md5': res['meta']['md5'],
            'sha256': res['meta']['sha256'],
            'size': res['meta']['size'],
            'path': path,
        }
        with mock.patch('mpfs.core.filesystem.hardlinks.common.AbstractLink.is_file_in_storage', return_value=True):
            self.json_ok('store', qs_params, headers={'user-agent': self.user_agent})
        res = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
        assert res['meta'].get('photoslice_album_type') == unicode(PhotosliceAlbumTypeConst.SCREENSHOTS)

    @parameterized.expand([
        ('/disk/1.jpg', 750, 1334,),
        ('/disk/1.png', 751, 1334,),
        ('/disk/1.png', 750, 1335,),
    ])
    def test_specific_not_screenshot(self, path, width, height):
        res = self.json_ok('store', {'uid': self.uid, 'path': path}, headers={'user-agent': self.user_agent})
        oid = res['oid']
        self._make_kladun_callbacks_with_image_size(self.uid, oid, width, height, 0)
        res = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
        assert res['meta'].get('photoslice_album_type') is None

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
        ({'screenshot': '1'}, UploadTypes.upload_common,),
        ({'mtime': '1234'}, UploadTypes.upload_hardlink_on_store,),
        ({}, UploadTypes.upload_hardlink_on_store,),
        ({'device_original_path': '/u0/folder/screenshot/1.jpg'}, UploadTypes.upload_hardlink_on_store,),
        ({'screenshot': '0'}, UploadTypes.upload_hardlink_on_store,),
        ({'screenshot': '1'}, UploadTypes.upload_hardlink_on_store,),
        ({'mtime': '1234'}, UploadTypes.upload_hardlink_on_callback,),
        ({}, UploadTypes.upload_hardlink_on_callback,),
        ({'device_original_path': '/u0/folder/screenshot/1.jpg'}, UploadTypes.upload_hardlink_on_callback,),
        ({'screenshot': '0'}, UploadTypes.upload_hardlink_on_callback,),
        ({'screenshot': '1'}, UploadTypes.upload_hardlink_on_callback,),

    ])
    def test_other_photos_arent_in_albums(self, opts, upload_type):
        path = '/disk/1.jpg'
        self._upload_by_type(
            upload_type, self.uid, path, media_type='image', opts=opts, headers={'user-agent': self.user_agent})
        res = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
        assert res['meta'].get('photoslice_album_type') is None

    @parameterized.expand([
        ({'etime': '1234'}, UploadTypes.upload_common,),
        ({}, UploadTypes.upload_common,),
    ])
    def test_upload_camera_video(self, opts, upload_type):
        path = '/photostream/1.mov'
        read_path = '/disk/Фотокамера/1.mov'
        self._upload_by_type(
            upload_type, self.uid, path, media_type='video', opts=opts, headers={'user-agent': self.user_agent})
        res = self.json_ok('info', {'uid': self.uid, 'path': read_path, 'meta': ''})
        assert res['meta'].get('photoslice_album_type') == unicode(PhotosliceAlbumTypeConst.CAMERA)

    @parameterized.expand([
        ({'etime': '1234'},),
        ({},),
    ])
    def test_upload_camera_video_hardlinked(self, opts):
        path = '/photostream/1.mov'
        hardlink_file_path = '/disk/2.mov'
        headers = {'user-agent': self.user_agent}
        self.upload_file(self.uid, hardlink_file_path, media_type='video', opts=copy.copy(opts), headers=headers)
        res = self.json_ok('info', {'uid': self.uid, 'path': hardlink_file_path, 'meta': ''})
        md5, sha256, size = res['meta']['md5'], res['meta']['sha256'], res['meta']['size']

        qs_params = {
            'uid': self.uid,
            'md5': md5,
            'sha256': sha256,
            'size': size,
            'path': path,
        }
        qs_params.update(opts)
        with mock.patch('mpfs.core.filesystem.hardlinks.common.AbstractLink.is_file_in_storage', return_value=True):
            self.json_error('store', qs_params, headers=headers, code=codes.FILE_EXISTS)

    @parameterized.expand([
        ({'mtime': '1234'}, UploadTypes.upload_common,),
        ({}, UploadTypes.upload_common,),
        ({'screenshot': '1'}, UploadTypes.upload_common,),
        ({'etime': '1234'}, UploadTypes.upload_common,),
        ({'mtime': '1234'}, UploadTypes.upload_hardlink_on_store,),
        ({}, UploadTypes.upload_hardlink_on_store,),
        ({'screenshot': '1'}, UploadTypes.upload_hardlink_on_store,),
        ({'etime': '1234'}, UploadTypes.upload_hardlink_on_store,),
        ({'mtime': '1234'}, UploadTypes.upload_hardlink_on_callback,),
        ({}, UploadTypes.upload_hardlink_on_callback,),
        ({'screenshot': '1'}, UploadTypes.upload_hardlink_on_callback,),
        ({'etime': '1234'}, UploadTypes.upload_hardlink_on_callback,),
    ])
    def test_other_videos_arent_in_albums(self, opts, upload_type):
        path = '/disk/1.mov'
        self._upload_by_type(
            upload_type, self.uid, path, media_type='video', opts=opts, headers={'user-agent': self.user_agent})
        res = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
        assert res['meta'].get('photoslice_album_type') is None
