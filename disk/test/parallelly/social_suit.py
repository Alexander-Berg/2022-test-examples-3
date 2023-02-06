# -*- coding: utf-8 -*-
"""Починка тестов.

creds:
  login: mpfs-test@yandex.ru
  pass: efdf2a79dd23f1f3

Если тесты падают с ошибкой SocialProxyInvalidToken:
  - залогиниться на https://passport-test.yandex.ru/auth
  - залогиниться на https://www.facebook.com
  - дернуть https://social-test.yandex.ru/broker2/start?retpath=http://disk.yandex.ru/close_broker.html&consumer=disk&application=facebook&scope=publish_actions,xmpp_login,user_birthday,email,user_photos&require_auth=1
"""
import os
import time
import json
import mock
import pytest
import smtplib
import logging


from hamcrest import assert_that, all_of, has_key, instance_of, is_, has_length, is_not, empty

from test.helpers.assertions import assert_log_contains

from test.base import DiskTestCase
from test.api_suit import set_up_mailbox, tear_down_mailbox
from test.helpers.operation_states import OperationState
from test.helpers.providers import EMAIL, FACEBOOK, TWITTER, INSTAGRAM
from test.helpers import locale
from test.helpers.stubs.manager import StubsManager
from test.helpers.stubs.services import SendEmailStub
from test.helpers.utils import DEVNULL_EMAIL
from test.fixtures.users import pdd_user, test_user, uid_a
from mpfs.core import base as core
from mpfs.core import social
from mpfs.core.social import socialproxy as proxy
from mpfs.core.services.socialproxy_service import SocialProxy
from mpfs.core.services.abook_service import AbookService
from mpfs.common import errors
from mpfs.common.static.tags import SUCCESS, FAILURE
from mpfs.common.util import mailer
from mpfs.config import settings


class CommonSocialMethods(DiskTestCase):
    """
    Базовый миксин для всех социальных штук

    Проверяем все на тестовом уиде 3000257047 из тестового паспорта
    Основные действия изучаем на фейсбучном аккаунте
    """
    uid = test_user.uid
    friend_uid = '166310690'
    pdd_uid = pdd_user.uid

    profiles = {
        'twitter': 100586,
        'facebook': 292422,
        'odnoklassniki': 100949,
        'vkontakte': 103121,
    }

    friend_facebook_id = '100004068433137'

    rights = {
        'twitter': {'link': '', 'auth': True},
        'facebook': {'link': '', 'auth': True},
        'odnoklassniki': {'link': '', 'auth': True},
        'vkontakte': {'link': '', 'auth': True},
    }

    old_passport_url = None
    old_blackbox_url = None

    @classmethod
    def setup_class(cls):
        super(CommonSocialMethods, cls).setup_class()
        from mpfs.core.services.passport_service import Passport
        from mpfs.core.services.passport_service import passport

        cls.old_passport_url = passport.passport_url
        cls.old_blackbox_url = passport.blackbox_url

        Passport.passport_url = 'http://passport-test-internal.yandex.ru/passport?mode=%s'
        passport.passport_url = 'http://passport-test-internal.yandex.ru/passport?mode=%s'
        passport.blackbox_url = 'http://pass-test.yandex.ru/blackbox?method=%s&userip=127.0.0.1'
        Passport.blackbox_url = 'http://pass-test.yandex.ru/blackbox?method=%s&userip=127.0.0.1'

    @classmethod
    def teardown_class(cls):
        from mpfs.core.services.passport_service import Passport
        from mpfs.core.services.passport_service import passport

        Passport.passport_url = cls.old_passport_url
        passport.passport_url = cls.old_passport_url
        passport.blackbox_url = cls.old_blackbox_url
        Passport.blackbox_url = cls.old_blackbox_url
        super(CommonSocialMethods, cls).teardown_class()


class AlbumSocialTestCase(CommonSocialMethods):
    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_public_album_social_wall_post(self):
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json={})
        pk = album['public']['public_key']
        result = self.json_ok('public_album_social_wall_post',
                              opts={'uid': self.uid, 'provider': FACEBOOK, 'public_key': pk})
        assert result['state'] == SUCCESS
        assert 'post_id' in result['result']

    def test_social_rights(self):
        self.json_ok('social_rights', opts={'uid': self.uid, 'scenario': 'wall_post'})


def mock_template_directory(fn):
    def wrapped(*args, **kwargs):
        templates_dir = os.path.abspath('fixtures/templates/')
        with mock.patch.dict(settings.system, {'fs_paths': {'template_dir': templates_dir}}):
            return fn(*args, **kwargs)

    return wrapped


class SmtpTestCase(CommonSocialMethods):
    stubs_manager = StubsManager(class_stubs=set(StubsManager.DEFAULT_CLASS_STUBS) - {SendEmailStub})

    @mock_template_directory
    def test_smtp_log(self):
        mail_resp = (250, '2.1.0 <disk-news@yandex.ru> ok')
        rcpt_resp = (250, '2.1.5 <%s> recipient ok' % DEVNULL_EMAIL)
        data_resp = (250, '2.0.0 Ok: queued on sas2-cfcb8986973a.qloud-c.yandex.net as 1622543697-srv70znyOQ-Yuo8qF7L')
        self.smtp_test_impl(mail_resp, rcpt_resp, data_resp, [mail_resp[1], rcpt_resp[1], data_resp[1]], False)

        rcpt_resp = (300, 'invalid recipient')
        self.smtp_test_impl(mail_resp, rcpt_resp, data_resp, rcpt_resp, False)

        rcpt_resp = (250, 'ok')
        data_resp = (554, '5.7.1 [2] Message rejected under suspicion of SPAM; https://ya.cc/1IrBc 1582024041-8A9BF6y0OJ-7LKiGJvw')
        self.smtp_test_impl(mail_resp, rcpt_resp, data_resp, data_resp, False)

        data_resp = (555, "The SMTP server didn't accept the data.")
        self.smtp_test_impl(mail_resp, rcpt_resp, data_resp, data_resp, True)


    def smtp_test_impl(self, mail_resp, rcpt_resp, data_resp, expected_logs, exception_expected):
        with mock.patch('smtplib.SMTP.mail', return_value=mail_resp), \
            mock.patch('smtplib.SMTP.rcpt', return_value=rcpt_resp), \
            mock.patch('smtplib.SMTP.data', return_value=data_resp), \
            mock.patch.object(logging.Logger, 'info') as mocked_log:
            try:
                mailer.send(DEVNULL_EMAIL, 'example', {'text': 'mailer test'})
            except Exception:
                assert exception_expected
            mocked_log.assert_called()
            for each in expected_logs:
                assert_log_contains(mocked_log, str(each))

class EmailProcessorTestCase(CommonSocialMethods):
    email_template = settings.invite['templates']['got']

    @mock_template_directory
    def test_send(self):
        letters = set_up_mailbox()
        mailer.send(DEVNULL_EMAIL, 'example', {'text': 'mailer test'})
        tear_down_mailbox()

        self.assertTrue(len(letters) > 0)
        self.assertEqual(letters[0]['args'][0], DEVNULL_EMAIL)

    @mock_template_directory
    def test_send_with_wrong_locale(self):
        """
        если такой локали нет, то посылаем на дефолтной
        """
        letters = set_up_mailbox()
        mailer.send(DEVNULL_EMAIL,
                    'example',
                    {'text': 'mailer test',
                     'locale': 'buhaha'})
        tear_down_mailbox()

        self.assertTrue(len(letters) > 0)
        self.assertEqual(letters[0]['args'][2]['locale'], locale.RU_RU)

    def test_send_with_cc(self):
        carbon_copy = [DEVNULL_EMAIL, DEVNULL_EMAIL]
        set_up_mailbox()
        msg = mailer.send(DEVNULL_EMAIL,
                          self.email_template,
                          {'locale': locale.EN_EN,
                           'cc': carbon_copy})
        tear_down_mailbox()
        self.assertEqual(msg['Cc'], ",".join(carbon_copy))

    @mock_template_directory
    def test_send_with_from(self):
        sender = u'Василий Орангутанов'
        msg1 = mailer.send(DEVNULL_EMAIL,
                           'example',
                           {'text': 'mailer test',
                            'locale': 'buhaha'},
                           sender_name=sender)

        msg2 = mailer.send(DEVNULL_EMAIL,
                           'exampleFrom',
                           {'text': 'mailer test',
                            'locale': 'buhaha',
                            'fromName': sender})

        self.assertEqual(msg1['From'], msg2['From'])

    def test_send_from_yandexcom(self):
        msg = mailer.send(DEVNULL_EMAIL, self.email_template, {'locale': locale.TR_TR})
        self.assertEqual(msg['From'], 'Yandex.Disk Ekibi <disk-news@yandex.com>')

    @mock_template_directory
    def test_send_spam_rejection(self):
        spam_exc = smtplib.SMTPDataError(554,
                                         '5.7.1 [2] Message rejected under suspicion of SPAM; https://ya.cc/1IrBc 1582024041-8A9BF6y0OJ-7LKiGJvw')
        with mock.patch.object(mailer, 'sendmail', side_effect=spam_exc) as mocked_send:
            res = mailer.send(DEVNULL_EMAIL, 'example', {'text': 'mailer test'})
            assert res

        data_exc = smtplib.SMTPDataError(555, "The SMTP server didn't accept the data.")
        with mock.patch.object(mailer, 'sendmail', side_effect=data_exc) as mocked_send:
            self.assertRaises(smtplib.SMTPDataError,
                              mailer.send,
                              DEVNULL_EMAIL, 'example', {'text': 'mailer test'})


class AbookTestCase(CommonSocialMethods):
    def test_abook_list_contacts(self):
        contacts = AbookService().get_contacts(self.uid)
        assert len(list(contacts)) > 0
        for contact in contacts:
            assert_that(contact, all_of(is_(instance_of(dict)),
                                        has_key("name"),
                                        has_key("avatar"),
                                        has_key("locale")),
                        u"Контакт не содержит обязательного поля")

    def test_abook_list_pdd_contacts(self):
        AbookService().add_email(self.pdd_uid, 'somebody@globus-karelia.ru')
        contacts = AbookService().get_contacts(self.pdd_uid)
        assert len(list(contacts)) > 0
        for contact in contacts:
            assert_that(contact, all_of(is_(instance_of(dict)),
                                        has_key("name"),
                                        has_key("avatar"),
                                        has_key("locale")),
                        u"Контакт не содержит обязательного поля")

    def test_abook_groups_fetch(self):
        abook_service_query_result = {'count': 2,
                                      'contact': [{'name': {'middle': 'Middle',
                                                            'full': 'First Middle Last',
                                                            'last': 'Last',
                                                            'first': 'First'},
                                                   'cid': 3,
                                                   'company': 'Company',
                                                   'tags': [],
                                                   'phone': ['+7 000 669 65 33'],
                                                   'mcid': 3,
                                                   'email': 'anastasia@zaryapc.example',
                                                   'tag': [],
                                                   'usage_count': 0,
                                                   'title': '123321',
                                                   'last_usage': 0,
                                                   'id': 4},
                                                  {'name': {'middle': 'Ivanovich',
                                                            'full': 'Ivanov Ivan Ivanovich',
                                                            'last': 'Ivanov',
                                                            'first': 'Ivan'},
                                                   'cid': 1,
                                                   'ya_directory': {'type': 'group',
                                                                    'id': 1130000000156389,
                                                                    'org_name': 'Fluffy Cats',
                                                                    'org_id': 777},
                                                   'tags': [],
                                                   'phone': ['+7 231 296 96 96'],
                                                   'mcid': 1,
                                                   'email': 'dfomina@abook-dev2.ws.yandex.ru',
                                                   'tag': [],
                                                   'usage_count': 1,
                                                   'department': 'Ivanov INC.',
                                                   'title': 'CEO',
                                                   'last_usage': 1444953600,
                                                   'id': 2}],
                                      'pager': {'items-count': 5}}

        def find_group_contact(specify_show_groups, show_groups_value=0):
            if specify_show_groups:
                result = self.json_ok('async_social_contacts',
                                      {'uid': self.uid, 'groups': show_groups_value})
            else:
                result = self.json_ok('async_social_contacts', {'uid': self.uid})
            oid = result['oid']

            result = {}
            contacts = None
            while not result.get('status') == OperationState.DONE.name:
                result = self.json_ok('status', {'uid': self.uid, 'oid': oid})
                time.sleep(0.5)

            for stage in result['stages']:
                if 'details' in stage:
                    contacts = stage['details']
                    break

            assert contacts is not None

            is_group_found = False
            for contact in contacts:
                if 'group' in contact:
                    assert 'groupid' in contact['group']
                    is_group_found = True
                    break

            return is_group_found

        with mock.patch.object(AbookService, 'open_url') as open_url_mock:
            open_url_mock.return_value = json.dumps(abook_service_query_result)

            # проверим, что если не указать параметр, группы не добавятся
            assert not find_group_contact(False)

            # проверим, что если указать параметр 0, группы не добавятся
            assert not find_group_contact(True, 0)

            # проверим, что если указать параметр 1, группы добавятся
            assert find_group_contact(True, 1)


class SocialProxyTestCase(CommonSocialMethods):
    def test_get_profiles(self):
        profiles = SocialProxy().get_profiles(self.uid)
        for provider, profile in self.profiles.iteritems():
            self.assertEqual(profiles[provider], profile)
        self.assertEqual(profiles, self.profiles)

    def test_get_profile_for_provider(self):
        profile_id = SocialProxy().get_profile_for_provider(self.friend_uid, TWITTER)
        self.assertEqual(profile_id, 5516)

    @pytest.mark.xfail(reason="https://st.yandex-team.ru/CHEMODAN-23062")
    def test_access_rights(self):
        self.assertEqual(proxy.user_rights(self.uid, 'invite'), self.rights)

    @pytest.mark.xfail(reason="https://st.yandex-team.ru/CHEMODAN-23062")
    def test_send_message_to_facebook(self):
        task = proxy.send_message(self.uid, FACEBOOK, self.friend_facebook_id, 'test message')

        task_done = False
        while not task_done:
            time.sleep(0.5)
            result = proxy.check_message(task)
            if result.get('state') in (SUCCESS, FAILURE):
                task_done = True

        self.assertEqual(result.get('state'), SUCCESS,
                         msg="Failed to send a facebook message. Reason: {}"
                         .format(result.get('reason')))


class ComplexOperationsTestCase(CommonSocialMethods):
    def test_access_rights(self):
        request = self.get_request({'uid': self.uid, 'groups': 0, 'scenario': 'invite'})
        for status in core.social_rights(request).values():
            assert_that(status, all_of(has_key("auth"),
                                       has_key("link")),
                        u"Элемент 'статус прав' не содержит обязательного поля")

    def test_send_bad_message(self):
        self.assertRaises(errors.SocialWrongProviderError,
                          social.send_message,
                          self.uid, TWITTER, 'xxx', 'example', {})

    def test_end_with_empty_profile(self):
        self.run_000_user_check(uid=uid_a)
        self.assertRaises(errors.SocialProxyNoProfiles,
                          social.send_message,
                          uid_a, FACEBOOK, 'xxx', 'referral/offer', {})

    @mock.patch.dict('mpfs.config.settings.feature_toggles', {'disallow_invite_disk_users': False})
    def test_send_invite_via_email(self):
        """Тестирует отправку инвайтов через email"""
        self.send_invite_with_retry(EMAIL, DEVNULL_EMAIL)

        for invite in self.get_invites_statuses(EMAIL):
            self.assertEqual(invite['status'], SUCCESS)

    @pytest.mark.xfail(reason="https://st.yandex-team.ru/CHEMODAN-23062")
    def test_send_invite_via_facebook(self):
        """Тестирует отправку инвайтов через facebook"""
        self.send_invite_with_retry(FACEBOOK, self.friend_facebook_id)

        for invite in self.get_invites_statuses(FACEBOOK):
            for friend in invite['details']:
                if friend['user']['userid'] == self.friend_facebook_id:
                    self.assertEqual(friend['user']['sent'], 1)

    def send_invite_with_retry(self, provider, address):
        def _send_invite(provider, address):
            request = self.get_request({'uid': self.uid,
                                        'provider': provider,
                                        'address': address,
                                        'info': {}})
            oid = core.async_user_invite_friend(request).get('oid')
            request.set_args({'uid': self.uid, 'oid': oid})

            while not core.status(request).get('status') in (OperationState.FAILED,
                                                             OperationState.COMPLETED):
                time.sleep(0.5)

            return core.status(request)

        retry_limit = 10
        while retry_limit > 0:
            result = _send_invite(provider, address)
            if result.get('status') == OperationState.COMPLETED:
                break
            retry_limit -= 1

        if retry_limit == 0:
            self.fail("Failed to send an invite via {}. Details: {}"
                      .format(provider, json.dumps(result.get('error'), indent=4)))

    def get_invites_statuses(self, provider):
        """Возвращает статусы инвайнов для провайдера"""
        request = self.get_request({'uid': self.uid})
        oid = core.async_invite_contacts(request).get('oid')

        request.set_args({'uid': self.uid, 'oid': oid})
        while not core.status(request).get('status') == OperationState.COMPLETED:
            time.sleep(0.5)

        return [invite for invite in core.status(request)['stages']
                if invite['service'] == provider]


@pytest.mark.skipif(True, reason=u'Отказались от этой функциональности')
class PhotoImportTestCase(CommonSocialMethods):
    def teardown_method(self, method):
        self.remove_created_users()
        super(PhotoImportTestCase, self).teardown_method(method)

    def test_granted_user_rights_for_facebook_import(self):
        result = social.user_rights(self.uid, 'photos_import')['facebook']
        self.assertTrue(result['auth'])
        self.assertFalse(result['link'])

    def test_socialproxy_get_albums(self):
        proxy = SocialProxy()
        response = proxy.get_albums(self.uid, FACEBOOK, locale.RU_RU)
        self.assertEqual(response.get('state'), SUCCESS)
        albums = response.get('result')
        self.assertTrue(len(albums) > 0)
        self.assertTrue(len(albums[0].get('aid')) > 0)
        self.assertTrue(len(albums[0].get('title')) > 0)
        self.assertEqual(albums[0].get('visibility'), 'public')

    def test_core_get_albums(self):
        request = self.get_request({'uid': self.uid, 'groups': 0, 'provider': FACEBOOK})
        response = core.social_get_albums(request)
        self.assertEqual(response.get('state'), SUCCESS)

    def test_social_list_user_photos(self):
        photos = social.user_photos(self.uid, FACEBOOK)
        assert_that(photos, is_not(empty()))

    def test_socialproxy_create_album(self):
        """
        только для ручного тестирования
        пока не умеем убирать альбом
        """

        """
        proxy = SocialProxy()
        response = proxy.create_album(self.test_uid, 'facebook', 'new2', 'friends')
        self.assertEqual(response.get('state'), 'success')
        aid = response.get('result').get('aid')
        self.assertTrue(int(aid) > 0)
        """

    def test_socialproxy_list_personal_photos(self):
        """
        Ломается, потому что нужен токен одноклассников
        """

        """
        result = proxy.album_photos(self.test_uid, 'odnoklassniki', 'personal')
        self.assertTrue(len(result) > 0)
        """

    def test_socialproxy_personal_photos_unsupported_provider(self):
        self.assertRaises(errors.SocialProxyInvalidParameters,
                          lambda: proxy.album_photos(self.uid, FACEBOOK, 'personal'))

    def test_socialproxy_album_photos_paginations(self):
        """Проверяет, что при запросе фотографий будут возвращены все фотографии.

        Фотографии получаются по странично (по паре фотографий за запрос).
        Метод `album_photos` должно возвращать весь список фотографий из альбома.
        """
        album_id = '419256684887099'
        total_photos = 7

        photos = proxy.album_photos(self.uid, FACEBOOK, album_id, True)
        assert_that(photos, has_length(total_photos))

    def test_socialproxy_user_photos_paginations(self):
        total_photos = 4
        photos = proxy.user_photos(self.uid, FACEBOOK)

        assert_that(photos, has_length(total_photos))

    def test_socialproxy_instagram_photos(self):
        """Тестируем функцию получения фото пользователя.

        Проверяем, что:
          * для тестового пользователя, у которого есть фоты, будет возвращен
          список, содержащий как минимум 1 элемент;
          * формат ответа от функции соответствует ее документации

        """
        expected_schema = {
            "type": "array",
            "minItems": 1,
            "items": {
                "type": "object",
                "properties": {
                    "pid": {"type": "string"},
                    "location": {
                        "type": "object",
                        "properties": {
                            "latitude": {"type": "number"},
                            "longitude": {"type": "number"}
                        }
                    },
                    "created": {"type": "string"},
                    "order_no": {"type": "integer"},
                    "url": {"type": "string"}
                },
                "required": ["pid", "created", "url"]
            }
        }
        actual_result = proxy.album_photos(self.uid, INSTAGRAM)

        self.assertHasValidSchema(actual_result, expected_schema)
