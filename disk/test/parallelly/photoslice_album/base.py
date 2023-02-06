# -*- coding: utf-8 -*-
import copy
import uuid

import mock
from enum import Enum
from lxml import etree

from mpfs.common.static import tags
from mpfs.core.photoslice.albums.static import PhotosliceAlbumTypeConst
from mpfs.core.user.constants import PHOTOSTREAM_AREA_PATH, PHOTOSTREAM_AREA, PHOTOUNLIM_AREA_PATH
from test.base import CommonDiskTestCase
from test.helpers.stubs.services import KladunStub
from test.parallelly.api.disk.base import DiskApiTestCase
from test.parallelly.json_api.store_suit import StoreUtilsMixin


class UploadTypes(Enum):
    upload_common = 1
    upload_hardlink_on_store = 2
    upload_hardlink_on_callback = 3


class PhotosliceAlbumBaseTestCase(StoreUtilsMixin, CommonDiskTestCase, DiskApiTestCase):
    andr_user_agent = 'Yandex.Disk {"os":"android 9","device":"phone","src":"disk.mobile","vsn":"5.15.0-1013",' \
                      '"id":"3c7caebe5877cbf7fa1e026d77fd5043","flavor":"prod",' \
                      '"uuid":"4cc24a719d7c68a55ccb81bab68954e4"}'
    ios_user_agent = 'Yandex.Disk {"os":"iOS","src":"disk.mobile","vsn":"3.72.17542",' \
                     '"id":"C75791BF-57C4-49BB-8890-2D93B7230D16","device":"phone",' \
                     '"uuid":"4f0f01010c16091dceaf20739fbba0e0"}'
    old_andr_user_agent = 'Yandex.Disk {"os":"android 9","device":"phone","src":"disk.mobile","vsn":"4.15.0-1013",' \
                          '"id":"3c7caebe5877cbf7fa1e026d77fd5043","flavor":"prod",' \
                          '"uuid":"4cc24a719d7c68a55ccb81bab68954e4"}'
    old_ios_user_agent = 'Yandex.Disk {"os":"iOS","src":"disk.mobile","vsn":"2.72.17542",' \
                         '"id":"C75791BF-57C4-49BB-8890-2D93B7230D16","device":"phone",' \
                         '"uuid":"4f0f01010c16091dceaf20739fbba0e0"}'
    user_agent = None

    def _upload_by_type(self, upload_type, uid, path, media_type=None, opts=None, headers=None):
        if upload_type is UploadTypes.upload_common:
            self.__upload_simple(uid, path, media_type=media_type, opts=opts, headers=headers)
        elif upload_type is UploadTypes.upload_hardlink_on_store:
            self.__upload_hardlink_on_store(uid, path, media_type=media_type, opts=opts, headers=headers)
        elif upload_type is UploadTypes.upload_hardlink_on_callback:
            self.__upload_hardlink_on_callback(uid, path, media_type=media_type, opts=opts, headers=headers)
        else:
            raise NotImplementedError('Unknown upload type %s' % upload_type)

    def __upload_simple(self, uid, path, media_type=None, opts=None, headers=None):
        self.upload_file(uid, path, media_type=media_type, opts=copy.copy(opts), headers=headers)

    def __upload_hardlink_on_store(self, uid, path, media_type=None, opts=None, headers=None):
        hardlink_file_path = '/disk/%s.%s' % (uuid.uuid4().hex, path.split('.')[-1])
        self.upload_file(uid, hardlink_file_path, media_type=media_type, opts=copy.copy(opts), headers=headers)
        res = self.json_ok('info', {'uid': uid, 'path': hardlink_file_path, 'meta': ''})
        md5, sha256, size = res['meta']['md5'], res['meta']['sha256'], res['meta']['size']

        qs_params = {
            'uid': uid,
            'md5': md5,
            'sha256': sha256,
            'size': size,
            'path': path,
        }
        qs_params.update(opts)
        with mock.patch('mpfs.core.filesystem.hardlinks.common.AbstractLink.is_file_in_storage', return_value=True), \
                mock.patch('mpfs.core.filesystem.base.Filesystem._check_full_file_upload_process_needed_by_hashes'):
            self.json_ok('store', qs_params, headers=headers)

    def __upload_hardlink_on_callback(self, uid, path, media_type=None, opts=None, headers=None):
        original_file_storage = 'disk'
        if path.startswith(PHOTOSTREAM_AREA_PATH):
            original_file_storage = PHOTOSTREAM_AREA
        hardlink_file_path = '/%s/%s.%s' % (original_file_storage, uuid.uuid4().hex, path.split('.')[-1])
        self.upload_file(uid, hardlink_file_path, media_type=media_type, opts=copy.copy(opts), headers=headers)
        if path.startswith(PHOTOSTREAM_AREA_PATH):
            hardlink_file_path = hardlink_file_path.replace(PHOTOSTREAM_AREA_PATH, PHOTOUNLIM_AREA_PATH)
        res = self.json_ok('info', {'uid': uid, 'path': hardlink_file_path, 'meta': ''})

        resolved_path = path
        if path.startswith(PHOTOSTREAM_AREA_PATH):
            resolved_path = resolved_path.replace(PHOTOSTREAM_AREA_PATH, PHOTOUNLIM_AREA_PATH)
        kladun_callback_params = {
            'uid': uid,
            'md5': res['meta']['md5'],
            'sha256': res['meta']['sha256'],
            'size': int(res['meta']['size']),
            'path': resolved_path,
        }
        body_1, _, _ = self.prepare_kladun_callbacks(**kladun_callback_params)

        qs_params = {
            'uid': uid,
            'path': path,
            'size': int(res['meta']['size'])
        }
        qs_params.update(opts)
        with mock.patch('mpfs.core.filesystem.base.is_photounlim_fraudulent_store', return_value=False):
            oid = self.json_ok('store', qs_params, headers=headers).get('oid')

        with self.patch_mulca_is_file_exist(func_resp=True), \
                KladunStub(status_values=(body_1,)):
            callback_opts = {'uid': self.uid, 'oid': oid, 'status_xml': etree.tostring(body_1), 'type': tags.COMMIT_FILE_INFO}
            self.service_error('kladun_callback', callback_opts)

    def _upload_file_with_photoslice_album_type(self, uid, path, photoslice_album_type=PhotosliceAlbumTypeConst.CAMERA):
        if photoslice_album_type == PhotosliceAlbumTypeConst.CAMERA:
            self.upload_file(uid, path, media_type='image', opts={'etime': 1234},
                             headers={'user-agent': self.andr_user_agent})
        else:
            raise NotImplementedError(
                'Don\'t know how to upload file for this kind of photoslice_album_type %s' % photoslice_album_type)
