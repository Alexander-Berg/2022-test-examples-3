# -*- coding: utf-8 -*-
import time

from mock import patch

# Такой порядок важен, тк. здесь происходит настройка
# тестового окружения
from test.base import DiskTestCase
from test.fixtures import users

from mpfs.common.util import to_json
from mpfs.core.services.directory_service import DirectoryService, DirectoryContact
from mpfs.common.util import mailer
from mpfs.core.social.share.processor import passport_entity
import mpfs.engine.process
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase


DIRECTORY_DEPARTMENT_RESPONSE = {
    'links': {},
    'pages': 1,
    'result': [
        {
            'birthday': '1988-05-30',
            'groups': [],
            'id': users.pdd_user_1.uid,
            'about': None,
            'name': {
                'middle': {'ru': 'Станиславович'},
                'last': {'ru': 'Архипов'},
                'first': {'ru': 'Георгий'}
            },
            'contacts': [{'type': 'phone', 'value': '+7 218 174 77 53'}],
            'gender': 'male',
            'department': {
                'description': None,
                'parents': [],
                'id': 5,
                'parent': {
                    'parent_id': 2,
                    'id': 3,
                    'name': {'ru': 'Служба обеспечения исследований'}
                },
                'name': {'ru': 'Группа разработки закупок'}
            },
            'position': {'ru': 'разработчик'},
            'login': 'garkhipov@abook-dev2.ws.yandex.ru',
            'email': 'garkhipov@abook-dev2.ws.yandex.ru'
        },
        {
            'birthday': '1982-01-25',
            'groups': [],
            'id': users.pdd_user_2.uid,
            'about': None,
            'name': {
                'middle': {'ru': 'Фёдорович'},
                'last': {'ru': 'Тарасов'},
                'first': {'ru': 'Егор'}
            },
            'contacts': [{'type': 'phone', 'value': '+7 651 772 37 85'}
            ],
            'gender': 'male',
            'department': {
                'description': None,
                'parents': [],
                'id': 5,
                'parent': {
                    'parent_id': 2,
                    'id': 3,
                    'name': {'ru': 'Служба обеспечения исследований'}
                },
                'name': {'ru': 'Группа разработки закупок'}
            },
            'position': {'ru': 'разработчик'},
            'login': 'etarasov@abook-dev2.ws.yandex.ru',
            'email': 'etarasov@abook-dev2.ws.yandex.ru'
        }
    ],
    'per_page': 20,
    'total': 2,
    'page': 1
}


DIRECTORY_GROUP_RESPONSE = {
    'links': {},
    'pages': 1,
    'result': [
        {
            'birthday': None,
            'groups': [
                {
                    'description': None,
                    'created': '2015-10-15T20:55:46.594192+03:00Z',
                    'label': None,
                    'id': 2,
                    'author_id': None,
                    'type': 'organization_admin',
                    'email': None,
                    'name': {
                        'ru': 'Администратор организации',
                        'en': 'Organization administrator'
                    }
                }
            ],
            'id': users.pdd_user_1.uid,
            'about': None,
            'name': {
                'last': {
                    'ru': 'Пупкин'
                },
                'first': {
                    'ru': 'Вася'
                }
            },
            'contacts': None,
            'gender': 'male',
            'department': {
                'description': None,
                'parents': [],
                'id': 1,
                'parent': None,
                'name': {
                    'ru': 'Все сотрудники',
                    'en': 'All employees'
                }
            },
            'position': None,
            'login': 'admin@abook-dev2.ws.yandex.ru',
            'email': 'admin@abook-dev2.ws.yandex.ru'
        }
    ],
    'per_page': 20,
    'total': 1,
    'page': 1
}


class DirectoryServiceTestCase(DiskTestCase):

    def setup_method(self, method):
        super(DirectoryServiceTestCase, self).setup_method(method)

    def test_department_fetch(self):
        with patch.object(DirectoryService, 'open_url') as open_url_mock:
            open_url_mock.return_value = to_json(DIRECTORY_DEPARTMENT_RESPONSE)

            directory_service = DirectoryService()
            org_id = 777
            contacts = directory_service.get_department_contacts(org_id, '5')

            assert len(contacts) == 2
            assert isinstance(contacts, list)
            for contact in contacts:
                assert isinstance(contact, DirectoryContact)
                assert len(contact.email)


class GroupSharingTestCase(DiskTestCase):

    FOLDER_OWNER_UID = users.pdd_user_3.uid
    CONTACT_1_UID = users.pdd_user_1.uid
    CONTACT_2_UID = users.pdd_user_2.uid
    FOLDER_PATH = '/disk/shared-folder'

    def setup_method(self, method):
        super(GroupSharingTestCase, self).setup_method(method)
        self.json_ok('user_init', {'uid': self.FOLDER_OWNER_UID})
        self.json_ok('user_init', {'uid': self.CONTACT_1_UID})
        self.json_ok('user_init', {'uid': self.CONTACT_2_UID})

    def test_department_invite(self):
        self.json_ok('mkdir', {'uid': self.FOLDER_OWNER_UID, 'path': '/disk/shared-folder'})
        result = self.json_ok('share_create_group', {'uid': self.FOLDER_OWNER_UID, 'path': self.FOLDER_PATH})
        gid = result['gid']

        result = self.json_ok('share_list_joined_folders', {'uid': self.CONTACT_1_UID})
        assert not len(result)

        result = self.json_ok('share_list_joined_folders', {'uid': self.CONTACT_2_UID})
        assert not len(result)

        with patch.object(DirectoryService, 'open_url') as open_url_mock:
            open_url_mock.return_value = to_json(DIRECTORY_DEPARTMENT_RESPONSE)

            result = self.json_ok('share_invite_user', {'uid': self.FOLDER_OWNER_UID, 'gid': gid,
                                                        'universe_service': 'ya_directory',
                                                        'universe_login': '777:department:5',
                                                        'auto_accept': 1})

            oid = result['oid']
            self.json_ok('status', {'uid': self.FOLDER_OWNER_UID, 'oid': oid})

            result = self.json_ok('status', {'uid': self.FOLDER_OWNER_UID, 'oid': oid})
            assert result.get('status') == 'DONE'

            result = self.json_ok('share_list_joined_folders', {'uid': self.CONTACT_1_UID})
            assert len(result) == 1
            assert result[0]['path'] == self.FOLDER_PATH

            result = self.json_ok('share_list_joined_folders', {'uid': self.CONTACT_2_UID})
            assert len(result) == 1
            assert result[0]['path'] == self.FOLDER_PATH

    def test_invite_for_noninitialized_user(self):
        self.remove_user(self.CONTACT_2_UID)

        self.json_ok('mkdir', {'uid': self.FOLDER_OWNER_UID, 'path': '/disk/shared-folder'})
        result = self.json_ok('share_create_group', {'uid': self.FOLDER_OWNER_UID, 'path': self.FOLDER_PATH})
        gid = result['gid']

        result = self.json_ok('share_list_joined_folders', {'uid': self.CONTACT_1_UID})
        assert not len(result)

        with patch.object(DirectoryService, 'open_url') as open_url_mock:
            open_url_mock.return_value = to_json(DIRECTORY_DEPARTMENT_RESPONSE)

            result = self.json_ok('share_invite_user', {'uid': self.FOLDER_OWNER_UID, 'gid': gid,
                                                        'universe_service': 'ya_directory',
                                                        'universe_login': '777:department:5'})

            result = self.json_ok('share_users_in_group', {'uid': self.FOLDER_OWNER_UID, 'gid': gid})
            users = result['users']
            for user in users:
                assert '@' in user['userid']  # мега-проверка на то, что это похоже на email ;)

    def test_group_invite_by_gid(self):
        self.json_ok('mkdir', {'uid': self.FOLDER_OWNER_UID, 'path': '/disk/shared-folder'})
        result = self.json_ok('share_create_group', {'uid': self.FOLDER_OWNER_UID, 'path': self.FOLDER_PATH})
        gid = result['gid']

        result = self.json_ok('share_list_joined_folders', {'uid': self.CONTACT_1_UID})
        assert not len(result)

        result = self.json_ok('share_list_joined_folders', {'uid': self.CONTACT_2_UID})
        assert not len(result)

        with patch.object(DirectoryService, 'open_url') as open_url_mock:
            open_url_mock.return_value = to_json(DIRECTORY_GROUP_RESPONSE)

            result = self.json_ok('share_invite_user', {'uid': self.FOLDER_OWNER_UID, 'gid': gid,
                                                        'universe_service': 'ya_directory',
                                                        'universe_login': '777:group:2', 'ip': '1.2.3.4',
                                                        'auto_accept': 1})

            oid = result['oid']
            self.json_ok('status', {'uid': self.FOLDER_OWNER_UID, 'oid': oid})

            result = self.json_ok('status', {'uid': self.FOLDER_OWNER_UID, 'oid': oid})
            assert result.get('status') == 'DONE'

            result = self.json_ok('share_list_joined_folders', {'uid': self.CONTACT_1_UID})
            assert len(result) == 1
            assert result[0]['path'] == self.FOLDER_PATH

    def test_group_invite_by_path(self):
        path = '/disk/shared-folder'
        self.json_ok('mkdir', {'uid': self.FOLDER_OWNER_UID, 'path': path})
        result = self.json_ok('share_create_group', {'uid': self.FOLDER_OWNER_UID, 'path': self.FOLDER_PATH})

        result = self.json_ok('share_list_joined_folders', {'uid': self.CONTACT_1_UID})
        assert not len(result)

        result = self.json_ok('share_list_joined_folders', {'uid': self.CONTACT_2_UID})
        assert not len(result)

        with patch.object(DirectoryService, 'open_url') as open_url_mock:
            open_url_mock.return_value = to_json(DIRECTORY_GROUP_RESPONSE)

            result = self.json_ok('share_invite_user', {'uid': self.FOLDER_OWNER_UID, 'path': path,
                                                        'universe_service': 'ya_directory',
                                                        'universe_login': '777:group:2', 'ip': '1.2.3.4',
                                                        'auto_accept': 1})

            oid = result['oid']
            self.json_ok('status', {'uid': self.FOLDER_OWNER_UID, 'oid': oid})

            result = self.json_ok('status', {'uid': self.FOLDER_OWNER_UID, 'oid': oid})
            assert result.get('status') == 'DONE'

            result = self.json_ok('share_list_joined_folders', {'uid': self.CONTACT_1_UID})
            assert len(result) == 1
            assert result[0]['path'] == self.FOLDER_PATH

    def test_group_invite_notifications(self):
        self.json_ok('mkdir', {'uid': self.FOLDER_OWNER_UID, 'path': '/disk/shared-folder'})
        result = self.json_ok('share_create_group', {'uid': self.FOLDER_OWNER_UID, 'path': self.FOLDER_PATH})
        gid = result['gid']

        result = self.json_ok('share_list_joined_folders', {'uid': self.CONTACT_1_UID})
        assert not len(result)

        result = self.json_ok('share_list_joined_folders', {'uid': self.CONTACT_2_UID})
        assert not len(result)

        found = [False]

        def fake_send(to, template, args=None, sender_email=None, sender_name=None):
            import mpfs.engine.process
            db = CollectionRoutedDatabase()
            result = db.group_invites.find({'gid': gid})

            for invite in result:
                if invite['universe_login'] == to:
                    if args['hash'] == invite['_id']:
                        found[0] = True
                        break

        with patch.object(DirectoryService, 'open_url') as open_url_mock:
            with patch.object(mailer, 'send', fake_send):
                open_url_mock.return_value = to_json(DIRECTORY_GROUP_RESPONSE)

                self.json_ok('share_invite_user', {'uid': self.FOLDER_OWNER_UID, 'gid': gid,
                                                   'universe_service': 'ya_directory',
                                                   'universe_login': '777:group:2', 'ip': '1.2.3.4'})

                result = self.json_ok('share_list_joined_folders', {'uid': self.CONTACT_1_UID})
                assert len(result) == 0

        assert found[0]

    def test_group_invite_owner(self):
        self.json_ok('mkdir', {'uid': self.CONTACT_1_UID, 'path': '/disk/shared-folder'})
        result = self.json_ok('share_create_group', {'uid': self.CONTACT_1_UID, 'path': self.FOLDER_PATH})
        gid = result['gid']

        result = self.json_ok('share_list_joined_folders', {'uid': self.CONTACT_1_UID})
        assert not len(result)

        result = self.json_ok('share_list_joined_folders', {'uid': self.CONTACT_2_UID})
        assert not len(result)

        found = [False]

        def fake_send(to, template, args=None, sender_email=None, sender_name=None):
            import mpfs.engine.process
            db = CollectionRoutedDatabase()
            result = db.group_invites.find({'gid': gid})

            for invite in result:
                if invite['universe_login'] == to:
                    if args['hash'] == invite['_id']:
                        found[0] = True
                        break

        with patch.object(DirectoryService, 'open_url') as open_url_mock:
            with patch.object(mailer, 'send', fake_send):
                open_url_mock.return_value = to_json(DIRECTORY_GROUP_RESPONSE)

                self.json_ok('share_invite_user', {'uid': self.CONTACT_1_UID, 'gid': gid,
                                                   'universe_service': 'ya_directory',
                                                   'universe_login': '777:group:2', 'ip': '1.2.3.4'})

                result = self.json_ok('share_list_joined_folders', {'uid': self.CONTACT_1_UID})
                assert len(result) == 0

        assert not found[0]  # нельзя пригласить себя

    def test_group_invite_to_already_intited_folder(self):
        # создаем ОП
        self.json_ok('mkdir', {'uid': self.FOLDER_OWNER_UID, 'path': '/disk/shared-folder'})
        result = self.json_ok('share_create_group', {'uid': self.FOLDER_OWNER_UID, 'path': self.FOLDER_PATH})
        gid = result['gid']

        result = self.json_ok('share_list_joined_folders', {'uid': self.CONTACT_1_UID})
        assert not len(result)

        # приглашаем туда пользователя CONTACT_1_UID - мокаем ответ паспорта, чтобы на email вернул нужный uid
        email = 'garkhipov@abook-dev2.ws.yandex.ru'
        real_userinfo = passport_entity.userinfo

        def fake_userinfo(*args, **kwargs):
            if kwargs.get('login') and kwargs['login'] == email:
                return {'uid': self.CONTACT_1_UID, 'language': 'ru'}
            return real_userinfo(*args, **kwargs)

        with patch.object(passport_entity, 'userinfo', fake_userinfo):
            self.json_ok('share_invite_user', {'uid': self.FOLDER_OWNER_UID, 'gid': gid, 'universe_service': 'email',
                                               'universe_login': email, 'ip': '1.2.3.4',
                                               'auto_accept': 1})

        result = self.json_ok('share_list_joined_folders', {'uid': self.CONTACT_1_UID})
        assert len(result) == 1

        # проверим количество инвайтов для пользователя (должно быть 1)
        db = CollectionRoutedDatabase()
        result = db.group_invites.find({'gid': gid, 'uid': self.CONTACT_1_UID})
        assert result.count() == 1

        # а теперь пытаемся пригласить его в ту же папку, но через директорию - приглашаем команду,
        # в которой пользователь, который уже принял приглашение, тоже состоит - новое приглашение добавиться не должно
        oid = None
        with patch.object(DirectoryService, 'open_url') as open_url_mock:
            open_url_mock.return_value = to_json(DIRECTORY_DEPARTMENT_RESPONSE)
            result = self.json_ok('share_invite_user', {'uid': self.FOLDER_OWNER_UID, 'gid': gid,
                                                        'universe_service': 'ya_directory',
                                                        'universe_login': '777:department:5', 'ip': '1.2.3.4',
                                                        'auto_accept': 1})
            oid = result['oid']

        # проверим количество инвайтов для пользователя (все еще должно быть 1 - не должно быть создано еще)
        db = CollectionRoutedDatabase()
        result = db.group_invites.find({'gid': gid, 'uid': self.CONTACT_1_UID})
        assert result.count() == 1

        # проверим статус операции, она не должна зафейлиться - если приглашение уже есть, то просто пропускаем этого
        # пользователя
        assert oid is not None
        result = self.json_ok('status', {'uid': self.FOLDER_OWNER_UID, 'oid': oid})
        assert result['status'] == 'DONE'
