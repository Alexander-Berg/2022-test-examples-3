# -*- coding: utf-8 -*-
import cjson
import re
import pytest

from mpfs.config import settings

from datetime import datetime
from mock import patch
from nose_parameterized import parameterized

from test.base import DiskTestCase, time_machine
from filesystem.base import CommonFilesystemTestCase
from test.base_suit import SharingTestCaseMixin
from test.helpers import products
from test.helpers.utils import DEVNULL_EMAIL
from test.helpers.stubs.services import ClckStub
from test.conftest import REAL_MONGO, INIT_USER_IN_POSTGRES
from test.parallelly.billing.base import BaseBillingTestCase
from test.common.sharing import CommonSharingMethods

from albums_suit import AlbumsBaseTestCase
from mpfs.common.static import tags, codes
from mpfs.common.static.tags.billing import *
from mpfs.common.static.tags.billing import RU_RU, EN_EN, UK_UA, TR_TR
from mpfs.common.util import ctimestamp
from mpfs.core import base as core
from mpfs.core.address import Address
from mpfs.core.billing.product import Product
from mpfs.core.bus import Bus
from mpfs.core.operations import manager
from mpfs.core.operations.social import PostAlbumToSocials
from mpfs.core.services.passport_service import Passport
from mpfs.core.services.socialproxy_service import SocialProxy
from mpfs.core.social.publicator import Publicator
from mpfs.invite import manager as invite_manager
from test.parallelly.times_suit import fake_time_with_increment

_not_none = object()
_missing = object()


class EventHistoryTestCase(DiskTestCase):
    dir_rawaddress = DiskTestCase.uid + ':/disk/dir'
    tgt_dir_rawaddress = dir_rawaddress + '_new'
    trash_dir_rawaddress = DiskTestCase.uid + ':/trash/dir'
    subdir_rawaddress = DiskTestCase.uid + ':/disk/dir/subdir'
    sub_subdir_rawaddress = DiskTestCase.uid + ':/disk/dir/subdir/subdir'

    def setup_method(self, method):
        super(EventHistoryTestCase, self).setup_method(method)
        from mpfs.core.event_history.logger import _log_raw_event_message
        patcher = patch('mpfs.core.event_history.logger._log_raw_event_message', wraps=_log_raw_event_message)
        self.mock_log = patcher.start()
        self.addCleanup(patcher.stop)

    def reset_mock_log(self):
        self.mock_log.reset_mock()

    def assert_logged_multiple(self, event_type, common_kwargs, *kwargs_list):
        self.assert_logged_count(len(kwargs_list))
        self.assert_logged_multiple_offset(0, event_type, common_kwargs, *kwargs_list)

    def assert_logged_multiple_offset(self, base_offset, event_type, common_kwargs, *kwargs_list):
        length = len(kwargs_list)
        for i in range(length):
            offset = i + 1 - length + base_offset
            all_kwargs = common_kwargs.copy()
            all_kwargs.update(kwargs_list[i])
            self.assert_logged_offset(offset, event_type, **all_kwargs)

    def assert_logged_single(self, expected_type, uid=None, **expected_kwargs):
        self.assert_logged_count(1)
        self.assert_logged(expected_type, uid=uid, **expected_kwargs)

    def assert_logged_count(self, expected_count):
        actual_count = len(self.mock_log.call_args_list)
        call_args_list = [self.extract_logged_arguments(call_args) for call_args in self.mock_log.call_args_list]
        self.assertEquals(expected_count, actual_count, 'Expected %s log lines, got %s:\n%s' %
                          (expected_count, actual_count, call_args_list))

    def assert_logged(self, expected_type, uid=None, **expected_kwargs):
        self.assert_logged_offset(0, expected_type, uid=uid, **expected_kwargs)

    def assert_logged_offset(self, offset, expected_type, uid=None, **expected_kwargs):
        self.assertTrue(self.mock_log.called, 'Nothing was logged')

        index = len(self.mock_log.call_args_list) + offset - 1
        actual_event_type, actual_kwargs = self.extract_logged_arguments(self.mock_log.call_args_list[index])

        expected_not_none_keys = []
        expected_missing_keys = []
        for key in expected_kwargs.keys():
            if expected_kwargs[key] is _missing:
                expected_kwargs.pop(key)
                expected_missing_keys.append(key)
            elif expected_kwargs[key] is _not_none:
                expected_kwargs.pop(key)
                expected_not_none_keys.append(key)

        expected_kwargs['uid'] = uid
        # В логах все является строками, поэтому преобразовываем ожидаемые значения в строки
        expected_kwargs = {key: unicode(value)
                           for key, value in expected_kwargs.iteritems()}

        try:
            self.assertIsNotNone(uid, "UID must be specified for every event")
            self.assertEqual(expected_type, actual_event_type)
            self.assertDictContainsSubset(expected_kwargs, actual_kwargs)

            forbidden_keys = actual_kwargs.viewkeys() & set(expected_missing_keys)
            forbidden_kwargs = {key: actual_kwargs[key] for key in forbidden_keys}
            self.assertFalse(forbidden_keys, 'Log must not contain following data: %s' % forbidden_kwargs)

            missing_keys = []
            none_keys = []
            for key in expected_not_none_keys:
                if key not in actual_kwargs:
                    missing_keys.append(key)
                elif actual_kwargs[key] is None:
                    none_keys.append(key)

            fail_msg = 'Keys are missing or None: missing = %s, none = %s' % (missing_keys, none_keys)
            self.assertFalse(missing_keys + none_keys, fail_msg)
        except AssertionError as e:
            if len(self.mock_log.call_args_list) > 1:
                raise AssertionError('at offset %s: %s' % (offset, e.message))
            else:
                raise

    def mkdir(self, address):
        Bus().mkdir(self.uid, address)
        uid, path = address.split(':')
        response = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': 'file_id'})
        return '%s:%s' % (self.uid, response['meta']['file_id'])

    @staticmethod
    def extract_logged_arguments(call_args):
        args, kwargs = call_args
        if 'message' in kwargs:
            message = kwargs['message']
        else:
            (message,) = args
        # удаляем префикс строки "tskv"
        pairs = message.split('\t', 1)[1]
        log_raw_data_dict = {}
        for kv in pairs.split('\t'):
            key, value = re.split(r'(?<!\\)=', kv, 1)
            log_raw_data_dict[key] = value

        event_type = log_raw_data_dict['event_type']
        return event_type, log_raw_data_dict

    def invite_and_activate(self, uid, path):
        args = {
            'rights': 660,
            'universe_login': 'mpfs-test-%s@yandex.ru' % uid,
            'universe_service': 'email',
            'avatar': 'http://localhost/echo',
            'name': 'mpfs-test',
            'connection_id': '1234',
            'uid': self.uid,
            'path': path
        }
        invite_result = self.json_ok('share_invite_user', args)
        self.json_ok('share_activate_invite', {'uid': uid, 'hash': invite_result['hash']})


class FsEventHistoryTestCase(EventHistoryTestCase, CommonFilesystemTestCase):
    trash_address = Address.Make(DiskTestCase.uid, '/trash/')
    video_file_address = Address.Make(DiskTestCase.uid, '/disk/video.mkv')
    image_file_address = Address.Make(DiskTestCase.uid, '/disk/image.png')
    trash_image_file_address = Address.Make(DiskTestCase.uid, '/trash/image.png')
    photounlim_image_file_address = Address.Make(DiskTestCase.uid, '/photounlim/image.png')
    font_file_address = Address.Make(DiskTestCase.uid, '/disk/font.ttf')
    tgt_image_file_address = Address.Make(DiskTestCase.uid, '/disk/image_new.png')

    def _mocked_trash_append(self, path):
        with patch('mpfs.core.address.Address.add_trash_suffix'):
            Bus().trash_append(self.uid, path)

    def test_mkdir(self):
        Bus().mkdir(self.uid, self.dir_rawaddress)
        self.assert_logged_single('fs-mkdir', uid=self.uid, owner_uid=self.uid, tgt_rawaddress=self.dir_rawaddress,
                                  resource_type='dir', resource_file_id=_not_none, tgt_folder_id=_not_none)

    def test_mksysdir(self):
        Bus().mksysdir(self.uid, 'fotki')
        self.assert_logged_single('fs-mksysdir', uid=self.uid, type='fotki')

    def test_copy_resource(self):
        Bus().mkdir(self.uid, self.dir_rawaddress)
        self.reset_mock_log()

        Bus().copy_resource(self.uid, self.dir_rawaddress, self.tgt_dir_rawaddress, False)

        self.assert_logged_single('fs-copy', uid=self.uid, owner_uid=self.uid, force=False, overwritten=False,
                                  resource_type='dir', resource_file_id=_not_none,
                                  src_folder_id=_not_none, tgt_folder_id=_not_none,
                                  src_rawaddress=self.dir_rawaddress, tgt_rawaddress=self.tgt_dir_rawaddress)

    def test_copy_resource_with_overwrite(self):
        self.upload_file(self.uid, self.image_file_address.path)
        self.upload_file(self.uid, self.tgt_image_file_address.path)
        self.reset_mock_log()

        Bus().copy_resource(self.uid, self.image_file_address.id, self.tgt_image_file_address.id, True)

        self.assert_logged_single('fs-copy', uid=self.uid, owner_uid=self.uid, force=True, overwritten=True,
                                  resource_type='file', resource_file_id=_not_none,
                                  src_folder_id=_not_none, tgt_folder_id=_not_none,
                                  src_rawaddress=self.image_file_address.id,
                                  tgt_rawaddress=self.tgt_image_file_address.id)

    def test_move_resource(self):
        Bus().mkdir(self.uid, self.dir_rawaddress)
        self.reset_mock_log()

        Bus().move_resource(self.uid, self.dir_rawaddress, self.tgt_dir_rawaddress, False)

        self.assert_logged_single('fs-move', uid=self.uid, owner_uid=self.uid, force=False, overwritten=False,
                                  resource_type='dir', resource_file_id=_not_none,
                                  src_folder_id=_not_none, tgt_folder_id=_not_none,
                                  src_rawaddress=self.dir_rawaddress, tgt_rawaddress=self.tgt_dir_rawaddress)

    def test_move_resource_with_overwrite(self):
        self.upload_file(self.uid, self.image_file_address.path)
        self.upload_file(self.uid, self.tgt_image_file_address.path)
        self.reset_mock_log()

        Bus().move_resource(self.uid, self.image_file_address.id, self.tgt_image_file_address.id, True)

        self.assert_logged_single('fs-move', uid=self.uid, owner_uid=self.uid, force=True, overwritten=True,
                                  resource_type='file', resource_file_id=_not_none,
                                  src_folder_id=_not_none, tgt_folder_id=_not_none,
                                  src_rawaddress=self.image_file_address.id,
                                  tgt_rawaddress=self.tgt_image_file_address.id)

    def test_move_resource_with_int_force(self):
        Bus().mkdir(self.uid, self.dir_rawaddress)
        self.reset_mock_log()

        Bus().move_resource(self.uid, self.dir_rawaddress, self.tgt_dir_rawaddress, 1)

        self.assert_logged_single('fs-move', uid=self.uid, owner_uid=self.uid, force=True, overwritten=False,
                                  resource_type='dir', resource_file_id=_not_none,
                                  src_folder_id=_not_none, tgt_folder_id=_not_none,
                                  src_rawaddress=self.dir_rawaddress, tgt_rawaddress=self.tgt_dir_rawaddress)

    def test_rm(self):
        Bus().mkdir(self.uid, self.dir_rawaddress)
        self.reset_mock_log()

        Bus().rm(self.uid, self.dir_rawaddress)

        self.assert_logged_single('fs-rm', uid=self.uid, resource_type='dir', resource_file_id=_not_none,
                                  tgt_folder_id=_not_none, tgt_rawaddress=self.dir_rawaddress)

    def test_rm_recursively(self):
        dir_id = self.mkdir(self.dir_rawaddress)
        self.mkdir(self.subdir_rawaddress)
        self.mkdir(self.sub_subdir_rawaddress)

        self.reset_mock_log()

        Bus().rm(self.uid, self.dir_rawaddress)

        self.assert_logged_offset(-2, 'fs-rm', uid=self.uid)

        self.assert_logged_multiple_offset(+0, 'fs-delete-subdir', {'uid': self.uid},
                                           {'resource_id': dir_id})

    def test_trash_append(self):
        Bus().mkdir(self.uid, self.dir_rawaddress)
        self.reset_mock_log()

        self._mocked_trash_append(self.dir_rawaddress)

        self.assert_logged_single('fs-trash-append', uid=self.uid, owner_uid=self.uid,
                                  resource_type='dir', resource_file_id=_not_none,
                                  src_folder_id=_not_none, tgt_folder_id=_not_none,
                                  src_rawaddress=self.dir_rawaddress, tgt_rawaddress=self.trash_dir_rawaddress)

    def test_trash_append_recursively(self):
        dir_id = self.mkdir(self.dir_rawaddress)
        self.mkdir(self.subdir_rawaddress)
        self.mkdir(self.sub_subdir_rawaddress)

        self.reset_mock_log()

        Bus().trash_append(self.uid, self.dir_rawaddress)

        self.assert_logged_offset(-2, 'fs-trash-append', uid=self.uid)

        self.assert_logged_multiple_offset(+0, 'fs-delete-subdir', {'uid': self.uid},
                                           {'resource_id': dir_id})

    def test_trash_drop_element(self):
        Bus().mkdir(self.uid, self.dir_rawaddress)
        self._mocked_trash_append(self.dir_rawaddress)
        self.reset_mock_log()

        Bus().trash_drop_element(self.uid, self.trash_dir_rawaddress)

        self.assert_logged_single('fs-trash-drop', uid=self.uid, owner_uid=self.uid, resource_type='dir',
                                  resource_file_id=_not_none, tgt_folder_id=_not_none,
                                  tgt_rawaddress=self.trash_dir_rawaddress, auto=False)

    def test_clean_trash(self):
        self.upload_file(self.uid, self.image_file_address.path)
        self._mocked_trash_append(self.image_file_address.id)
        self.reset_mock_log()

        Bus().clean_trash(self.uid, period=-1)
        self.assert_logged_single('fs-trash-drop', uid=self.uid, owner_uid=self.uid, auto=True, resource_type='file',
                                  resource_file_id=_not_none, tgt_folder_id=_not_none,
                                  tgt_rawaddress=self.image_file_address.clone_to_parent(self.trash_address).id)

    def test_limit_clean_trash(self):

        for idx in range(10):
            path = '/disk/file_test%s' % idx
            self.upload_file(self.uid, path)
            self.json_ok('trash_append', {'uid': self.uid, 'path': path})
        for idx in range(10):
            path = '/disk/folder_test%s' % idx
            self.json_ok('mkdir', {'uid': self.uid, 'path': path})
            self.json_ok('trash_append', {'uid': self.uid, 'path': path})

        self.reset_mock_log()

        with patch.dict(settings.system['system'], {'trash_autoclean_resource_limit': 19}):
            Bus().clean_trash(self.uid, period=-1)

        # лог должен быть пустым, поскольку операций не выполнялось
        self.assert_logged_count(0)

        # в конзине должно остаться 20 элементов + список возвращает саму корзину
        assert len(self.json_ok('list', {'uid': self.uid, 'path': '/trash'})) == 21

    def test_trash_drop_all(self):
        Bus().trash_drop_all(self.uid, self.uid)
        self.assert_logged_single('fs-trash-drop-all', uid=self.uid, owner=self.uid)

    def test_trash_restore(self):
        Bus().mkdir(self.uid, self.dir_rawaddress)
        self._mocked_trash_append(self.dir_rawaddress)
        self.reset_mock_log()

        Bus().trash_restore(self.uid, self.trash_dir_rawaddress)

        self.assert_logged_single('fs-trash-restore', uid=self.uid, owner_uid=self.uid,
                                  resource_type='dir', resource_file_id=_not_none,
                                  type='trash_restore', subtype='disk',
                                  src_folder_id=_not_none, tgt_folder_id=_not_none,
                                  src_rawaddress=self.trash_dir_rawaddress, tgt_rawaddress=self.dir_rawaddress)

    @parameterized.expand([
        ('store', 'fs-store', _missing, _missing, _not_none, 'file', 1356048000, _missing, _missing),
        ('move', 'fs-move', False, False, _not_none, 'file', 1356048000, _not_none, _not_none),
        ('copy', 'fs-copy', False, False, _not_none, 'file', 1356060000, _not_none, _not_none),
        ('trash_append', 'fs-trash-append', _missing, _missing, _not_none, 'file', 1356048000, _not_none, _not_none),
        ('trash_drop', 'fs-trash-drop', _missing, _missing, _not_none, 'file', 1356048000, _missing, _missing),
        ('rm', 'fs-rm', _missing, _missing, _not_none, 'file', 1356048000, _missing, _missing),
        ('restore', 'fs-trash-restore', _missing, _missing, _not_none, 'file', 1356048000, _not_none, _not_none),
    ])
    def test_logging_timestamps_on_resource_operations(self, action, fs_event, correct_force, correct_overwritten,
                                                       correct_owner_uid, correct_resource_type, correct_tgt_ctime,
                                                       correct_src_folder_id, correct_src_rawaddress):
        self.reset_mock_log()
        timestamp_1 = 1356048000
        with time_machine(datetime.fromtimestamp(timestamp_1)):
            self.upload_file(self.uid, self.image_file_address.path, opts={'etime': str(timestamp_1)})

        timestamp_2 = 1356060000
        with time_machine(datetime.fromtimestamp(timestamp_2)):
            if action == 'store':
                pass
            elif action == 'move':
                Bus().move_resource(self.uid, self.image_file_address.id, self.tgt_image_file_address.id, False)
            elif action == 'copy':
                Bus().copy_resource(self.uid, self.image_file_address.id, self.tgt_image_file_address.id, False)
            elif action == 'trash_append':
                self._mocked_trash_append(self.image_file_address.id)
            elif action == 'trash_drop':
                self._mocked_trash_append(self.image_file_address.id)
                self.reset_mock_log()
                Bus().trash_drop_element(self.uid, self.trash_image_file_address.id)
            elif action == 'rm':
                Bus().rm(self.uid, self.image_file_address.id)
            elif action == 'restore':
                self._mocked_trash_append(self.image_file_address.id)
                self.reset_mock_log()
                Bus().trash_restore(self.uid, self.trash_image_file_address.id)
            else:
                raise NotImplementedError()

        self.assert_logged(fs_event, uid=self.uid, owner_uid=correct_owner_uid, force=correct_force,
                           overwritten=correct_overwritten,
                           resource_type=correct_resource_type, resource_file_id=_not_none, src_folder_id=correct_src_folder_id,
                           tgt_folder_id=_not_none, src_rawaddress=correct_src_rawaddress, tgt_rawaddress=_not_none,
                           tgt_ctime=correct_tgt_ctime, tgt_utime=timestamp_1, tgt_etime=timestamp_1)

    def test_store(self):
        self.upload_file(self.uid, self.video_file_address.path, media_type='video')
        self.assert_logged_single('fs-store', uid=self.uid, owner_uid=self.uid, type='store', subtype='disk',
                                  resource_type='file', resource_media_type='video',
                                  lenta_media_type='video',
                                  resource_file_id=_not_none, tgt_folder_id=_not_none,
                                  tgt_rawaddress=self.video_file_address.id, changes='')

    def test_store_to_photounlim(self):
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
        self.upload_file(self.uid, self.photounlim_image_file_address.path, media_type='image')
        self.assert_logged_single('fs-store', uid=self.uid, owner_uid=self.uid, type='store', subtype='photounlim',
                                  resource_type='file', resource_media_type='image',
                                  lenta_media_type='image',
                                  resource_file_id=_not_none, tgt_folder_id=_not_none,
                                  tgt_rawaddress=self.photounlim_image_file_address.id, changes='')

    def test_store_screenshot(self):
        opts = {'public': '1', 'screenshot': '1'}
        self.upload_file(self.uid, self.image_file_address.path, media_type='image', opts=opts)
        self.assert_logged_offset(-1, 'fs-set-public', uid=self.uid, type='store', subtype='disk',
                                  tgt_rawaddress=_not_none)
        self.assert_logged_offset(+0, 'fs-store', uid=self.uid, owner_uid=self.uid, type='store', subtype='disk',
                                  resource_type='file', resource_media_type='image',
                                  lenta_media_type='image',
                                  resource_file_id=_not_none, tgt_folder_id=_not_none,
                                  tgt_rawaddress=self.image_file_address.id,
                                  changes='public,screenshot', public_key=_not_none, short_url=_not_none)

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Postgres test')
    def test_hardlink_copy(self):
        Bus().mkfile(self.uid, self.uid + ':/disk/file.txt', data=self.file_data)
        self.reset_mock_log()

        copy_address = Address.Make(self.uid, '/disk/file_hl_copy.txt')
        Bus().hardlink_copy(
            self.uid,
            copy_address.id,
            self.file_data['meta']['md5'],
            self.file_data['size'],
            self.file_data['meta']['sha256'],
            oper_type='store'
        )

        self.assert_logged_single('fs-hardlink-copy', uid=self.uid, owner_uid=self.uid, type='store', subtype='disk',
                                  resource_type='file', resource_media_type='document',
                                  lenta_media_type='document',
                                  resource_file_id=_not_none, tgt_folder_id=_not_none,
                                  tgt_rawaddress=copy_address.id, changes='')

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Postgres test')
    def test_hardlink_copy_public(self):
        Bus().mkfile(self.uid, self.uid + ':/disk/file.txt', data=self.file_data)
        self.reset_mock_log()

        copy_address = Address.Make(self.uid, '/disk/file_hl_copy.txt')
        Bus().store(
                self.uid,
                copy_address.id,
                md5=self.file_data['meta']['md5'],
                size=self.file_data['size'],
                sha256=self.file_data['meta']['sha256'],
                changes={'public': '1'}
        )

        self.assert_logged_offset(-1, 'fs-set-public', uid=self.uid, type='store', subtype='disk',
                                  tgt_rawaddress=_not_none)
        self.assert_logged_offset(+0, 'fs-hardlink-copy', uid=self.uid, owner_uid=self.uid,
                                  type='store', subtype='disk',
                                  resource_type='file', resource_media_type='document',
                                  resource_file_id=_not_none, tgt_folder_id=_not_none,
                                  tgt_rawaddress=copy_address.id, changes='public',
                                  public_key=_not_none, short_url=_not_none)

    def test_dstore(self):
        self.upload_file(self.uid, self.font_file_address.path, media_type='font')
        self.reset_mock_log()

        self.dstore_file(self.uid, self.font_file_address.path)

        self.assert_logged_single('fs-store', uid=self.uid, owner_uid=self.uid, type='dstore', subtype='disk',
                                  resource_type='file', resource_media_type='font',
                                  lenta_media_type='other',
                                  resource_file_id=_not_none, tgt_folder_id=_not_none,
                                  tgt_rawaddress=self.font_file_address.id)

    def test_bulk(self):
        Bus().mkdir(self.uid, self.dir_rawaddress)
        bulk_action = cjson.encode(
            [
                {
                    'action': 'copy',
                    'params': {
                        'uid': self.uid,
                        'src': self.dir_rawaddress,
                        'dst': self.dir_rawaddress + '_copied',
                        'force': None,
                    }
                },
                {
                    'action': 'move',
                    'params': {
                        'uid': long(self.uid),
                        'src': self.dir_rawaddress,
                        'dst': self.dir_rawaddress + '_moved',
                        'force': None,
                    }
                },
                {
                    'action': 'rm',
                    'params': {
                        'uid': self.uid,
                        'path': self.dir_rawaddress + '_copied',
                    }
                },
                {
                    'action': 'rm',
                    'params': {
                        'uid': self.uid,
                        'path': self.dir_rawaddress + '_moved',
                    }
                },
            ]
        )

        operation = manager.create_operation(
            self.uid,
            'bulk',
            'filesystem',
            odata=dict(cmd=bulk_action),
        )
        operation = manager.get_operation(operation.uid, operation.id)
        result = operation.get_status()
        self.assertEqual(result[tags.STATUS], codes.COMPLETED)

        for item in result[tags.PROTOCOL]:
            if item[tags.STATUS] != 'DONE':
                raise ValueError('operation "%s" failed with status "%s"\n:%s' % (item[tags.TYPE], item[tags.STATUS],
                                                                                  item[tags.RESULT]))

        self.assert_logged_offset(-3, 'fs-copy', uid=self.uid, force=False,
                                  resource_type='dir', resource_file_id=_not_none,
                                  src_folder_id=_not_none, tgt_folder_id=_not_none,
                                  src_rawaddress=self.dir_rawaddress, tgt_rawaddress=self.dir_rawaddress + '_copied')
        self.assert_logged_offset(-2, 'fs-move', uid=self.uid, force=False,
                                  resource_type='dir', resource_file_id=_not_none,
                                  src_folder_id=_not_none, tgt_folder_id=_not_none,
                                  src_rawaddress=self.dir_rawaddress, tgt_rawaddress=self.dir_rawaddress + '_moved')
        self.assert_logged_offset(-1, 'fs-rm', uid=self.uid, resource_file_id=_not_none,
                                  tgt_folder_id=_not_none, src_folder_id=_missing,
                                  tgt_rawaddress=self.dir_rawaddress + '_copied')
        self.assert_logged_offset(+0, 'fs-rm', uid=self.uid, resource_file_id=_not_none,
                                  tgt_folder_id=_not_none, src_folder_id=_missing,
                                  tgt_rawaddress=self.dir_rawaddress + '_moved')


class PublicationEventHistoryTestCase(EventHistoryTestCase):
    uid_2 = '4001210263'

    def setup_method(self, method):
        super(PublicationEventHistoryTestCase, self).setup_method(method)
        self.create_user(uid=self.uid_2)
        Bus().mkdir(self.uid, self.dir_rawaddress)
        self.reset_mock_log()

    def test_set_public(self):
        Publicator().set_public(self.uid, self.dir_rawaddress)

        self.assert_logged_single('fs-set-public', uid=self.uid, owner_uid=self.uid,
                                  resource_type='dir', resource_file_id=_not_none,
                                  tgt_rawaddress=self.dir_rawaddress, tgt_folder_id=_not_none,
                                  public_key=_not_none, short_url=_not_none)

    def test_set_public_already_public_resource(self):
        for _ in xrange(3):
            Publicator().set_public(self.uid, self.dir_rawaddress)

        self.assert_logged_single('fs-set-public', uid=self.uid, owner_uid=self.uid,
                                  resource_type='dir', resource_file_id=_not_none,
                                  tgt_rawaddress=self.dir_rawaddress, tgt_folder_id=_not_none,
                                  public_key=_not_none, short_url=_not_none)

    def test_set_private(self):
        Publicator().set_public(self.uid, self.dir_rawaddress)
        self.reset_mock_log()

        Publicator().set_private(self.uid, self.dir_rawaddress)

        self.assert_logged_single('fs-set-private', uid=self.uid, owner_uid=self.uid,
                                  resource_type='dir', resource_file_id=_not_none,
                                  tgt_rawaddress=self.dir_rawaddress, tgt_folder_id=_not_none)

    def test_public_copy(self):
        result = Publicator().set_public(self.uid, self.dir_rawaddress)
        Bus().mksysdir(self.uid, 'downloads')
        self.reset_mock_log()

        Publicator().grab(self.uid_2, result['hash'], 'dir_grabbed')

        self.assert_logged('fs-copy', uid=self.uid_2, owner_uid=self.uid_2, resource_file_id=_not_none,
                           src_rawaddress=self.dir_rawaddress,
                           tgt_rawaddress=self.uid_2 + u':/disk/Загрузки/dir_grabbed',
                           src_folder_id=_not_none, tgt_folder_id=_not_none)


class FsGroupEventHistoryTestCase(EventHistoryTestCase):
    uid_1 = '198311081'
    uid_2 = '198311082'

    common_group = {
        EventHistoryTestCase.uid: Address.Make(EventHistoryTestCase.uid, '/disk/share/', type='dir'),
        uid_1: Address.Make(uid_1, '/disk/dir1/share/', type='dir'),
        uid_2: Address.Make(uid_2, '/disk/dir2/share/', type='dir')
    }
    common_file = {uid: address.get_child_file('file.txt')
                   for uid, address in common_group.items()}
    renamed_common_file = {uid: address.get_child_file('file_renamed.txt')
                           for uid, address in common_group.items()}

    common_dir = {uid: address.get_child_folder('dir')
                  for uid, address in common_group.items()}
    common_subdir = {uid: address.get_child_folder('subdir')
                     for uid, address in common_dir.items()}

    share1_group = {uid: Address.Make(uid, '/disk/share1/', type='dir')
                    for uid in [EventHistoryTestCase.uid, uid_1]}
    share1_file = {uid: address.get_child_file('file.txt')
                   for uid, address in share1_group.items()}

    share2_group = {uid: Address.Make(uid, '/disk/share2/', type='dir')
                    for uid in [EventHistoryTestCase.uid, uid_2]}
    share2_file = {uid: address.get_child_file('file.txt')
                   for uid, address in share2_group.items()}

    trash_file = {uid: Address.Make(uid, '/trash/file.txt', type='file')
                  for uid in [EventHistoryTestCase.uid, uid_1, uid_2]}

    folder_ids = {}

    def setup_method(self, method):
        super(FsGroupEventHistoryTestCase, self).setup_method(method)
        self.create_user(self.uid_1)
        self.create_user(self.uid_2)

    def load_folder_ids(self, add=None):
        r = {}
        file_ids = set()
        lst = (
            list(self.common_group.iteritems())
            + list(self.share1_group.iteritems())
            + list(self.share2_group.iteritems())
        )
        if add:
            for d in add:
                lst.extend(d.iteritems())
        for uid, address in lst:
            response = self.json_ok('info', {'uid': uid, 'path': address.path, 'meta': 'file_id'})
            file_id = response['meta']['file_id']
            r[(uid, address.path)] = '%s:%s' % (uid, file_id)
            file_ids.add(file_id)
        return r

    def test_public_copy_root_folder(self):
        """Для публичного копирования рутовой ОП должен писаться `src_folder_id`
        """
        folder_path = '/disk/test_folder'
        self.json_ok('mkdir', {'uid': self.uid_1, 'path': folder_path})
        folder_resource_id = self.json_ok('info', {'uid': self.uid_1, 'path': folder_path, 'meta': 'resource_id'})['meta']['resource_id']
        self.json_ok('share_create_group', {'uid': self.uid_1, 'path': folder_path})
        public_hash = self.json_ok('set_public', {'uid': self.uid_1, 'path': folder_path})['hash']

        self.mock_log.reset_mock()
        self.json_ok('async_public_copy', {'uid': self.uid_2, 'private_hash': public_hash})
        args, kwargs = self.mock_log.call_args_list[-1]
        assert 'src_folder_id=%s' % folder_resource_id in args[0]

    def test_mkdir(self):
        """
        Владелец общей папки с 2 приглашенными и подтвердившими участие пользователями создает
        в корне расшаренной папке подпапку.
        """
        self._create_several_groups_with_users()

        # владелец общей папки /disk/share/ создает в ней директорию dir
        new_dir_path = self.common_group[self.uid].id + 'dir'
        Bus().mkdir(self.uid, new_dir_path)
        response = self.json_ok('info', {'uid': self.uid, 'path': new_dir_path, 'meta': 'file_id'})
        new_dir_folder_id = '%s:%s' % (self.uid, response['meta']['file_id'])

        # тк в этой группе (/disk/share/) сейчас 3 участника, то
        # должно залогироваться 3 записи, по 1 на каждого участника

        # # но тк папка ресурса /disk/share/dir не является корневой (ее папка = /disk/share/dir)
        # # это значит что tgt_folder_id будет у всех одинаковый,

        self.assert_logged_multiple(
            'fs-mkdir',
            {
                'user_uid': self.uid, 'owner_uid': self.uid, 'resource_type': 'dir',
                'resource_file_id': _not_none, 'tgt_folder_id': _not_none
            },
            {
                'uid': self.uid, 'tgt_rawaddress': self.common_group[self.uid].id + 'dir',
                'tgt_folder_id': new_dir_folder_id
            },
            {
                'uid': self.uid_1, 'tgt_rawaddress': self.common_group[self.uid_1].id + 'dir',
                'tgt_folder_id': new_dir_folder_id
            },
            {
                'uid': self.uid_2, 'tgt_rawaddress': self.common_group[self.uid_2].id + 'dir',
                'tgt_folder_id': new_dir_folder_id
            }
        )

    def test_copy(self):
        src_file_address = Address.Make(self.uid, '/disk/file.txt')

        self._create_several_groups_with_users()
        folder_ids = self.load_folder_ids()

        self.upload_file(self.uid, src_file_address.path)

        self.mock_log.reset_mock()

        Bus().copy_resource(self.uid, src_file_address.id, self.common_file[self.uid].id, False)

        # тк папка ресурса /disk/share/file.txt = /disk/share/ = корень общей папки

        self.assert_logged_multiple(
            'fs-copy',
            {
                'user_uid': self.uid, 'owner_uid': self.uid, 'resource_type': 'file',
                'resource_file_id': _not_none,
            },
            {
                'uid': self.uid,
                'src_rawaddress': src_file_address.id, 'src_folder_id': _not_none,
                'tgt_rawaddress': self.common_file[self.uid].id,
                'tgt_folder_id': folder_ids[(self.uid, self.common_group[self.uid].path)]
            },
            {
                'uid': self.uid_1,
                'src_rawaddress': _missing,
                'src_folder_id': _missing,
                'tgt_rawaddress': self.common_file[self.uid_1].id,
                'tgt_folder_id': folder_ids[(self.uid_1, self.common_group[self.uid_1].path)]
            },
            {
                'uid': self.uid_2,
                'src_rawaddress': _missing,
                'src_folder_id': _missing,
                'tgt_rawaddress': self.common_file[self.uid_2].id,
                'tgt_folder_id': folder_ids[(self.uid_2, self.common_group[self.uid_2].path)]
            }
        )

    def test_copy_from_common_share_to_share_with_distinct_subscribers(self):
        self._create_several_groups_with_users()
        self.upload_file(self.uid_1, self.common_file[self.uid_1].path)
        self.reset_mock_log()

        Bus().copy_resource(self.uid_1, self.common_file[self.uid_1].id, self.share1_file[self.uid_1].id, False)

        folder_ids = self.load_folder_ids()
        self.assert_logged_multiple(
            'fs-copy',
            {
                'user_uid': self.uid_1, 'owner_uid': self.uid,
                'resource_type': 'file', 'resource_file_id': _not_none,
            },
            {
                'uid': self.uid,
                'src_rawaddress': self.common_file[self.uid].id,
                'src_folder_id': folder_ids[(self.uid, self.common_group[self.uid].path)],
                'tgt_rawaddress': self.share1_file[self.uid].id,
                'tgt_folder_id': folder_ids[(self.uid, self.share1_group[self.uid].path)]

            },
            {
                'uid': self.uid_1,
                'src_rawaddress': self.common_file[self.uid_1].id,
                'src_folder_id': folder_ids[(self.uid_1, self.common_group[self.uid_1].path)],
                'tgt_rawaddress': self.share1_file[self.uid_1].id,
                'tgt_folder_id': folder_ids[(self.uid_1, self.share1_group[self.uid_1].path)]
            }
        )

    def test_copy_from_share_with_distinct_subscribers_to_share_with_distinct_subscribers(self):
        self._create_several_groups_with_users()
        self.upload_file(self.uid, self.share2_file[self.uid].path)
        self.reset_mock_log()

        # /disk/share2/file.txt -> /disk/share1/file.txt
        Bus().copy_resource(self.uid, self.share2_file[self.uid].id, self.share1_file[self.uid].id, False)

        folder_ids = self.load_folder_ids()
        self.assert_logged_multiple(
            'fs-copy',
            {
                'user_uid': self.uid, 'owner_uid': self.uid,
                'resource_type': 'file', 'resource_file_id': _not_none,
            },
            {
                'uid': self.uid,
                'src_rawaddress': self.share2_file[self.uid].id,
                'src_folder_id': folder_ids[(self.uid, self.share2_group[self.uid].path)],
                'tgt_rawaddress': self.share1_file[self.uid].id,
                'tgt_folder_id': folder_ids[(self.uid, self.share1_group[self.uid].path)]
            },
            {
                'uid': self.uid_1,
                'src_rawaddress': _missing,
                'src_folder_id': _missing,
                'tgt_rawaddress': self.share1_file[self.uid_1].id,
                'tgt_folder_id': folder_ids[(self.uid_1, self.share1_group[self.uid_1].path)]
            }
        )

    def test_move(self):
        self._create_several_groups_with_users()
        self.upload_file(self.uid, self.share2_file[self.uid].path)
        self.reset_mock_log()

        Bus().move_resource(self.uid, self.share2_file[self.uid].id, self.share1_file[self.uid].id, False)
        # /disk/share2/file.txt -> /disk/share1/file.txt

        folder_ids = self.load_folder_ids()
        self.assert_logged_multiple(
            'fs-move',
            {
                'user_uid': self.uid, 'owner_uid': self.uid,
                'resource_type': 'file', 'resource_file_id': _not_none
            },
            {
                'uid': self.uid,
                'src_rawaddress': self.share2_file[self.uid].id,
                'src_folder_id': folder_ids[(self.uid, self.share2_group[self.uid].path)],
                'tgt_rawaddress': self.share1_file[self.uid].id,
                'tgt_folder_id': folder_ids[(self.uid, self.share1_group[self.uid].path)]
            },
            {
                'uid': self.uid_1,
                'src_rawaddress': _missing,
                'src_folder_id': _missing,
                'tgt_rawaddress': self.share1_file[self.uid_1].id,
                'tgt_folder_id': folder_ids[(self.uid_1, self.share1_group[self.uid_1].path)]
            },
            {
                'uid': self.uid_2,
                'src_rawaddress': self.share2_file[self.uid_2].id,
                'src_folder_id': folder_ids[(self.uid_2, self.share2_group[self.uid_2].path)],
                'tgt_rawaddress': _missing,
                'tgt_folder_id': _missing
            }
        )

    def test_rename(self):
        self._create_several_groups_with_users()
        self.upload_file(self.uid, self.common_file[self.uid].path)
        self.reset_mock_log()

        Bus().move_resource(self.uid, self.common_file[self.uid].id, self.renamed_common_file[self.uid].id, False)

        folder_ids = self.load_folder_ids()
        # у каждого свой folder_id, тк папка ресурса = корень общей
        self.assert_logged_multiple(
            'fs-move',
            {
                'user_uid': self.uid, 'owner_uid': self.uid,
                'resource_type': 'file', 'resource_file_id': _not_none,
            },
            {
                'uid': self.uid,
                'src_rawaddress': self.common_file[self.uid].id,
                'src_folder_id': folder_ids[(self.uid, self.common_group[self.uid].path)],
                'tgt_rawaddress': self.renamed_common_file[self.uid].id,
                'tgt_folder_id': folder_ids[(self.uid, self.common_group[self.uid].path)]
            },
            {
                'uid': self.uid_1,
                'src_rawaddress': self.common_file[self.uid_1].id,
                'src_folder_id': folder_ids[(self.uid_1, self.common_group[self.uid_1].path)],
                'tgt_rawaddress': self.renamed_common_file[self.uid_1].id,
                'tgt_folder_id': folder_ids[(self.uid_1, self.common_group[self.uid_1].path)]
            },
            {
                'uid': self.uid_2,
                'src_rawaddress': self.common_file[self.uid_2].id,
                'src_folder_id': folder_ids[(self.uid_2, self.common_group[self.uid_2].path)],
                'tgt_rawaddress': self.renamed_common_file[self.uid_2].id,
                'tgt_folder_id': folder_ids[(self.uid_2, self.common_group[self.uid_2].path)]
            }
        )

    def test_trash_append(self):
        self._create_several_groups_with_users()
        self.upload_file(self.uid_2, self.common_file[self.uid_2].path)
        self.reset_mock_log()

        with patch('mpfs.core.address.Address.add_trash_suffix'):
            Bus().trash_append(self.uid_1, self.common_file[self.uid_1].id)

        folder_ids = self.load_folder_ids()
        self.assert_logged_multiple(
            'fs-trash-append',
            {
                'user_uid': self.uid_1, 'owner_uid': self.uid_1,
                'resource_type': 'file', 'resource_file_id': _not_none,
            },
            {
                'uid': self.uid,
                'src_rawaddress': self.common_file[self.uid].id,
                'src_folder_id': folder_ids[(self.uid, self.common_group[self.uid].path)],
                'tgt_rawaddress': _missing,
                'tgt_folder_id': _missing
            },
            {
                'uid': self.uid_1,
                'src_rawaddress': self.common_file[self.uid_1].id,
                'src_folder_id': folder_ids[(self.uid_1, self.common_group[self.uid_1].path)],
                'tgt_rawaddress': self.trash_file[self.uid_1].id,
                'tgt_folder_id': '%s:%s' % (self.uid_1, '/trash')
            },
            {
                'uid': self.uid_2,
                'src_rawaddress': self.common_file[self.uid_2].id,
                'src_folder_id': folder_ids[(self.uid_2, self.common_group[self.uid_2].path)],
                'tgt_rawaddress': _missing,
                'tgt_folder_id': _missing
            }
        )

    def test_trash_append_recursively(self):
        self._create_several_groups_with_users()

        self.mkdir(self.common_dir[self.uid].id)
        self.mkdir(self.common_subdir[self.uid].id)
        folder_ids = self.load_folder_ids(add=(self.common_dir, self.common_subdir))

        self.reset_mock_log()
        Bus().trash_append(self.uid_1, self.common_dir[self.uid_1].id)

        self.assert_logged_offset(-5, 'fs-trash-append', uid=self.uid)
        self.assert_logged_offset(-4, 'fs-delete-subdir', uid=self.uid, user_uid=self.uid_1,
                                  resource_id=folder_ids[(self.uid, self.common_dir[self.uid].path)])

        self.assert_logged_offset(-3, 'fs-trash-append', uid=self.uid_1)
        self.assert_logged_offset(-2, 'fs-delete-subdir', uid=self.uid_1, user_uid=self.uid_1,
                                  resource_id=folder_ids[(self.uid, self.common_dir[self.uid].path)])

        self.assert_logged_offset(-1, 'fs-trash-append', uid=self.uid_2)
        self.assert_logged_offset(+0, 'fs-delete-subdir', uid=self.uid_2, user_uid=self.uid_1,
                                  resource_id=folder_ids[(self.uid, self.common_dir[self.uid].path)])

    def test_trash_restore(self):
        self._create_several_groups_with_users()
        folder_ids = self.load_folder_ids()

        self.upload_file(self.uid_1, self.common_file[self.uid_1].path)
        Bus().trash_append(self.uid_2, self.common_file[self.uid_2].id)
        self.reset_mock_log()

        Bus().trash_restore(self.uid, self.trash_file[self.uid].id)

        self.assert_logged_multiple(
            'fs-trash-restore',
            {
                'user_uid': self.uid, 'owner_uid': self.uid,
                'resource_type': 'file', 'resource_file_id': _not_none,
                'type': 'trash_restore', 'subtype': 'disk',
            },
            {
                'uid': self.uid,
                'src_rawaddress': self.trash_file[self.uid].id,
                'src_folder_id': _not_none,
                'tgt_rawaddress': self.common_file[self.uid].id,
                'tgt_folder_id': folder_ids[(self.uid, self.common_group[self.uid].path)],
                'type': 'trash_restore', 'subtype': 'disk',
            },
            {
                'uid': self.uid_1,
                'src_rawaddress': _missing,
                'src_folder_id': _missing,
                'tgt_rawaddress': self.common_file[self.uid_1].id,
                'tgt_folder_id': folder_ids[(self.uid_1, self.common_group[self.uid_1].path)],
                'type': 'trash_restore', 'subtype': 'disk',
            },
            {
                'uid': self.uid_2,
                'src_rawaddress': _missing,
                'src_folder_id': _missing,
                'tgt_rawaddress': self.common_file[self.uid_2].id,
                'tgt_folder_id': folder_ids[(self.uid_2, self.common_group[self.uid_2].path)],
                'type': 'trash_restore', 'subtype': 'disk',
            },
        )

    def test_rm(self):
        self._create_several_groups_with_users()
        self.upload_file(self.uid_1, self.common_file[self.uid_1].path)
        self.reset_mock_log()

        Bus().rm(self.uid, self.common_file[self.uid_2].id)

        folder_ids = self.load_folder_ids()
        self.assert_logged_multiple(
            'fs-rm',
            {
                'user_uid': self.uid, 'owner_uid': self.uid,
                'resource_type': 'file', 'resource_file_id': _not_none,
                'src_folder_id': _missing
            },
            {
                'uid': self.uid,
                'src_rawaddress': _missing,
                'tgt_rawaddress': self.common_file[self.uid].id,
                'tgt_folder_id': folder_ids[(self.uid, self.common_group[self.uid].path)]
            },
            {
                'uid': self.uid_1,
                'src_rawaddress': _missing,
                'tgt_rawaddress': self.common_file[self.uid_1].id,
                'tgt_folder_id': folder_ids[(self.uid_1, self.common_group[self.uid_1].path)]
            },
            {
                'uid': self.uid_2,
                'src_rawaddress': _missing,
                'tgt_rawaddress': self.common_file[self.uid_2].id,
                'tgt_folder_id': folder_ids[(self.uid_2, self.common_group[self.uid_2].path)]
            }
        )

    def test_rm_recursively(self):
        self._create_several_groups_with_users()

        self.mkdir(self.common_dir[self.uid].id)
        self.mkdir(self.common_subdir[self.uid].id)
        folder_ids = self.load_folder_ids(add=(self.common_dir, self.common_subdir))

        self.reset_mock_log()
        Bus().rm(self.uid_1, self.common_dir[self.uid_1].id)

        self.assert_logged_offset(-5, 'fs-rm', uid=self.uid)
        self.assert_logged_offset(-4, 'fs-delete-subdir', uid=self.uid, user_uid=self.uid_1,
                                  resource_id=folder_ids[(self.uid, self.common_dir[self.uid].path)])

        self.assert_logged_offset(-3, 'fs-rm', uid=self.uid_1)
        self.assert_logged_offset(-2, 'fs-delete-subdir', uid=self.uid_1, user_uid=self.uid_1,
                                  resource_id=folder_ids[(self.uid, self.common_dir[self.uid].path)])

        self.assert_logged_offset(-1, 'fs-rm', uid=self.uid_2)
        self.assert_logged_offset(+0, 'fs-delete-subdir', uid=self.uid_2, user_uid=self.uid_1,
                                  resource_id=folder_ids[(self.uid, self.common_dir[self.uid].path)])

    def test_store(self):
        self._create_several_groups_with_users()
        self.reset_mock_log()

        self.upload_file(self.uid, self.common_file[self.uid].path)

        folder_ids = self.load_folder_ids()
        self.assert_logged_multiple(
            'fs-store',
            {
                'user_uid': self.uid, 'owner_uid': self.uid,
                'resource_type': 'file',
                'type': 'store', 'subtype': 'disk',
                'resource_file_id': _not_none,
                'src_rawaddress': _missing,
                'src_folder_id': _missing,
            },
            {
                'uid': self.uid,
                'tgt_rawaddress': self.common_file[self.uid].id,
                'tgt_folder_id': folder_ids[(self.uid, self.common_group[self.uid].path)]
            },
            {
                'uid': self.uid_1,
                'tgt_rawaddress': self.common_file[self.uid_1].id,
                'tgt_folder_id': folder_ids[(self.uid_1, self.common_group[self.uid_1].path)]
            },
            {
                'uid': self.uid_2,
                'tgt_rawaddress': self.common_file[self.uid_2].id,
                'tgt_folder_id': folder_ids[(self.uid_2, self.common_group[self.uid_2].path)]
            }
        )

    def test_dstore(self):
        self._create_several_groups_with_users()
        self.upload_file(self.uid, self.common_file[self.uid].path)
        self.reset_mock_log()

        self.dstore_file(self.uid, self.common_file[self.uid].path)

        folder_ids = self.load_folder_ids()
        self.assert_logged_multiple(
            'fs-store',
            {
                'user_uid': self.uid, 'owner_uid': self.uid,
                'resource_type': 'file', 'type': 'dstore', 'subtype': 'disk',
                'resource_file_id': _not_none,
                'src_rawaddress': _missing,
                'src_folder_id': _missing,
            },
            {
                'uid': self.uid,
                'tgt_rawaddress': self.common_file[self.uid].id,
                'tgt_folder_id': folder_ids[(self.uid, self.common_group[self.uid].path)]
            },
            {
                'uid': self.uid_1,
                'tgt_rawaddress': self.common_file[self.uid_1].id,
                'tgt_folder_id': folder_ids[(self.uid_1, self.common_group[self.uid_1].path)]
            },
            {
                'uid': self.uid_2,
                'tgt_rawaddress': self.common_file[self.uid_2].id,
                'tgt_folder_id': folder_ids[(self.uid_2, self.common_group[self.uid_2].path)]
            }
        )

    def test_set_public(self):
        self._create_several_groups_with_users()
        self.upload_file(self.uid, self.common_file[self.uid].path)
        self.reset_mock_log()

        Publicator().set_public(self.uid_1, self.common_file[self.uid_1].id)

        folder_ids = self.load_folder_ids()
        self.assert_logged_multiple(
            'fs-set-public',
            {
                'user_uid': self.uid_1, 'owner_uid': self.uid, 'resource_type': 'file',
                'resource_file_id': _not_none,
                'src_rawaddress': _missing,
                'src_folder_id': _missing,
            },
            {
                'uid': self.uid,
                'tgt_rawaddress': self.common_file[self.uid].id,
            },
            {
                'uid': self.uid_1,
                'tgt_rawaddress': self.common_file[self.uid_1].id,
            },
            {
                'uid': self.uid_2,
                'tgt_rawaddress': self.common_file[self.uid_2].id,
            }
        )

    def test_set_private(self):
        self._create_several_groups_with_users()
        self.upload_file(self.uid, self.common_file[self.uid].path)
        Publicator().set_public(self.uid_1, self.common_file[self.uid_1].id)
        self.reset_mock_log()

        Publicator().set_private(self.uid_2, self.common_file[self.uid_2].id)

        folder_ids = self.load_folder_ids()
        self.assert_logged_multiple(
            'fs-set-private',
            {
                'user_uid': self.uid_2, 'owner_uid': self.uid,
                'resource_type': 'file', 'resource_file_id': _not_none,
                'src_rawaddress': _missing, 'src_folder_id': _missing,
            },
            {
                'uid': self.uid,
                'tgt_rawaddress': self.common_file[self.uid].id,
                'tgt_folder_id': folder_ids[(self.uid, self.common_group[self.uid].path)]
            },
            {
                'uid': self.uid_1,
                'tgt_rawaddress': self.common_file[self.uid_1].id,
                'tgt_folder_id': folder_ids[(self.uid_1, self.common_group[self.uid_1].path)]
            },
            {
                'uid': self.uid_2,
                'tgt_rawaddress': self.common_file[self.uid_2].id,
                'tgt_folder_id': folder_ids[(self.uid_2, self.common_group[self.uid_2].path)]
            }
        )

    def _create_several_groups_with_users(self):
        base_share_path = self.common_group[self.uid].path
        # создаем группу с владельцем self.uid и участниками self.uid_1, self.uid_2
        # путь у владельца - /disk/share/
        self._create_group_with_users([self.uid_1, self.uid_2], base_share_path)
        # создаем группу с владельцем self.uid и участником self.uid_1
        # путь у владельца - /disk/share1/
        self._create_group_with_users([self.uid_1], self.share1_group[self.uid_1].path)
        # создаем группу с владельцем self.uid и участником self.uid_2
        # путь у владельца - /disk/share2/
        self._create_group_with_users([self.uid_2], self.share2_group[self.uid_2].path)

        # меняем путь, по которому доступна общая папка для участников
        # теперь у self.uid_1 общая папка владельца /disk/share/ будет в /disk/dir1/share/
        # теперь у self.uid_2 общая папка владельца /disk/share/ будет в /disk/dir2/share/
        for address in [self.common_group[uid] for uid in [self.uid_1, self.uid_2]]:
            Bus().mkdir(address.uid, address.get_parent().id)
            Bus().move_resource(address.uid, address.uid + ':' + base_share_path, address.id, False)

        self.reset_mock_log()

    def _create_group_with_users(self, invite_uids, path):
        """Создать группу с участниками.

        Создает группу в которой владелец `self.uid`, а подтвердившие участие
        пользователи - `invite_uids`.
        """
        Bus().mkdir(self.uid, self.uid + ':' + path)

        self.json_ok('share_create_group', {'uid': self.uid, 'path': path})
        for invite_uid in invite_uids:
            self.invite_and_activate(invite_uid, path)


class SharingEventHistoryTestCase(SharingTestCaseMixin, EventHistoryTestCase):
    subscriber_uid = '198311083'
    uid_2 = '198311084'
    invitee_uid = '198311085'
    owner_address = Address.Make(EventHistoryTestCase.uid, '/disk/share')
    uid1_address = Address.Make(subscriber_uid, '/disk/dir1/share1')

    def setup_method(self, method):
        super(SharingEventHistoryTestCase, self).setup_method(method)
        for uid in [self.subscriber_uid, self.uid_2, self.invitee_uid]:
            self.create_user(uid)

    def test_create_group(self):
        Bus().mkdir(self.uid, self.owner_address.id)
        self.reset_mock_log()

        result = self.json_ok('share_create_group', {'uid': self.uid, 'path': self.owner_address.path})
        self.assert_logged_single('share-create-group', uid=self.uid, gid=result['gid'], owner_uid=self.uid,
                                  path=self.owner_address.path, subscriber_count=0)

    def test_unshare_folder(self):
        gid = self.create_share()

        self.json_ok('share_unshare_folder', {'uid': self.uid, 'gid': gid})
        self.assert_logged_count(5)
        self.assert_logged_multiple_offset(
            -2, 'share-unshare-folder',
            {'gid': gid, 'owner_uid': self.uid, 'subscriber_count': 1},
            {'uid': self.uid, 'path': self.owner_address.path, 'rights': _missing},
            {'uid': self.subscriber_uid, 'path': self.uid1_address.path, 'rights': 660},
            {'uid': self.invitee_uid, 'path': self.owner_address.path, 'rights': 640, 'invite_hash': _not_none}
        )
        self.assert_logged_offset(-1, 'share-unshare-folder-user',
                                  gid=gid, subscriber_count=1, path=self.owner_address.path, rights=660,
                                  owner_uid=self.uid, uid=self.uid, user_uid=self.subscriber_uid)
        self.assert_logged_offset(+0, 'share-unshare-folder-invitee',
                                  gid=gid, subscriber_count=1, path=self.owner_address.path, rights=640,
                                  owner_uid=self.uid, uid=self.uid, user_uid=self.invitee_uid)

    def test_invite_user(self):
        gid, invite_hash = self.create_share_and_invite(reset_mock=False)
        # 'user_uid' должно быть = self.uid_2
        self.assert_logged_multiple('share-invite-user', {'gid': gid, 'owner_uid': self.uid, 'subscriber_count': 1},
                                    {'uid': self.uid, 'path': self.owner_address.path, 'invite_hash': _missing},
                                    {'uid': self.subscriber_uid, 'path': self.uid1_address.path,
                                     'invite_hash': _missing},
                                    {'uid': self.uid_2, 'path': self.owner_address.path, 'invite_hash': invite_hash})

    def test_remove_invite(self):
        gid, invite_hash = self.create_share_and_invite()

        self.json_ok('share_remove_invite', {'uid': self.uid, 'gid': gid,
                                             'universe_login': self.get_uid_username(self.uid_2),
                                             'universe_service': 'email'})
        self.assert_logged_multiple(
            'share-remove-invite',
            {'gid': gid, 'owner_uid': self.uid, 'user_uid': self.uid_2, 'rights': 640, 'subscriber_count': 1},
            {'uid': self.uid, 'path': self.owner_address.path},
            {'uid': self.subscriber_uid, 'path': self.uid1_address.path},
            {'uid': self.uid_2, 'path': self.owner_address.path, 'invite_hash': invite_hash})

    def test_activate_invite(self):
        gid, invite_hash = self.create_invite_and_activate(reset_mock=False, rights=660)

        self.assert_logged_multiple(
            'share-activate-invite',
            {'gid': gid, 'owner_uid': self.uid, 'user_uid': self.uid_2, 'rights': 660, 'subscriber_count': 2},
            {'uid': self.uid, 'path': self.owner_address.path, 'invite_hash': _missing},
            {'uid': self.uid_2, 'path': self.owner_address.path, 'invite_hash': invite_hash},
            {'uid': self.subscriber_uid, 'path': self.uid1_address.path, 'invite_hash': _missing}
        )

    def test_reject_invite(self):
        gid, invite_hash = self.create_share_and_invite()

        self.json_ok('share_reject_invite', {'uid': self.uid_2, 'hash': invite_hash})
        self.assert_logged_multiple('share-reject-invite',
                                    {'gid': gid, 'owner_uid': self.uid, 'user_uid': self.uid_2, 'subscriber_count': 1},
                                    {'uid': self.uid, 'path': self.owner_address.path},
                                    {'uid': self.subscriber_uid, 'path': self.uid1_address.path},
                                    {'uid': self.uid_2, 'path': self.owner_address.path, 'invite_hash': invite_hash})

    def test_leave_group(self):
        gid, _ = self.create_invite_and_activate()

        self.json_ok('share_leave_group', {'uid': self.uid_2, 'gid': gid})
        self.assert_logged_multiple('share-leave-group',
                                    {'gid': gid, 'owner_uid': self.uid, 'user_uid': self.uid_2, 'subscriber_count': 1},
                                    {'uid': self.uid, 'path': self.owner_address.path},
                                    {'uid': self.uid_2, 'path': self.owner_address.path},
                                    {'uid': self.subscriber_uid, 'path': self.uid1_address.path})

    def test_trash_append_shared_folder_by_guest(self):
        gid, _ = self.create_invite_and_activate()

        self.json_ok('trash_append', {'uid': self.uid_2, 'path': self.owner_address.path})
        self.assert_logged_count(4)
        self.assert_logged_multiple_offset(-1, 'share-leave-group',
                                           {'gid': gid, 'owner_uid': self.uid, 'user_uid': self.uid_2, 'subscriber_count': 1},
                                           {'uid': self.uid, 'path': self.owner_address.path},
                                           {'uid': self.uid_2, 'path': self.owner_address.path},
                                           {'uid': self.subscriber_uid, 'path': self.uid1_address.path})
        self.assert_logged('fs-trash-append', uid=self.uid_2, owner_uid=self.uid_2, resource_type='dir',
                           resource_file_id=_not_none, src_folder_id=_not_none, tgt_folder_id=_missing,
                           src_rawaddress=_not_none, tgt_rawaddress=_not_none, tgt_ctime=_missing, tgt_utime=_missing,
                           tgt_etime=_missing)

    def test_kick_from_group(self):
        gid, _ = self.create_invite_and_activate()

        self.json_ok('share_kick_from_group', {'uid': self.uid, 'gid': gid, 'user_uid': self.uid_2})
        self.assert_logged_multiple('share-kick-from-group',
                                    {'gid': gid, 'owner_uid': self.uid, 'user_uid': self.uid_2, 'subscriber_count': 1},
                                    {'uid': self.uid, 'path': self.owner_address.path},
                                    {'uid': self.uid_2, 'path': self.owner_address.path},
                                    {'uid': self.subscriber_uid, 'path': self.uid1_address.path})

    def test_change_rights(self):
        gid, _ = self.create_invite_and_activate()

        self.json_ok('share_change_rights', {'uid': self.uid, 'gid': gid, 'user_uid': self.uid_2, 'rights': 660})
        self.assert_logged_multiple(
            'share-change-rights',
            {'gid': gid, 'owner_uid': self.uid, 'user_uid': self.uid_2, 'prev_rights': 640, 'rights': 660,
             'subscriber_count': 2},
            {'uid': self.uid, 'path': self.owner_address.path},
            {'uid': self.uid_2, 'path': self.owner_address.path},
            {'uid': self.subscriber_uid, 'path': self.uid1_address.path}
        )

    @pytest.mark.skipif(not REAL_MONGO,
                        reason='https://st.yandex-team.ru/CHEMODAN-34246')
    def test_change_group_owner(self):
        gid, _ = self.create_invite_and_activate()

        self.json_ok('share_change_group_owner', {'uid': self.uid_2, 'gid': gid, 'owner': self.uid})
        self.assert_logged_multiple('share-change-group-owner',
                                    {'gid': gid, 'owner_uid': self.uid_2,
                                     'user_uid': self.uid, 'subscriber_count': '2'},
                                    {'uid': self.uid_2, 'path': self.owner_address.path},
                                    {'uid': self.uid, 'path': self.owner_address.path},
                                    {'uid': self.subscriber_uid, 'path': self.uid1_address.path})

    def create_share(self):
        Bus().mkdir(self.uid, self.owner_address.id)
        result = self.json_ok('share_create_group', {'uid': self.uid, 'path': self.owner_address.path})

        self.invite_and_activate(self.subscriber_uid, self.owner_address.path)
        Bus().mkdir(self.subscriber_uid, self.uid1_address.get_parent().id)
        Bus().move_resource(self.subscriber_uid,
                            Address.Make(self.subscriber_uid, self.owner_address.path).id,
                            self.uid1_address.id,
                            False)

        self.share_invite(result['gid'], self.invitee_uid, rights=640, login=self.get_uid_username(self.invitee_uid))

        self.reset_mock_log()
        return result['gid']

    def create_share_and_invite(self, reset_mock=True, rights=640):
        gid = self.create_share()
        self.reset_mock_log()

        uid = self.uid_2
        login = self.get_uid_username(uid)

        def userinfo_mock(*args, **kwargs):
            if kwargs.get('login') == login:
                return {'uid': uid,
                        'login': login,
                        'language': 'ru',
                        'username': 'owner name',
                        'public_name': 'owner n.',
                        'email': DEVNULL_EMAIL,
                        'decoded_email': DEVNULL_EMAIL}
            return {'uid': None,
                    'login': None,
                    'language': None,
                    'username': 'friend name',
                    'public_name': 'friend n.',
                    'email': DEVNULL_EMAIL,
                    'decoded_email': DEVNULL_EMAIL}

        with patch.object(Passport, 'userinfo', new=userinfo_mock):
            invite_hash = self.share_invite(gid, self.uid_2, rights=rights, login=login)

        if reset_mock:
            self.reset_mock_log()
        return gid, invite_hash

    def create_invite_and_activate(self, reset_mock=True, rights=640):
        gid, invite_hash = self.create_share_and_invite(rights=rights)
        self.reset_mock_log()

        self.json_ok('share_activate_invite', {'uid': self.uid_2, 'hash': invite_hash})
        if reset_mock:
            self.reset_mock_log()
        return gid, invite_hash

    @staticmethod
    def get_uid_username(uid):
        return 'mpfs-test-%s@yandex.ru' % uid


class AlbumEventHistoryTestCase(EventHistoryTestCase, AlbumsBaseTestCase):
    uid_1 = '198311083'

    def setup_method(self, method):
        super(AlbumEventHistoryTestCase, self).setup_method(method)
        self.create_user(self.uid_1)

    def test_create_album(self):
        album = self.json_ok('albums_create', opts={'uid': self.uid, 'title': 'test_title'})
        self.assert_logged('album-create', uid=self.uid, album_id=album['id'], album_title='test_title',
                           album_item_count=0)

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_create_from_folder(self):
        album = self._create_album_from_folder_with_two_images()
        self.assert_logged_offset(-2, 'album-create', uid=self.uid, album_id=album['id'], album_title=album['title'],
                                  album_item_count=2)
        self.assert_logged_offset(-1, 'album-create-item', uid=self.uid,
                                  album_id=album['id'], album_title=album['title'],
                                  album_item_type='resource', resource_address=self.uid + ':/disk/album_images/01.jpg',
                                  resource_type='file', resource_media_type='image', resource_file_id=_not_none)
        self.assert_logged_offset(+0, 'album-create-item', uid=self.uid,
                                  album_id=album['id'], album_title=album['title'],
                                  album_item_type='resource', resource_address=self.uid + ':/disk/album_images/02.jpg',
                                  resource_type='file', resource_media_type='image', resource_file_id=_not_none)

    def test_create_album_with_album_inside(self):
        child_album = self.json_ok('albums_create', opts={'uid': self.uid, 'title': 'child'})

        album_dict = {'title': 'parent', 'items': [{'type': 'album', 'album_id': child_album['id']}]}
        parent_album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)

        self.assert_logged_offset(-1, 'album-create', uid=self.uid, album_id=parent_album['id'], album_title='parent',
                                  album_item_count=1)
        self.assert_logged_offset(+0, 'album-create-item',
                                  uid=self.uid, album_id=parent_album['id'], album_item_type='album',
                                  child_album_id=child_album['id'], child_album_title='child')

    def test_remove_album(self):
        album = self.json_ok('albums_create', opts={'uid': self.uid})
        self.json_ok('album_remove', opts={'uid': self.uid, 'album_id': album['id']})
        self.assert_logged('album-remove', uid=self.uid, album_id=album['id'], album_title=album['title'])

    def test_change_album_title(self):
        prev_title = 'test_title'
        new_title = 'new_test_title'
        album = self.json_ok('albums_create', opts={'uid': self.uid, 'title': prev_title})
        self.json_ok('album_set_attr', opts={'uid': self.uid, 'album_id': album['id'], 'title': new_title})
        self.assert_logged('album-change-title', uid=self.uid, album_id=album['id'], album_title=new_title,
                           prev_album_title=prev_title)

    def test_change_album_cover(self):
        album, _ = self._create_album_and_append_item()
        self.json_ok('album_set_attr', opts={'uid': self.uid, 'album_id': album['id'], 'cover': 0})
        self.assert_logged('album-change-cover', uid=self.uid, album_id=album['id'])

    def test_change_album_cover_offset(self):
        album, _ = self._create_album_and_append_item()
        self.json_ok('album_set_attr', opts={'uid': self.uid, 'album_id': album['id'], 'cover_offset_y': 1})
        self.assert_logged('album-change-cover-offset', uid=self.uid, album_id=album['id'])

    def test_album_append_item(self):
        album, item = self._create_album_and_append_item()
        self.assert_logged('album-items-append', uid=self.uid, album_id=album['id'], album_title=album['title'],
                           album_item_id=item['id'], album_item_type='resource',
                           resource_address=self.uid + ':' + item['object']['path'],
                           resource_type='file', resource_media_type='image', resource_file_id=_not_none)

    def test_album_item_remove(self):
        album, item = self._create_album_and_append_item()
        self.json_ok('album_item_remove', opts={'uid': self.uid, 'album_id': album['id'], 'item_id': item['id']})
        self.assert_logged('album-items-remove', uid=self.uid, album_id=album['id'], album_title=album['title'],
                           album_item_id=item['id'], album_item_type='resource',
                           resource_address=self.uid + ':' + item['object']['path'],
                           resource_type='file', resource_media_type='image', resource_file_id=_not_none)

    def test_album_publication(self):
        album = self.json_ok('albums_create', opts={'uid': self.uid})
        self.json_ok('album_unpublish', opts={'uid': self.uid, 'album_id': album['id']})
        self.json_ok('album_publish', opts={'uid': self.uid, 'album_id': album['id']})

        self.json_ok('album_unpublish', opts={'uid': self.uid, 'album_id': album['id']})
        self.assert_logged('album-change-publicity', uid=self.uid, album_id=album['id'], album_is_public=0)

        self.json_ok('album_publish', opts={'uid': self.uid, 'album_id': album['id']})
        self.assert_logged('album-change-publicity', uid=self.uid, album_id=album['id'], album_is_public=1)

    def test_add_shared_resource(self):
        path = '/disk/images'
        self.json_ok('mkdir', opts={'uid': self.uid, 'path': path})
        self.upload_file(self.uid, path + '/image.jpg', media_type='image')

        renamed_path = path + '_renamed'
        self.json_ok('share_create_group', {'uid': self.uid, 'path': path})
        self.invite_and_activate(self.uid_1, path)
        Bus().move_resource(self.uid_1,
                            Address.Make(self.uid_1, path).id,
                            Address.Make(self.uid_1, renamed_path).id,
                            False)

        self.json_ok('albums_create_from_folder', opts={'uid': self.uid_1, 'title': 'title', 'path': renamed_path})
        self.assert_logged_offset(+0, 'album-create-item', uid=self.uid_1,
                                  album_item_type='shared_resource',
                                  resource_address=self.uid_1 + ':/disk/images_renamed/image.jpg',
                                  resource_type='file', resource_media_type='image', resource_file_id=_not_none)

    def test_post_to_socials(self):
        album = self.json_ok('albums_create', opts={'uid': self.uid, 'title': 'test_title'})
        self.reset_mock_log()

        with patch.object(SocialProxy, 'wall_post', return_value={'state': 'success'}):
            args = {'uid': self.uid, 'provider': 'facebook', 'public_key': album['public']['public_key']}
            self.json_ok('public_album_social_wall_post', args)
            self.assert_logged('album-post-to-social', uid=self.uid, album_id=album['id'], provider='facebook')

    def test_post_to_socials_async(self):
        album = self.json_ok('albums_create', opts={'uid': self.uid, 'title': 'test_title'})
        self.reset_mock_log()

        with patch.object(SocialProxy, 'wall_post', return_value={'state': 'success'}):
            op = PostAlbumToSocials(id='12346', uid=self.uid, data={'provider': 'vk', 'album_id': album['id']})
            op._post_album()
            self.assert_logged('album-post-to-social', uid=self.uid, album_id=album['id'], provider='vk')

    def _create_album_and_append_item(self):
        img_path = '/disk/01.jpg'
        album = self.json_ok('albums_create', opts={'uid': self.uid})
        self.upload_file(self.uid, img_path, media_type='image')
        item = self.json_ok('album_append_item', opts={'uid': self.uid, 'album_id': album['id'], 'type': 'resource',
                                                       'path': img_path})
        return album, item

    def _create_album_from_folder_with_two_images(self):
        path = '/disk/album_images'
        with patch('time.time', fake_time_with_increment()):
            self.json_ok('mkdir', opts={'uid': self.uid, 'path': path})
            self.upload_file(self.uid, path + '/01.jpg', media_type='image')
            self.upload_file(self.uid, path + '/02.jpg', media_type='image')
            result = self.json_ok('albums_create_from_folder', opts={'uid': self.uid, 'title': 'test_title', 'path': path})
        return result


class BillingEventHistoryTestCase(EventHistoryTestCase, BaseBillingTestCase):
    @staticmethod
    def _product_names(pid):
        product_names = Product(pid).get_names()
        return {'product_name_%s' % lang: product_names[lang]
                for lang in (RU_RU, UK_UA, EN_EN, TR_TR)}

    def test_order_new(self):
        self._create_service()
        self.assert_logged_single('billing-order-new', uid=self.uid, price=30, currency='RUB',
                                  product_id='test_1kb_for_one_second', product_period='seconds:1',
                                  **self._product_names('test_1kb_for_one_second'))

    def test_buy_new(self):
        self._create_and_pay_service(reset_mock=False)
        self.assert_logged_offset(-1, 'space-enlarge', uid=self.uid, reason='test_1kb_for_one_second',
                                  product_id='test_1kb_for_one_second', product_is_free=False,
                                  product_period='seconds:1',
                                  old_limit=products.INITIAL_10GB.amount, new_limit=1024 + products.INITIAL_10GB.amount,
                                  **self._product_names('test_1kb_for_one_second'))
        self.assert_logged_offset(+0, 'billing-buy-new', uid=self.uid, product_id='test_1kb_for_one_second',
                                  auto=False, price=30, currency='RUB', status='success', status_code='')

    def test_prolongate(self):
        sid = self._create_and_pay_service()
        result = self.billing_ok('service_prolongate',
                                 {'uid': self.uid, 'sid': sid, 'payment_method': 'bankcard', 'ip': self.localhost})
        self.manual_success_callback_on_order(result['number'])
        self.assert_logged_single('billing-prolong', uid=self.uid, product_id='test_1kb_for_one_second',
                                  price=30, currency='RUB', auto=False, status='success', status_code='')

    def test_unsubscribe(self):
        btime = ctimestamp()
        with self.patch_check_order(btime * 1000):
            sid = self._create_and_pay_service(product='test_1kb_for_one_minute', auto=1)

        with time_machine(datetime.fromtimestamp(btime - 1)):
            self.service_unsubscribe(sid)
        self.assert_logged_single('billing-unsubscribe', uid=self.uid, product_id='test_1kb_for_one_minute', auto=False,
                                  price=30, currency='RUB', product_period='seconds:60')

    def test_delete(self):
        args = {
            'uid': self.uid,
            'line': 'distribution',
            'pid': 'kingston_flash',
            'ip': self.localhost,
            'service.btime': '123456789123',
            'product.amount': '1024',
        }
        sid = self.billing_ok('service_create', args).get('sid')
        self.service_delete(sid)
        self.assert_logged_offset(-1, 'space-reduce', uid=self.uid, reason='kingston_flash',
                                  product_id='kingston_flash', product_is_free=True,
                                  old_limit=1024 + products.INITIAL_10GB.amount, new_limit=products.INITIAL_10GB.amount,
                                  **self._product_names('kingston_flash'))
        self.assert_logged_offset(+0, 'billing-delete', uid=self.uid, product_id='kingston_flash', auto=False,
                                  price=0, currency='RUB', product_period='months:12')

    def _create_and_pay_service(self, product='test_1kb_for_one_second', auto=0, reset_mock=True):
        order_number = self._create_service(product=product, auto=auto)

        self.reset_mock_log()

        self.pay_order(number=order_number)
        if auto == 0:
            self.manual_success_callback_on_order(order_number)
        else:
            self.manual_success_callback_on_subscription(number=order_number)

        if reset_mock:
            self.reset_mock_log()

        return self.get_services_list()[0][SID]

    def _create_service(self, product='test_1kb_for_one_second', auto=0):
        self.bind_user_to_market(market='RU')
        return self.place_order(product=product, auto=auto)


class InviteEventHistoryTestCase(EventHistoryTestCase):
    def test_activate_without_referral(self):
        c = invite_manager.generate_eternal_code('mpfs')
        invite_manager.activate_code('mpfs', c.hash, '123456')
        self.assert_logged('invite-activation', uid='123456', project='mpfs', owner_uid=None)

    def test_activate_with_referral(self):
        c = invite_manager.generate_eternal_code('mpfs', referral=self.uid)
        invite_manager.activate_code('mpfs', c.hash, '123456')
        self.assert_logged('invite-activation', uid='123456', project='mpfs', owner_uid=self.uid)

    @patch.dict('mpfs.config.settings.feature_toggles', {'disallow_invite_disk_users': False})
    def test_send(self):
        request = self.get_request({'uid': self.uid, 'provider': 'email',
                                    'address': DEVNULL_EMAIL, 'info': {}})
        core.async_user_invite_friend(request)
        self.assert_logged('invite-sent', uid=self.uid, project='mpfs', provider='email', address=DEVNULL_EMAIL)


class LentaPublicLogVisitTestCase(CommonSharingMethods, EventHistoryTestCase):
    # TODO Допилить
    SHARE_FOLDER = '/disk/share'
    RESOURCE_NAME = 'folder_or_file'

    def test_common_dir(self):
        params = {'uid': self.uid, 'path': '/disk/1'}
        self.json_ok('mkdir', params)
        short_url = self.json_ok('set_public', params)['short_url']
        self.json_ok('lenta_public_log_visit', {'uid': self.uid, 'short_url': short_url})

        # TODO: https://st.yandex-team.ru/CHEMODAN-31556
        # logged_data = {
        #   u'public_hash': 'QQzhTzp0AyFYHI1qOt0ZxgccVRogi/Fi58L0leS5hpI=',
        #   u'resource_is_shared': False,
        #   u'resource_type': 'dir',
        #   u'short_url': u'http://dummy.ya.net/6b653b84-47c3-440f-83c5-d293f4748ddb',
        #   u'uid': u'128280859',
        #   u'uid_is_invited': False,
        #   u'uid_is_owner': True
        # }
        # self.assert_logged('public-visit', **logged_data)

    def test_common_file(self):
        params = {'uid': self.uid, 'path': '/disk/1.jpg'}
        self.upload_file(params['uid'], params['path'], media_type='image')
        short_url = self.json_ok('set_public', params)['short_url']
        self.json_ok('lenta_public_log_visit', {'uid': self.uid, 'short_url': short_url})

        # TODO: https://st.yandex-team.ru/CHEMODAN-31556
        # logged_data = {
        #   u'public_hash': 'QQzhTzp0AyFYHI1qOt0ZxgccVRogi/Fi58L0leS5hpI=',
        #   u'resource_is_shared': False,
        #   u'resource_type': 'dir',
        #   u'short_url': u'http://dummy.ya.net/6b653b84-47c3-440f-83c5-d293f4748ddb',
        #   u'uid': u'128280859',
        #   u'uid_is_invited': False,
        #   u'uid_is_owner': True
        # }
        # self.assert_logged('public-visit', **logged_data)

    def _setup_shared(self):
        self.json_ok('user_init', {'uid': self.uid_3})

        self.SHARE_FOLDER = '/disk/share'
        self.OWNER = self.uid
        self.INVITED = self.uid_3

        self.json_ok('mkdir', {'uid': self.OWNER, 'path': self.SHARE_FOLDER})
        gid = self.json_ok('share_create_group', {'uid': self.OWNER, 'path': self.SHARE_FOLDER})['gid']
        hsh = self.json_ok('share_invite_user', {'uid': self.OWNER,
                                                 'gid': gid,
                                                 'universe_login': self.email_3,
                                                 'universe_service': 'email',
                                                 'rights': '660'})['hash']
        self.json_ok('share_activate_invite', {'uid': self.INVITED, 'hash': hsh})

    def test_shared_dir(self):
        self._setup_shared()
        params = {'uid': self.uid, 'path': '%s/1' % self.SHARE_FOLDER}
        self.json_ok('mkdir', params)
        short_url = self.json_ok('set_public', params)['short_url']
        self.json_ok('lenta_public_log_visit', {'uid': self.OWNER, 'short_url': short_url})
        self.json_ok('lenta_public_log_visit', {'uid': self.INVITED, 'short_url': short_url})
        self.json_ok('lenta_public_log_visit', {'uid': '123321', 'short_url': short_url})

    def test_visit_public_album(self):
        """Протестировать логирование захода по публичной ссылке альбома."""
        album = self.json_ok(
            'albums_create_with_items', opts={'uid': self.uid},
            json={'items': []}
        )
        short_url = album['public']['short_url']
        public_hash = album['public']['public_key']
        with patch('mpfs.core.services.clck_service.Clck.short_url_to_public_hash', return_value=public_hash):
            with patch('mpfs.core.event_history.logger.log_raw_event', return_value=None) as mock_log_raw_event:
                self.json_ok('lenta_public_log_visit', {'uid': self.uid, 'short_url': short_url})
                assert mock_log_raw_event.called
                args, kwargs = mock_log_raw_event.call_args
                assert 'event_type' in kwargs
                assert kwargs['event_type'] == 'public-visit'

                assert 'data' in kwargs
                data = kwargs['data']
                assert 'public_hash' in data
                assert 'resource_type' in data
                assert 'short_url' in data
                assert 'uid' in data
                assert 'uid_is_owner' in data
                assert data['public_hash'] == public_hash
                assert data['resource_type'] == 'album'  # важно
                assert data['short_url'] == short_url
                assert data['uid'] == str(self.uid)
                assert data['uid_is_owner'] is True

    def test_product_ukrainian_name_in_log(self):
        """Проверить что в логе появляется локализованное имя акции на украинском языке
        (на примере акции 32 Гб за автозагрузку)."""
        from mpfs.core.event_history.logger import CatchHistoryLogging
        with CatchHistoryLogging(catch_messages=True) as catcher:
            self.billing_ok('service_create', {
                'uid': self.uid,
                'line': 'bonus',
                'pid': '32_gb_autoupload',
                'ip': '127.0.0.1'
            })
            [message] = catcher.get_messages()
            assert u'product_name_ru=Мобильная автозагрузка' in message
            assert u'product_name_en=Auto-upload from mobile devices' in message
            assert u'product_name_tr=Mobil cihazlardan otomatik yükle' in message

            assert u'product_name_ua=Мобільне автозавантаження' in message
            assert u'product_name_uk=Мобільне автозавантаження' in message

    def test_photounlim_root_uses_resource_id(self):
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
        mobile_headers = {'Yandex-Cloud-Request-ID': 'ios-test'}
        file_in_photounlim_address = Address.Make(self.uid, '/photounlim/0.jpg')
        self.upload_file(self.uid, '/photostream/0.jpg', media_type='image', headers=mobile_headers)
        resource_id = self.json_ok('info', {'uid': self.uid, 'path': '/photounlim', 'meta': 'resource_id'})['meta']['resource_id']
        self.assert_logged_single('fs-store', uid=self.uid, owner_uid=self.uid, type='store', subtype='photounlim',
                                  resource_type='file', resource_media_type='image',
                                  lenta_media_type='image',
                                  resource_file_id=_not_none, tgt_folder_id=resource_id,
                                  tgt_rawaddress=file_in_photounlim_address.id, changes='')
