# -*- coding: utf-8 -*-
import copy

import mock
import pytest
from nose_parameterized import parameterized

from mpfs.common.static import codes
from mpfs.core.photoslice.albums.static import PhotosliceAlbumTypeConst
from test.conftest import INIT_USER_IN_POSTGRES
from test.parallelly.photoslice_album.base import PhotosliceAlbumBaseTestCase, UploadTypes


@pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Tests for PG only')
class PhotosliceAndrOldAlbumTestCase(PhotosliceAlbumBaseTestCase):

    def setup_method(self, method):
        super(PhotosliceAndrOldAlbumTestCase, self).setup_method(method)
        self.user_agent = self.old_andr_user_agent

    @parameterized.expand([
        ({'screenshot': '1'}, UploadTypes.upload_common,),
        ({'screenshot': '1'}, UploadTypes.upload_hardlink_on_store,),
        ({'screenshot': '1'}, UploadTypes.upload_hardlink_on_callback,),
    ])
    def test_upload_screenshot(self, opts, upload_type):
        path = '/disk/1.jpg'
        self._upload_by_type(
            upload_type, self.uid, path, media_type='image', opts=opts, headers={'user-agent': self.user_agent})
        res = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
        assert res['meta'].get('photoslice_album_type') == unicode(PhotosliceAlbumTypeConst.SCREENSHOTS)

    @parameterized.expand([
        ({'etime': '123456789'}, UploadTypes.upload_common,),
        ({'etime': '123456789'}, UploadTypes.upload_hardlink_on_store,),
        ({'etime': '123456789'}, UploadTypes.upload_hardlink_on_callback,),
    ])
    def test_upload_camera_photo(self, opts, upload_type):
        path = '/disk/1.jpg'
        self._upload_by_type(
            upload_type, self.uid, path, media_type='image', opts=opts, headers={'user-agent': self.user_agent})
        res = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
        assert res['meta'].get('photoslice_album_type') == unicode(PhotosliceAlbumTypeConst.CAMERA)

    @parameterized.expand([
        ({'mtime': '1234'}, UploadTypes.upload_common,),
        ({}, UploadTypes.upload_common,),
        ({'screenshot': '0'}, UploadTypes.upload_common,),
        ({'mtime': '1234'}, UploadTypes.upload_hardlink_on_store,),
        ({}, UploadTypes.upload_hardlink_on_store,),
        ({'screenshot': '0'}, UploadTypes.upload_hardlink_on_store,),
        ({'mtime': '1234'}, UploadTypes.upload_hardlink_on_store,),
        ({}, UploadTypes.upload_hardlink_on_store,),
        ({'screenshot': '0'}, UploadTypes.upload_hardlink_on_store,),
    ])
    def test_other_photos_from_new_andr_arent_in_albums(self, opts, upload_type):
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
        ({'etime': '1234'}, UploadTypes.upload_common,),
        ({}, UploadTypes.upload_common,),
        ({'screenshot': '1'}, UploadTypes.upload_common,),
        ({'etime': '1234'}, UploadTypes.upload_hardlink_on_store,),
        ({}, UploadTypes.upload_hardlink_on_store,),
        ({'screenshot': '1'}, UploadTypes.upload_hardlink_on_store,),
        ({'etime': '1234'}, UploadTypes.upload_hardlink_on_store,),
        ({}, UploadTypes.upload_hardlink_on_store,),
        ({'screenshot': '1'}, UploadTypes.upload_hardlink_on_store,),
    ])
    def test_other_videos_from_new_andr_arent_in_albums(self, opts, upload_type):
        path = '/disk/1.mov'
        self._upload_by_type(
            upload_type, self.uid, path, media_type='video', opts=opts, headers={'user-agent': self.user_agent})
        res = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
        assert res['meta'].get('photoslice_album_type') is None
