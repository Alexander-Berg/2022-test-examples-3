# -*- coding: utf-8 -*-
import random
import re
import time
import datetime
from threading import Thread
from urllib2 import HTTPError

import mock
import os
import pytest
import urllib

from hamcrest import contains, has_entries
from nose_parameterized import parameterized

import mpfs.engine.process
from mpfs.common import errors
from mpfs.common.static import codes
from mpfs.common.util.overdraft import OVERDRAFT_KEY, OVERDRAFT_RESET_COUNT_FIELD

from mpfs.config import settings
from mpfs.core.billing.dao.overdraft import OverdraftDAO
from mpfs.core.filesystem.hardlinks.common import FileChecksums
from mpfs.core.metastorage.control import support_prohibited_cleaning_users, disk_info
from mpfs.core.services.djfs_albums import djfs_albums
from mpfs.core.services.startrek_service import startrek_service
from mpfs.core.support.operations import ReindexFacesAlbumOperation
from mpfs.core.user.base import User
from mpfs.dao.session import Session
from mpfs.metastorage.mongo.collections.system import DiskInfoCollection

from test.base import DiskTestCase, patch_open_url
from test.api_suit import set_up_mailbox, tear_down_mailbox
from test.fixtures import users
from mpfs.metastorage.mongo.binary import Binary
from mpfs.common.static import codes
from mpfs.core.filesystem import hardlinks
from mpfs.core import factory
from mpfs.core.operations import manager
from mpfs.core.support.comment import select as select_comments_by_uid
from mpfs.core.services.clck_service import Clck
from test.conftest import REAL_MONGO, INIT_USER_IN_POSTGRES
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase
from test.helpers.size_units import GB
from test.helpers.stubs.services import KladunStub

db = CollectionRoutedDatabase()


class SupportAPITestCase(DiskTestCase):

    uid_1 = users.user_1.uid
    uid_2 = users.user_3.uid
    uid_3 = users.user_4.uid
    localhost = '127.0.0.1'

    def test_search(self):
        """
        Проверяем, что ручка поиска ищет и файлы по имени и папки
        """
        folder_1_path = '/disk/folder-1'
        folder_2_path = os.path.join(folder_1_path, 'folder-2')
        folder_3_path = '/disk/folder-3'
        folder_4_path = '/disk/folder-4'
        folders = [folder_1_path, folder_2_path, folder_3_path, folder_4_path]
        for f in folders:
            self.json_ok('mkdir', {'uid': self.uid, 'path': f})

        file_1_path = os.path.join(folder_1_path, 'file-1')
        file_2_path = os.path.join(folder_1_path, 'file-2')
        file_3_path = os.path.join(folder_2_path, 'file-3')
        file_4_path = os.path.join(folder_3_path, 'file-4')
        file_5_path = os.path.join(folder_4_path, 'file-5')
        files = [file_1_path, file_2_path, file_3_path, file_4_path, file_5_path]
        for f in files:
            self.upload_file(self.uid, f)

        # проверяем, что поиск по файлам работает
        result = self.support_ok('file_search', {'uid': self.uid, 'query': 'file'})
        assert result['total'] == len(files)
        for f in files:
            assert f in map(lambda i: i['path'], result['result'])

        # проверяем, что поиск по папкам тоже работает
        result = self.support_ok('file_search', {'uid': self.uid, 'query': 'folder'})
        assert result['total'] == len(folders)
        for f in folders:
            assert f in map(lambda i: i['path'], result['result'])

        # проверяем поиск конкретной папки
        result = self.support_ok('file_search', {'uid': self.uid, 'query': 'folder-2'})
        assert result['total'] == 1
        assert result['result'][0]['path'] == folder_2_path

    def test_download_url(self):
        self.upload_file(self.uid, '/disk/file')
        opts = {'uid': self.uid, 'path': '/disk/file'}
        result = self.support_ok('download_url', opts)
        self.assertTrue(result.get('file'))

        from mpfs.core.services.mail_service import Mail

        class FakeMailService(Mail):
            def process_response(self, *args, **kwargs):
                from lxml import etree
                result = etree.fromstring(open('fixtures/xml/mail_service1.xml').read())
                return result

        from mpfs.core.filesystem.resources.mail import MailFile
        MailFile.service_class = FakeMailService
        opts = {'uid': self.uid, 'path': '/mail/folder:2280000140035024981/file:2280000020845606474:1.1'}
        result = self.support_ok('download_url', opts)
        self.assertTrue('webattach' in result['file'])

    def test_block_unblock_public_file(self):
        self.upload_file(self.uid, '/disk/file')
        opts = {'uid': self.uid, 'path': '/disk/file'}
        result = self.json_ok('set_public', opts)
        private_hash = result.get('hash')

        opts = {
            'moderator': 'testmoder',
            'comment': 'testcomment',
            'private_hash': private_hash,
            'type': 'block_file',
            'view': 'st',
            'link': 'http://mosgorsud.ru/aaa',
            'notify': 0,
        }

        result = self.support_ok('block_public_file', opts)
        self.assertTrue(result)

        opts = {'private_hash': private_hash}
        self.json_error('public_info', opts, code=96)

        opts = {'uid': self.uid, 'path': '/disk/file'}
        self.json_error('set_public', opts, code=96)

    def test_block_unblock_public_folder(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        private_hash = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/folder'})['hash']

        self.json_ok('public_list', {'private_hash': private_hash})

        opts = {
            'moderator': 'testmoder',
            'comment': 'testcomment',
            'private_hash': private_hash,
            'type': 'block_file',
            'view': 'st',
            'link': 'http://mosgorsud.ru/aaa',
            'notify': 0,
        }

        result = self.support_ok('block_public_file', opts)
        self.assertTrue(result)

        opts = {'private_hash': private_hash}
        self.json_error('public_info', opts, code=96)

        opts = {'uid': self.uid, 'path': '/disk/folder'}
        self.json_error('set_public', opts, code=96)

        self.support_ok('unblock_file', {'uid': self.uid, 'path': '%s:/disk/folder' % self.uid, 'moderator': 'testmoder'})

        self.json_error('public_info', {'private_hash': private_hash}, code=96)

        self.json_ok('set_public', {'uid': self.uid, 'path': '%s:/disk/folder' % self.uid})

    def test_block_unblock_public_file_and_hid(self):
        self.upload_file(self.uid, '/disk/file')

        info = self.json_ok('info', {'uid': self.uid, 'path': '/disk/file', 'meta': ''})
        md5 = info['meta']['md5']
        size = info['meta']['size']
        sha256 = info['meta']['sha256']
        hid = Binary(hardlinks.common.construct_hid(md5, size, sha256))

        private_hash = self.json_ok(
            'set_public',
            {'uid': self.uid, 'path': '/disk/file'})['hash']

        opts = {
            'moderator': 'testmoder',
            'comment': 'testcomment',
            'private_hash': private_hash,
            'type': 'block_file',
            'view': 'st',
            'link': 'http://mosgorsud.ru/aaa',
            'notify': 0,
            'hid_block_type': 'only_view_delete',
        }

        result = self.support_ok('block_public_file', opts)

        self.assertTrue(result)
        blocked_hid_value = db.support_blocked_hids.find_one({'hid': {'$in': [hid, ]}})
        assert blocked_hid_value
        assert 'ctime' in blocked_hid_value

        self.json_error(
            'async_social_export_photos',
            {
                'uid': self.uid,
                'provider': 'vk',
                'albumid': 111,
                'photos': '["/disk/file"]'
            },
            code=153
        )

        self.json_error('public_info', {'private_hash': private_hash}, code=96)

        self.support_ok('unblock_file', {
            'uid': self.uid,
            'path': '%s:/disk/file' % self.uid,
            'moderator': 'testmoder',
            'unblock_hid': True,
        })

        assert not db.support_blocked_hids.find_one({'hid': {'$in': [hid, ]}})

        self.json_ok('set_public', {'uid': self.uid, 'path': '%s:/disk/file' % self.uid})

    def test_find_by_hash(self):
        self.upload_file(self.uid, '/disk/file2')

        opts = {'uid': self.uid, 'path': '/disk/file2'}
        result = self.json_ok('set_public', opts)
        private_hash = result.get('hash')

        opts = {'uid': self.uid, 'path': '/disk/file2'}
        result = self.json_ok('set_private', opts)

        opts = {'private_hash': private_hash}
        result = self.json_error('public_info', opts, code=71)
        self.assertTrue(result['code'])

        opts = {'uid': self.uid, 'path': '/disk/file2'}
        result = self.json_ok('set_public', opts)
        private_hash = result.get('hash')

        opts = {
            'moderator': 'testmoder',
            'comment': 'testcomment',
            'private_hash': private_hash,
            'type': 'block_file',
            'view': 'st',
            'link': 'http://mosgorsud.ru/aaa',
            'notify': 0,
        }
        data = {
            'link': opts['link'],
            'view': opts['view'],
        }

        result = self.support_ok('block_public_file', opts)

        opts = {'private_hash': private_hash}

        self.json_error('public_info', opts, code=96, data=data)

        opts = {'hash': private_hash}
        result = self.support_ok('find_by_hash', opts)
        self.assertTrue(result['hash'])

    def test_restore_deleted(self):
        self.upload_file(self.uid, '/disk/file')
        opts = {'uid': self.uid, 'path': '/disk/file'}
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            result = self.json_ok('async_trash_append', opts)
        opts = {'uid': self.uid}
        result = self.json_ok('async_trash_drop_all', opts)
        opts = {'uid': self.uid, 'path': '/hidden'}
        result = self.support_ok('list', opts)
        path = result[1]['path']
        opts = {
            'uid': self.uid,
            'dest': '%s:%s' % (self.uid, '/disk/restored'),
            'path': '%s:%s' % (self.uid, path),
            'force': 0,
        }
        self.service_ok('restore_deleted', opts)
        opts = {'uid': self.uid, 'path': '/disk/restored/file'}
        result = self.json_ok('info', opts)
        opts = {'uid': self.uid, 'path': '/hidden'}
        result = self.support_ok('list', opts)
        self.assertTrue(len(result) == 1)

    @parameterized.expand([
        ('file', 'file'),
        ('file:12345', 'file'),
        ('file:aegaeg:12345', 'file:aegaeg'),
        ('file:aegaeg:12345.15', 'file:aegaeg'),
        ('file:12345:54321', 'file:12345'),
        ('file:12345:54321.15', 'file:12345'),
        ('file:12345.15:54321.15', 'file:12345.15'),
    ])
    def test_restore_deleted_timestamp(self, hidden_name, restored_name):
        self.upload_file(self.uid, '/disk/file')
        self.json_ok('rm', {'uid': self.uid, 'path': '/disk/file'})
        result = self.support_ok('list', {'uid': self.uid, 'path': '/hidden'})
        name = result[1]['name']

        session = Session.create_from_uid(self.uid)
        session.execute("UPDATE disk.files SET name = :newname WHERE uid = :uid AND name = :oldname", {
            'newname': hidden_name,
            'oldname': name,
            'uid': self.uid,
        })

        self.service_ok('restore_deleted', {
            'uid': self.uid,
            'dest': '%s:%s' % (self.uid, '/disk/restored'),
            'path': '%s:%s' % (self.uid, '/hidden/' + hidden_name),
            'force': 0,
        })
        self.json_ok('info', {'uid': self.uid, 'path': '/disk/restored/' + restored_name})
        result = self.support_ok('list', {'uid': self.uid, 'path': '/hidden'})
        self.assertTrue(len(result) == 1)

    def test_async_restore_deleted(self):
        self.upload_file(self.uid, '/disk/file2')
        opts = {'uid': self.uid, 'path': '/disk/file2'}
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.json_ok('async_trash_append', opts)

        opts = {'uid': self.uid}
        self.json_ok('async_trash_drop_all', opts)

        opts = {'uid': self.uid, 'path': '/hidden'}
        result = self.support_ok('list', opts)
        path = [x for x in result if '/hidden/file2:' in x.get('path')][0].get('path')
        opts = {
            'uid': self.uid,
            'dest': '%s:%s' % (self.uid, '/disk/restored'),
            'path': '%s:%s' % (self.uid, path),
            'force': 0,
        }
        self.support_ok('async_restore_deleted', opts)

        opts = {'uid': self.uid, 'path': '/disk/restored'}
        result = self.json_ok('list', opts)
        self.assertTrue([x for x in result if x.get('path') == '/disk/restored/file2'])

        opts = {'uid': self.uid, 'path': '/hidden/file2'}
        result = self.json_error('info', opts, code=71)

    def test_block_user(self):
        opts = {
            'uid': self.uid,
            'moderator': 'moderator',
            'comment': 'comment',
        }
        result = self.support_ok('block_user', opts)
        self.assertTrue(result['status'] == 1)

        self.json_error('list', {'uid': self.uid, 'path': '/disk'}, code=119)
        self.support_ok('list', {'uid': self.uid, 'path': '/disk'})

        opts = {
            'uid': self.uid,
            'moderator': 'moderator',
            'comment': 'comment',
        }
        result = self.support_ok('unblock_user', opts)
        self.assertTrue(result['status'] == 1)

        self.json_ok('list', {'uid': self.uid, 'path': '/disk'})
        self.support_ok('list', {'uid': self.uid, 'path': '/disk'})

    def test_set_delete_date_for_blocked_files(self):
        filename = '/disk/file%s' % random.randint(1, 100000)

        self.upload_file(self.uid, filename)

        opts = {'uid': self.uid, 'path': filename}
        result = self.json_ok('set_public', opts)

        private_hash = result.get('hash')

        opts = {
            'moderator': 'testmoder',
            'comment': 'testcomment',
            'private_hash': private_hash,
            'type': 'block_file',
            'view': 'st',
            'link': 'http://mosgorsud.ru/aaa',
            'notify': 0,
        }

        result = self.support_ok('block_public_file', opts)
        self.assertTrue(result)

        opts = {'uid': self.uid, 'path': filename}
        result = self.json_ok('trash_append', opts)

        opts = {'hash': private_hash}
        result = self.support_ok('find_by_hash', opts)
        self.assertTrue(result['del_date'])

        file_info = db.support_mpfs.find_one({'data.address' : '%s:%s' % (self.uid, filename)})
        self.assertTrue('del_date' in file_info['data'])
        self.assertTrue(isinstance(file_info['data']['del_date'], int))

    def test_trash_restore_all(self):
        opts = {'uid': self.uid, 'path': '/disk/folder_with_file'}
        self.json_ok('mkdir', opts)
        self.upload_file(self.uid, '/disk/file')
        self.upload_file(self.uid, '/disk/folder_with_file/file')
        opts = {'uid': self.uid, 'path': '/disk/file'}
        result = self.json_ok('async_trash_append', opts)
        opts = {'uid': self.uid, 'path': '/disk/folder_with_file'}
        result = self.json_ok('async_trash_append', opts)

        opts = {
            'uid': self.uid,
        }
        result = self.support_ok('trash_restore_all', opts)

        opts = {'uid': self.uid, 'path': '/disk'}
        result = self.json_ok('list', opts)
        restored = [x.get('id') for x in result if '/disk/trash' in x.get('id')][0]
        opts = {'uid': self.uid, 'path': restored}
        result = self.json_ok('list', opts)
        ids = [x.get('id') for x in result]
        self.assertTrue(restored + 'folder_with_file/' in ids)
        self.assertTrue(restored + 'file' in ids)

    def test_fail_operations(self):
        manager.create_operation(self.uid,
                                 'store',
                                 'disk',
                                 odata={'path': '%s:/disk/file-1.jpg' % self.uid,
                                        'md5': '098f6bcd4621d373cade4e832627b4f6',
                                        'size': 111})

        opts = {'uid': self.uid}
        result = self.json_ok('active_operations', opts)
        self.assertTrue(len(result) > 0)

        opts = {'uid': self.uid}
        self.support_ok('fail_operations', opts)

        opts = {'uid': self.uid}
        result = self.json_ok('active_operations', opts)
        self.assertTrue(len(result) == 0)

    def test_set_limit_based_on_services(self):
        '''
        Тест на выставление лимита по значениям услуг из billing_services

        Заводим юзеру разные услуги, в т.ч. с изменяемым местом
        Далее портим limit
        Далее пересчитываем и смотрим, что все зашибись

        '''
        # фиксированное место (250 ГБ)
        opts = {
            'uid': self.uid,
            'line': 'bonus',
            'pid': 'blat_250',
            'ip'   : self.localhost,
        }
        self.billing_ok('service_create', opts)

        # изменяемое место (1000 Б)
        opts = {
            'uid': self.uid,
            'line': 'bonus',
            'pid': 'yandex_mail_birthday',
            'product.amount': 1000,
            'ip'   : self.localhost,
        }
        self.billing_ok('service_create', opts)

        # проверяем, что получилось 10ГБ + 250ГБ + 1000Б = 279172875240 байт
        opts = {'uid': self.uid}
        correct_limit = self.json_ok('space', opts).get('limit')

        # портим лимит
        from mpfs.core.filesystem.quota import Quota
        Quota().set_limit(123456, uid=self.uid)

        # проверяем, что лимит попорчен
        opts = {'uid': self.uid}
        broken_limit = self.json_ok('space', opts).get('limit')
        self.assertEqual(broken_limit, 123456)

        # запускаем исправление
        opts = {'uid': self.uid, 'moderator': 'robot'}
        result = self.support_ok('set_limit_by_services', opts)
        print result

        # проверяем, что лимит исправлен
        opts = {'uid': self.uid}
        fixed_limit = self.json_ok('space', opts).get('limit')
        self.assertEqual(fixed_limit, correct_limit)

    def test_copy_move_blocked_hid(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder/folder1'})
        self.upload_file(self.uid, '/disk/folder/file')
        self.upload_file(self.uid, '/disk/folder/folder1/file1')
        self.upload_file(self.uid, '/disk/folder/folder1/file2')
        private_hash = self.json_ok(
            'set_public',
            {'uid': self.uid, 'path': '/disk/folder/folder1/file1'})['hash']

        opts = {
            'moderator': 'testmoder',
            'comment': 'testcomment',
            'private_hash': private_hash,
            'type': 'block_file',
            'view': 'st',
            'link': '',
            'notify': 0,
            'hid_block_type': 'only_view_delete',
        }
        self.support_ok('block_public_file', opts)

        # Копируем папку
        opts = {
            'uid': self.uid,
            'src': '/disk/folder',
            'dst': '/disk/folder_copy',
        }
        self.json_ok('async_copy', opts)

        # Проверяем, что она не скопировалась
        self.json_error('info', {'uid': self.uid, 'path': '/disk/folder_copy'}, code=71)

        # Копируем файл
        opts = {
            'uid': self.uid,
            'src': '/disk/folder/folder1/file1',
            'dst': '/disk/file_copy',
        }
        self.json_ok('async_copy', opts)

        # Проверяем, что он не скопировался
        self.json_error('info', {'uid': self.uid, 'path': '/disk/file_copy'}, code=71)

        # Перемещаем папку
        opts = {
            'uid': self.uid,
            'src': '/disk/folder',
            'dst': '/disk/folder_copy',
        }
        self.json_ok('async_move', opts)

        # Проверяем, что она не переместилась
        self.json_error('info', {'uid': self.uid, 'path': '/disk/folder_copy'}, code=71)
        # Проверяем, исходная папка осталась на месте
        self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder'})

        # Перемещаем файл
        opts = {
            'uid': self.uid,
            'src': '/disk/folder/folder1/file1',
            'dst': '/disk/file_copy',
        }
        self.json_ok('async_move', opts)

        # Проверяем, что он не переместился
        self.json_error('info', {'uid': self.uid, 'path': '/disk/file_copy'}, code=71)
        # Проверяем, что исходный файл остался на месте
        self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder/folder1/file1'})

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_upload_blocked_hid(self):
        self.upload_file(self.uid, '/disk/file')
        info = self.json_ok('info', {'uid': self.uid, 'path': '/disk/file', 'meta': ''})
        private_hash = self.json_ok(
            'set_public',
            {'uid': self.uid, 'path': '/disk/file'})['hash']

        opts = {
            'moderator': 'testmoder',
            'comment': 'testcomment',
            'private_hash': private_hash,
            'type': 'block_file',
            'view': 'st',
            'link': '',
            'notify': 0,
            'hid_block_type': 'only_view_delete',
        }
        self.support_ok('block_public_file', opts)

        with self.patch_mulca_is_file_exist(func_resp=True):
            self.json_error('store', {
                'uid': self.uid,
                'path': '/disk/file2',
                'size': info['meta']['size'],
                'md5': info['meta']['md5'],
                'sha256': info['meta']['sha256'],
            }, code=153)

        file_data = {
            'size': info['meta']['size'],
            'md5': info['meta']['md5'],
            'sha256': info['meta']['sha256'],
        }
        with self.patch_mulca_is_file_exist(func_resp=True):
            self.upload_file(self.uid, '/disk/file2', file_data=file_data, callback_failed=True)

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Postgres test')
    def test_set_public_blocked_hid(self):
        self.upload_file(self.uid, '/disk/file')
        info = self.json_ok('info', {'uid': self.uid, 'path': '/disk/file', 'meta': ''})

        with self.patch_mulca_is_file_exist(func_resp=True):
            self.json_ok('store', {
                'uid': self.uid,
                'path': '/disk/file2',
                'size': info['meta']['size'],
                'md5': info['meta']['md5'],
                'sha256': info['meta']['sha256'],
            })

        private_hash = self.json_ok(
            'set_public',
            {'uid': self.uid, 'path': '/disk/file'})['hash']

        opts = {
            'moderator': 'testmoder',
            'comment': 'testcomment',
            'private_hash': private_hash,
            'type': 'block_file',
            'view': 'st',
            'link': '',
            'notify': 0,
            'hid_block_type': 'only_view_delete',
        }
        self.support_ok('block_public_file', opts)

        self.json_error('set_public', {'uid': self.uid, 'path': '/disk/file2'}, code=153)

    def test_block_hid_from_related_public_address(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        self.upload_file(self.uid, '/disk/folder/file')
        private_hash = self.json_ok(
            'set_public',
            {'uid': self.uid, 'path': '/disk/folder'})['hash']
        private_relative_hash = "%s%s" % (private_hash, ':/file')

        opts = {
            'moderator': 'testmoder',
            'comment': 'testcomment',
            'private_hash': private_relative_hash,
            'type': 'block_file',
            'view': 'st',
            'link': '',
            'notify': 0,
            'hid_block_type': 'only_view_delete',
        }
        self.support_ok('block_public_file', opts)

        #  Проверяем, что папка не заблокировалась
        self.json_ok('public_info', {'private_hash': private_hash})

        #  А файл заблокировался по хиду
        self.json_error('set_public', {'uid': self.uid, 'path': '/disk/folder/file'}, code=153)

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_get_uids_with_public_hid(self):
        self.create_user(self.uid_1)
        self.create_user(self.uid_2)
        self.upload_file(self.uid, '/disk/file')
        meta1 = self.json_ok('info', {'uid': self.uid, 'path': '/disk/file', 'meta': ''})['meta']

        with self.patch_mulca_is_file_exist(func_resp=True):
            self.json_ok(
                'store',
                {
                    'uid': self.uid_1, 'path': '/disk/file',
                    'size': meta1['size'], 'md5': meta1['md5'], 'sha256': meta1['sha256']
                }
            )
            self.json_ok(
                'store',
                {
                    'uid': self.uid_2, 'path': '/disk/file',
                    'size': meta1['size'],
                    'md5': meta1['md5'], 'sha256': meta1['sha256']
                }
            )
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/file'})
        self.json_ok('set_public', {'uid': self.uid_1, 'path': '/disk/file'})
        uids = self.support_ok('uids_with_public_hid', {'hid': meta1['hid']})

        assert self.uid in uids
        assert self.uid_1 in uids
        assert self.uid_2 not in uids

        self.json_ok('set_public', {'uid': self.uid_2, 'path': '/disk/file'})

        uids = self.support_ok('uids_with_public_hid', {'hid': meta1['hid']})

        assert self.uid in uids
        assert self.uid_1 in uids
        assert self.uid_2 in uids

        self.json_ok('set_private', {'uid': self.uid_2, 'path': '/disk/file'})

        uids = self.support_ok('uids_with_public_hid', {'hid': meta1['hid']})

        assert self.uid in uids
        assert self.uid_1 in uids
        assert self.uid_2 not in uids

    def test_get_block_history(self):
        for i in range(5):
            self.support_ok(
                'batch_block',
                {
                    'hids': '%i' % i,
                    'uids': '',
                    'public_hashes': '',
                    'moderator': 'testmoder',
                    'comment_text': 'testcomment',
                    'comment_type': ''
                }
            )
            time.sleep(1)

        all_history = self.support_ok('get_block_history')
        all_ctimes = [i['ctime'] for i in all_history['result']]
        assert all_history['total'] == 5

        history = self.support_ok('get_block_history', {'amount': 2, 'offset': 0, 'order': 0})
        ctimes = [i['ctime'] for i in history['result']]
        assert sorted(all_ctimes, reverse=True)[:2] == ctimes
        assert history['total'] == 5

        history = self.support_ok('get_block_history', {'amount': 3, 'offset': 0, 'order': 1})
        ctimes = [i['ctime'] for i in history['result']]
        assert sorted(all_ctimes)[:3]
        assert history['total'] == 5

        history = self.support_ok('get_block_history', {'amount': 2, 'offset': 1, 'order': 1})
        ctimes = [i['ctime'] for i in history['result']]
        assert sorted(all_ctimes)[1:3]
        assert history['total'] == 5

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_batch_block(self):
        self.create_user(self.uid_1)
        self.create_user(self.uid_2)
        self.upload_file(self.uid, '/disk/file')
        self.upload_file(self.uid_1, '/disk/file')
        self.upload_file(self.uid_1, '/disk/file2')

        info1 = self.json_ok('info', {'uid': self.uid, 'path': '/disk/file', 'meta': ''})
        info2 = self.json_ok('info', {'uid': self.uid_1, 'path': '/disk/file', 'meta': ''})
        info3 = self.json_ok('info', {'uid': self.uid_1, 'path': '/disk/file2', 'meta': ''})

        with self.patch_mulca_is_file_exist(func_resp=True):
            self.json_ok(
                'store', {
                    'uid': self.uid_2,
                    'path': '/disk/hardlinked_file_before_block',
                    'size': info1['meta']['size'],
                    'md5': info1['meta']['md5'],
                    'sha256': info1['meta']['sha256'],
                },
            )

        private_hash1 = self.json_ok(
            'set_public',
            {'uid': self.uid, 'path': '/disk/file'})['hash']
        private_hash2 = self.json_ok(
            'set_public',
            {'uid': self.uid_1, 'path': '/disk/file'})['hash']

        self.support_ok(
            'batch_block',
            {
                'uids': '%s,%s' % (self.uid, self.uid_1),
                'hids': '%s,%s,%s' % (info1['meta']['hid'], info2['meta']['hid'], info3['meta']['hid']),
                'public_hashes': '%s,%s' % (private_hash1, private_hash2),
                'moderator': 'testmoder',
                'comment_text': 'testcomment',
                'comment_type': ''
            }
        )

        self.json_error('list', {'uid': self.uid, 'path': '/disk'}, code=119)
        self.json_error('list', {'uid': self.uid_1, 'path': '/disk'}, code=119)

        with self.patch_mulca_is_file_exist(func_resp=True):
            self.json_error(
                'store',
                {
                    'uid': self.uid_2,
                    'path': '/disk/hardlinked_file',
                    'size': info1['meta']['size'],
                    'md5': info1['meta']['md5'],
                    'sha256': info1['meta']['sha256'],
                },
                code=153,
            )

        with self.patch_mulca_is_file_exist(func_resp=True):
            self.json_error(
                'store',
                {
                    'uid': self.uid_2,
                    'path': '/disk/hardlinked_file',
                    'size': info3['meta']['size'],
                    'md5': info3['meta']['md5'],
                    'sha256': info3['meta']['sha256'],
                },
                code=153,
            )

        self.json_error('video_url', {'uid': self.uid_2, 'path': '/disk/hardlinked_file_before_block'}, code=153)

        history = self.support_ok('get_block_history', {'amount': 100, 'offset': 0})['result']
        assert len(history) > 0

        self.support_ok(
            'batch_unblock',
            {
                'id': history[0]['_id'],
                'moderator': 'testmoder',
                'comment_text': 'testcomment',
            }
        )

        self.json_ok('list', {'uid': self.uid, 'path': '/disk'})
        self.json_ok('list', {'uid': self.uid_1, 'path': '/disk'})

        with self.patch_mulca_is_file_exist(func_resp=True):
            self.json_ok(
                'store',
                {
                    'uid': self.uid_2,
                    'path': '/disk/hardlinked_file',
                    'size': info1['meta']['size'],
                    'md5': info1['meta']['md5'],
                    'sha256': info1['meta']['sha256'],
                },
            )

        history1 = self.support_ok('get_block_history', {'amount': 100, 'offset': 0})['result']
        assert len(history1) > len(history)

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_async_block_uids_by_hid(self):
        self.upload_file(self.uid, '/disk/file')
        meta1 = self.json_ok('info', {'uid': self.uid, 'path': '/disk/file', 'meta': ''})['meta']

        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/file'})

        self.support_ok(
            'async_block_uids_by_hid',
            {'hid': meta1['hid'], 'moderator': 'm', 'comment_text': 'c', 'type': 'only_view_delete'}
        )

        self.json_error('list', {'uid': self.uid, 'path': '/disk'}, code=119)

        self.support_ok('unblock_user', {'uid': self.uid, 'moderator': 'moderator', 'comment': 'comment'})

        with self.patch_mulca_is_file_exist(func_resp=True):
            self.json_error(
                'store',
                {
                    'uid': self.uid, 'path': '/disk/new_file',
                    'size': meta1['size'], 'md5': meta1['md5'],
                    'sha256': meta1['sha256']
                },
                code=153
            )

    @pytest.mark.skipif(not REAL_MONGO,
                        reason='https://st.yandex-team.ru/CHEMODAN-34246')
    def test_async_block_uids_by_hid_protection(self):
        self.create_user(self.uid_1)
        self.upload_file(self.uid, '/disk/file')
        meta1 = self.json_ok('info', {'uid': self.uid, 'path': '/disk/file', 'meta': ''})['meta']

        with self.patch_mulca_is_file_exist(func_resp=True):
            self.json_ok(
                'store',
                {
                    'uid': self.uid_1, 'path': '/disk/file',
                    'size': meta1['size'], 'md5': meta1['md5'], 'sha256': meta1['sha256']
                }
            )
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/file'})
        self.json_ok('set_public', {'uid': self.uid_1, 'path': '/disk/file'})
        self.support_ok('uids_with_public_hid', {'hid': meta1['hid']})

        letters = set_up_mailbox(real_send=False)

        self.support_ok(
            'async_block_uids_by_hid',
            {'hid': meta1['hid'], 'moderator': 'm', 'comment_text': 'c', 'type': 'only_view_delete'}
        )

        assert self.uid in letters[0]['args'][2]['body']
        assert self.uid_1 in letters[0]['args'][2]['body']

        self.json_ok('list', {'uid': self.uid, 'path': '/disk'})
        self.json_ok('list', {'uid': self.uid_1, 'path': '/disk'})

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Postgres test')
    def test_async_block_uids_by_hid_additional_uids_non_empty(self):
        """Протестировать блокировку переданных дополнительных UIDов."""
        hid, uid_1, uid_2 = self._test_async_block_uids_by_hid_setup()
        uid_3 = self.user_3.uid
        self.create_user(uid_3)

        moderator = 'sasha_grey'
        comment_text = 'your ad here'

        with mock.patch('mpfs.core.support.controllers.UIDS_WITH_PUBLIC_HID_COUNT', len((uid_1, uid_2, uid_3))):
            self.support_ok('async_block_uids_by_hid', {
                'hid': hid,
                'moderator': moderator,
                'comment_text': comment_text,
                'type': 'only_view_delete',
                'additional_uids': uid_3
            })

        self.json_error('user_info', {'uid': uid_1, 'meta': ''}, code=codes.USER_BLOCKED)
        self.json_error('user_info', {'uid': uid_2, 'meta': ''}, code=codes.USER_BLOCKED)
        self.json_error('user_info', {'uid': uid_3, 'meta': ''}, code=codes.USER_BLOCKED)

        # проверяем что дополнительные пользователи заблокированы с тем же текстом комментария и модератором
        comments = select_comments_by_uid(uid_3)
        assert len(comments) == 1
        [comment] = comments
        assert comment['comment'] == comment_text
        assert comment['moderator'] == moderator
        assert comment['type'] == 'block_user'

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Postgres test')
    def test_async_block_uids_by_hid_additional_uids_not_passed(self):
        """Протестировать случай, когда параметр `additional_uids` вообще не передан."""
        hid, uid_1, uid_2 = self._test_async_block_uids_by_hid_setup()
        moderator = 'sasha_grey'
        comment_text = 'your ad here'

        with mock.patch('mpfs.core.support.controllers.UIDS_WITH_PUBLIC_HID_COUNT', len((uid_1, uid_2))):
            self.support_ok('async_block_uids_by_hid', {
                'hid': hid,
                'moderator': moderator,
                'comment_text': comment_text,
                'type': 'only_view_delete',
            })

        self.json_error('user_info', {'uid': uid_1, 'meta': ''}, code=codes.USER_BLOCKED)
        self.json_error('user_info', {'uid': uid_2, 'meta': ''}, code=codes.USER_BLOCKED)

    def test_resolve_public_short_url(self):
        public_hash = urllib.unquote('FhgCZqDAr0UZWu14bVpB6fkwchRf5V/n8yVNb34j0tA%3D')
        with mock.patch.object(Clck, 'short_url_to_public_hash', return_value=public_hash):
            result = self.support_ok('resolve_public_short_url', {'short_url': 'https://yadi.sk/d/EZiDUapAympaL/'})
            assert isinstance(result, dict)
            assert 'public_hash' in result
            assert result['public_hash'] == public_hash

    def _test_async_block_uids_by_hid_setup(self):
        uid_1 = self.user_1.uid
        uid_2 = self.user_2.uid
        uid_3 = self.user_3.uid

        self.create_user(uid_1)
        self.create_user(uid_2)
        self.create_user(uid_3)

        path = '/disk/file.txt'
        self.upload_file(uid_1, path)
        meta = self.json_ok('info', {'uid': uid_1, 'path': path, 'meta': ''})['meta']
        with self.patch_mulca_is_file_exist(func_resp=True):
            self.json_ok(
                'store',
                {
                    'uid': uid_2, 'path': path,
                    'size': meta['size'], 'md5': meta['md5'], 'sha256': meta['sha256']
                }
            )

        self.json_ok('set_public', {'uid': uid_1, 'path': path})
        self.json_ok('set_public', {'uid': uid_2, 'path': path})

        meta_1 = self.json_ok('info', {'uid': uid_1, 'path': path, 'meta': ''})['meta']
        meta_2 = self.json_ok('info', {'uid': uid_2, 'path': path, 'meta': ''})['meta']

        assert meta_1['hid'] == meta_2['hid']
        return meta_1['hid'], uid_1, uid_2

    def test_add_user_to_prohibited_cleaning(self):
        """Проверить, что юзер добавляется в список пользователей, для которых запрещена чистка."""
        uid = self.uid
        comment = 'your ad here'
        moderator = 'moderator_nickname'
        # добавляем запись
        result = self.support_ok('add_user_to_prohibited_cleaning', {
            'uid': uid,
            'comment': comment,
            'moderator': moderator
        })
        assert 'success' in result
        assert result['success'] is True

        # проверяем что она появилась
        result = self.support_ok('list_prohibited_cleaning_users', {})
        [only_user] = result
        assert 'uid' in only_user
        assert 'moderator' in only_user
        assert 'ctime' in only_user
        assert 'comment' in only_user

        assert only_user['uid'] == uid
        assert only_user['moderator'] == moderator
        assert only_user['comment'] == comment

    def test_remove_user_from_prohibited_cleaning_user_exists(self):
        """Проверить, что юзер удаляется из списка пользователей, для которых запрещена чистка.

        Случай, когда пользователь присутствует в списке.
        """
        uid = self.uid
        comment = 'your ad here'
        moderator = 'moderator_nickname'
        # добавляем запись
        self.support_ok('add_user_to_prohibited_cleaning', {
            'uid': uid,
            'comment': comment,
            'moderator': moderator
        })
        result = self.support_ok('list_prohibited_cleaning_users', {})
        assert len(result) == 1
        self.support_ok('remove_user_from_prohibited_cleaning', {'uid': uid})
        result = self.support_ok('list_prohibited_cleaning_users', {})
        assert result == []

    def test_remove_user_from_prohibited_cleaning_user_does_not_exist(self):
        """Проверить, что юзер удаляется из списка пользователей, для которых запрещена чистка.

        Случай, когда пользователь отсутствует в списке.
        """
        uid = self.uid
        result = self.support_ok('list_prohibited_cleaning_users', {})
        assert result == []
        self.support_ok('remove_user_from_prohibited_cleaning', {'uid': uid})
        result = self.support_ok('list_prohibited_cleaning_users', {})
        assert result == []

    def test_remove_user_from_prohibited_cleaning_user_exists_among_others(self):
        """Проверить, что юзер удаляется из списка пользователей, для которых запрещена чистка.

        Случай, когда пользователь присутствует в списке, но там он не один.
        """
        uid_1 = self.uid_1
        uid_2 = self.uid_2
        self.support_ok('add_user_to_prohibited_cleaning', {
            'uid': uid_1,
            'comment': 'your ad here 1',
            'moderator': 'moderator_nickname_1'
        })
        self.support_ok('add_user_to_prohibited_cleaning', {
            'uid': uid_2,
            'comment': 'your ad here 2',
            'moderator': 'moderator_nickname_2'
        })
        result = self.support_ok('list_prohibited_cleaning_users', {})
        uids = [o['uid'] for o in result]
        assert uid_1 in uids
        assert uid_2 in uids

        self.support_ok('remove_user_from_prohibited_cleaning', {'uid': uid_1})
        result = self.support_ok('list_prohibited_cleaning_users', {})
        uids = [o['uid'] for o in result]
        assert uid_1 not in uids
        assert uid_2 in uids

    def test_list_prohibited_cleaning_users(self):
        """Протестировать отдачу списка пользователей, для которых запрещена чистка."""
        uid_1 = self.uid_1
        uid_2 = self.uid_2
        self.support_ok('add_user_to_prohibited_cleaning', {
            'uid': uid_1,
            'comment': 'your ad here 1',
            'moderator': 'moderator_nickname_1'
        })
        self.support_ok('add_user_to_prohibited_cleaning', {
            'uid': uid_2,
            'comment': 'your ad here 2',
            'moderator': 'moderator_nickname_2'
        })
        result = self.support_ok('list_prohibited_cleaning_users', {})
        assert contains(
            result,
            has_entries({
                'uid': uid_1,
                'comment': 'your ad here 1',
                'moderator': 'moderator_nickname_1'
            }),
            has_entries({
                'uid': uid_2,
                'comment': 'your ad here 2',
                'moderator': 'moderator_nickname_2'
            })
        )

    def test_max_quantity_of_users_with_prohibited_cleaning(self):
        """Протестировать, что нельзя добавить в список больше определённого количества пользователей."""
        uid_1 = self.uid_1
        uid_2 = self.uid_2
        uid_3 = self.uid_3
        patched_max_count = 2
        with mock.patch(
            'mpfs.metastorage.mongo.collections.support.SUPPORT_PROHIBITED_CLEANING_USERS_MAX_COUNT',
            patched_max_count
        ):
            self.support_ok('add_user_to_prohibited_cleaning', {
                'uid': uid_1,
                'comment': 'your ad here 1',
                'moderator': 'moderator_nickname_1'
            })
            result = self.support_ok('list_prohibited_cleaning_users', {})
            assert len(result) == 1

            self.support_ok('add_user_to_prohibited_cleaning', {
                'uid': uid_2,
                'comment': 'your ad here 2',
                'moderator': 'moderator_nickname_2'
            })
            result = self.support_ok('list_prohibited_cleaning_users', {})
            assert len(result) == 2

            # больше нельзя добавить из-за ограничения
            self.support_error('add_user_to_prohibited_cleaning', {
                'uid': uid_3,
                'comment': 'your ad here 3',
                'moderator': 'moderator_nickname_3'
            }, code=codes.SUPPORT_TOO_MANY_USERS_WITH_PROHIBITED_CLEANING)
            result = self.support_ok('list_prohibited_cleaning_users', {})
            assert len(result) == patched_max_count

    def test_check_ok_startrek_issue(self):
        self.support_ok('get_file_checksums', {
            'uid': self.uid,
            'path': '/disk/myfile.txt',
            'startrek_issue': 'DISKSUP-12345',
        })

    @parameterized.expand([
        ('Empty issue', ''),
        ('Invalid number', 'DISKSUP-aeaegaeg'),
        ('Empty number', 'DISKSUP-'),
    ])
    def test_check_error_startrek_issue(self, comment, issue):
        self.support_error('get_file_checksums', {
            'uid': self.uid,
            'path': '/disk/myfile.txt',
            'startrek_issue': issue,
        })

    @parameterized.expand([
        (),
        ('md5'),
        ('sha256'),
        ('size'),
        ('md5', 'sha256', 'size'),
    ])
    def test_get_file_checksums(self, *incorrect_fields):
        md5 = '18f8b0d02d9305a87e3f262c46ec6f64'
        sha256 = '6140d051a97bb40d10e0f994d47651faba623646685698c4a155e695770028fc'
        size = 20210616

        s_md5 = 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa' if 'md5' in incorrect_fields else md5
        s_sha256 = 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa' if 'sha256' in incorrect_fields else sha256
        s_size = 20210618 if 'size' in incorrect_fields else size

        self.upload_file(self.uid, '/disk/file.txt', file_data={
            'md5': md5,
            'sha256': sha256,
            'size': size,
        })

        with KladunStub(checksums_obj=FileChecksums(s_md5, s_sha256, s_size)), \
             mock.patch.object(startrek_service, 'create_comment') as create_comment_mock:

            self.support_ok('get_file_checksums', {
                'uid': self.uid,
                'path': '/disk/file.txt',
                'startrek_issue': 'DISKSUP-12345',
            })
            assert create_comment_mock.call_count == 2
            assert create_comment_mock.call_args_list[0][0][0] == 'DISKSUP-12345'
            assert create_comment_mock.call_args_list[1][0][0] == 'DISKSUP-12345'
            assert 'GetFileChecksumsOperation' in create_comment_mock.call_args_list[0][0][1]

            assert ('MD5: %s' % md5) in create_comment_mock.call_args_list[1][0][1]
            assert ('SHA256: %s' % sha256) in create_comment_mock.call_args_list[1][0][1]
            assert ('File size: %s' % size) in create_comment_mock.call_args_list[1][0][1]

            assert ('MD5: %s' % s_md5) in create_comment_mock.call_args_list[1][0][1]
            assert ('SHA256: %s' % s_sha256) in create_comment_mock.call_args_list[1][0][1]
            assert ('File size: %s' % s_size) in create_comment_mock.call_args_list[1][0][1]

            if len(incorrect_fields) == 0:
                assert '!!(green)' in create_comment_mock.call_args_list[1][0][1]
            else:
                assert '!!(red)' in create_comment_mock.call_args_list[1][0][1]

    def test_get_file_checksums_raise_kladun(self):
        md5 = '18f8b0d02d9305a87e3f262c46ec6f64'
        sha256 = '6140d051a97bb40d10e0f994d47651faba623646685698c4a155e695770028fc'
        size = 20210616

        self.upload_file(self.uid, '/disk/file.txt', file_data={
            'md5': md5,
            'sha256': sha256,
            'size': size,
        })

        with KladunStub(checksums_obj=errors.APIError), \
             mock.patch.object(startrek_service, 'create_comment') as create_comment_mock:

            self.support_ok('get_file_checksums', {
                'uid': self.uid,
                'path': '/disk/file.txt',
                'startrek_issue': 'DISKSUP-12345',
            })

            assert create_comment_mock.call_count == 2
            assert create_comment_mock.call_args_list[0][0][0] == 'DISKSUP-12345'
            assert create_comment_mock.call_args_list[1][0][0] == 'DISKSUP-12345'
            assert 'GetFileChecksumsOperation' in create_comment_mock.call_args_list[0][0][1]

            assert ('MD5: %s' % md5) not in create_comment_mock.call_args_list[1][0][1]
            assert ('SHA256: %s' % sha256) not in create_comment_mock.call_args_list[1][0][1]
            assert ('File size: %s' % size) not in create_comment_mock.call_args_list[1][0][1]

    def teardown_method(self, method):
        tear_down_mailbox()
        super(SupportAPITestCase, self).teardown_method(method)


class ForceHandleOperationsTestCase(DiskTestCase):

    testfolder_tree = [
        '/',
        '/testfolder',
        '/testfolder/folder1',
        '/testfolder/folder1/1.txt',
        '/testfolder/folder1/2.txt',
        '/testfolder/folder2',
        '/testfolder/folder2/3.txt',
        '/testfolder/folder2/4.txt',
        '/testfolder/folder2/5.txt',
        '/testfolder/folder2/6.txt',
        '/testfolder/folder2/7.txt',
        '/testfolder/folder2/folder3',
        '/testfolder/folder2/folder3/8.txt',
    ]

    otherfiles_tree = [
        '/',
        '/otherfile1.txt',
        '/otherfolder',
        '/otherfolder/otherfile2.txt',
    ]

    def create_folders(self):
        for resource in self.testfolder_tree + self.otherfiles_tree:
            if resource == '/':
                continue
            if not resource.endswith('.txt'):
                self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk%s' % resource})

    def create_files(self):
        self.create_folders()
        for resource in self.testfolder_tree + self.otherfiles_tree:
            if resource == '/':
                continue
            if resource.endswith('.txt'):
                self.upload_file(self.uid, '/disk%s' % resource)

    def test_force_remove_folder_without_files(self):
        self.create_folders()
        with mock.patch.object(startrek_service, 'create_comment') as create_comment_mock, \
             mock.patch('mpfs.core.support.operations.SUPPORT_FORCE_HANDLE_CHUNK_SIZE', 2):
            self.support_ok('force_remove_folder', {
                'uid': self.uid,
                'path': '/disk/testfolder',
                'startrek_issue': 'DISKSUP-12345',
            })

            self.check_startrek_comments(create_comment_mock)
            self.check_tree('hidden', ['/'])
            self.check_tree('disk', filter(lambda resource: not resource.endswith('.txt'), self.otherfiles_tree))

    def test_force_remove_folder(self):
        self.create_files()
        with mock.patch.object(startrek_service, 'create_comment') as create_comment_mock, \
             mock.patch('mpfs.core.support.operations.SUPPORT_FORCE_HANDLE_CHUNK_SIZE', 2):
            self.support_ok('force_remove_folder', {
                'uid': self.uid,
                'path': '/disk/testfolder',
                'startrek_issue': 'DISKSUP-12345',
            })

            self.check_startrek_comments(create_comment_mock)
            self.check_tree('hidden', self.testfolder_tree)
            self.check_tree('disk', self.otherfiles_tree)

    def test_force_drop_trash(self):
        self.create_files()
        self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/testfolder'})
        self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/otherfolder'})
        self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/otherfile1.txt'})

        with mock.patch.object(startrek_service, 'create_comment') as create_comment_mock, \
             mock.patch('mpfs.core.support.operations.SUPPORT_FORCE_HANDLE_CHUNK_SIZE', 2):
            self.support_ok('force_drop_trash', {
                'uid': self.uid,
                'startrek_issue': 'DISKSUP-12345',
            })

            self.check_startrek_comments(create_comment_mock)
            self.check_tree('trash', ['/'])
            self.check_tree('hidden', self.testfolder_tree + self.otherfiles_tree)

    def test_force_restore_trash(self):
        self.create_files()
        trash_folder_path = self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/testfolder'})['this']['id']
        self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/otherfolder'})
        self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/otherfile1.txt'})

        with mock.patch.object(startrek_service, 'create_comment') as create_comment_mock, \
             mock.patch('mpfs.core.support.operations.SUPPORT_FORCE_HANDLE_CHUNK_SIZE', 2):
            self.support_ok('force_restore_trash_folder', {
                'uid': self.uid,
                'path': trash_folder_path,
                'startrek_issue': 'DISKSUP-12345',
            })

            self.check_startrek_comments(create_comment_mock)
            self.check_tree('trash', self.otherfiles_tree)
            self.check_tree('disk', self.testfolder_tree)
            assert len(support_prohibited_cleaning_users.list_all()) == 0

    def test_force_restore_hidden(self):
        self.create_files()
        self.json_ok('rm', {'uid': self.uid, 'path': '/disk/testfolder'})
        self.json_ok('rm', {'uid': self.uid, 'path': '/disk/otherfolder'})
        self.json_ok('rm', {'uid': self.uid, 'path': '/disk/otherfile1.txt'})

        with mock.patch.object(startrek_service, 'create_comment') as create_comment_mock, \
             mock.patch('mpfs.core.support.operations.SUPPORT_FORCE_HANDLE_CHUNK_SIZE', 2):
            self.support_ok('force_restore_hidden_folder', {
                'uid': self.uid,
                'path': '/hidden/testfolder',
                'startrek_issue': 'DISKSUP-12345',
            })

            self.check_startrek_comments(create_comment_mock)

            # из-за реализации непустые папки остаются в hidden
            self.check_tree('hidden', self.otherfiles_tree + ['/testfolder', '/testfolder/folder1', '/testfolder/folder2', '/testfolder/folder2/folder3'])
            self.check_tree('disk', self.testfolder_tree)

    def check_startrek_comments(self, create_comment_mock):
        assert create_comment_mock.call_count == 2
        assert self.uid in create_comment_mock.call_args[0][1]

    def check_tree(self, storage, expected_tree):
        def get_paths(tree):
            path = tree['this']['path']

            # вырезаем суффиксы после удаления ресурсов
            path = re.sub('_[a-f0-9]{40}', '', path)
            path = re.sub(':\d+\.\d+$', '', path)

            paths = [path]
            for child in tree['list']:
                paths = paths + get_paths(child)
            return paths

        tree = self.json_ok('fulltree', {'uid': self.uid, 'path': '/%s' % storage})
        assert set(expected_tree) == set(get_paths(tree))


class ReindexFacesAlbumTestCase(DiskTestCase):
    def setup_method(self, method):
        super(ReindexFacesAlbumTestCase, self).setup_method(method)
        session = Session.create_from_uid(self.uid)
        session.execute("UPDATE disk.user_index SET faces_indexing_state = :state WHERE uid = :uid",
                        {'uid': self.uid, 'state': 'running'})

    def test_timeout_reindex(self):
        with mock.patch.object(startrek_service, 'create_comment') as create_comment_mock, \
             mock.patch.object(djfs_albums, 'reindex_faces'), \
             mock.patch('mpfs.core.support.operations.SUPPORT_REINDEX_ALBUMS_TIMEOUT', 5), \
             mock.patch('mpfs.core.support.operations.ReindexFacesAlbumOperation.reenque', new=self.get_reenque_mock()), \
             mock.patch('mpfs.core.support.operations.SUPPORT_REINDEX_ALBUMS_REENQUE_DELAY', 1):

            self.support_ok('reindex_faces_album', {
                'uid': self.uid,
                'startrek_issue': 'DISKSUP-12345',
            })
            assert create_comment_mock.call_args_list[1][0][1] == u'Операция закончилась с ошибкой'

    def test_reindex_with_reset_albums(self):
        with mock.patch.object(startrek_service, 'create_comment'), \
             mock.patch.object(djfs_albums, 'reindex_faces') as reindex_faces_mock, \
             mock.patch('mpfs.core.support.operations.ReindexFacesAlbumOperation.reenque', new=self.get_reenque_mock()), \
             mock.patch('mpfs.core.support.operations.SUPPORT_REINDEX_ALBUMS_TIMEOUT', 10), \
             mock.patch('mpfs.core.support.operations.SUPPORT_REINDEX_ALBUMS_REENQUE_DELAY', 1):

            self.support_ok('reindex_faces_album', {
                'uid': self.uid,
                'startrek_issue': 'DISKSUP-12345',
            })
            assert reindex_faces_mock.call_count == 1
            assert reindex_faces_mock.call_args[0][0] == {'uid': self.uid, 'reset_albums': True}

    def test_reindex_correct(self):
        with mock.patch.object(startrek_service, 'create_comment') as create_comment_mock, \
             mock.patch.object(djfs_albums, 'reindex_faces', new=self.reindex_faces_mock), \
             mock.patch('mpfs.core.support.operations.ReindexFacesAlbumOperation.reenque', new=self.get_reenque_mock()), \
             mock.patch('mpfs.core.support.operations.SUPPORT_REINDEX_ALBUMS_TIMEOUT', 10), \
             mock.patch('mpfs.core.support.operations.SUPPORT_REINDEX_ALBUMS_REENQUE_DELAY', 1):

            self.support_ok('reindex_faces_album', {
                'uid': self.uid,
                'startrek_issue': 'DISKSUP-12345',
            })
            assert create_comment_mock.call_args_list[1][0][1] == u'Переиндексация альбомов лиц для пользователя %s завершена' % self.uid

    def get_reenque_mock(self):
        def reenque_mock(operation_self, delay):
            time.sleep(delay)
            super(ReindexFacesAlbumOperation, operation_self).reenque(delay)
        return reenque_mock

    def reindex_faces_mock(self, *args):
        def thread_func():
            time.sleep(5)
            session = Session.create_from_uid(self.uid)
            session.execute("UPDATE disk.user_index SET faces_indexing_state = 'reindexed' WHERE uid = :uid",
                            {'uid': self.uid})

        thread = Thread(target=thread_func)
        thread.start()

        return {}


class UnblockOverdraftUserTestCase(DiskTestCase):
    overdraft_dao = OverdraftDAO()

    def setUp(self):
        super(UnblockOverdraftUserTestCase, self).setUp()
        self.overdraft_dao.update_or_create(self.uid, datetime.date.today())
        self.user = User(self.uid)
        DiskInfoCollection().put(self.uid, '/limit', 100 * GB)
        DiskInfoCollection().put(self.uid, '/total_size', 500 * GB)

    def test_not_overdrafter(self):
        self.overdraft_dao.delete(self.uid)
        response = self.support_ok('overdraft_allow_unblock', {'uid': self.uid})
        assert not response['allowed']

    def test_not_blocked(self):
        response = self.support_ok('overdraft_allow_unblock', {'uid': self.uid})
        assert not response['allowed']

    def test_blocked_by_load(self):
        User(self.uid).set_block(True, 'load DB')
        response = self.support_ok('overdraft_allow_unblock', {'uid': self.uid})
        assert not response['allowed']

    def test_blocked_by_overdraft(self):
        self.user.set_block(True, 'blocked for overdraft')
        response = self.support_ok('overdraft_allow_unblock', {'uid': self.uid})
        assert response['allowed']

    def test_blocked_by_overdraft_5_ones(self):
        disk_info.put(self.uid, OVERDRAFT_KEY, {OVERDRAFT_RESET_COUNT_FIELD: 5})
        self.user.set_block(True, 'blocked for overdraft')
        response = self.support_ok('overdraft_allow_unblock', {'uid': self.uid})
        assert not response['allowed']

    def test_unblock(self):
        self.user.set_block(True, 'blocked for overdraft')
        with mock.patch('mpfs.core.services.djfs_api_service.djfs_api_legacy.reset_overdraft') as djfs_request:
            self.support_ok('overdraft_unblock', {'uid': self.uid})
            assert djfs_request.call_count == 1
            assert djfs_request.call_args[0][0] == self.uid

        assert not self.user.is_blocked()


class TestModerationQueue(DiskTestCase):
    def load_queue(self):
        get_args = lambda: {
            "created" : datetime.datetime.utcnow(),
            "hid" : str(random.randint(0, 9)) * 32,
            "source" : "anti-fo",
            "status" : "not-moderated",
            "description" : "Test",
            "links" : [
                {
                    "type" : "folder",
                    "url" : "https://yadi.sk/d/6r7rRVJsasGTp"
                },
            ]
        }
        for i in range(20):
            args = get_args()
            args['created'] += datetime.timedelta(0, i)
            db.support_moderation_queue.insert(args, fsync=True, w=1)
        for i in range(2):
            args = get_args()
            args['status'] = 'wtf'
            db.support_moderation_queue.insert(args, fsync=True, w=1)

    def test_get_moderation_list(self):
        self.load_queue()

        self.assertEqual(len(self.support_ok('get_moderation_list')['result']), 22)

        args = {
            'status': 'wtf',
        }
        self.assertEqual(len(self.support_ok('get_moderation_list', args)['result']), 2)

        args = {
            'status': 'not-moderated',
        }
        self.assertEqual(len(self.support_ok('get_moderation_list', args)['result']), 20)

        args = {
            'status': 'not-moderated',
            'offset': 5,
        }
        self.assertEqual(len(self.support_ok('get_moderation_list', args)['result']), 15)

        args = {
            'status': 'not-moderated',
            'offset': 35,
        }
        self.assertEqual(len(self.support_ok('get_moderation_list', args)['result']), 0)

        args = {
            'status': 'not-moderated',
            'offset': 6,
            'amount': 5
        }
        result = self.support_ok('get_moderation_list', args)['result']
        self.assertEqual(len(result), 5)

        prev_created = None
        for item in result:
            if prev_created:
                print item['created'], prev_created
                self.assertTrue(item['created'] > prev_created)
            prev_created = item['created']

    def test_set_status(self):
        self.load_queue()
        test_id = db.support_moderation_queue.find_one()['_id']

        args = {
            'moderator': 'Moder',
            'status': 'Done',
            '_id': str(test_id),
        }
        self.support_ok('set_status', args)
        result = self.support_ok('get_moderation_list', {'status': 'Done'})['result']
        self.assertTrue(len(result) == 1)
        self.assertTrue(result[0]['status'] == 'Done')
        self.assertTrue(result[0]['moderator'] == 'Moder')
        self.assertTrue(type(result[0]['moderation_time']) == int)

    @pytest.mark.skipif(not REAL_MONGO,
                        reason='https://st.yandex-team.ru/CHEMODAN-34246')
    def test_get_moderation_queue_count(self):
        self.load_queue()
        created = time.mktime(datetime.datetime.combine(datetime.date.today(), datetime.datetime.min.time()).timetuple())
        assert self.support_ok('get_moderation_queue_count', {'created': created}) == 20
        assert self.support_ok('get_moderation_queue_count', {'created': created, 'status': 'wtf'}) == 2
