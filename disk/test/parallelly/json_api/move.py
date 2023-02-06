# -*- coding: utf-8 -*-
import before_after
import mock
from hamcrest import assert_that, calling, raises, has_items, has_entry, has_item

from nose_parameterized import parameterized
from pymongo.errors import OperationFailure

from mpfs.core.bus import Bus
from test.parallelly.json_api.base import CommonJsonApiTestCase

from mpfs.common.static import codes
from test.fixtures import users


class MoveResourceTestCase(CommonJsonApiTestCase):

    @parameterized.expand([
        ('1', '1', True, {'lenta_block_id': '100500'}),  # get_lenta_block_id = 1, return_status = 1
        ('1', '0', False, ''),  # get_lenta_block_id = 1, return_status = 0
        ('0', '1', False, {}),  # get_lenta_block_id = 0, return_status = 1
        ('0', '0', False, ''),  # get_lenta_block_id = 0, return_status = 0
        (None, None, False, ''),  # get_lenta_block_id is not passed, return_status is not passed
        ('1', None, False, ''),  # get_lenta_block_id = 1, return_status is not passed
        ('0', None, False, ''),  # get_lenta_block_id = 0, return_status is not passed
        (None, '1', False, {}),  # get_lenta_block_id is not passed, return_status = 1
        (None, '0', False, ''),  # get_lenta_block_id is not passed, return_status = 0
    ])
    def test_move_file_returns_lenta_block_id(self, get_lenta_block_id, return_status, expected_lenta_called,
                                              expected_result):
        uid = users.user_1.uid

        self.create_user(uid)
        self.upload_file(uid, '/disk/test.jpg')
        with mock.patch(
            'mpfs.core.services.lenta_loader_service.LentaLoaderService.open_url',
            return_value=(200, '{"block_id":"100500"}', {})
        ) as mocked_open_url:
            params = {
                'uid': uid,
                'src': '/disk/test.jpg',
                'dst': '/disk/test_new.jpg',
            }
            if get_lenta_block_id is not None:
                params['get_lenta_block_id'] = get_lenta_block_id
            if return_status is not None:
                params['return_status'] = return_status

            result = self.json_ok('move', params)

            if expected_lenta_called:
                assert mocked_open_url.called
                args, kwargs = mocked_open_url.call_args
                assert kwargs['method'] == 'POST'
                event_log_message = kwargs['pure_data']
                assert 'event_type=fs-move' in event_log_message.split('\t')
            else:
                assert not mocked_open_url.called

            assert result == expected_result

    def test_move_file_without_extension(self):
        """Проверить переименовывание .jpg файла в файл без расширения.

        Медиатип должен остаться старый.
        По мотивам https://st.yandex-team.ru/CHEMODAN-35241.
        """

        uid = users.user_1.uid
        self.create_user(uid)
        self.upload_file(uid, '/disk/i.jpg', file_data={'mimetype': 'image/jpeg'})
        result = self.json_ok('info', {
            'uid': uid,
            'path': '/disk/i.jpg',
            'meta': ''
        })
        old_media_type = result['meta']['mediatype']
        self.json_ok('move', {
            'uid': uid,
            'src': '/disk/i.jpg',
            'dst': '/disk/i',
        })
        result = self.json_ok('info', {
            'uid': uid,
            'path': '/disk/i',
            'meta': ''
        })
        new_media_type = result['meta']['mediatype']

        assert old_media_type == new_media_type == 'image'

    def test_rename_shared_folder_cursor_fail(self):
        """
        Проверяем кейс переименования ОП с публичными файлами в случае, если при перемещении файлов произошла ошибка
        `OperationFailure: cursor id '<Здесь могла бы быть ваша реклама>' not valid at server`
        В этом случае должны поретраить запрос и пойти дальше. После переименования ссылка на файл должны остаться
        валидной.
        """
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})

        public_file_path = '/disk/folder/file-1.txt'
        self.upload_file(self.uid, public_file_path)
        self.upload_file(self.uid, '/disk/folder/file-2.txt')
        self.upload_file(self.uid, '/disk/folder/file-3.txt')

        public_resp = self.json_ok('set_public', {'uid': self.uid, 'path': public_file_path})
        public_hash = public_resp['hash']
        self.json_ok('share_create_group', {'uid': self.uid, 'path': '/disk/folder'})

        from mpfs.core.filesystem.dao.resource import ResourceDAO
        real_find = ResourceDAO.find

        def fake_find(self, spec, *args, **kwargs):
            cursor = real_find(self, spec, *args, **kwargs)
            for item in cursor:
                if item['key'] != public_file_path and fake_find.is_first_time:
                    fake_find.is_first_time = False
                    raise OperationFailure('cursor id \'%s\' not valid at server' % id(cursor))
                yield item
        fake_find.is_first_time = True

        from mpfs.metastorage.mongo.collections import base
        with mock.patch.object(base, 'OPERATIONS_SHARED_FOLDER_ASYNC_MOVE_ENABLE_CURSOR_INVALIDATION', True), \
                mock.patch.object(base, 'OPERATIONS_SHARED_FOLDER_ASYNC_MOVE_CURSOR_INVALIDATION_RETRY_COUNT', 2), \
                mock.patch.object(ResourceDAO, 'find', fake_find):
            self.json_ok('async_move', {'uid': self.uid, 'src': '/disk/folder', 'dst': '/disk/moved'})

        self.json_ok('public_info', {'private_hash': public_hash, 'meta': ''})

        self.json_error('list', {'uid': self.uid, 'path': '/disk/folder'}, code=codes.LIST_NOT_FOUND)
        listing = self.json_ok('list', {'uid': self.uid, 'path': '/disk/moved'})
        assert len(listing) == 4  # folder + 3 files

    def test_move_race(self):
        u"""Проверяем два конкуретных перемещения.

        Исходное состояние диска:
        /disk/
        ├── EHOT.jpg
        └── New Folder
            └── FOXY.jpg

        Делаем два перемещения:
        /disk/New Folder -> /disk/pets pics
        /disk/EHOT.jpg -> /disk/New Folder/EHOT.jpg

        Второе перемещение должно упасть об лок первого перемещения.

        Должны прийти к такому состоянию диска:
        /disk/
        ├── EHOT.jpg
        └── pets pics
            └── FOXY.jpg
        """
        old_dst_dir_path = '/disk/New Folder'
        new_dst_dir_path = '/disk/pets pics'
        file_name = 'EHOT.jpg'
        file_from_dir_name = 'FOXY.jpg'
        src_file_path = '/disk/%s' % file_name
        dst_file_path_in_old_dir = '%s/%s' % (old_dst_dir_path, file_name)
        self.json_ok('mkdir', {'uid': self.uid, 'path': old_dst_dir_path})
        self.upload_file(self.uid, src_file_path)
        self.upload_file(self.uid, '%s/%s' % (old_dst_dir_path, file_from_dir_name))

        def move_concurrently(*args, **kwargs):
            from mpfs.common.errors import ResourceLocked
            assert_that(calling(Bus().move_resource).with_args(self.uid,
                                                               '%s:%s' % (self.uid, src_file_path),
                                                               '%s:%s' % (self.uid, dst_file_path_in_old_dir),
                                                               False),
                        raises(ResourceLocked))

        with before_after.after('mpfs.core.filesystem.base.Filesystem.base_copy', move_concurrently):
            self.json_ok('move', {'uid': self.uid,
                                  'src': old_dst_dir_path,
                                  'dst': new_dst_dir_path})

        root_resources = self.json_ok('list', {'uid': self.uid,
                                               'path': '/disk'})
        # В корне должна быть новая папка и файл на старом месте
        assert_that(root_resources, has_items(has_entry('path', new_dst_dir_path), has_entry('path', src_file_path)))
        dir_resources = self.json_ok('list', {'uid': self.uid,
                                     'path': new_dst_dir_path})
        # На новом месте в папке должен быть ее ресурс
        assert_that(dir_resources, has_item(has_entry('path', '%s/%s' % (new_dst_dir_path, file_from_dir_name))))
        self.json_error('list', {'uid': self.uid,
                                 'path': old_dst_dir_path})
