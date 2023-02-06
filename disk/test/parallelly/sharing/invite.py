# -*- coding: utf-8 -*-
import os
import hashlib

import mock

from nose_parameterized import parameterized
from hamcrest import (
    assert_that,
    calling,
    raises,
    has_item,
    has_entry,
    contains_string,
    is_,
    is_not
)
from lxml import etree

from mpfs.common.static.tags import experiment_names
from test.common.sharing import CommonSharingMethods
from test.helpers.stubs.services import SearchDBStub, PushServicesStub, PassportStub
import mpfs.engine.process
from test.base_suit import set_up_open_url, tear_down_open_url
from test.fixtures.users import pdd_user
from test.parallelly.api.disk.auth_suit import Response

import mpfs.engine.process
from mpfs.common.errors.share import ShareNotFound
from mpfs.core.factory import get_resources_by_resource_ids
from mpfs.core.address import ResourceId
from mpfs.core.social.share.link import LinkToGroup
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase
from mpfs.common.static import codes
from mpfs.common.util import mailer


db = CollectionRoutedDatabase()


class InviteShareTestCase(CommonSharingMethods):
    def setup_method(self, method):
        super(InviteShareTestCase, self).setup_method(method)
        self.json_ok('user_init', {'uid': self.uid_1})
        self.json_ok('user_init', {'uid': self.uid_3})
        self.make_dirs()

    def test_has_shared_flag_mismatch(self):
        u"""Проверяем что в запросах создающих сущность ОП не будет зафиксировано несоответствий."""
        self.json_ok('mkdir', {'uid': self.uid_1, 'path': '/disk/folder'})
        self.upload_file(self.uid_1, '/disk/folder/f1.ext')

        with mock.patch('mpfs.core.user.common.log_smart_common_requests') as mocked_logger:
            gid = self.json_ok('share_create_group', {'uid': self.uid_1, 'path': '/disk/folder'})['gid']
            hsh = self.json_ok('share_invite_user', {'uid': self.uid_1, 'gid': gid, 'universe_login': self.email_3,
                                                     'universe_service': 'email', 'rights': '660'})['hash']
            self.json_ok('share_activate_invite', {'uid': self.uid_3, 'hash': hsh})

        logged_messages = [call_args.args[0] for call_args in mocked_logger.call_args_list]
        assert_that(logged_messages, is_not(has_item(contains_string('match with real state=False'))))

    @parameterized.expand([
        ('all_active', {experiment_names.REQUESTS_TO_COMMON_FOR_GROUPS_SET_FLAG: True,
                        experiment_names.REQUESTS_TO_COMMON_FOR_GROUPS_USE_FLAG: True}),
        ('only_set', {experiment_names.REQUESTS_TO_COMMON_FOR_GROUPS_SET_FLAG: True,
                      experiment_names.REQUESTS_TO_COMMON_FOR_GROUPS_USE_FLAG: False}),
        ('all_disabled', {experiment_names.REQUESTS_TO_COMMON_FOR_GROUPS_SET_FLAG: False,
                          experiment_names.REQUESTS_TO_COMMON_FOR_GROUPS_USE_FLAG: False}),
    ])
    def test_has_shared_folder(self, case_name, exps):
        def fake_is_feature_active(_, name):
            return exps.get(name)

        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)
        hsh = self.invite_user(uid=self.uid_1, email=self.email_1, ext_gid=gid)
        self.activate_invite(uid=self.uid_1, hash=hsh)

        # проверяем владельца
        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder2'}
        with mock.patch('mpfs.common.util.experiments.logic.ExperimentManager.is_feature_active',
                        fake_is_feature_active):
            listing_result = self.json_ok('list', opts)

        assert_that(listing_result, has_item(has_entry('id', '/disk/new_folder/folder2/file3')),
                    "result should contain a file inside shared folder for owner user")

        # проверяем приглашенного
        opts = {'uid': self.uid_1, 'path': '/disk/folder2'}
        with mock.patch('mpfs.common.util.experiments.logic.ExperimentManager.is_feature_active',
                        fake_is_feature_active):
            listing_result = self.json_ok('list', opts)

        assert_that(listing_result, has_item(has_entry('id', '/disk/folder2/file3')),
                    "result should contain a file inside shared folder for invited user")

    def test_has_shared_folders_exps_mismatch(self):
        from mpfs.core.user.base import User
        from mpfs.core.metastorage.control import disk_info
        from mpfs.core.user.constants import HAS_SHARED_FOLDERS_FIELD
        def fake_is_feature_active(_, name):
            return {experiment_names.REQUESTS_TO_COMMON_FOR_GROUPS_SET_FLAG: True,
                    experiment_names.REQUESTS_TO_COMMON_FOR_GROUPS_USE_FLAG: False}.get(name)

        self.create_group()
        # сбрасываем флажок (и чистим кэш, чтобы при проверке взять актуальное значение)
        disk_info.remove(self.uid, HAS_SHARED_FOLDERS_FIELD)
        LinkToGroup.reset()

        with mock.patch('mpfs.common.util.experiments.logic.ExperimentManager.is_feature_active',
                        fake_is_feature_active), \
            mock.patch('mpfs.core.user.common.log_smart_common_requests') as mocked_logger:
            User(self.uid).has_shared_folders()

        assert_that(mocked_logger.call_args.args[0], contains_string('match with real state=False'))

    def test_invites_only_from_owner(self):
        # https://st.yandex-team.ru/CHEMODAN-27899
        owner = {
            'uid': self.uid_1,
            'email': self.email_1,
            'dir_path': '/disk/owner_dir',
            'gid': None,
        }
        villain = {
            'uid': self.uid_3,
            'email': self.email_3,
            'dir_path': '/disk/villain_dir',
            'gid': None,
        }
        users = (owner, villain)
        for user in users:
            self.json_ok('mkdir', {'uid': user['uid'], 'path': user['dir_path']})
            user['gid'] = self.json_ok('share_create_group', {'uid': user['uid'], 'path': user['dir_path']})['gid']

        # зная gid группы злоумышленник сам высылает себе инвайт.
        bug_params = {
            'uid': villain['uid'],
            'gid': owner['gid'],
            'universe_login': villain['email'],
            'universe_service': 'email'
        }
        # Но теперь нет
        self.json_error('share_invite_user', bug_params, code=108)

        # проверяем, что режим с группой-папкой тоже работает
        params = {
            'uid': villain['uid'],
            'path': villain['dir_path'],
            'universe_login': owner['email'],
            'universe_service': 'email'
        }
        self.json_ok('share_invite_user', params)

        # проверяем, что режим с обычной папкой работает
        self.json_ok('mkdir', {'uid': villain['uid'], 'path': '/disk/common_dir'})
        params = {
            'uid': villain['uid'],
            'path': '/disk/common_dir',
            'universe_login': owner['email'],
            'universe_service': 'email'
        }
        self.json_ok('share_invite_user', params)

    def test_group_in_group(self):
        self.create_group()
        args = {'uid': self.uid, 'path': '/disk/new_folder/folder2/folder3'}
        self.mail_error('share_create_group', args)

    def test_invite_reject_invite_accept(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, ext_gid=gid)

        # Reject invite, user
        xiva_requests = set_up_open_url()
        opts = {
            'uid': self.uid_3,
            'hash': hsh,
        }
        with PushServicesStub() as push_service:
            self.mail_ok('share_reject_invite', opts)
            pushes = [PushServicesStub.parse_send_call(push_args)
                      for push_args in push_service.send.call_args_list]
        actor_push = pushes[0]
        owner_push = pushes[1]

        assert actor_push['uid'] == self.uid_3
        assert actor_push['json_payload']['root']['parameters']['type'] == 'invite_rejected'
        assert actor_push['json_payload']['root']['parameters']['for'] == 'actor'
        assert actor_push['json_payload']['values'][0]['parameters']['gid'] == gid
        assert actor_push['json_payload']['values'][0]['parameters']['hash'] == hsh

        assert owner_push['uid'] == self.uid
        assert owner_push['json_payload']['root']['parameters']['type'] == 'invite_rejected'
        assert owner_push['json_payload']['root']['parameters']['for'] == 'owner'
        assert owner_push['json_payload']['values'][0]['parameters']['gid'] == gid
        assert owner_push['json_payload']['values'][0]['parameters']['path'] == '/disk/new_folder/folder2/'

        # Assert invite is rejected
        opts = {
            'uid': self.uid_3,
        }
        result = self.mail_ok('share_list_not_approved_folders', opts)
        children_length = len(list(result.iterchildren()))
        self.assertEqual(children_length, 0)

        hsh = self.invite_user(uid=self.uid_3, ext_gid=gid)
        #=======================================================================
        # Remove invite, owner
        def hashed(val):
            if not isinstance(val, (str, unicode)):
                val = str(val)
            elif isinstance(val, unicode):
                val = val.encode('utf-8')
            else:
                val = str(val)
            return hashlib.md5(val).hexdigest()

        _id = hashed('%s:%s:%s' % (gid, self.email_3, 'email'))
        self.assertNotEqual(list(db.group_invites.find({'_id': _id})), None)
        opts = {
            'uid': self.uid,
            'gid': gid,
            'universe_login': self.email_3,
            'universe_service': 'email',
        }


        with PushServicesStub() as push_service:
            self.mail_ok('share_remove_invite', opts)
            pushes = [PushServicesStub.parse_send_call(push_args)
                      for push_args in push_service.send.call_args_list]

        assert len(pushes) == 2, 'Должно быть только 2 пуша (для actor и для owner)'

        owner_push = pushes[0]
        actor_push = pushes[1]

        assert owner_push['uid'] == self.uid
        assert owner_push['json_payload']['root']['parameters']['type'] == 'invite_removed'
        assert owner_push['json_payload']['root']['parameters']['for'] == 'owner'
        assert owner_push['json_payload']['values'][0]['parameters']['gid'] == gid
        assert owner_push['json_payload']['values'][0]['parameters']['path'] == '/disk/new_folder/folder2/'
        for expected_key in ('universe_login', 'universe_service', 'name', 'avatar'):
            assert owner_push['json_payload']['values'][1]['parameters'].get(expected_key) is not None

        assert actor_push['uid'] == self.uid_3
        assert actor_push['json_payload']['root']['parameters']['type'] == 'invite_removed'
        assert actor_push['json_payload']['root']['parameters']['for'] == 'actor'
        assert actor_push['json_payload']['values'][0]['parameters']['gid'] == gid
        assert actor_push['json_payload']['values'][0]['parameters']['path'] == '/disk/folder2/'

        # Assert invite is removed
        result = self.mail_ok('share_list_not_approved_folders',
                              {'uid': self.uid_3})
        children_length = len(list(result.iterchildren()))
        self.assertEqual(children_length, 0)

    def test_invite_to_ya_ru(self):
        gid = self.create_group()
        self.invite_user(uid=self.uid_3, ext_gid=gid)

        invites_count = db.group_invites.find({'gid': gid, 'uid': str(self.uid_3)}).count()
        self.invite_user(email='mpfs-test-3@ya.ru')
        self.assertEqual(db.group_invites.find({'gid': gid, 'uid': str(self.uid_3)}).count(), invites_count + 1)

    def test_change_invite_rights(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, ext_gid=gid)

        def change_rights(rights):
            opts = {
                'uid': self.uid,
                'gid': gid,
                'universe_login': 'mpfs-test-3@yandex.ru',
                'universe_service': 'email',
                'rights': rights,
            }
            open_url_data = set_up_open_url()
            self.mail_ok('share_change_rights', opts)
            tear_down_open_url()
            notify_sent_actor = False
            notify_sent_owner = False
            notify_sent_user = False
            url = 'http://localhost/service/echo?uid='
            # self.fail(open_url_data)
            for k, v in open_url_data.iteritems():
                if k.startswith(url):
                    for each in v:
                        share_tag = etree.fromstring(each['pure_data'])
                        if share_tag.tag == 'share' and share_tag.get('type') == 'rights_changed':
                            if share_tag.get('for') == 'owner':
                                notify_sent_owner = True
                            elif share_tag.get('for') == 'user':
                                notify_sent_user = True
                            elif share_tag.get('for') == 'actor':
                                notify_sent_actor = True
            self.assertTrue(notify_sent_owner)
            self.assertFalse(notify_sent_actor)
            self.assertFalse(notify_sent_user)
            opts = {
                'uid': self.uid_3,
            }
            result = self.mail_ok('share_list_not_approved_folders', opts)
            children_length = len(list(result.iterchildren()))
            self.assertEqual(children_length, 1)
            good_result_unknown = ('ctime', 'size')
            good_result_defined = {
                'owner_name': 'Vasily P.',
                'hash': hsh,
                'folder_name': 'folder2',
                'owner_uid': str(self.uid),
                'status': 'proposed',
                'rights': rights,
                'gid': gid
            }
            found = False
            for folder in result.iterfind('folder'):
                if folder.find('hash').text == hsh:
                    found = True
                    for each in folder.iterchildren():
                        try:
                            self.assertEqual(each.text, good_result_defined[each.tag])
                        except KeyError:
                            self.assertTrue(each.tag in good_result_unknown)
            self.assertTrue(found)

        change_rights('640')
        change_rights('660')

    def test_invite_third_user(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder2'}
        listing_result = self.mail_ok('list', opts)
        self.assertEqual(
            int(listing_result.find('folder-list').find('folder').find('meta').find('group').find('user_count').text),
            2)

        args = {
            'gid': gid,
            'uid': self.uid,
        }
        folder_users = self.mail_ok('share_users_in_group', args)
        users_count = len(list(folder_users.find('users').iterfind('user')))
        self.assertEqual(users_count, 2)
        folder_data = db.user_data.find_one({'uid': self.uid_1, 'key': '/disk/folder2'})
        self.assertEqual(folder_data, None)

        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder2'}
        listing_result = self.mail_ok('list', opts)
        self.assertEqual(
            int(listing_result.find('folder-list').find('folder').find('meta').find('group').find('user_count').text),
            2)

        hsh = self.invite_user(uid=self.uid_1, email=self.email_1, ext_gid=gid)

        invite_data = db.group_invites.find_one({'universe_login': self.email_1, 'gid': gid, '_id': hsh})
        folder_data = db.user_data.find_one({'uid': self.uid_1, 'key': '/disk/folder2'})
        self.assertEqual(folder_data, None)
        self.assertNotEqual(invite_data, None)

        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder2'}
        listing_result = self.mail_ok('list', opts)
        self.assertEqual(
            int(listing_result.find('folder-list').find('folder').find('meta').find('group').find('user_count').text),
            3)

        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder2'}
        listing_result = self.mail_ok('list', opts)
        self.assertEqual(
            int(listing_result.find('folder-list').find('folder').find('meta').find('group').find('user_count').text),
            3)

        folder_info = self.activate_invite(self.uid_1, hsh)
        self.assertEqual(folder_info['id'], '/disk/folder2/')
        args = {
            'gid': gid,
            'uid': self.uid,
        }
        folder_users = self.mail_ok('share_users_in_group', args)
        users_count = len(list(folder_users.find('users').iterfind('user')))
        self.assertEqual(users_count, 3)

    def test_check_users_count(self):
        gid = self.create_group()

        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        hsh = self.invite_user(uid=self.uid_1, email=self.email_1, ext_gid=gid)
        self.activate_invite(uid=self.uid_1, hash=hsh)

        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder2'}
        listing_result = self.mail_ok('list', opts)
        self.assertEqual(
            int(listing_result.find('folder-list').find('folder').find('meta').find('group').find('user_count').text),
            3)

        opts = {'uid': self.uid_1, 'path': '/disk/folder2'}
        listing_result = self.mail_ok('list', opts)
        self.assertEqual(
            int(listing_result.find('folder-list').find('folder').find('meta').find('group').find('user_count').text),
            3)

    def test_get_link_by_uid(self):
        """
        Тестирование метода get_link_by_uid у Group
        """
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_1, email=self.email_1, ext_gid=gid)
        self.activate_invite(uid=self.uid_1, hash=hsh)

        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder2', 'meta': 'resource_id'}
        resource_id = self.json_ok('info', opts)['meta']['resource_id']
        resource_id = ResourceId.parse(resource_id)
        resources = get_resources_by_resource_ids(self.uid, [resource_id])

        group_link = resources[0].group.get_link_by_uid(self.uid_1)
        assert_that(group_link, is_(LinkToGroup))
        assert group_link.is_rw()

        assert_that(calling(resources[0].group.get_link_by_uid).with_args(self.uid_3), raises(ShareNotFound))

    def test_reinvite_buddy(self):
        gid = self.create_group()

        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        args = {
            'uid': self.uid,
            'gid': gid,
            'rights': 660,
            'universe_login': 'mpfs-test-3@yandex.ru',
            'universe_service': 'email',
            'avatar': 'http://localhost/echo',
            'name': 'mpfs-test',
        }
        result = self.mail_error('share_invite_user', args)
        self.assertEqual(result.find('error').find('code').text, '112')
        self.inspect_all()

    def test_try_to_make_share_in_shared(self):
        self.create_group()

        args = {'uid': self.uid, 'path': '/disk/folder2/folder3/file4'}
        self.mail_error('share_create_group', args)
        self.inspect_all()

    def test_invite_pdd(self):
        from mpfs.config import settings
        settings.feature_toggles['allow_pdd'] = True

        path = '/disk/pdd_folder'

        self.json_ok('mkdir', {'uid': self.uid_3, 'path': path})
        result = self.mail_ok('share_create_group', {'uid': self.uid_3, 'path': path})
        gid = None
        for each in result.getchildren():
            if each.tag == 'gid' and each.text and isinstance(each.text, str):
                gid = each.text
        self.assertIsNotNone(gid)

        self.json_ok('user_init', {'uid': pdd_user.uid})

        args = {
            'rights': '660',
            'universe_login': pdd_user.email,
            'universe_service': 'email',
            'avatar': 'http://localhost/echo',
            'name': 'mpfs-test',
            'connection_id': '1234',
            'uid': self.uid_3,
            'gid': gid,
        }
        self.mail_ok('share_invite_user', args)
        result = self.mail_ok('share_list_not_approved_folders', {'uid': pdd_user.uid})
        self.assertIsNotNone(result.find('folder'))

    def try_to_create_group_on_file(self):
        args = {'uid': self.uid, 'path': '/disk/new_folder/folder2/file3'}
        self.mail_error('share_create_group', args, code=77)
        args = {
            'uid': self.uid,
            'path': '/disk/new_folder/folder2/file3',
            'rights': 660,
            'universe_login': self.email,
            'universe_service': 'email',
            'avatar': 'http://localhost/echo',
            'name': 'mpfs-test',
        }
        self.mail_error('share_invite_user', args, code=77)

    def test_invite_cyrillic(self):
        email = self.email_cyrillic
        args = {
            'rights': 660,
            'universe_login': email,
            'universe_service': 'email',
            'avatar': 'http://localhost/echo',
            'name': 'mpfs-test',
            'connection_id': '1234',
            'uid': self.uid,
            'path': '/disk/new_folder/folder2',
        }
        result = self.json_ok('share_invite_user', args)
        self.assertTrue('hash' in result)

    def test_invite_dots(self):
        email = self.email_cyrillic_dots
        args = {
            'rights': 660,
            'universe_login': email,
            'universe_service': 'email',
            'avatar': 'http://localhost/echo',
            'name': 'mpfs-test',
            'connection_id': '1234',
            'uid': self.uid,
            'path': '/disk/new_folder/folder2',
        }
        result = self.json_ok('share_invite_user', args)
        self.assertTrue('hash' in result)

        email = self.email_dots
        args = {
            'rights': 660,
            'universe_login': email,
            'universe_service': 'email',
            'avatar': 'http://localhost/echo',
            'name': 'mpfs-test',
            'connection_id': '1234',
            'uid': self.uid,
            'path': '/disk/new_folder/folder2',
        }
        result = self.json_ok('share_invite_user', args)
        self.assertTrue('hash' in result)

    def test_auto_accept_invite(self):
        """ Проверка автоматического принятия приглашения в общую папку.

        1. Создаем группу
        2. Приглашаем пользователя и автоматически подтверждаем приглашение
        3. Проверяем, что у приглашенного появилась общая директория
        """

        owner_uid = self.uid
        owner_folder = '/disk/new_folder/folder2'
        gid = self.create_group(owner_uid, owner_folder)

        args = {
            'rights': 660,
            'universe_login': self.email_3,
            'universe_service': 'email',
            'avatar': 'http://localhost/echo',
            'name': 'mpfs-test',
            'connection_id': '1234',
            'uid': owner_uid,
            'path': owner_folder,
            'auto_accept': '1',
        }
        set_up_open_url()
        self.json_ok('share_invite_user', args)
        tear_down_open_url()

        response = self.json_ok('share_folder_info', {'gid': gid, 'uid': self.uid_3})
        user_folder = response['id']

        get_name = lambda x: os.path.split(x)[-1]
        assert get_name(owner_folder) == get_name(user_folder)

        response = self.json_ok('info', {'uid': self.uid_3, 'path': user_folder, 'meta': 'group'})
        assert 'group' in response['meta']

    @parameterized.expand([
        ('tr', False, None, 'ru', 'ru'),
        ('tr',  True, None, 'en', 'en'),
        ('tr',  True, 'en', 'ru', 'en'),
        (None,  True, 'tr', 'ru', 'tr'),
        (None,  True, None, 'en', 'en'),
        (None, False, None, 'en', 'en'),
        (None, False, None, None, 'ru'),
    ])
    def test_email_locale(self, query_locale, participant_has_passport_entry, participant_passport_language,
                          owner_passport_language, expected_email_locale):
        folder_path = '/disk/test'
        self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path})
        gid = self.create_group(self.uid, folder_path)

        participant_email = self.email_1 if participant_has_passport_entry else 'unknown_email@test.com'

        params = {
            'uid': self.uid,
            'gid': gid,
            'universe_service': 'email',
            'universe_login': participant_email,
            'ip': '1.2.3.4'
        }

        if query_locale:
            params['locale'] = query_locale

        if owner_passport_language:
            PassportStub.update_info_by_uid(self.uid, language=owner_passport_language)
        else:
            PassportStub.update_info_by_uid(self.uid, language='')

        if participant_passport_language:
            PassportStub.update_info_by_uid(self.uid_1, language=participant_passport_language)
        else:
            PassportStub.update_info_by_uid(self.uid_1, language='')

        with mock.patch.object(mailer, 'send') as mocked_send:
            self.json_ok('share_invite_user', params)
            assert mocked_send.called
            args, _ = mocked_send.call_args
            params = args[2]

            assert 'locale' in params
            assert params['locale'] == expected_email_locale


class ShareInviteInfoTestCase(CommonSharingMethods):
    """Набор тестов для ручки `share_invite_info`."""
    endpoint = 'share_invite_info'

    def setup_method(self, method):
        super(ShareInviteInfoTestCase, self).setup_method(method)
        self.json_ok('user_init', {'uid': self.uid_1})
        self.xiva_subscribe(self.uid_1)
        self.json_ok('user_init', {'uid': self.uid_3})
        self.xiva_subscribe(self.uid_3)
        args = {'uid': self.uid, 'path': '/disk/new_folder'}
        self.json_ok('mkdir', args)
        args = {'uid': self.uid, 'path': '/disk/new_folder/folder2'}
        self.json_ok('mkdir', args)

    def test_share_invite_info_default(self):
        gid = self.create_group()

        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {
            'hash': hsh,
            'uid': self.uid_3,
        }
        result = self.mail_ok(self.endpoint, opts)
        invite = result.find('invite')
        self.assertEqual(invite.find('id').text, '/disk/folder2')
        opts = {
            'hash': hsh,
            'uid': self.uid,
        }
        self.mail_error('share_invite_info', opts)

    def test_share_invite_info_with_include_files_count(self):
        """Протестировать ответ, когда необходимо получить количество файлов в группе."""
        # Количество файлов получаем из поиска.
        gid = self.create_group()

        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {
            'hash': hsh,
            'uid': self.uid_3,
            'include_files_count': 1
        }
        with SearchDBStub() as stub:
            stub.folder_size.return_value = {
                'path': '/disk/some/path/to/file/or/folder',
                'size': '100500',
                'files_count': '300'
            }
            result = self.json_ok(self.endpoint, opts)
            assert 'files_count' in result
            assert result['files_count'] == 300

    def test_share_invite_info_without_include_files_count(self):
        """Протестировать ответ, когда количество файлов в группе не надо получать."""
        gid = self.create_group()

        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {
            'hash': hsh,
            'uid': self.uid_3,
        }
        with SearchDBStub() as stub:
            stub.folder_size.return_value = {
                'path': '/disk/some/path/to/file/or/folder',
                'size': '100500',
                'files_count': '300'
            }
            result = self.json_ok(self.endpoint, opts)
            assert not stub.folder_size.called
            assert 'files_count' not in result

    def test_invite_user_when_limit_for_users_is_reached(self):
        folder_path = '/disk/full_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path})
        self.create_group(path=folder_path)
        from mpfs.config import settings
        with mock.patch.dict(settings.social, {'max_users_number': 0}):
            args = {
                'rights': '660',
                'universe_login': self.email_3,
                'universe_service': 'email',
                'avatar': 'http://localhost/echo',
                'name': 'mpfs-test',
                'connection_id': '1234',
                'uid': self.uid,
                'path': folder_path,
            }
            self.json_error('share_invite_user', args, code=codes.SHARE_USERS_LIMIT_REACHED)

    def test_spam_check_resolution_spam(self):
        gid = self.create_group()
        fake_resp = Response('<spam>1</spam>')

        with mock.patch('mpfs.core.services.spam_checker_service.SpamCheckerService.request', return_value=fake_resp):
            with mock.patch.object(mailer, 'send') as mocked_send:
                hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
                # no email send
                assert not mocked_send.called
                # can activate
                self.activate_invite(uid=self.uid_3, hash=hsh)

    def test_spam_check_resolution_not_spam(self):
        gid = self.create_group()
        fake_resp = Response('<spam receipt="test_so_receipt">0</spam>')

        with mock.patch('mpfs.core.services.spam_checker_service.SpamCheckerService.request', return_value=fake_resp):
            with mock.patch.object(mailer, 'send') as mocked_send:
                hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
                # email send
                assert mocked_send.called
                assert mocked_send.call_args[1]['headers']['X-Yandex-CF-Receipt'] == 'test_so_receipt'
                # can activate
                self.activate_invite(uid=self.uid_3, hash=hsh)
