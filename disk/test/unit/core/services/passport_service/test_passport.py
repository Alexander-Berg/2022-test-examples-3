# -*- coding: utf-8 -*-
import json

from jinja2 import Template
from hamcrest import assert_that, calling, raises
import mock
from nose_parameterized import parameterized

from test.helpers.stubs.services import PassportResponseMock
from test.unit.base import NoDBTestCase
from test.fixtures.users import default_user

from mpfs.common.errors import PassportBadResult
from mpfs.engine.process import setup_host_details


class Response(object):
    def __init__(self, content):
        self.content = content


class UserInfoTestCase(NoDBTestCase):
    """Проверяет метод Passport.userinfo()."""

    def setup_method(self, method):
        from mpfs.core.services.passport_service import passport
        self.passport_service = passport
        self.passport_service.reset()
        # Паспорту нужно передавать IP хоста, поэтому подготавлием эти данные
        setup_host_details()

    def test_error_response(self):
        """Тестируем обработку ошибки обращения к паспорту

        https://doc.yandex-team.ru/blackbox/concepts/blackboxErrors.xml#blackboxErrors
        """
        bad_response = {'exception': {'value': 'OK',
                                      'id': 0},
                        'error': 'BlackBox error: Missing userip argument'}
        with mock.patch(
            'mpfs.core.services.passport_service.Passport._process_response',
            return_value=bad_response
        ):
            assert_that(calling(self.passport_service.userinfo).with_args(uid=default_user.uid),
                        raises(PassportBadResult))

    def test_error_response_in_list(self):
        """Тестируем обрабатку ответа от паспорта если у него произошла ошибка при выдачи userinfo"""
        bad_response = {'users': [{'exception': {'value': 'DB_EXCEPTION',
                                                 'id': 10},
                                   'error': 'Fatal BlackBox error: dbpool exception in sezam dbfields fetch',
                                   'id': '11806301'}]}
        with mock.patch(
            'mpfs.core.services.passport_service.Passport._process_response',
            return_value=bad_response
        ):
            assert_that(calling(self.passport_service.userinfo).with_args(uid=default_user.uid),
                        raises(PassportBadResult))

    @parameterized.expand([('missing', {'profiles': [{'key': 10}]}),
                           ('empty', {'users': []}),
                           ('not_a_list', {'users': {}})])
    def test_wrong_response_format(self, case_name, bad_response):
        """Testing response with wrong format processing"""
        with mock.patch(
            'mpfs.core.services.passport_service.Passport._process_response',
            return_value=bad_response
        ):
            assert_that(calling(self.passport_service.userinfo).with_args(uid=default_user.uid),
                        raises(PassportBadResult))

    def test_userinfo_called_with_12_alias(self):
        """Проверить что `userinfo` всегда запрашивает алиас #12.

        https://wiki.yandex-team.ru/passport/dbmoving/#tipyaliasov
        https://wiki.yandex-team.ru/disk/mpfs/meps/mep-029/#zagruzkaattachejjmailishakkauntami
        """
        user_uid = '123456789'  # random
        self.passport_service.reset()
        response_file = 'fixtures/passport/responses/passport_userinfo_mailish_account_response.json'

        def render_response_func(template):
            return template.render(uid=user_uid)

        with PassportResponseMock(response_file, render_response_func) as passport_mock:
            self.passport_service.userinfo(uid=user_uid)
            assert passport_mock.request.called
            _, kwargs = passport_mock.request.call_args
            assert 'aliases' in kwargs['params']
            assert '12' in kwargs['params']['aliases'].split(',')

    def test_userinfo_contains_is_mailish_key_for_non_mailish_user(self):
        """Проверить, что для обычного пользователя в данных отданных `userinfo` присутствуют необходимые ключи."""
        user_uid = '123456789'  # random
        self.passport_service.reset()
        response_file = 'fixtures/passport/responses/passport_userinfo_ordinary_account_response.json'

        def render_response_func(template):
            return template.render(uid=user_uid, id=user_uid)

        with PassportResponseMock(response_file, render_response_func):
            data = self.passport_service.userinfo(uid=user_uid)
            assert 'is_mailish' in data
            assert data['is_mailish'] is False

    def test_userinfo_for_mailish_user(self):
        """Проверить, что для `mailish` пользователя в данных отданных `userinfo` присутствуют необходимые ключи."""
        mailish_user_uid = '520161280'
        response_file = 'fixtures/passport/responses/passport_userinfo_mailish_account_response.json'

        def render_response_func(template):
            return template.render(
                uid=mailish_user_uid,
                login='yatestvs@gmail.com',
                display_name='yatestvs@gmail.com',
                public_name='yatestvs@gmail.com',
            )

        with PassportResponseMock(response_file, render_response_func):
            data = self.passport_service.userinfo(uid=mailish_user_uid)
            assert 'uid' in data
            assert data['uid'] == mailish_user_uid

            assert 'is_mailish' in data
            assert data['is_mailish'] is True

            assert 'login' in data
            assert data['login'] == 'yatestvs@gmail.com'

            assert 'display_name' in data
            assert data['display_name'] == 'yatestvs@gmail.com'

            assert 'public_name' in data
            assert data['public_name'] == 'yatestvs@gmail.com'

    def test_userinfo_has_mail360_value(self):
        """Тестируем что значение аттрибута, возвращаемое паспортом, корректно превращается в свойство userinfo"""
        def render_response_func(template):
            response = json.loads(template.render())
            user = response['users'][0]
            user['attributes']['197'] = '1'
            return json.dumps(response)

        with PassportResponseMock(render_response_func=render_response_func):
            user_info = self.passport_service.userinfo(uid=default_user.uid)

        assert 'has_mail360' in user_info
        assert user_info['has_mail360'] is True


