# -*- coding: utf-8 -*-
import mock
import pytest
from parameterized import parameterized

from mpfs.core.photoslice.albums.static import PhotosliceAlbumTypeConst
from test.conftest import INIT_USER_IN_POSTGRES
from test.parallelly.photoslice_album.base import PhotosliceAlbumBaseTestCase


@pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Tests for PG only')
class CommonPhotosliceAlbumTestCase(PhotosliceAlbumBaseTestCase):

    @parameterized.expand([
        (PhotosliceAlbumBaseTestCase.andr_user_agent, ),
        (PhotosliceAlbumBaseTestCase.ios_user_agent, ),
        (PhotosliceAlbumBaseTestCase.old_andr_user_agent, ),
        (PhotosliceAlbumBaseTestCase.old_ios_user_agent, ),
    ])
    def test_album_type_cant_be_inherited(self, user_agent):
        hardlink_file_path = '/disk/2.jpg'
        self.upload_file(self.uid, hardlink_file_path, media_type='image',)
        self.json_ok('setprop', {'uid': self.uid, 'path': hardlink_file_path, 'photoslice_album_type': PhotosliceAlbumTypeConst.CAMERA})
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
            self.json_ok('store', qs_params, headers={'user-agent': user_agent})
        res = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
        assert res['meta'].get('photoslice_album_type') is None
