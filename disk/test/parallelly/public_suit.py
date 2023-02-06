# -*- coding: utf-8 -*-

"""
Тесты, проверяющие корректную работу кода, связанного с публичными папками.
В настоящий момент здесь есть тесты на часть функционала ручек `public_list` и `public_info`
(в частности работа с блокировками).
"""
import copy
import datetime

import mock
import pytest
from attrdict import AttrDict
from parameterized import parameterized

from mpfs.common.static.tags import PUBLIC_PASSWORD_TOKEN
from mpfs.core.base import PUBLIC_PASSWORD_HEADER, PUBLIC_PASSWORD_TOKEN_HEADER
from mpfs.core.billing.dao.overdraft import OverdraftDAO
from mpfs.core.public_links.tasks import ExpiredLinksCleanerManager
from mpfs.core.services.passport_service import Passport
from mpfs.core.user.constants import PUBLIC_UID
from mpfs.metastorage.mongo.collections.system import DiskInfoCollection
from test.base import DiskTestCase, time_machine
from test.common.sharing import CommonSharingMethods
from test.helpers.size_units import GB
from test.helpers.stubs.services import PassportStub
from test.helpers.stubs.resources.users_info import DEFAULT_USERS_INFO, update_info_by_uid

from mpfs.common.static import codes
from mpfs.common.util import datetime_to_unixtime


class PublicInfoTestCase(DiskTestCase):
    """Набор тестов для ручки `public_info.`"""

    def test_public_info_for_resource_inside_blocked_dir_by_relative_path(self):
        """Протестировать запрос `public_info` для ресурса внутри *публичной заблокированной папки*,
        который запрашивают через публичный хеш и относительный путь.
        X -> Y, где X - публичная заблокированная папка, Y - подпапка X, Y - запрашиваемая.
        """
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir/subdir'})
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/dir'})
        response = self.json_ok('info', {'uid': self.uid, 'path': '/disk/dir', 'meta': ''})
        public_hash = response['meta']['public_hash']

        # публичная папка не заблокирована
        self.json_ok('public_info', {
            'private_hash': public_hash
        })

        # блокируем публичную папку
        result = self.support_ok('block_public_file', {
            'moderator': 'moderator',
            'comment': 'comment',
            'private_hash': public_hash,
            'type': 'block_file',
            'view': 'st',
            'link': 'https://rkn.gov.ru/',
            'notify': 0,
        })
        self.assertTrue(result)

        # публичная папка теперь заблокирована
        self.json_error('public_info', {
            'private_hash': public_hash
        }, code=codes.RESOURCE_BLOCKED)

        # подпапка внутри заблокированной публичной папки тоже недоступна
        self.json_error('public_info', {
            'private_hash': public_hash + ':/subdir'
        }, code=codes.RESOURCE_BLOCKED)

    def test_public_info_for_blocked_dir_resource_inside_public_folder_1(self):
        """Протестировать запрос `public_info` для *заблокированной папки внутри публичной папки*,
        которую запрашивают через публичный хеш и относительный путь.
        Запрашиваемый ресурс является заблокированным сам по себе (имеет флаг).
        X -> Y, где X - публичная папка, Y - заблокированная папка, Y - запрашиваемая.
        """
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir/subdir'})
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/dir'})
        response = self.json_ok('info', {'uid': self.uid, 'path': '/disk/dir', 'meta': ''})
        public_hash = response['meta']['public_hash']

        # публичная папка не заблокирована
        self.json_ok('public_info', {
            'private_hash': public_hash
        })

        # блокируем подпапку внутри публичной папки
        result = self.support_ok('block_public_file', {
            'moderator': 'moderator',
            'comment': 'comment',
            'private_hash': public_hash + ':/subdir',
            'type': 'block_file',
            'view': 'st',
            'link': 'https://rkn.gov.ru/',
            'notify': 0,
        })
        self.assertTrue(result)

        # подпапка внутри публичной папки заблокирована
        self.json_error('public_info', {
            'private_hash': public_hash + ':/subdir'
        }, code=codes.RESOURCE_BLOCKED)

        # публичная папка не заблокирована
        self.json_ok('public_info', {
            'private_hash': public_hash
        })

    def test_public_info_for_blocked_dir_resource_inside_public_folder_2(self):
        """Протестировать запрос `public_info` для *папки внутри публичной папки,
        имеющей среди родителей заблокированную подпапку этой же публичной папки*,
        которую запрашивают через публичный хеш и относительный путь.
        Запрашиваемый ресурс является потомком заблокированного.
        X -> Y -> Z, где X - публичная папка, Y - заблокированная папка, Z - запрашиваемая.
        """
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir/subdir1'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir/subdir1/subdir2'})
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/dir'})
        response = self.json_ok('info', {'uid': self.uid, 'path': '/disk/dir', 'meta': ''})
        public_hash = response['meta']['public_hash']

        # публичная папка не заблокирована
        self.json_ok('public_info', {
            'private_hash': public_hash
        })

        # блокируем подпапку внутри публичной папки
        result = self.support_ok('block_public_file', {
            'moderator': 'moderator',
            'comment': 'comment',
            'private_hash': public_hash + ':/subdir1',
            'type': 'block_file',
            'view': 'st',
            'link': 'https://rkn.gov.ru/',
            'notify': 0,
        })
        self.assertTrue(result)

        # подпапка-потомок заблокированной папки внутри публичной папки недоступна
        self.json_error('public_info', {
            'private_hash': public_hash + ':/subdir1/subdir2'
        }, code=codes.RESOURCE_BLOCKED)

        # публичная папка не заблокирована
        self.json_ok('public_info', {
            'private_hash': public_hash
        })

    def test_public_info_two_embedded_public_dirs(self):
        """Протестировать что если создать 2 вложенных публичных папок и заблокировать верхнюю по иерархии, то
        при запросе к нижней по иерархии (без относительного пути!) мы получим сообщение о блокировке.
        """
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir1'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir1/dir2'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir1/dir2/dir3'})
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/dir1/dir2'})
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/dir1/dir2/dir3'})

        response = self.json_ok('info', {'uid': self.uid, 'path': '/disk/dir1/dir2', 'meta': ''})
        dir2_public_hash = response['meta']['public_hash']

        response = self.json_ok('info', {'uid': self.uid, 'path': '/disk/dir1/dir2/dir3', 'meta': ''})
        dir3_public_hash = response['meta']['public_hash']

        result = self.support_ok('block_public_file', {
            'moderator': 'moderator',
            'comment': 'comment',
            'private_hash': dir2_public_hash,
            'type': 'block_file',
            'view': 'st',
            'link': 'https://rkn.gov.ru/',
            'notify': 0,
        })
        self.assertTrue(result)

        self.json_error('public_info', {
            'private_hash': dir3_public_hash
        }, code=codes.RESOURCE_BLOCKED)

    def test_public_info_for_deleted_account(self):
        """Протестировать работу ручки `public_info` для случая, когда ссылка принадлежит удаленному пользователю."""
        uid = self.uid

        self.json_ok('mkdir', {'uid': uid, 'path': '/disk/test'})
        self.json_ok('set_public', {'uid': uid, 'path': '/disk/test'})

        public_hash = self.json_ok('info', {'uid': uid, 'path': '/disk/test', 'meta': ''})['meta']['public_hash']

        with PassportStub(userinfo=DEFAULT_USERS_INFO['deleted_account']) as stub:
            self.json_error('public_info', {
                'private_hash': public_hash
            }, code=codes.RESOURCE_NOT_FOUND)
            assert stub.userinfo.called

    @parameterized.expand([
        (50, 0, False),
        (1000, 0, False),
        (50, 10, False),
        (1000, 10, False),
        (50, 13, False),
        (1000, 13, False),
        (50, 14, False),
        (1000, 14, True),
        (50, 15, False),
        (1000, 15, True),
        (50, 40, False),
        (1000, 40, True),
    ])
    def test_overdraft(self, used_space, days_after_overdraft, error):

        OverdraftDAO().update_or_create(self.uid, datetime.datetime(2021, 12, 1) - datetime.timedelta(days=days_after_overdraft))
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/public_folder'})
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/public_folder'})

        public_hash = self.json_ok('info', {'uid': self.uid, 'path': '/disk/public_folder', 'meta': ''})['meta']['public_hash']
        with time_machine(datetime.datetime(2021, 12, 1)):
            DiskInfoCollection().put(self.uid, '/limit', 100 * GB)
            DiskInfoCollection().put(self.uid, '/total_size', used_space * GB)

            if error:
                self.json_error('public_info', {'private_hash': public_hash}, code=codes.OVERDRAFT_USER_PUBLIC_LINK_IS_DISABLED)
            else:
                self.json_ok('public_info', {'private_hash': public_hash})

    def test_public_info_for_blocked_by_passport_account(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test'})
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/test'})
        public_hash = self.json_ok('info', {'uid': self.uid, 'path': '/disk/test', 'meta': ''})['meta']['public_hash']

        update_info_by_uid(self.uid, is_enabled=False)

        self.json_error('public_info', {'private_hash': public_hash}, code=codes.RESOURCE_NOT_FOUND)


class PublicListTestCase(CommonSharingMethods, DiskTestCase):
    """Набор тестов для ручки `public_list`"""

    def test_public_list_for_deleted_account(self):
        """Протестировать работу ручки `public_list` для случая, когда ссылка принадлежит удаленному пользователю."""
        uid = self.uid

        self.json_ok('mkdir', {'uid': uid, 'path': '/disk/test'})
        self.json_ok('set_public', {'uid': uid, 'path': '/disk/test'})

        public_hash = self.json_ok('info', {'uid': uid, 'path': '/disk/test', 'meta': ''})['meta']['public_hash']

        with PassportStub(userinfo=DEFAULT_USERS_INFO['deleted_account']) as stub:
            self.json_error('public_list', {
                'private_hash': public_hash
            }, code=codes.RESOURCE_NOT_FOUND)
            assert stub.userinfo.called

    @parameterized.expand([
        (50, 0, False),
        (1000, 0, False),
        (50, 10, False),
        (1000, 10, False),
        (50, 13, False),
        (1000, 13, False),
        (50, 14, False),
        (1000, 14, True),
        (50, 15, False),
        (1000, 15, True),
        (50, 40, False),
        (1000, 40, True),
    ])
    def test_overdraft(self, used_space, days_after_overdraft, error):
        OverdraftDAO().update_or_create(self.uid, datetime.datetime(2021, 12, 1) - datetime.timedelta(days=days_after_overdraft))
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/public_folder'})
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/public_folder'})

        public_hash = self.json_ok('info', {'uid': self.uid, 'path': '/disk/public_folder', 'meta': ''})['meta']['public_hash']
        with time_machine(datetime.datetime(2021, 12, 1)):
            DiskInfoCollection().put(self.uid, '/limit', 100 * GB)
            DiskInfoCollection().put(self.uid, '/total_size', used_space * GB)

            if error:
                self.json_error('public_list', {'private_hash': public_hash}, code=codes.OVERDRAFT_USER_PUBLIC_LINK_IS_DISABLED)
            else:
                self.json_ok('public_list', {'private_hash': public_hash})

    def test_public_list_for_blocked_by_passport_account(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test'})
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/test'})
        public_hash = self.json_ok('info', {'uid': self.uid, 'path': '/disk/test', 'meta': ''})['meta']['public_hash']

        update_info_by_uid(self.uid, is_enabled=False)

        self.json_error('public_list', {'private_hash': public_hash}, code=codes.RESOURCE_NOT_FOUND)

    def test_public_list_for_blocked_public_folder(self):
        """Протестировать что при попытке получить содержимое заблокированной публичной папки будет получена ошибка."""
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir'})
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/dir'})
        response = self.json_ok('info', {'uid': self.uid, 'path': '/disk/dir', 'meta': ''})
        public_hash = response['meta']['public_hash']

        # публичная папка не заблокирована
        self.json_ok('public_list', {
            'private_hash': public_hash
        })

        # блокируем публичную папку
        result = self.support_ok('block_public_file', {
            'moderator': 'moderator',
            'comment': 'comment',
            'private_hash': public_hash,
            'type': 'block_file',
            'view': 'st',
            'link': 'https://rkn.gov.ru/',
            'notify': 0,
        })
        self.assertTrue(result)

        # публичная папка теперь недоступна, тк при блокировке корня происходит удаление симлинка,
        # а значит это 404 по сути
        self.json_error('public_list', {
            'private_hash': public_hash
        }, code=codes.RESOURCE_NOT_FOUND)

    def test_public_list_for_public_folder_with_internal_blocked_folder(self):
        """Протестировать отдачу содержимого публичной папки, когда внутри есть заблокированная папка.
        Она не должна войти в выдачу."""
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir/a/'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir/b/'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir/b/c/'})
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/dir/'})
        response = self.json_ok('info', {'uid': self.uid, 'path': '/disk/dir', 'meta': ''})
        public_hash = response['meta']['public_hash']

        # публичная папка не заблокирована
        self.json_ok('public_list', {
            'private_hash': public_hash
        })

        # блокируем подпапку публичной папки
        result = self.support_ok('block_public_file', {
            'moderator': 'moderator',
            'comment': 'comment',
            'private_hash': public_hash + ':/b',
            'type': 'block_file',
            'view': 'st',
            'link': 'https://rkn.gov.ru/',
            'notify': 0,
        })
        self.assertTrue(result)

        # теперь внутри публичной папки /dir не заблокированной остается только /a (/dir/a/)

        # получаем содержимое публичной папки
        response = self.json_ok('public_list', {
            'private_hash': public_hash
        })
        assert len(response) == 2  # /dir + /dir/a
        folders_names = {d['name'] for d in response if d['type'] == 'dir'}
        files_names = {d['name'] for d in response if d['type'] == 'file'}
        assert 'dir' in folders_names
        assert 'a' in folders_names
        assert not files_names

    def test_public_list_page_blocked_items_num_field(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir'})
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/dir/'})
        for i in range(5):
            self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir/dir_%i' % i})
        for i in range(5):
            self.upload_file(self.uid, '/disk/dir/file_%i' % i)
        response = self.json_ok('info', {'uid': self.uid, 'path': '/disk/dir', 'meta': ''})
        public_hash = response['meta']['public_hash']

        for block_resource_name in ('dir_1', 'dir_4', 'file_1', 'file_4'):
            result = self.support_ok('block_public_file', {
                'moderator': 'moderator',
                'comment': 'comment',
                'private_hash': public_hash + ':/%s' % block_resource_name,
                'type': 'block_file',
                'view': 'st',
                'link': 'https://rkn.gov.ru/',
                'notify': 0,
            })
            self.assertTrue(result)

        response = self.json_ok('public_list', {
            'private_hash': public_hash,
            'meta': ''
        })
        assert len(response) == 1 + 3 + 3
        for item in response:
            assert item['meta']['page_blocked_items_num'] == 4

        response = self.json_ok('public_list', {
            'private_hash': public_hash,
            'meta': '', 'amount': 5
        })
        assert len(response) == 1 + 3
        for item in response:
            assert item['meta']['page_blocked_items_num'] == 2

        response = self.json_ok('public_list', {
            'private_hash': public_hash,
            'meta': '', 'offset': 5, 'amount': 6
        })
        assert len(response) == 4
        for item in response:
            assert item['meta']['page_blocked_items_num'] == 2

    def test_public_list_for_blocked_folder_inside_public_folder(self):
        """Протестировать что невозможно получить содержимое заблокированной папки или потомка заблокированной папки
        внутри публичной папки."""
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir/a/'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir/a/d'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir/b/'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir/b/c/'})
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/dir/'})
        response = self.json_ok('info', {'uid': self.uid, 'path': '/disk/dir', 'meta': ''})
        public_hash = response['meta']['public_hash']

        # публичная папка не заблокирована
        self.json_ok('public_list', {
            'private_hash': public_hash
        })

        # блокируем подпапку публичной папки
        result = self.support_ok('block_public_file', {
            'moderator': 'moderator',
            'comment': 'comment',
            'private_hash': public_hash + ':/b',
            'type': 'block_file',
            'view': 'st',
            'link': 'https://rkn.gov.ru/',
            'notify': 0,
        })
        self.assertTrue(result)

        # корень публичной папки мы не трогали, он доступен
        self.json_ok('public_list', {
            'private_hash': public_hash
        })

        # заблокированная папка недоступна
        self.json_error('public_list', {
            'private_hash': public_hash + ':/b'
        }, code=codes.RESOURCE_BLOCKED)

        # потомки заблокированной папки недоступны
        self.json_error('public_list', {
            'private_hash': public_hash + ':/b/c/'
        }, code=codes.RESOURCE_BLOCKED)

        # остальные папки не затронуты
        self.json_ok('public_list', {
            'private_hash': public_hash + ':/a'
        })
        self.json_ok('public_list', {
            'private_hash': public_hash + ':/a/d/'
        })

    def test_user_in_meta(self):
        """Протестировать включение информации о владельце в ответ.

        Если передан `user` в `meta`, то возвращается публичная информация о владельце.
        """
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir'})
        self.upload_file(self.uid, '/disk/dir/test.txt')
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/dir'})
        response = self.json_ok('info', {'uid': self.uid, 'path': '/disk/dir', 'meta': ''})
        public_hash = response['meta']['public_hash']

        result = self.json_ok('public_list', {
            'private_hash': public_hash,
            'meta': 'user'
        })

        first_element = result[0]

        assert 'meta' in first_element
        assert 'user' in first_element['meta']
        keys = first_element['meta']['user'].keys()
        assert not set(keys) ^ {'uid', 'login', 'locale', 'username', 'display_name', 'public_name'}

    def test_user_with_empty_meta(self):
        """Протестировать НЕвключение информации о владельце в ответ, когда передан пустой `meta`"""
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir'})
        self.upload_file(self.uid, '/disk/dir/test.txt')
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/dir'})
        response = self.json_ok('info', {'uid': self.uid, 'path': '/disk/dir', 'meta': ''})
        public_hash = response['meta']['public_hash']

        result = self.json_ok('public_list', {
            'private_hash': public_hash,
            'meta': ''
        })

        first_element = result[0]

        assert 'meta' in first_element
        assert 'user' not in first_element['meta']

    def test_user_without_meta(self):
        """Протестировать НЕвключение информации о владельце в ответ, когда `meta` вообще не передан."""
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir'})
        self.upload_file(self.uid, '/disk/dir/test.txt')
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/dir'})
        response = self.json_ok('info', {'uid': self.uid, 'path': '/disk/dir', 'meta': ''})
        public_hash = response['meta']['public_hash']

        result = self.json_ok('public_list', {
            'private_hash': public_hash,
        })

        first_element = result[0]

        assert 'meta' not in first_element

    def test_public_list_for_public_file(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir'})
        self.upload_file(self.uid, '/disk/dir/test.txt')
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/dir/test.txt'})
        response = self.json_ok('info', {'uid': self.uid, 'path': '/disk/dir/test.txt', 'meta': ''})
        public_hash = response['meta']['public_hash']

        result = self.json_ok('public_list', {
            'private_hash': public_hash,
        })
        assert isinstance(result, dict)
        assert result['name'] == 'test.txt'

    def test_set_relative_address_bug_root_address(self):
        """Проверить случай создания публичной папки из расшаренной папки.

        Создаем публичную папку с файлом /disk/share. Приглашаем пользователя.
        Приглашенным у себя ее переименовываем на /disk/share_*. Важно чтоб название содержало
        префиксом название у владельца. Публикуем приглашенным. Пытаемся получить контент по публичному хешу.
        Запрашиваем корень публичной папки.
        """
        # https://st.yandex-team.ru/CHEMODAN-33124
        self.create_user(self.user_1.uid)
        self.create_user(self.user_2.uid)
        owner_shared_folder_path = '/disk/share'
        self.json_ok('mkdir', {'uid': self.user_1.uid, 'path': owner_shared_folder_path})
        self.json_ok('share_create_group', {'uid': self.user_1.uid, 'path': owner_shared_folder_path})
        self.upload_file(self.user_1.uid, owner_shared_folder_path + '/trump.jpg')
        hash_ = self.invite_user(uid=self.user_2.uid, owner=self.user_1.uid, email=self.user_2.email,
                                 path=owner_shared_folder_path)
        self.activate_invite(uid=self.user_2.uid, hash=hash_)
        # очень важно чтоб префиксом был адрес владельца
        non_owner_shared_folder_new_path = owner_shared_folder_path + '_bla_bla'
        self.json_ok('move', {
            'uid': self.user_2.uid,
            'src': owner_shared_folder_path,
            'dst': non_owner_shared_folder_new_path
        })
        result = self.json_ok('set_public', {'uid': self.user_2.uid, 'path': non_owner_shared_folder_new_path})
        public_hash = result['hash']
        self.json_ok('public_list', {'private_hash': public_hash})

    def test_set_relative_address_bug_relative_address(self):
        """Проверить случай создания публичной папки из расшаренной папки.

        Создаем публичную папку с файлом /disk/share. Приглашаем пользователя.
        Приглашенным у себя ее переименовываем на /disk/share_*. Важно чтоб название содержало
        префиксом название у владельца. Публикуем приглашенным. Пытаемся получить контент по публичному хешу.
        Запрашиваем путь относительно корня публичной папки.
        """
        # https://st.yandex-team.ru/CHEMODAN-33124
        self.create_user(self.user_1.uid)
        self.create_user(self.user_2.uid)
        owner_shared_folder_path = '/disk/share'
        self.json_ok('mkdir', {'uid': self.user_1.uid, 'path': owner_shared_folder_path})
        self.json_ok('mkdir', {'uid': self.user_1.uid, 'path': owner_shared_folder_path + '/folder'})
        self.json_ok('share_create_group', {'uid': self.user_1.uid, 'path': owner_shared_folder_path})
        self.upload_file(self.user_1.uid, owner_shared_folder_path + '/trump.jpg')
        self.upload_file(self.user_1.uid, owner_shared_folder_path + '/folder' '/putin.jpg')
        hash_ = self.invite_user(uid=self.user_2.uid, owner=self.user_1.uid, email=self.user_2.email,
                                 path=owner_shared_folder_path)
        self.activate_invite(uid=self.user_2.uid, hash=hash_)
        # очень важно чтоб префиксом был адрес владельца
        non_owner_shared_folder_new_path = owner_shared_folder_path + '_bla_bla'
        self.json_ok('move', {
            'uid': self.user_2.uid,
            'src': owner_shared_folder_path,
            'dst': non_owner_shared_folder_new_path
        })
        result = self.json_ok('set_public', {'uid': self.user_2.uid, 'path': non_owner_shared_folder_new_path})
        public_hash = result['hash']
        self.json_ok('public_list', {'private_hash': public_hash + ':/folder'})

    def test_move_shared_public_folder(self):
        self.create_user(self.uid_1)

        owner_shared_folder_path = '/disk/shared'
        self.json_ok('mkdir', {'uid': self.uid, 'path': owner_shared_folder_path})

        self.create_group(uid=self.uid, path=owner_shared_folder_path)
        invite_hash = self.invite_user(uid=self.uid_1, owner=self.uid, email=self.email_1,
                                       path=owner_shared_folder_path)
        self.activate_invite(uid=self.uid_1, hash=invite_hash)

        set_public_result = self.json_ok('set_public', {'uid': self.uid, 'path': owner_shared_folder_path})
        public_hash = set_public_result['hash']

        self.json_ok('public_info', {'private_hash': public_hash})

        self.json_ok('async_move', {'uid': self.uid, 'src': owner_shared_folder_path, 'dst': '/disk/any-other-path'})

        self.json_ok('public_info', {'private_hash': public_hash})

    def test_public_previews_not_eternal(self):
        file_path = '/disk/file_1.jpg'
        self.upload_file(self.uid, file_path)
        result = self.json_ok('set_public', {'uid': self.uid, 'path': file_path})
        params = {'meta': 'custom_preview',
                  'private_hash': result['hash'],
                  'preview_size': 'S'}
        result = self.json_ok('public_list', params)
        print result
        assert '/inf/' not in result['meta']['custom_preview']

    def test_block_root_of_public_folder(self):
        """Расшаренная папка должна блокироваться при блокировке рутовой папки."""
        self.create_user(self.uid_1)

        owner_shared_folder_path = '/disk/shared'
        self.json_ok('mkdir', {'uid': self.uid, 'path': owner_shared_folder_path})
        self.create_group(uid=self.uid, path=owner_shared_folder_path)
        invite_hash = self.invite_user(uid=self.uid_1, owner=self.uid, email=self.email_1,
                                       path=owner_shared_folder_path)
        self.activate_invite(uid=self.uid_1, hash=invite_hash)
        self.json_ok('set_public', {'uid': self.uid, 'path': owner_shared_folder_path})

        # Рутовая папка изначально доступна
        response = self.json_ok('info', {'uid': self.uid, 'path': owner_shared_folder_path, 'meta': ''})
        owner_public_hash = response['meta']['public_hash']
        self.json_ok('public_info', {
            'private_hash': owner_public_hash
        })

        # Пошаренная рутовая папка изначально доступна
        response = self.json_ok('info', {'uid': self.uid_1, 'path': owner_shared_folder_path, 'meta': ''})
        shared_public_hash = response['meta']['public_hash']
        self.json_ok('public_info', {
            'private_hash': shared_public_hash
        })

        # Блокируем рутовую папку
        result = self.support_ok('block_public_file', {
            'moderator': 'moderator',
            'comment': 'comment',
            'private_hash': owner_public_hash,
            'type': 'block_file',
            'view': 'st',
            'link': 'https://rkn.gov.ru/',
            'notify': 0,
        })
        self.assertTrue(result)

        # Рутовая папка недоступна
        self.json_error('public_info', {
            'private_hash': owner_public_hash
        }, code=codes.RESOURCE_BLOCKED)

        # Рутовая пошаренная папка тоже должна быть недоступна
        self.json_error('public_info', {
            'private_hash': shared_public_hash
        }, code=codes.RESOURCE_BLOCKED)


class SetPublicTestCase(DiskTestCase):

    def test_set_public_for_descendants_of_blocked_public(self):
        """Протестировать, что невозможно опубликовать ресурс-потомок заблокированной папки."""
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir1'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir1/dir2'})
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/dir1'})

        response = self.json_ok('info', {'uid': self.uid, 'path': '/disk/dir1', 'meta': ''})
        dir1_public_hash = response['meta']['public_hash']

        result = self.support_ok('block_public_file', {
            'moderator': 'moderator',
            'comment': 'comment',
            'private_hash': dir1_public_hash,
            'type': 'block_file',
            'view': 'st',
            'link': 'https://rkn.gov.ru/',
            'notify': 0,
        })
        self.assertTrue(result)

        self.json_error('set_public', {
            'uid': self.uid,
            'path': '/disk/dir1/dir2'
        }, code=codes.RESOURCE_BLOCKED)


class SetPublicSettingsTestCase(DiskTestCase):

    def test_set_public_settings_for_public(self):
        """Позитивный сценарий"""
        from mpfs.core.base import PUBLIC_SETTINGS_FEATURE
        self.json_ok('set_ps_billing_feature', {'uid': self.uid, 'feature_name': PUBLIC_SETTINGS_FEATURE, 'value': 1})
        test_dt = datetime_to_unixtime(datetime.datetime(2022, 6, 1))
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir1'})
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/dir1'})
        public_hash = self.json_ok('info', {'uid': self.uid, 'path': '/disk/dir1', 'meta': ''})['meta']['public_hash']
        with time_machine(datetime.datetime(2020, 6, 1)):
            self.json_ok(
                'set_public_settings',
                {'uid': self.uid, 'path': '/disk/dir1'},
                json={'available_until': test_dt, 'read_only': True})

        response = self.json_ok('get_public_settings', {'uid': self.uid, 'path': '/disk/dir1'})
        assert response['available_until'] == test_dt
        assert response['read_only']
        assert not response['have_password']
        with time_machine(datetime.datetime(2020, 6, 1)):
            response = self.json_ok('get_public_settings_by_hash', {'uid': self.uid, 'private_hash': public_hash})
            assert response['available_until'] == test_dt
            assert response['read_only']
            assert not response['have_password']
            # проверка, что пользователю без диска(в том числе анониму) отдадим настройки
            self.json_ok('get_public_settings_by_hash', {'uid': self.user_2.uid, 'private_hash': public_hash})
            self.json_ok('get_public_settings_by_hash', {'uid': PUBLIC_UID, 'private_hash': public_hash})

        # сброс времени жизни ссылки
        with time_machine(datetime.datetime(2020, 6, 1)):
            self.json_ok(
                'set_public_settings',
                {'uid': self.uid, 'path': '/disk/dir1'},
                json={'available_until': None}
            )

        response = self.json_ok('get_public_settings', {'uid': self.uid, 'path': '/disk/dir1'})
        assert response['available_until'] is None
        assert response['read_only']
        assert not response['have_password']

        response = self.json_ok('get_public_settings_by_hash', {'uid': self.uid, 'private_hash': public_hash})
        assert response['available_until'] is None
        assert response['read_only']
        assert not response['have_password']

    def test_set_public_settings_for_private(self):
        """Установка настроек несуществуюещго ресурса"""
        from mpfs.core.base import PUBLIC_SETTINGS_FEATURE
        self.json_ok('set_ps_billing_feature',
                     {'uid': self.uid, 'feature_name': PUBLIC_SETTINGS_FEATURE, 'value': 1})

        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir1'})
        with time_machine(datetime.datetime(2020, 6, 1)):
            self.json_error(
                'set_public_settings',
                {'uid': self.uid, 'path': '/disk/dir1'},
                json={'available_until': datetime_to_unixtime(datetime.datetime(2022, 6, 1)), 'read_only': True},
                code=codes.RESOURCE_NOT_FOUND
            )

    def test_set_public_settings_empty_body(self):
        """Дергать ручку с пустым body"""
        from mpfs.core.base import PUBLIC_SETTINGS_FEATURE
        self.json_ok('set_ps_billing_feature', {'uid': self.uid, 'feature_name': PUBLIC_SETTINGS_FEATURE, 'value': 1})

        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir1'})
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/dir1'})

        self.json_error(
            'set_public_settings',
            {'uid': self.uid, 'path': '/disk/dir1'},
            code=codes.BAD_REQUEST_ERROR
        )
        self.json_error(
            'set_public_settings',
            {'uid': self.uid, 'path': '/disk/dir1'},
            json={},
            code=codes.BAD_REQUEST_ERROR
        )

    def test_set_public_settings_fail_if_not_available_for_private(self):
        """Установка настроек при отключенной фиче"""
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir1'})
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/dir1'})
        self.json_error('set_public_settings', {'uid': self.uid, 'path': '/disk/dir1'},
                        json={'available_until': 100, 'read_only': False},
                        code=codes.PUBLIC_LINK_SETTINGS_IS_DISABLED)


    def test_get_expired(self):
        """Проверка истекшего по времени ресурса"""
        from mpfs.core.base import PUBLIC_SETTINGS_FEATURE
        self.json_ok('set_ps_billing_feature', {'uid': self.uid, 'feature_name': PUBLIC_SETTINGS_FEATURE, 'value': 1})
        self.create_user(self.user_2.uid)
        dir_path = '/disk/dir1'
        self.json_ok('mkdir', {'uid': self.uid, 'path': dir_path})
        self.json_ok('mkdir', {'uid': self.uid, 'path': dir_path+'/inner'})
        self.upload_file(uid=self.uid, path=dir_path+'/inner/test.txt')
        self.upload_file(self.uid, dir_path+'/test1.jpg', media_type='image')
        self.upload_file(self.uid, dir_path+'/test2.jpg', media_type='image')
        self.json_ok('set_public', {'uid': self.uid, 'path': dir_path})
        with time_machine(datetime.datetime(2020, 6, 1)):
            self.json_ok(
                'set_public_settings',
                {'uid': self.uid, 'path': dir_path},
                json={'available_until': datetime_to_unixtime(datetime.datetime(2022, 6, 1))}
            )
        response = self.json_ok('info', {'uid': self.uid, 'path': dir_path, 'meta': ''})
        public_hash = response['meta']['public_hash']

        with time_machine(datetime.datetime(2022, 6, 1)):
            # нельзя проставлять в прошлое
            self.json_error(
                'set_public_settings',
                {'uid': self.uid, 'path': dir_path, 'available_until': datetime_to_unixtime(datetime.datetime(2007, 6, 1))}
            )

        # публичная папка не заблокирована
        with time_machine(datetime.datetime(2022, 1, 1)):
            self.json_ok('public_info', {'private_hash': public_hash})
            self.json_ok('public_list', {'private_hash': public_hash})
            self.json_ok('public_url', {'private_hash': public_hash})
            self.json_ok('public_fulltree', {'uid': self.uid, 'private_hash': public_hash + ':/inner'})
            self.json_ok('public_dir_size', {'uid': self.uid, 'private_hash': public_hash})
            self.json_ok('public_copy', {'uid': self.user_2.uid, 'private_hash': public_hash})
            self.json_ok('async_public_copy', {'uid': self.user_2.uid, 'private_hash': public_hash})
            self.json_ok('public_check_folder_content_media_type', {'private_hash': public_hash})

        # публичная папка не доступна
        with time_machine(datetime.datetime(2023, 1, 1)):
            self.json_error('public_info', {'private_hash': public_hash}, code=codes.RESOURCE_NOT_FOUND)
            self.json_error('public_list', {'private_hash': public_hash}, code=codes.RESOURCE_NOT_FOUND)
            self.json_error('public_url', {'private_hash': public_hash}, code=codes.RESOURCE_NOT_FOUND)
            self.json_error('public_fulltree', {'private_hash': public_hash + ':/inner'}, code=codes.RESOURCE_NOT_FOUND)
            self.json_error('public_dir_size', {'uid': self.uid, 'private_hash': public_hash}, code=codes.RESOURCE_NOT_FOUND)
            self.json_error('public_copy', {'uid': self.user_2.uid, 'private_hash': public_hash},code=codes.RESOURCE_NOT_FOUND)
            self.json_error('async_public_copy', {'uid': self.user_2.uid, 'private_hash': public_hash}, code=codes.RESOURCE_NOT_FOUND)
            self.json_error('public_check_folder_content_media_type', {'private_hash': public_hash}, code=codes.RESOURCE_NOT_FOUND)

        # уходим в будущее и запускаем чистку
        with time_machine(datetime.datetime(2023, 1, 1)):
            ExpiredLinksCleanerManager().run()

        with time_machine(datetime.datetime(2022, 1, 1)):
            self.json_error('public_info', {'private_hash': public_hash}, code=codes.RESOURCE_NOT_FOUND)
            self.json_error('public_list', {'private_hash': public_hash}, code=codes.RESOURCE_NOT_FOUND)
            self.json_error('public_url', {'private_hash': public_hash}, code=codes.RESOURCE_NOT_FOUND)
            self.json_error('public_fulltree', {'private_hash': public_hash + ':/inner'}, code=codes.RESOURCE_NOT_FOUND)
            self.json_error('public_dir_size', {'uid': self.uid, 'private_hash': public_hash}, code=codes.RESOURCE_NOT_FOUND)
            self.json_error('public_copy', {'uid': self.user_2.uid, 'private_hash': public_hash}, code=codes.RESOURCE_NOT_FOUND)
            self.json_error('async_public_copy', {'uid': self.user_2.uid, 'private_hash': public_hash}, code=codes.RESOURCE_NOT_FOUND)
            self.json_error('public_check_folder_content_media_type', {'private_hash': public_hash}, code=codes.RESOURCE_NOT_FOUND)

    def test_get_read_only_copy(self):
        """Проверка ресурса доступного только на чтение"""
        from mpfs.core.base import PUBLIC_SETTINGS_FEATURE
        self.json_ok('set_ps_billing_feature', {'uid': self.uid, 'feature_name': PUBLIC_SETTINGS_FEATURE, 'value': 1})
        self.create_user(self.user_2.uid)
        dir_path = '/disk/dir1'
        self.json_ok('mkdir', {'uid': self.uid, 'path': dir_path})
        self.json_ok('mkdir', {'uid': self.uid, 'path': dir_path + '/inner'})
        self.upload_file(uid=self.uid, path=dir_path + '/inner/test.txt')
        self.json_ok('set_public', {'uid': self.uid, 'path': dir_path})
        self.json_ok(
            'set_public_settings',
            {'uid': self.uid, 'path': dir_path},
            json={'read_only': True}
        )
        response = self.json_ok('info', {'uid': self.uid, 'path': dir_path, 'meta': ''})
        public_hash = response['meta']['public_hash']
        folder_resource = self.json_ok('public_info', {'private_hash': public_hash, 'meta': ''})['resource']
        assert 'folder_url' not in folder_resource['meta']
        assert folder_resource['meta']['read_only']
        file_resource = self.json_ok('public_info', {'private_hash': public_hash + ':' + '/inner/test.txt', 'meta': ''})['resource']
        assert 'file_url' not in file_resource['meta']
        assert file_resource['meta']['read_only']

        list_resp = self.json_ok('public_list', {'private_hash': public_hash, 'meta': ''})
        for item in list_resp:
            assert 'file_url' not in item['meta']
            assert 'folder_url' not in item['meta']
            assert item['meta']['read_only']

        self.json_error('public_copy', {'uid': self.user_2.uid, 'private_hash': public_hash},
                        code=codes.FORBIDDEN)
        self.json_error('async_public_copy', {'uid': self.user_2.uid, 'private_hash': public_hash},
                        code=codes.FORBIDDEN)

    def test_get_read_only_all_endpoints(self):
        from mpfs.core.base import PUBLIC_SETTINGS_FEATURE
        self.json_ok('set_ps_billing_feature', {'uid': self.uid, 'feature_name': PUBLIC_SETTINGS_FEATURE, 'value': 1})
        self.create_user(self.user_2.uid)
        dir_path = '/disk/dir1'
        self.json_ok('mkdir', {'uid': self.uid, 'path': dir_path})
        self.json_ok('mkdir', {'uid': self.uid, 'path': dir_path + '/inner'})
        self.upload_file(uid=self.uid, path=dir_path + '/inner/test.txt')
        self.json_ok('set_public', {'uid': self.uid, 'path': dir_path})
        self.json_ok(
            'set_public_settings',
            {'uid': self.uid, 'path': dir_path},
            json={'read_only': True}
        )
        response = self.json_ok('info', {'uid': self.uid, 'path': dir_path, 'meta': ''})
        public_hash = response['meta']['public_hash']

        public_info = self.json_ok('public_info', {'private_hash': public_hash, 'meta': ''})
        assert 'file_url' not in public_info['resource']['meta']
        assert 'folder_url' not in public_info['resource']['meta']
        assert public_info['resource']['meta']['read_only']
        public_list = self.json_ok('public_list', {'private_hash': public_hash, 'meta': ''})
        for item in public_list:
            assert 'file_url' not in item['meta']
            assert 'folder_url' not in item['meta']
            assert item['meta']['read_only']
        public_url_folder = self.json_ok('public_url', {'private_hash': public_hash, 'meta': ''})
        assert 'folder' not in public_url_folder
        assert public_url_folder['read_only']
        public_url_file = self.json_ok('public_url', {'private_hash': public_hash + ':' + '/inner/test.txt', 'meta': ''})
        assert 'file' not in public_url_file
        assert public_url_file['read_only']
        public_fulltree = self.json_ok('public_fulltree', {'uid': self.uid, 'private_hash': public_hash + ':/inner', 'meta': ''})
        assert 'folder_url' not in public_fulltree['this']['meta']
        assert 'file_url' not in public_fulltree['this']['meta']
        assert public_fulltree['this']['meta']['read_only']
        for item in public_fulltree['list']:
            assert 'folder_url' not in item['this']['meta']
            assert 'file_url' not in item['this']['meta']
            assert item['this']['meta']['read_only']

    def test_password_info(self):
        from mpfs.core.base import PUBLIC_SETTINGS_FEATURE
        self.json_ok('set_ps_billing_feature', {'uid': self.uid, 'feature_name': PUBLIC_SETTINGS_FEATURE, 'value': 1})
        self.create_user(self.user_2.uid)
        dir_path = '/disk/dir1'
        self.json_ok('mkdir', {'uid': self.uid, 'path': dir_path})
        self.json_ok('mkdir', {'uid': self.uid, 'path': dir_path + '/inner'})
        self.upload_file(uid=self.uid, path=dir_path + '/inner/test.txt')
        self.json_ok('set_public', {'uid': self.uid, 'path': dir_path})
        self.json_ok(
            'set_public_settings',
            {'uid': self.uid, 'path': dir_path},
            json={'password': 'secret'}
        )
        response = self.json_ok('info', {'uid': self.uid, 'path': dir_path, 'meta': ''})
        public_hash = response['meta']['public_hash']
        self.json_error('public_info', {'private_hash': public_hash, 'meta': ''})
        self.json_ok('public_info', {'private_hash': public_hash, 'meta': ''}, headers={PUBLIC_PASSWORD_HEADER: 'secret'})
        self.json_error('public_info', {'private_hash': public_hash + ':' + '/inner/test.txt', 'meta': ''})
        resp = self.json_ok(
            'public_info',
            {'private_hash': public_hash + ':' + '/inner/test.txt', 'meta': ''},
            headers={PUBLIC_PASSWORD_HEADER: 'secret'}
        )
        assert PUBLIC_PASSWORD_TOKEN in resp
        self.json_error('public_list', {'private_hash': public_hash, 'meta': ''}, code=codes.FORBIDDEN)
        self.json_ok(
            'public_list',
            {'private_hash': public_hash, 'meta': ''},
            headers={PUBLIC_PASSWORD_HEADER: 'secret'}
        )

        self.json_error('public_copy', {'uid': self.user_2.uid, 'private_hash': public_hash},
                        code=codes.FORBIDDEN)
        self.json_error('async_public_copy', {'uid': self.user_2.uid, 'private_hash': public_hash},
                        code=codes.FORBIDDEN)

        self.json_ok(
            'public_copy',
            {'uid': self.user_2.uid, 'private_hash': public_hash},
            headers={PUBLIC_PASSWORD_HEADER: 'secret'}
        )
        self.json_ok(
            'async_public_copy',
            {'uid': self.user_2.uid, 'private_hash': public_hash},
            headers={PUBLIC_PASSWORD_HEADER: 'secret'}
        )

    def test_get_password_all_public_endpoints(self):
        from mpfs.core.base import PUBLIC_SETTINGS_FEATURE
        self.json_ok('set_ps_billing_feature', {'uid': self.uid, 'feature_name': PUBLIC_SETTINGS_FEATURE, 'value': 1})
        self.create_user(self.user_2.uid)
        dir_path = '/disk/dir1'
        password = 'secret'
        wrong_password = 'oops'
        self.json_ok('mkdir', {'uid': self.uid, 'path': dir_path})
        self.json_ok('mkdir', {'uid': self.uid, 'path': dir_path+'/inner'})
        self.upload_file(uid=self.uid, path=dir_path+'/inner/test.txt')
        self.upload_file(self.uid, dir_path+'/test1.jpg', media_type='image')
        self.upload_file(self.uid, dir_path+'/test2.jpg', media_type='image')
        self.json_ok('set_public', {'uid': self.uid, 'path': dir_path})
        self.json_ok(
            'set_public_settings',
            {'uid': self.uid, 'path': dir_path},
            json={'password': password}
        )
        response = self.json_ok('info', {'uid': self.uid, 'path': dir_path, 'meta': ''})
        public_hash = response['meta']['public_hash']

        # публичная папка не заблокирована
        self.json_ok('public_info', {'private_hash': public_hash}, headers={PUBLIC_PASSWORD_HEADER: 'secret'})
        self.json_ok('public_list', {'private_hash': public_hash}, headers={PUBLIC_PASSWORD_HEADER: 'secret'})
        self.json_ok('public_url', {'private_hash': public_hash}, headers={PUBLIC_PASSWORD_HEADER: 'secret'})
        self.json_ok(
            'public_fulltree',
            {'uid': self.uid, 'private_hash': public_hash + ':/inner'},
            headers={PUBLIC_PASSWORD_HEADER: password}
        )
        self.json_ok(
            'public_dir_size',
            {'uid': self.uid, 'private_hash': public_hash},
            headers={PUBLIC_PASSWORD_HEADER: password}
        )
        self.json_ok(
            'public_copy',
            {'uid': self.user_2.uid, 'private_hash': public_hash},
            headers={PUBLIC_PASSWORD_HEADER: password}
        )
        self.json_ok(
            'async_public_copy',
            {'uid': self.user_2.uid, 'private_hash': public_hash},
            headers={PUBLIC_PASSWORD_HEADER: password}
        )
        self.json_ok(
            'public_check_folder_content_media_type',
            {'private_hash': public_hash},
            headers={PUBLIC_PASSWORD_HEADER: password}
        )

        # публичная папка не доступна без пароля
        self.json_error('public_info', {'private_hash': public_hash}, code=codes.FORBIDDEN)
        self.json_error('public_list', {'private_hash': public_hash}, code=codes.FORBIDDEN)
        self.json_error('public_url', {'private_hash': public_hash}, code=codes.FORBIDDEN)
        self.json_error('public_fulltree', {'private_hash': public_hash + ':/inner'}, code=codes.FORBIDDEN)
        self.json_error('public_copy', {'uid': self.user_2.uid, 'private_hash': public_hash},code=codes.FORBIDDEN)
        self.json_error('async_public_copy', {'uid': self.user_2.uid, 'private_hash': public_hash}, code=codes.FORBIDDEN)
        self.json_error('public_dir_size', {'uid': self.uid, 'private_hash': public_hash}, code=codes.FORBIDDEN)
        self.json_error('public_check_folder_content_media_type', {'private_hash': public_hash}, code=codes.FORBIDDEN)

        # публичная папка не доступна с неправильным паролем
        self.json_error('public_info', {'private_hash': public_hash}, headers={PUBLIC_PASSWORD_HEADER: wrong_password}, code=codes.SYMLINK_INVALID_PASSWORD)
        self.json_error('public_list', {'private_hash': public_hash}, headers={PUBLIC_PASSWORD_HEADER: wrong_password}, code=codes.SYMLINK_INVALID_PASSWORD)
        self.json_error('public_url', {'private_hash': public_hash}, headers={PUBLIC_PASSWORD_HEADER: wrong_password}, code=codes.SYMLINK_INVALID_PASSWORD)
        self.json_error('public_fulltree', {'private_hash': public_hash + ':/inner'}, headers={PUBLIC_PASSWORD_HEADER: wrong_password},
                        code=codes.SYMLINK_INVALID_PASSWORD)
        self.json_error('public_copy', {'uid': self.user_2.uid, 'private_hash': public_hash}, headers={PUBLIC_PASSWORD_HEADER: wrong_password},
                        code=codes.SYMLINK_INVALID_PASSWORD)
        self.json_error('async_public_copy', {'uid': self.user_2.uid, 'private_hash': public_hash}, headers={PUBLIC_PASSWORD_HEADER: wrong_password},
                        code=codes.SYMLINK_INVALID_PASSWORD)
        self.json_error('public_dir_size', {'uid': self.uid, 'private_hash': public_hash}, headers={PUBLIC_PASSWORD_HEADER: wrong_password},
                        code=codes.SYMLINK_INVALID_PASSWORD)
        self.json_error('public_check_folder_content_media_type', {'private_hash': public_hash}, headers={PUBLIC_PASSWORD_HEADER: wrong_password},
                        code=codes.SYMLINK_INVALID_PASSWORD)

    def test_reset_public_settings_on_disabled_feature(self):
        """Сброс настроек при отключенной фиче"""
        from mpfs.core.base import PUBLIC_SETTINGS_FEATURE
        dir_path = '/disk/dir1'
        self.json_ok('mkdir', {'uid': self.uid, 'path': dir_path})
        self.json_ok('set_public', {'uid': self.uid, 'path': dir_path})

        self.json_ok('set_ps_billing_feature', {'uid': self.uid, 'feature_name': PUBLIC_SETTINGS_FEATURE, 'value': 1})
        self.json_ok('set_public_settings', {'uid': self.uid, 'path': dir_path}, json={'read_only': True})
        test_dt = datetime_to_unixtime(datetime.datetime(2022, 6, 1))
        with time_machine(datetime.datetime(2020, 1, 1)):
            self.json_ok('set_public_settings', {'uid': self.uid, 'path': dir_path}, json={'available_until': test_dt})
            response = self.json_ok('get_public_settings', {'uid': self.uid, 'path': dir_path})
        assert response['available_until'] == test_dt
        assert response['read_only']
        # выключаем фичу
        self.json_ok('set_ps_billing_feature', {'uid': self.uid, 'feature_name': PUBLIC_SETTINGS_FEATURE, 'value': 0})

        # можно сбросить настройку
        self.json_ok('set_public_settings', {'uid': self.uid, 'path': dir_path}, json={'read_only': False})
        self.json_ok('set_public_settings', {'uid': self.uid, 'path': dir_path}, json={'available_until': None})
        response = self.json_ok('get_public_settings', {'uid': self.uid, 'path': dir_path})
        assert response['available_until'] is None
        assert not response['read_only']

        # но установить уже нельзя
        self.json_error(
            'set_public_settings',
            {'uid': self.uid, 'path': dir_path},
            json={'read_only': True},
            code=codes.PUBLIC_LINK_SETTINGS_IS_DISABLED
        )
        with time_machine(datetime.datetime(2020, 1, 1)):
            self.json_error(
                'set_public_settings',
                {'uid': self.uid, 'path': dir_path},
                json={'available_until': test_dt},
                code=codes.PUBLIC_LINK_SETTINGS_IS_DISABLED
            )

    @mock.patch.object(Passport, 'userinfo', return_value=AttrDict({'external_organization_ids': [1234]}))
    def test_set_org_id_info(self, user_info):
        """Позитивный сценарий шаринга на организацию"""
        from mpfs.core.base import PUBLIC_SETTINGS_FEATURE
        self.json_ok('set_ps_billing_feature', {'uid': self.uid, 'feature_name': PUBLIC_SETTINGS_FEATURE, 'value': 1})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir1'})
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/dir1'})
        self.json_ok('set_public_settings', {'uid': self.uid, 'path': '/disk/dir1'},
                     json={'external_organization_id': 1234})

        response = self.json_ok('get_public_settings', {'uid': self.uid, 'path': '/disk/dir1'})
        assert response['external_organization_ids'] == [1234]
        assert not response['available_until']
        assert not response['read_only']
        assert not response['have_password']
        assert user_info.called

        # сбрасываем шаринг на организацию
        self.json_ok('set_public_settings', {'uid': self.uid, 'path': '/disk/dir1'},
                     json={'external_organization_id': None})
        response = self.json_ok('get_public_settings', {'uid': self.uid, 'path': '/disk/dir1'})
        assert response['external_organization_ids'] == []
        assert not response['available_until']
        assert not response['read_only']
        assert not response['have_password']

    @mock.patch.object(Passport, 'userinfo', return_value=AttrDict({'external_organization_ids': [1234]}))
    def test_set_org_id_for_other_organisation(self, user_info):
        """Попытка пошарить файл на организацию, в которой не состоишь"""
        from mpfs.core.base import PUBLIC_SETTINGS_FEATURE
        self.json_ok('set_ps_billing_feature', {'uid': self.uid, 'feature_name': PUBLIC_SETTINGS_FEATURE, 'value': 1})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir1'})
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/dir1'})
        self.json_error('set_public_settings', {'uid': self.uid, 'path': '/disk/dir1'},
                     json={'external_organization_id': 4321}, code=codes.FORBIDDEN)

    def test_public_endpoints_for_link_with_org_limitation(self):
        from mpfs.core.base import PUBLIC_SETTINGS_FEATURE
        user_1 = copy.deepcopy(DEFAULT_USERS_INFO[self.uid])
        user_1['external_organization_ids'] = [1234]
        with mock.patch.object(Passport, 'userinfo', return_value=user_1):
            dir_path = '/disk/dir1'
            self.json_ok('set_ps_billing_feature', {'uid': self.uid, 'feature_name': PUBLIC_SETTINGS_FEATURE, 'value': 1})
            self.json_ok('mkdir', {'uid': self.uid, 'path': dir_path})
            self.json_ok('mkdir', {'uid': self.uid, 'path': dir_path + '/inner'})
            self.upload_file(uid=self.uid, path=dir_path + '/inner/test.txt')
            self.upload_file(self.uid, dir_path + '/test1.jpg', media_type='image')
            self.upload_file(self.uid, dir_path + '/test2.jpg', media_type='image')

            self.json_ok('set_public', {'uid': self.uid, 'path': dir_path})
            self.json_ok('set_public_settings', {'uid': self.uid, 'path': dir_path}, json={'external_organization_id': 1234})
            response = self.json_ok('info', {'uid': self.uid, 'path': '/disk/dir1', 'meta': ''})
            public_hash = response['meta']['public_hash']

        # у юзера состоящего в той же организации доступ есть
        uid = self.user_2.uid
        self.json_ok('user_init', {'uid': uid})
        user_2 = copy.deepcopy(DEFAULT_USERS_INFO[uid])
        user_2['external_organization_ids'] = [1234]
        with mock.patch.object(Passport, 'userinfo', return_value=user_2):
            self.json_ok('public_info', {'uid': uid, 'private_hash': public_hash})
            self.json_ok('public_list', {'uid': uid, 'private_hash': public_hash})
            self.json_ok('public_url', {'uid': uid, 'private_hash': public_hash})
            self.json_ok('public_fulltree', {'uid': uid, 'private_hash': public_hash + ':/inner'})
            self.json_ok('public_dir_size', {'uid': uid, 'private_hash': public_hash})
            self.json_ok('public_copy', {'uid': uid, 'private_hash': public_hash})
            self.json_ok('async_public_copy', {'uid': uid, 'private_hash': public_hash})
            self.json_ok('public_check_folder_content_media_type', {'uid': uid, 'private_hash': public_hash})

        # у юзера из другой организации доступа нет
        uid = self.user_3.uid
        self.json_ok('user_init', {'uid': uid})
        user_3 = copy.deepcopy(DEFAULT_USERS_INFO[uid])
        user_3['external_organization_ids'] = [4321]
        with mock.patch.object(Passport, 'userinfo', return_value=user_3):
            self.json_error('public_info', {'uid': uid, 'private_hash': public_hash}, code=codes.RESOURCE_NOT_FOUND)
            self.json_error('public_list', {'uid': uid, 'private_hash': public_hash}, code=codes.RESOURCE_NOT_FOUND)
            self.json_error('public_url', {'uid': uid, 'private_hash': public_hash}, code=codes.RESOURCE_NOT_FOUND)
            self.json_error('public_fulltree', {'uid': uid, 'private_hash': public_hash + ':/inner'}, code=codes.RESOURCE_NOT_FOUND)
            self.json_error('public_dir_size', {'uid': uid, 'private_hash': public_hash}, code=codes.RESOURCE_NOT_FOUND)
            self.json_error('public_copy', {'uid': uid, 'private_hash': public_hash}, code=codes.RESOURCE_NOT_FOUND)
            self.json_error('async_public_copy', {'uid': uid, 'private_hash': public_hash}, code=codes.RESOURCE_NOT_FOUND)
            self.json_error('public_check_folder_content_media_type', {'uid': uid, 'private_hash': public_hash}, code=codes.RESOURCE_NOT_FOUND)

        # незалогинов тоже доступа нет

        self.json_error('public_info', {'private_hash': public_hash}, code=codes.RESOURCE_NOT_FOUND)
        self.json_error('public_list', {'private_hash': public_hash}, code=codes.RESOURCE_NOT_FOUND)
        self.json_error('public_url', {'private_hash': public_hash}, code=codes.RESOURCE_NOT_FOUND)
        self.json_error('public_fulltree', {'private_hash': public_hash + ':/inner'}, code=codes.RESOURCE_NOT_FOUND)
        self.json_error('public_dir_size', {'private_hash': public_hash}, code=codes.RESOURCE_NOT_FOUND)
        self.json_error('public_check_folder_content_media_type', {'private_hash': public_hash}, code=codes.RESOURCE_NOT_FOUND)

    def test_set_public_settings_with_disk_pro(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir1'})
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/dir1'})
        # фича доступна для диск про тарифов
        with mock.patch('mpfs.core.user.standart.StandartUser.only_disk_pro_without_ps_billing_and_b2b_enabled', return_value=True):
            self.json_ok(
                'set_public_settings',
                {'uid': self.uid, 'path': '/disk/dir1'},
                json={'read_only': True},
            )

    def test_password_token_info(self):
        # точка входа для получения токена ручка public_info, остальные ручки только принимают токен
        from mpfs.core.base import PUBLIC_SETTINGS_FEATURE
        self.json_ok('set_ps_billing_feature', {'uid': self.uid, 'feature_name': PUBLIC_SETTINGS_FEATURE, 'value': 1})
        self.create_user(self.user_2.uid)
        dir_path = '/disk/dir1'
        self.json_ok('mkdir', {'uid': self.uid, 'path': dir_path})
        self.json_ok('mkdir', {'uid': self.uid, 'path': dir_path + '/inner'})
        self.upload_file(uid=self.uid, path=dir_path + '/inner/test.txt')
        self.json_ok('set_public', {'uid': self.uid, 'path': dir_path})
        self.json_ok(
            'set_public_settings',
            {'uid': self.uid, 'path': dir_path},
            json={'password': 'secret'}
        )
        response = self.json_ok('info', {'uid': self.uid, 'path': dir_path, 'meta': ''})
        public_hash = response['meta']['public_hash']

        # получаем на запрос с паролем в ответе токен
        resp = self.json_ok(
            'public_info',
            {'private_hash': public_hash + ':' + '/inner/test.txt', 'meta': ''},
            headers={PUBLIC_PASSWORD_HEADER: 'secret'}
        )
        assert PUBLIC_PASSWORD_TOKEN in resp
        token = resp[PUBLIC_PASSWORD_TOKEN]

        # проверяем публичные ручки с пробросом токена
        self.json_ok(
            'public_info',
            {'private_hash': public_hash + ':' + '/inner/test.txt', 'meta': ''},
            headers={PUBLIC_PASSWORD_TOKEN_HEADER: token}
        )
        self.json_ok('public_info', {'private_hash': public_hash}, headers={PUBLIC_PASSWORD_TOKEN_HEADER: token})
        self.json_ok('public_list', {'private_hash': public_hash}, headers={PUBLIC_PASSWORD_TOKEN_HEADER: token})
        self.json_ok('public_url', {'private_hash': public_hash}, headers={PUBLIC_PASSWORD_TOKEN_HEADER: token})
        self.json_ok(
            'public_fulltree',
            {'uid': self.uid, 'private_hash': public_hash + ':/inner'},
            headers={PUBLIC_PASSWORD_TOKEN_HEADER: token}
        )
        self.json_ok(
            'public_dir_size',
            {'uid': self.uid, 'private_hash': public_hash},
            headers={PUBLIC_PASSWORD_TOKEN_HEADER: token}
        )
        self.json_ok(
            'public_copy',
            {'uid': self.user_2.uid, 'private_hash': public_hash},
            headers={PUBLIC_PASSWORD_TOKEN_HEADER: token}
        )
        self.json_ok(
            'async_public_copy',
            {'uid': self.user_2.uid, 'private_hash': public_hash},
            headers={PUBLIC_PASSWORD_TOKEN_HEADER: token}
        )
        self.json_ok(
            'public_check_folder_content_media_type',
            {'private_hash': public_hash},
            headers={PUBLIC_PASSWORD_TOKEN_HEADER: token}
        )

        # публичная папка не доступна с неправильным токеном
        wrong_token = 'oops'
        self.json_error('public_info', {'private_hash': public_hash}, headers={PUBLIC_PASSWORD_TOKEN_HEADER: wrong_token},
                        code=codes.SYMLINK_INVALID_PASSWORD_TOKEN)
        self.json_error('public_list', {'private_hash': public_hash}, headers={PUBLIC_PASSWORD_TOKEN_HEADER: wrong_token},
                        code=codes.SYMLINK_INVALID_PASSWORD_TOKEN)
        self.json_error('public_url', {'private_hash': public_hash}, headers={PUBLIC_PASSWORD_TOKEN_HEADER: wrong_token},
                        code=codes.SYMLINK_INVALID_PASSWORD_TOKEN)
        self.json_error('public_fulltree', {'private_hash': public_hash + ':/inner'},
                        headers={PUBLIC_PASSWORD_TOKEN_HEADER: wrong_token},
                        code=codes.SYMLINK_INVALID_PASSWORD_TOKEN)
        self.json_error('public_copy', {'uid': self.user_2.uid, 'private_hash': public_hash},
                        headers={PUBLIC_PASSWORD_TOKEN_HEADER: wrong_token},
                        code=codes.SYMLINK_INVALID_PASSWORD_TOKEN)
        self.json_error('async_public_copy', {'uid': self.user_2.uid, 'private_hash': public_hash},
                        headers={PUBLIC_PASSWORD_TOKEN_HEADER: wrong_token},
                        code=codes.SYMLINK_INVALID_PASSWORD_TOKEN)
        self.json_error('public_dir_size', {'uid': self.uid, 'private_hash': public_hash},
                        headers={PUBLIC_PASSWORD_TOKEN_HEADER: wrong_token},
                        code=codes.SYMLINK_INVALID_PASSWORD_TOKEN)
        self.json_error('public_check_folder_content_media_type', {'private_hash': public_hash},
                        headers={PUBLIC_PASSWORD_TOKEN_HEADER: wrong_token},
                        code=codes.SYMLINK_INVALID_PASSWORD_TOKEN)

    def test_password_token_expired(self):
        # точка входа для получения токена ручка public_info, остальн
        from mpfs.core.base import PUBLIC_SETTINGS_FEATURE
        self.json_ok('set_ps_billing_feature', {'uid': self.uid, 'feature_name': PUBLIC_SETTINGS_FEATURE, 'value': 1})
        self.create_user(self.user_2.uid)
        dir_path = '/disk/dir1'
        self.json_ok('mkdir', {'uid': self.uid, 'path': dir_path})
        self.json_ok('mkdir', {'uid': self.uid, 'path': dir_path + '/inner'})
        self.upload_file(uid=self.uid, path=dir_path + '/inner/test.txt')
        self.json_ok('set_public', {'uid': self.uid, 'path': dir_path})
        self.json_ok(
            'set_public_settings',
            {'uid': self.uid, 'path': dir_path},
            json={'password': 'secret'}
        )
        response = self.json_ok('info', {'uid': self.uid, 'path': dir_path, 'meta': ''})
        public_hash = response['meta']['public_hash']

        # получаем на запрос с паролем в ответе токен
        with time_machine(datetime.datetime(2020, 1, 1)):
            resp = self.json_ok(
                'public_info',
                {'private_hash': public_hash + ':' + '/inner/test.txt', 'meta': ''},
                headers={PUBLIC_PASSWORD_HEADER: 'secret'}
            )
            assert PUBLIC_PASSWORD_TOKEN in resp
            token = resp[PUBLIC_PASSWORD_TOKEN]

            # проверяем публичные ручки с пробросом токена
            self.json_ok(
                'public_info',
                {'private_hash': public_hash + ':' + '/inner/test.txt', 'meta': ''},
                headers={PUBLIC_PASSWORD_TOKEN_HEADER: token}
            )
        # не даем ссылку с протухшим токеном
        with time_machine(datetime.datetime(2020, 1, 2)):
            self.json_error(
                'public_info',
                {'private_hash': public_hash + ':' + '/inner/test.txt', 'meta': ''},
                headers={PUBLIC_PASSWORD_TOKEN_HEADER: token},
                code=codes.SYMLINK_PASSWORD_TOKEN_EXPIRED
            )

    def test_get_public_direct_url_for_quickmove(self):
        # https://st.yandex-team.ru/DISKBACK-117
        self.create_user(self.user_2.uid)
        dir_path = '/disk/dir1'
        file_path = dir_path + '/inner/test.txt'
        self.json_ok('mkdir', {'uid': self.uid, 'path': dir_path})
        self.json_ok('mkdir', {'uid': self.uid, 'path': dir_path + '/inner'})
        self.upload_file(uid=self.uid, path=file_path)
        self.json_ok('set_public', {'uid': self.uid, 'path': dir_path})
        response = self.json_ok('info', {'uid': self.uid, 'path': dir_path, 'meta': ''})
        public_hash = response['meta']['public_hash']

        self.json_ok('public_url', {'private_hash': public_hash + ':' + '/inner/test.txt', 'meta': ''})

        # удаляем папку в которой содержится файл
        self.json_ok('trash_append', {'uid': self.uid, 'path': dir_path})

        # убеждаемся что файл не находится
        self.json_error('public_url', {'private_hash': public_hash + ':' + '/inner/test.txt', 'meta': ''}, code=codes.RESOURCE_NOT_FOUND)

        # убеждаемся что файл не находится для быстромувного
        with mock.patch('mpfs.core.filesystem.base.is_quick_move_enabled', return_value=True):
            self.service(
                'public_direct_url',
                {'uid': self.user_2.uid, 'private_hash': public_hash + ':' + '/inner/test.txt'},
                status=[404, ]
            )

        # убеждаемся что файл не находится для не быстромувного, делаем запрос не из под владельца
        with mock.patch('mpfs.core.filesystem.base.is_quick_move_enabled', return_value=False):
            self.service(
                'public_direct_url',
                {'uid': self.user_2.uid, 'private_hash': public_hash + ':' + '/inner/test.txt'},
                status=[404, ]
            )

