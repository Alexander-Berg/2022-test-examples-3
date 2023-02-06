#!/usr/bin/python
# -*- coding: utf-8 -*-
import pytest

from test.parallelly.publication.base import BasePublicationMethods
from test.conftest import REAL_MONGO
from test.fixtures.users import user_1
from test.fixtures.kladun import KladunMocker
from test.helpers.stubs.resources.users_info import update_info_by_uid

import attrdict
import random
import hashlib
import re
import copy
from lxml import etree
import mock

import mpfs.engine.process

from mpfs.common.util import to_json
from mpfs.common.static import codes, tags
from mpfs.core.address import Address, PublicAddress
from mpfs.core.bus import Bus
from mpfs.core.operations import manager
from mpfs.core.operations.filesystem.copy import CopyMailDisk, CopyToDisk
from mpfs.core.services.lenta_loader_service import LentaLoaderService
from mpfs.metastorage.mongo.util import decompress_data
from test.helpers.stubs.services import KladunStub
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase


mpfs_db = CollectionRoutedDatabase()


class FileActionsPublicationTestCase(BasePublicationMethods):

    def setup_method(self, method):
        super(FileActionsPublicationTestCase, self).setup_method(method)

        # сохраняем метод лока ресурса для теста test_do_not_lock_original_file_on_public_copy
        from mpfs.core.metastorage.control import fs_locks
        self.original_fs_lock_release = copy.copy(fs_locks.release)

    def teardown_method(self, method):
        # возвращаем метод лока ресурса, поломанный в тесте test_do_not_lock_original_file_on_public_copy
        from mpfs.core.metastorage.control import fs_locks
        fs_locks.release = self.original_fs_lock_release

        super(FileActionsPublicationTestCase, self).teardown_method(method)

    def test_save_path(self):
        self.upload_file(self.uid, '/disk/1.jpg')
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/d1'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/d2'})
        hsh = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/1.jpg'})['hash']

        # Синхронная ручка
        ## С указанием save_path
        info = self.json_ok('public_copy', {'uid': self.uid, 'private_hash': hsh, 'save_path': '/disk/d1'})
        assert info['path'] == '/disk/d1/1.jpg'
        ## Без указания save_path
        info = self.json_ok('public_copy', {'uid': self.uid, 'private_hash': hsh})
        assert info['path']== u'/disk/Загрузки/1.jpg'

        # Асинхронная ручка
        ## С указанием save_path
        self.json_error('info', {'uid': self.uid, 'path': '/disk/d2/1.jpg'})
        copy_meth = self.json_ok('async_public_copy', {'uid': self.uid, 'private_hash': hsh, 'save_path': '/disk/d2'})
        self.assertDictContainsSubset({'target_path': '/disk/d2/1.jpg'}, copy_meth)
        self.json_ok('info', {'uid': self.uid, 'path': '/disk/d2/1.jpg'})
        ## Без указания save_path
        self.json_error('info', {'uid': self.uid, 'path': u'/disk/Загрузки/1 (1).jpg'})
        copy_meth = self.json_ok('async_public_copy', {'uid': self.uid, 'private_hash': hsh})
        self.assertDictContainsSubset({'target_path': u'/disk/Загрузки/1 (1).jpg'}, copy_meth)
        self.json_ok('info', {'uid': self.uid, 'path': u'/disk/Загрузки/1 (1).jpg'})

    def _public_copy_setup_for_lenta_testing(self):
        self.upload_file(self.uid, '/disk/1.jpg')
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/d1'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/d2'})
        hsh = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/1.jpg'})['hash']
        return hsh

    def _public_copy_request(self, open_url_return_value, side_effect=None, meta=None, save_path='/disk/d1'):
        hsh = self._public_copy_setup_for_lenta_testing()

        with mock.patch(
            'mpfs.core.services.lenta_loader_service.LentaLoaderService.open_url',
            return_value=open_url_return_value
        ) as mock_open_url:
            if side_effect:
                mock_open_url.side_effect = side_effect
            params = {
                'uid': self.uid,
                'private_hash': hsh,
                'save_path': save_path,
            }
            if meta is not None:
                params.update({'meta': meta})
            info = self.json_ok('public_copy', params)

            return info, mock_open_url

    def test_public_copy_return_lenta_block_id_when_service_200_empty_meta(self):
        """Протестировать что оповещается Лента и возвращается ID блока с пустым
        параметром `meta` при 200 ответе от Ленты."""
        info, _ = self._public_copy_request(
            open_url_return_value=(200, '{"block_id":"100500"}', {}),
            meta=''
        )
        assert 'meta' in info
        assert 'lenta_block_id' in info['meta']
        assert info['meta']['lenta_block_id'] == '100500'

    def test_public_copy_sends_correct_data_to_lenta(self):
        """Проверить что мы отправляем все необходимые данные в Ленту."""
        from mpfs.core.services.lenta_loader_service import LentaLoaderService
        self.create_user(self.user_2.uid)
        self.upload_file(self.user_2.uid, '/disk/setup.exe')

        hsh = self.json_ok('set_public', {'uid': self.user_2.uid, 'path': '/disk/setup.exe'})['hash']
        with mock.patch.object(
            LentaLoaderService,
            'save_file_from_public',
            wraps=LentaLoaderService.save_file_from_public
        ) as mock_save_file_from_public:
            params = {
                'uid': self.uid,
                'private_hash': hsh,
                'save_path': '/disk',
                'name': 'setup.exe',
                'meta': 'lenta_block_id'
            }
            self.json_ok('public_copy', params)
            args, kwargs = mock_save_file_from_public.call_args
            if 'fs_copy_event_log_message' in kwargs:
                message = kwargs['fs_copy_event_log_message']
            else:
                (message,) = args

            pairs = message.split()[1:]  # сплитим по табу и обрезаем tskv
            pairs = dict([pair.split('=') for pair in pairs])
            assert 'src_folder_id' in pairs
            assert 'tgt_folder_id' in pairs

    def test_public_copy_return_lenta_block_id_when_service_200_lenta_block_id_in_meta(self):
        """Протестировать что оповещается Лента и возвращается ID блока с
        параметром `meta` в котором есть `lenta_block_id` при 200 ответе от Ленты."""
        info, _ = self._public_copy_request(
            open_url_return_value=(200, '{"block_id":"100500"}', {}),
            meta='visible,lenta_block_id'
        )
        assert 'meta' in info
        assert 'lenta_block_id' in info['meta']
        assert info['meta']['lenta_block_id'] == '100500'

    def test_public_copy_not_return_lenta_block_id_when_service_200_without_meta(self):
        """Протестировать что ID блока не возвращается при непереданном параметре `meta`."""
        info, mock_open_url = self._public_copy_request(
            open_url_return_value=(200, '{"block_id":"100500"}', {})
        )
        assert 'meta' not in info

    def test_public_copy_lenta_loader_method_input_data(self):
        """Если метод и хедеры будут не строковые, а юникодные,
        то получим UnicodeDecodeError при запросе во внешний сервис."""
        info, mock_open_url = self._public_copy_request(
            open_url_return_value=(200, '{"block_id":"100500"}', {})
        )
        assert mock_open_url.called
        args, kwargs = mock_open_url.call_args
        assert 'headers' in kwargs
        assert 'Content-Type' in kwargs['headers']
        assert isinstance(kwargs['headers']['Content-Type'], str)
        assert not isinstance(kwargs['headers']['Content-Type'], unicode)
        assert kwargs['headers']['Content-Type'] == 'text/plain'

        assert 'method' in kwargs
        assert isinstance(kwargs['method'], str)
        assert not isinstance(kwargs['method'], unicode)

        assert kwargs['method'] == 'POST'

    def test_public_copy_return_lenta_block_id_when_service_non_200(self):
        """Протестировать что оповещается Лента и не возвращается ID блока."""
        info, _ = self._public_copy_request(
            open_url_return_value=(500, 'Service Unavailable', {}),
            meta=''
        )
        assert 'meta' in info
        assert 'lenta_block_id' not in info['meta']

    def test_public_copy_return_lenta_block_id_when_service_raises(self):
        """Протестировать что не возвращается ID блока Ленты, когда сервис рейзит ошибку."""
        info, _ = self._public_copy_request(
            open_url_return_value=(500, 'Service Unavailable', {}),
            side_effect=LentaLoaderService.api_error,
            meta=''
        )
        assert 'meta' in info
        assert 'lenta_block_id' not in info['meta']

    def test_grab_public_file_again(self):
        """
        Протестировать повторное получение публичного файла.
        """

        self.grab_public_file()
        for i in (1, 2):
            self.grab_public_file()

            addr = Address.Make(self.uid, '%s%s%s' %
                                (u'/disk/Загрузки/public.some.info file (', i, ').ext'))
            info = self.json_ok('info', {
                'uid': self.uid,
                'path': addr.id,
                'connection_id': ''
            })
            assert info['id'] == addr.path

    def test_grab_empty_public_file(self):
        """
        Протестировать копирование пустого публичного файла.
        """
        faddr = Address.Make(self.uid, self.pub_file).id
        self.make_dir()

        data = self.file_data
        data['size'] = 0
        Bus().mkfile(self.uid, faddr, data=self.file_data)
        self.json_ok('set_public', {'uid': self.uid, 'path': faddr, 'connection_id': ''})

        resource = Bus().resource(self.uid, faddr)
        hash_ = resource.get_public_hash()
        self.json_ok('public_copy', {
            'uid': self.uid,
            'private_hash': hash_,
            'name': 'empty_public_file.txt',
            'connection_id': ''
        })

    def test_async_public_copy_notifies_lenta_loader_with_folder(self):
        """Проверить, что при сохранении папки оповещается Лента."""
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test'})
        hash_ = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/test'})['hash']
        with mock.patch.object(
            LentaLoaderService,
            'start_saving_folder_from_public',
            wraps=LentaLoaderService.start_saving_folder_from_public
        ) as mocked_start_saving_folder_from_public:
            self.json_ok('async_public_copy', {'uid': self.uid, 'private_hash': hash_})
            assert mocked_start_saving_folder_from_public.called
            args, kwargs = mocked_start_saving_folder_from_public.call_args
            uid, raw_resource_id, resource_path = args
            assert uid == self.uid
            # тестируем что это resource_id, а не file_id
            from mpfs.core.address import ResourceId
            ResourceId.parse(raw_resource_id)
            assert resource_path == u'/disk/Загрузки/test'

    def test_async_public_copy_does_not_notify_lenta_loader_with_file(self):
        """Проверить, что при сохранении файла НЕ оповещается Лента."""
        self.upload_file(self.uid, '/disk/test.jpg')
        hash_ = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/test.jpg'})['hash']
        with mock.patch.object(
            LentaLoaderService,
            'start_saving_folder_from_public',
            wraps=LentaLoaderService.start_saving_folder_from_public
        ) as mocked_start_saving_folder_from_public:
            self.json_ok('async_public_copy', {'uid': self.uid, 'private_hash': hash_})
            assert not mocked_start_saving_folder_from_public.called

    def test_async_public_copy_disk_disk_folder_calls_lenta_loader_start_saving_folder_from_public(self):
        """Протестировать, что при сохранении публичной папки дёргается правильный метод Ленты."""
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test'})
        hash_ = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/test'})['hash']
        with mock.patch(
            'mpfs.core.services.lenta_loader_service.LentaLoaderService.start_saving_folder_from_public',
            return_value='100500'
        ) as mocked_start_saving_folder_from_public:
            self.json_ok('async_public_copy', {'uid': self.uid, 'private_hash': hash_})
            mocked_start_saving_folder_from_public.assert_called_once()

    def test_async_public_copy_returns_lenta_block_id_with_folder_when_service_200(self):
        """Протестировать, что идентификатор блока возвращается в ответе,
        если бэкенд Ленты нормально отработал."""
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test'})
        hash_ = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/test'})['hash']
        with mock.patch(
            'mpfs.core.services.lenta_loader_service.LentaLoaderService.open_url',
            return_value=(200, '{"block_id":"100500"}', {})
        ):
            result = self.json_ok(
                'async_public_copy',
                {
                    'uid': self.uid,
                    'private_hash': hash_,
                }
            )
            assert 'lenta_block_id' in result
            assert result[tags.LENTA_BLOCK_ID] == '100500'

    def test_async_public_copy_does_not_return_lenta_block_id_with_folder_when_service_500(self):
        """Протестировать, что идентификатор блока НЕ возвращается в ответе,
        если бэкенд Ленты отработал НЕ нормально."""
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test'})
        hash_ = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/test'})['hash']
        with mock.patch(
            'mpfs.core.services.lenta_loader_service.LentaLoaderService.open_url',
            return_value=(500, 'Internal Server Error', {})
        ):
            result = self.json_ok(
                'async_public_copy',
                {
                    'uid': self.uid,
                    'private_hash': hash_,
                }
            )
            assert tags.LENTA_BLOCK_ID not in result

    def test_async_public_copy_disk_disk_folder_predefined_file_id_is_passed_to_operation_with_folder(self):
        """Протестировать, что при копировании папки, мы сразу генерим `predefined_file_id`
        и передаем его в операцию."""
        from mpfs.core.filesystem.resources.base import Resource
        from mpfs.core.operations import manager
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test'})
        hash_ = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/test'})['hash']
        with mock.patch(
            'mpfs.core.services.lenta_loader_service.LentaLoaderService.start_saving_folder_from_public',
            wraps=LentaLoaderService().start_saving_folder_from_public
        ) as mocked_start_saving_folder_from_public:
            with mock.patch(
                'mpfs.core.services.lenta_loader_service.LentaLoaderService.open_url',
                return_value=(200, '{"block_id": "100500"}', {})
            ) as mocked_lenta_loader_open_url:
                with mock.patch(
                    'mpfs.core.filesystem.resources.base.Resource.generate_file_id',
                    wraps=Resource.generate_file_id
                ) as mocked_generate_file_id:
                    with mock.patch(
                        'mpfs.core.operations.manager.create_operation',
                        wraps=manager.create_operation
                    ) as mocked_create_operation:
                        self.json_ok('async_public_copy', {'uid': self.uid, 'private_hash': hash_})
                        assert mocked_generate_file_id.called
                        assert mocked_create_operation.called

                        args, kwargs = mocked_create_operation.call_args
                        assert 'odata' in kwargs
                        assert 'predefined_file_id' in kwargs['odata']
                        assert mocked_start_saving_folder_from_public.call_count == 1
                        assert mocked_lenta_loader_open_url.called
                        # 1 вызов: start_saving_folder_from_public
                        assert mocked_lenta_loader_open_url.call_count == 1

                        (
                            start_saving_folder_from_public_call_args,
                        ) = mocked_lenta_loader_open_url.call_args_list

                        args, kwargs = start_saving_folder_from_public_call_args
                        assert kwargs['method'] == 'POST'
                        assert kwargs['url'].endswith('/api/start_saving_folder_from_public')
                        assert 'folderPath=' in kwargs['pure_data']
                        assert 'folderId=' in kwargs['pure_data']

    def test_async_public_copy_attach_file_status_returns_lenta_block_id_when_lenta_200(self):
        """Проверить что при сохранении файла из аттачей в диск,
        после отработки операции ручка `status` возвращает `lenta_block_id`."""
        self.create_user(self.user_1.uid)
        self.create_user(self.user_2.uid)

        self.upload_file(self.user_1.uid, '/attach/trump.jpg')
        folder_info, file_info = self.json_ok('list', {
            'path': '/attach',
            'uid': self.user_1.uid,
            'meta': ''
        })
        hash_ = file_info['meta']['public_hash']

        with mock.patch(
            'mpfs.core.services.lenta_loader_service.LentaLoaderService.open_url',
            return_value=(200, '{"block_id":"100500"}', {})
        ):
            result = self.json_ok('async_public_copy', {'uid': self.user_2.uid, 'private_hash': hash_})
            result = self.json_ok('status', {'uid': self.user_2.uid, 'oid': result['oid']})
            assert result['status'] == 'DONE'
            assert tags.LENTA_BLOCK_ID in result
            assert result[tags.LENTA_BLOCK_ID] == '100500'

    def test_async_public_copy_attach_file_passes_correct_params_to_lenta_backend(self):
        """Проверить что мы отправляем все необходимые данные в Бэкенд Ленты."""
        self.create_user(self.user_1.uid)
        self.create_user(self.user_2.uid)

        self.upload_file(self.user_1.uid, '/attach/trump.jpg')
        folder_info, file_info = self.json_ok('list', {
            'path': '/attach',
            'uid': self.user_1.uid,
            'meta': ''
        })
        hash_ = file_info['meta']['public_hash']

        with mock.patch.object(
            LentaLoaderService,
            'save_file_from_public',
            wraps=LentaLoaderService.save_file_from_public
        ) as mocked_save_file_from_public:
            self.json_ok(
                'async_public_copy',
                {
                    'uid': self.user_2.uid,
                    'private_hash': hash_,
                }
            )

            # проверяем что бэкенд Ленты был оповещен с нужными параметрами
            args, kwargs = mocked_save_file_from_public.call_args
            if 'fs_copy_event_log_message' in kwargs:
                message = kwargs['fs_copy_event_log_message']
            else:
                (message,) = args

            pairs = message.split()[1:]  # сплитим по табу и обрезаем tskv
            pairs = dict([pair.split('=') for pair in pairs])
            assert 'src_folder_id' in pairs
            assert 'tgt_folder_id' in pairs
            # это не все данные необходимые, но одни из самых важных

    def test_async_public_copy_attach_file_status_does_not_return_lenta_block_id_when_lenta_500(self):
        """Протестировать что при сохранении файла из атача, при падении Ленты ручка статуса
        не вернет `lenta_block_id`.
        """
        self.create_user(self.user_1.uid)
        self.create_user(self.user_2.uid)

        self.upload_file(self.user_1.uid, '/attach/trump.jpg')
        folder_info, file_info = self.json_ok('list', {
            'path': '/attach',
            'uid': self.user_1.uid,
            'meta': ''
        })
        hash_ = file_info['meta']['public_hash']

        with mock.patch(
            'mpfs.core.services.lenta_loader_service.LentaLoaderService.open_url',
            return_value=(500, 'Internal Server Error', {})
        ):
            result = self.json_ok(
                'async_public_copy',
                {
                    'uid': self.user_2.uid,
                    'private_hash': hash_,
                }
            )

            result = self.json_ok('status', {'uid': self.user_2.uid, 'oid': result['oid']})
            assert result['status'] == 'DONE'
            assert tags.LENTA_BLOCK_ID not in result

    def test_grab_invisible_public_file(self):
        """При публичном копировании всегда проставляем meta.visible = 1"""
        self.upload_file(self.uid, '/disk/1.jpg', opts={'visible': 0})
        hsh = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/1.jpg'})['hash']

        file_info = self.json_ok('info', {'uid': self.uid, 'path': '/disk/1.jpg', 'meta': 'visible'})
        assert file_info['meta']['visible'] == 0

        # Синхронно
        public_file = self.json_ok('public_copy', {'uid': self.uid, 'private_hash': hsh})
        pf_info = self.json_ok('info', {'uid': self.uid, 'path': public_file['path'], 'meta': 'visible'})
        assert pf_info['meta']['visible'] == 1
        self.json_ok('rm', {'uid': self.uid, 'path': public_file['path'], 'meta': 'visible'})

        # Aсинхронно
        self.json_ok('async_public_copy', {'uid': self.uid, 'private_hash': hsh})
        pf_info = self.json_ok('info', {'uid': self.uid, 'path': public_file['path'], 'meta': 'visible'})
        assert pf_info['meta']['visible'] == 1

    def test_async_public_copy_infected_file(self):
        self.uid_1 = user_1.uid
        self.create_user(self.uid_1)
        self.make_dir(True)
        self.make_file()
        self.upload_file(self.uid, '/disk/pub/infected file.ext',
                         file_data={'drweb': 'infected'})
        file_data = decompress_data(mpfs_db.user_data.find_one(
            {'uid': self.uid, 'path': '/disk/pub/infected file.ext'})['zdata'])
        self.assertEqual(file_data['meta']['drweb'], 2)
        opts = {
            'uid': self.uid,
            'path': '/disk/pub',
            'meta': ''
            }
        folder_info = self.json_ok('info', opts)
        public_hash = folder_info['meta']['public_hash']
        public_addr = PublicAddress.Make(public_hash, '/infected file.ext')
        opts = {
            'uid': self.uid_1,
            'private_hash': public_addr.id,
            }
        self.json_error('async_public_copy', opts, code=129)
        public_addr = PublicAddress.Make(public_hash, '/' + self.pub_filename)
        opts = {
            'uid': self.uid_1,
            'private_hash': public_addr.id,
            }
        self.json_ok('async_public_copy', opts)

        all_files = mpfs_db.user_data.find({'uid': self.uid_1})
        self.assertTrue(any([f['key'].endswith(self.pub_filename) for f in all_files]))
        self.assertEqual(mpfs_db.user_data.find_one(
            {'uid': self.uid_1, 'path': '/disk/Загрузки/infected file.ext'}), None)

    @pytest.mark.skipif(not REAL_MONGO,
                        reason='https://st.yandex-team.ru/CHEMODAN-34246')
    def test_copy_public_file(self):
        self.uid_3 = user_1.uid
        self.create_user(self.uid_3)
        self.upload_file(self.uid, '/disk/public_file.txt')
        opts = {
            'uid': self.uid,
            'path': '/disk/public_file.txt',
            }
        self.json_ok('set_public', opts)
        file_info = mpfs_db.user_data.find_one(
            {'uid': self.uid, 'key': '/disk/public_file.txt'})
        self.assertNotEqual(file_info, None)
        stids = dict((i['type'], i['stid'])
                     for i in file_info['data']['stids'])
        self.assertTrue('pmid' in stids)
        self.assertEqual(type(stids['pmid']), unicode)
        opts = {
            'uid': self.uid,
            'path': '/disk/public_file.txt',
            'meta': '',
            }
        file_info = self.json_ok('info', opts)
        public_hash = file_info['meta']['public_hash']
        self.assertTrue('pmid' in file_info['meta'])

        self.upload_file(self.uid, '/disk/public_file_1.txt')
        opts = {
            'uid': self.uid,
            'path': '/disk/public_file_1.txt',
            }
        self.json_ok('set_public', opts)
        opts['meta'] = ''
        file_info_1 = self.json_ok('info', opts)
        public_hash_1 = file_info_1['meta']['public_hash']
        self.assertTrue('pmid' in file_info_1['meta'])
        opts = {
            'uid': self.uid_3,
            'private_hash': public_hash,
            'name': None,
            'connection_id': '',
            }
        self.json_ok('public_copy', opts)
        file_info = mpfs_db.user_data.find_one(
            {'uid': self.uid_3, 'key': '/disk/Загрузки/public_file.txt'})
        self.assertNotEqual(file_info, None)
        stids = dict((i['type'], i['stid'])
                     for i in file_info['data']['stids'])
        self.assertTrue('pmid' in stids)
        self.assertTrue(stids['pmid'])
        self.assertEqual(type(stids['pmid']), unicode)
        #======================================================================
        # CHEMODAN_9878
        # Файл будет копироваться по хардлинку, удаление pmid у всех копий.
        for uid in (self.uid_3, self.uid):
            public_hardlink = mpfs_db.user_data.find_one(
                {'uid': uid, 'key': '/disk/public_file_1.txt'})
            if public_hardlink:
                for coll in ('user_data', 'trash', 'hidden_data', 'attach_data'):
                    for element in mpfs_db[coll].find({'uid': uid, 'hid': public_hardlink['hid']}):
                        stids_data = dict((i['type'], i['stid'])
                                          for i in element['data']['stids'])
                        stids_data.pop('pmid')
                        element['data']['stids'] = map(
                            lambda (k, v): {'type': k, 'stid': v}, stids_data.iteritems())
                        mpfs_db[coll].update(
                            {'uid': uid, '_id': element['_id']}, {'$set': {'data': element['data']}})
        count_before = mpfs_db.user_data.find({'uid': self.uid_3}).count()
        opts = {
            'uid': self.uid_3,
            'private_hash': public_hash_1,
            'name': None,
            'connection_id': '',
            }
        self.json_ok('public_copy', opts)
        self.assertEqual(count_before + 1,
                         mpfs_db.user_data.find({'uid': self.uid_3}).count())
        file_info_1 = mpfs_db.user_data.find_one(
            {'uid': self.uid_3, 'key': '/disk/Загрузки/public_file_1.txt'})
        self.assertNotEqual(file_info_1, None)
        self.assertEqual(public_hardlink['hid'], file_info_1['hid'])
        for each in file_info_1['data']['stids']:
            if each['type'] == 'pmid':
                self.fail()
        opts = {
            'uid': self.uid_3,
            'path': '/disk/Загрузки/public_file.txt',
            'meta': '',
            }
        file_info = self.json_ok('info', opts)
        self.assertTrue('pmid' in file_info['meta'])
        opts = {
            'uid': self.uid_3,
            'path': '/disk/Загрузки/public_file_1.txt',
            'meta': '',
            }
        file_info = self.json_ok('info', opts)
        self.assertFalse('pmid' in file_info['meta'])

    def test_do_not_lock_original_file_on_public_copy(self):
        """
        Проверяем, что оригинальный файл не лочится при его публичном копировании
        https://st.yandex-team.ru/CHEMODAN-22243
        """
        self.create_user(self.uid_1)

        self.upload_file(self.uid, '/disk/public_file.txt')
        result = self.json_ok('set_public',  {'uid': self.uid, 'path': '/disk/public_file.txt'})
        private_hash = result['hash']

        # ломаем лочилку ресурсов, чтобы не удаляла данные
        from mpfs.core.metastorage.control import fs_locks
        fs_locks.release = lambda x, y: True

        # делаем копию синхронно
        self.json_ok('public_copy', {'uid': self.uid_1, 'private_hash': private_hash})

        # получаем все проставленные локи по self.uid и смотрим, что лока на оригинальный файл не было
        all_locks = fs_locks.find_all(self.uid)
        assert '/disk/public_file.txt' not in all_locks

        # делаем копию асинхронно
        self.json_ok('async_public_copy', {'uid': self.uid_1, 'private_hash': private_hash})

        # получаем все проставленные локи по self.uid и смотрим, что лока на оригинальный файл не было
        all_locks = fs_locks.find_all(self.uid)
        assert '/disk/public_file.txt' not in all_locks

    def test_async_copy_default_mail_disk_status_return_lenta_block_id_when_lenta_200(self):
        """Проверить что при сохранении файла из почты в диск,
        после отработки операции ручка `status` возвращает `lenta_block_id`."""
        self.create_user(self.user_1.uid)

        with mock.patch(
            'mpfs.core.services.mail_service.MailTVM.get_mime_part_name',
            return_value='test.jpg'
        ):
            with mock.patch(
                'mpfs.core.services.lenta_loader_service.LentaLoaderService.open_url',
                return_value=(200, '{"block_id":"100500"}', {})
            ):
                result = self.json_ok('async_copy_default', {
                    'uid': self.user_1.uid,
                    'path': '/mail/file:2280000020845606474:1.1'
                })
                assert 'oid' in result
                oid = result['oid']
                assert 'type' in result
                assert result['type'] == 'copy'

                result = self.json_ok('status', {'uid': self.user_1.uid, 'oid': oid})

                assert result['status'] == 'WAITING'

                KladunMocker().mock_kladun_callbacks_for_upload_from_service(self.user_1.uid, oid)

                result = self.json_ok('status', {'uid': self.user_1.uid, 'oid': oid})
                assert result['status'] == 'DONE'

                assert tags.LENTA_BLOCK_ID in result
                assert result[tags.LENTA_BLOCK_ID] == '100500'

    def test_async_copy_default_mail_disk_notify_lenta_correct_data_if_hard_linked(self):
        """Проверить случай, когда файл схардлинкался."""
        uid = self.uid

        random_size = str(random.randint(1, 100000))
        random_md5 = hashlib.md5(random_size).hexdigest()
        random_sha256 = hashlib.sha256(random_size).hexdigest()

        # загружаем файл с нужными параметрами, чтоб на него могли схардлинкаться
        self.upload_file(uid, '/disk/random.file', file_data={
            'size': random_size,
            'md5': random_md5,
            'sha256': random_sha256
        })

        with mock.patch(
            'mpfs.core.services.mail_service.MailTVM.get_mime_part_name',
            return_value='test.jpg'
        ):
            # создаем операцию
            result = self.json_ok('async_copy_default', {
                'uid': uid,
                'path': '/mail/file:2280000020845606474:1.1'
            })
            assert 'oid' in result
            oid = result['oid']

        # подставляем параметры как у файла, который у нас есть (можем схардлинкать)
        with open('fixtures/xml/kladun/upload-from-service/commitFileInfo_hard_linked.xml') as f:
            template = f.read()
            commit_file_info_xml_data = template.replace(
                '{{ uid }}', str(uid)
            ).replace('{{ oid }}', str(oid)).replace('{{ size }}', random_size).replace(
                '{{ md5 }}', random_md5
            ).replace('{{ sha256 }}', random_sha256)

        with KladunStub(
            status_values=(
                etree.fromstring(commit_file_info_xml_data),
            )
        ):
            with mock.patch(
                'mpfs.core.filesystem.base.Filesystem.hardlink',
                return_value=attrdict.AttrDict({'file_id': '100500'})
            ):
                with mock.patch(
                    'mpfs.core.services.mulca_service.Mulca.is_file_exist',
                    return_value=True
                ):
                    lenta_block_id = '100500300'
                    with mock.patch.object(
                        LentaLoaderService,
                        'save_file_from_public',
                        return_value=lenta_block_id
                    ) as mocked_save_file_from_public:
                        # даунлоадер скачивает файл и хардлинкает его
                        result = self.service('kladun_callback', {
                            'uid': uid,
                            'oid': oid,
                            'status_xml': commit_file_info_xml_data,
                            'type': 'commitFileInfo'
                        })
                        assert result['code'] == codes.KLADUN_HARDLINK_FOUND
                        assert mocked_save_file_from_public.called

                        result = self.json_ok('status', {
                            'uid': uid,
                            'oid': oid
                        })
                        assert tags.LENTA_BLOCK_ID in result
                        assert result[tags.LENTA_BLOCK_ID] == lenta_block_id

    def test_async_copy_default_mail_disk_notify_lenta_correct_data(self):
        self.create_user(self.user_1.uid)

        with mock.patch(
            'mpfs.core.services.mail_service.MailTVM.get_mime_part_name',
            return_value='test.jpg'
        ):
            with mock.patch.object(
                LentaLoaderService,
                'save_file_from_public',
                wraps=LentaLoaderService().save_file_from_public
            ) as mocked_save_file_from_public:

                result = self.json_ok('async_copy_default', {
                    'uid': self.user_1.uid,
                    'path': '/mail/file:2280000020845606474:1.1'
                })
                assert 'oid' in result
                oid = result['oid']

                KladunMocker().mock_kladun_callbacks_for_upload_from_service(self.user_1.uid, oid)

                # проверяем что корректные данные передали Ленте
                args, kwargs = mocked_save_file_from_public.call_args
                if 'fs_copy_event_log_message' in kwargs:
                    message = kwargs['fs_copy_event_log_message']
                else:
                    (message,) = args

                pairs = message.split()[1:]  # сплитим по табу и обрезаем tskv
                pairs = dict([pair.split('=') for pair in pairs])
                assert 'tgt_folder_id' in pairs
                assert 'tgt_rawaddress' in pairs
                assert 'resource_file_id' in pairs
                assert 'subtype' in pairs
                assert pairs['subtype'] == 'mail_disk'
                assert 'type' in pairs
                assert pairs['type'] == 'copy'

    def test_async_copy_default_mail_disk_post_correct_data_to_kladun(self):
        """Проверить, что при передаче mail mid и part id в ручку, они пробрасываются в кладун
        как есть и не преобразуются в stid."""
        # https://st.yandex-team.ru/CHEMODAN-35942
        uid = self.user_1.uid
        self.create_user(uid)

        with mock.patch(
            'mpfs.core.services.mail_service.MailTVM.get_mime_part_name',
            return_value='test.jpg'
        ):
            with mock.patch(
                'mpfs.core.operations.filesystem.copy.CopyToDisk.post_request_to_kladun',
                wraps=CopyToDisk.post_request_to_kladun
            ) as mocked_post_request_to_kladun:
                mail_mid = '2280000020845606474'
                mail_hid = '1.1'
                result = self.json_ok('async_copy_default', {
                    'uid': self.user_1.uid,
                    'path': '/mail/file:%s:%s' % (mail_mid, mail_hid)
                })
                assert 'oid' in result

                assert mocked_post_request_to_kladun.called
                args, kwargs = mocked_post_request_to_kladun.call_args
                (post_data,) = args
                print post_data
                assert post_data['source-service'] == 'mail2'
                assert post_data['service-file-id'] == '%s:%s/%s' % (uid, mail_mid, mail_hid)

    def test_async_public_copy_disk_disk_status_return_lenta_block_id_if_lenta_loader_200(self):
        """Проверить, что после выполнения операции `async_public_copy` ручка `status`
        возвращает ключ `lenta_block_id`."""
        self.create_user(self.user_1.uid)
        self.create_user(self.user_2.uid)

        self.upload_file(self.user_1.uid, '/disk/trump.jpg')
        result = self.json_ok('set_public', {
            'uid': self.user_1.uid,
            'path': '/disk/trump.jpg'
        })
        public_hash = result['hash']

        lenta_block_id = '100500'
        with mock.patch(
            'mpfs.core.services.lenta_loader_service.LentaLoaderService.open_url',
            return_value=(200, to_json({'block_id': lenta_block_id}), {})
        ):
            result = self.json_ok(
                'async_public_copy',
                {
                    'uid': self.user_2.uid,
                    'private_hash': public_hash,  # :)
                }
            )

        oid = result['oid']
        result = self.json_ok('status', {
            'uid': self.user_2.uid,
            'oid': oid
        })
        assert tags.LENTA_BLOCK_ID in result
        assert result[tags.LENTA_BLOCK_ID] == lenta_block_id

    def test_async_copy_default_mail_disk_set_completed_called_after_lenta_block_id_obtained(self):
        """Проверить что перед тем как операция будет выполнена (COMPLETED)
        в ней будет выставлен ключ `lenta_block_id`."""
        self.create_user(self.user_1.uid)

        with mock.patch(
            'mpfs.core.services.mail_service.MailTVM.get_mime_part_name',
            return_value='test.jpg'
        ):
            lenta_block_id = '100500'
            with mock.patch(
                'mpfs.core.services.lenta_loader_service.LentaLoaderService.open_url',
                return_value=(200, '{"block_id":"%s"}' % lenta_block_id, {})
            ):
                result = self.json_ok('async_copy_default', {
                    'uid': self.user_1.uid,
                    'path': '/mail/file:2280000020845606474:1.1'
                })
                assert 'oid' in result
                oid = result['oid']
                assert 'type' in result
                assert result['type'] == 'copy'

                result = self.json_ok('status', {'uid': self.user_1.uid, 'oid': oid})

                operation = manager.get_operation(uid=self.user_1.uid, oid=oid)
                assert isinstance(operation, CopyMailDisk)

                assert result['status'] == 'WAITING'

                original_change_state = CopyMailDisk.change_state

                call_args_list = []

                def change_state_with_assert(self_, *args, **kwargs):
                    # метод, который подменяет оригинальный метод изменения состояния
                    # и сохраняет контекст с которым бы была вызвана оригинальная функция
                    call_args_list.append((self_, args, kwargs))
                    return original_change_state(self_, *args, **kwargs)

                CopyMailDisk.change_state = change_state_with_assert

                KladunMocker().mock_kladun_callbacks_for_upload_from_service(self.user_1.uid, oid)

                # проверяем что вообще было вызвано изменение стейта на COMPLETED
                states = []
                for call in call_args_list:
                    operation, args, kwargs = call
                    (state,) = args
                    states.append(state)

                assert codes.COMPLETED in states

                # ищем тот вызов, когда проставлялся COMPLETED и смотрим есть ли в данных операции ключ `lenta_block_id`
                for call in call_args_list:
                    operation, args, kwargs = call
                    (state,) = args
                    if state != codes.COMPLETED:
                        continue

                    assert tags.LENTA_BLOCK_ID in operation.data
                    assert operation.data[tags.LENTA_BLOCK_ID] == lenta_block_id

                result = self.json_ok('status', {'uid': self.user_1.uid, 'oid': oid})

                assert result['status'] == 'DONE'

                assert tags.LENTA_BLOCK_ID in result
                assert result[tags.LENTA_BLOCK_ID] == lenta_block_id

    def test_async_public_copy_disk_disk_folder_lenta_loader_called_once(self):
        """Проверить сохранение папки из паблика (из диска в диск). Важно чтобы мы не дёргали Ленту 2 и более раз."""

        self.create_user(self.user_1.uid)
        self.create_user(self.user_2.uid)

        self.json_ok('mkdir', {
            'uid': self.user_1.uid,
            'path': '/disk/public_folder'
        })
        result = self.json_ok('set_public', {
            'uid': self.user_1.uid,
            'path': '/disk/public_folder'
        })
        public_hash = result['hash']

        lenta_block_id = '100500'
        with mock.patch(
            'mpfs.core.services.lenta_loader_service.LentaLoaderService'
            '.start_saving_folder_from_public',
            return_value=lenta_block_id
        ) as mocked_start_saving_folder_from_public:
            with mock.patch(
                'mpfs.core.services.lenta_loader_service.LentaLoaderService'
                '.process_log_line_and_return_created_block_id'
            ) as mocked_process_log_line_and_return_created_block_id:

                result = self.json_ok('async_public_copy', {
                    'uid': self.user_2.uid,
                    'private_hash': public_hash
                })
                oid = result['oid']

                mocked_start_saving_folder_from_public.assert_called_once()

                assert tags.LENTA_BLOCK_ID in result
                assert result[tags.LENTA_BLOCK_ID] == lenta_block_id

                assert not mocked_process_log_line_and_return_created_block_id.called

                result = self.json_ok('status', {
                    'oid': oid,
                    'uid': self.user_2.uid
                })
                assert tags.LENTA_BLOCK_ID in result
                assert result[tags.LENTA_BLOCK_ID] == lenta_block_id

    def test_async_public_copy_disk_disk_folder_lenta_called_with_correct_file_id(self):
        """Протестировать что в Ленту передается при этом корректный идентификатор файла/папки."""
        self.create_user(self.user_1.uid)
        self.create_user(self.user_2.uid)

        dir_name = 'public_folder'
        self.json_ok('mkdir', {
            'uid': self.user_1.uid,
            'path': '/disk/%s' % dir_name
        })
        result = self.json_ok('set_public', {
            'uid': self.user_1.uid,
            'path': '/disk/%s' % dir_name
        })
        public_hash = result['hash']

        lenta_block_id = '100500'
        with mock.patch(
            'mpfs.core.services.lenta_loader_service.LentaLoaderService'
            '.start_saving_folder_from_public',
            return_value=lenta_block_id
        ) as mocked_start_saving_folder_from_public:

            self.json_ok('async_public_copy', {
                'uid': self.user_2.uid,
                'private_hash': public_hash,
                'save_path': '/disk'
            })
            args, kwargs = mocked_start_saving_folder_from_public.call_args
            uid, raw_resource_id_passed_to_lenta, path = args

            result = self.json_ok('list', {
                'uid': self.user_2.uid,
                'path': '/disk/%s' % dir_name,
                'meta': 'resource_id'
            })
            [folder_info] = result
            raw_resource_id_in_database = folder_info['meta']['resource_id']

            assert raw_resource_id_passed_to_lenta == raw_resource_id_in_database

    def test_list_with_all_meta_after_rm_public_file_succeeds(self):
        """https://st.yandex-team.ru/CHEMODAN-37231"""
        self.upload_file(self.uid, '/disk/file')
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/file'})
        self.json_ok('rm', {'uid': self.uid, 'path': '/disk/file'})
        self.support_ok('list', {'uid': self.uid, 'path': '/hidden/', 'meta': ''})

    def test_async_public_copy_by_any_url(self):
        self.upload_file(self.uid, '/disk/1.jpg')
        uid2 = self.user_2.uid
        self.create_user(uid2)
        urls = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/1.jpg'})
        for n, url_type in enumerate(['url', 'short_url', 'hash', 'short_url_named'], 1):
            url = urls[url_type]
            file_name = 'test_%s' % n
            self.json_ok('async_public_copy',
                         {'uid': uid2, 'private_hash': url, 'save_path': '/disk', 'name': file_name})
            info = self.json_ok('info', {'uid': uid2, 'path': '/disk/' + file_name})
            assert 'file' == info['type']

    def test_async_public_copy_file_from_folder_by_any_url(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/d1'})
        self.upload_file(self.uid, '/disk/d1/1.jpg')
        uid2 = self.user_2.uid
        self.create_user(uid2)
        urls = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/d1'})
        for n, (url_type, path) in enumerate([('url', '%3A/1.jpg'), ('short_url', '/1.jpg'), ('hash', ':/1.jpg'),
                                              ('short_url_named', '/1.jpg')], 1):
            url = urls[url_type]
            file_name = 'test_%s' % n
            if url_type == 'short_url_named':
                private_hash = url.replace('?', path + '?')
            else:
                private_hash = url + path
            self.json_ok('async_public_copy',
                         {'uid': uid2, 'private_hash': private_hash, 'save_path': '/disk', 'name': file_name})
            info = self.json_ok('info', {'uid': uid2, 'path': '/disk/' + file_name})
            assert 'file' == info['type']

    def test_public_copy_for_blocked_by_passport_account(self):
        self.create_user(self.user_2.uid)
        self.upload_file(uid=self.uid, path='/disk/public_file.jpg')
        public_hash = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/public_file.jpg'})['hash']

        update_info_by_uid(self.uid, is_enabled=False)

        self.json_error('public_copy', {'uid': self.user_2.uid, 'private_hash': public_hash}, code=codes.RESOURCE_NOT_FOUND)
        self.json_error('async_public_copy', {'uid': self.user_2.uid, 'private_hash': public_hash}, code=codes.RESOURCE_NOT_FOUND)

    def test_public_copy_sends_djfs_albums_callbacks(self):
        """
        Проверяем, что при копировании с паблика отправляем таск для djfs
        """
        self.create_user(self.user_1.uid)
        self.create_user(self.user_2.uid)
        self.create_user(self.user_3.uid)

        self.json_ok('mkdir', {
            'uid': self.user_1.uid,
            'path': '/disk/public_folder'
        })
        self.upload_file_with_coordinates(self.user_1.uid, '/disk/public_folder/file.jpg')
        result = self.json_ok('set_public', {
            'uid': self.user_1.uid,
            'path': '/disk/public_folder'
        })
        public_hash = result['hash']

        # Проверяем асинхронное копирование
        with mock.patch('mpfs.core.albums.logic.common.send_djfs_albums_callback') as mock_obj:
            self.json_ok('async_public_copy', {
                'uid': self.user_2.uid,
                'private_hash': public_hash,
                'force_djfs_albums_callback': True
            })
            assert mock_obj.call_count == 1
            assert mock_obj.call_args_list[0][0][0] == self.user_2.uid

        # Проверяем синхронное копирование
        with mock.patch('mpfs.core.albums.logic.common.send_djfs_albums_callback') as mock_obj:
            self.json_ok('public_copy', {
                'uid': self.user_3.uid,
                'private_hash': public_hash,
                'force_djfs_albums_callback': True
            })
            assert mock_obj.call_count == 1
            assert mock_obj.call_args_list[0][0][0] == self.user_3.uid
