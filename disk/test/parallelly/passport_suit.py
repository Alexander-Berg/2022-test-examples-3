# -*- coding: utf-8 -*-
import mock
from contextlib import contextmanager

from nose_parameterized import parameterized

from test.base import MpfsBaseTestCase
from test.helpers.stubs.resources.users_info import RAW_PASSPORT_USER_INFO
from test.fixtures.users import (
    old_time_registered_user,
    default_user,
    turkish_user,
    user_9,
    test_user,
    share_master,
    user_with_plus,
)
from test.fixtures.passport.hosted_domains import (HOSTED_DOMAINS_BY_ALIAS, HOSTED_DOMAINS_BY_MASTER,
                                                   HOSTED_DOMAINS_EMPTY)

import mpfs.core.services.passport_service as passport


@contextmanager
def mock_process_response():
    before = passport.Passport._process_response
    result = {}

    def my_process_response(self, *args, **kwargs):
        result['args'] = args
        result['kwargs'] = kwargs
        return ''

    try:
        passport.Passport._process_response = my_process_response
        yield result
    finally:
        passport.Passport._process_response = before


class TestPassport(MpfsBaseTestCase):
    def setup_method(self, method):
        super(TestPassport, self).setup_method(method)
        self.uid = default_user.uid
        self.passport = passport.Passport()
        self.passport.service_log = self.log

    def test_25_regdate(self):
        self.assertEqual(self.passport.userinfo(old_time_registered_user.uid).get('reg_date'), 1271364726)

    def test_30_country(self):
        self.assertEqual(self.passport.userinfo(turkish_user.uid).get('country'), 'tr')
        self.assertEqual(self.passport.userinfo(old_time_registered_user.uid).get('country'), 'ru')
        self.assertEqual(self.passport.userinfo(1).get('country'), None)

    def test_40_suid(self):
        with mock.patch('mpfs.core.services.common_service.SERVICES_TVM_2_0_ENABLED', False):
            res = self.passport.get_suid_dbid(test_user.uid)
            self.assertEqual(res['suid'], test_user.suid)

    def test_45_karma(self):
        res = self.passport.userinfo(old_time_registered_user.uid)
        self.assertEqual(res['karma'], 0)

    @parameterized.expand([
        'id',
        'login',
        'username',
        'display_name',
        'public_name',
        'avatar',
        'email',
        'decoded_email',
        'karma',
        'locations',
        'pdd',
        'pdd_domain',
        'reg_date',
        'firstname',
        'lastname',
        'sex',
        'birth_date',
        'country',
        'city',
        'language',
        'has_disk',
        'undeletable',
        'has_mobile_disk',
        'has_music',
        'has_desktop_disk',
        'personalization',
        'is_app_password_enabled',
        'is_2fa_enabled',
        'has_mail360'
    ])
    def test_userinfo_fields_set_with_personal(self, field):
        obj = self.passport.userinfo(uid=user_9.uid, all_emails=True, personal=True)
        assert field in obj

    def test_userinfo_fields_set_without_personal(self):
        """Протестировать что поля приватной информации возвращаются, но вместо соответствующих значений стоит None."""
        obj = self.passport.userinfo(uid=user_9.uid, all_emails=True, personal=False)
        for key in ('sex', 'birth_date', 'city'):
            assert key in obj
            assert obj[key] is None

    def test_userinfo_reg_date_is_timestamp(self):
        """Проверить что время регистрации отдается в формате таймстемпа"""
        obj = self.passport.userinfo(uid=user_9.uid)
        assert isinstance(obj['reg_date'], int) or obj['reg_date'] is None

    def test_karma_is_integer(self):
        obj = self.passport.userinfo(uid=user_9.uid)
        assert isinstance(obj['karma'], int)

    @parameterized.expand([
        ('with_plus', user_with_plus.uid, True),
        ('without_plus', default_user.uid, False),
    ])
    def test_has_plus(self, case_name, uid, expected_value):
        user_info = self.passport.userinfo(uid=uid)
        assert 'has_plus' in user_info
        assert user_info['has_plus'] == expected_value

    def test_userinfo_sex_value(self):
        """Проверить что пол имеет одно из допустимых значений"""
        obj = self.passport.userinfo(uid=user_9.uid, personal=True)
        assert obj['sex'] in (None, 'U', 'M', 'F')
        obj = self.passport.userinfo(uid=user_9.uid, personal=False)
        assert obj['sex'] is None

    def test_first_name_and_last_name(self):
        obj = self.passport.userinfo(uid=user_9.uid, personal=True)
        assert obj['firstname'] == user_9.firstname
        assert obj['lastname'] == user_9.lastname

    def test_public_name(self):
        obj = self.passport.userinfo(uid=user_9.uid, personal=True)
        assert obj['public_name'] == '%s %s.' % (user_9.firstname, user_9.lastname[0])

    def test_userinfo_with_wrong_uid(self):
        obj = self.passport.userinfo(uid=99999999999999999)
        assert 'uid' in obj
        assert obj['uid'] is None

    def test_portal_login(self):
        obj = self.passport.get_portal_login(uid=share_master.uid)
        assert share_master.login == obj

    @parameterized.expand([
        ('external_organization_ids', [110091, 110093]),
        ('pdd_org_id', '7401243')
    ])
    def test_parse_passport_attributes(self, field, expected_value):
        user_info = self.passport._process_userinfo_json_data(RAW_PASSPORT_USER_INFO)[0]
        assert field in user_info
        assert expected_value == user_info[field]


class GetAllDomainList(MpfsBaseTestCase):
    """Класс тестов метода Passport::get_all_domain_list.
    """
    DOMAIN_FOR_QUERY = ''  # подойдет что угодно, т.к. мы мокаем метод запроса

    @classmethod
    def setUpClass(cls):
        super(GetAllDomainList, cls).setUpClass()
        cls.passport = passport.Passport()

    @mock.patch.object(passport.Passport, 'get_hosted_domains',
                       return_value=HOSTED_DOMAINS_BY_ALIAS)
    def test_get_all_domains_by_alias_success(self, mocked_get_hosted_domains):
        domain_list = self.passport.get_all_domain_list(self.DOMAIN_FOR_QUERY)
        assert mocked_get_hosted_domains.called
        assert len(domain_list) == 2  # алиас + мастер

    @mock.patch.object(passport.Passport, 'get_hosted_domains',
                       return_value=HOSTED_DOMAINS_BY_MASTER)
    def test_get_hosted_domains_by_main_domain_success(self, mocked_get_hosted_domains):
        domain_list = self.passport.get_all_domain_list(self.DOMAIN_FOR_QUERY)
        assert mocked_get_hosted_domains.called
        assert len(domain_list) == 3  # алиасы + мастер

    @mock.patch.object(passport.Passport, 'get_hosted_domains',
                       return_value=HOSTED_DOMAINS_EMPTY)
    def test_get_hosted_domains_domain_not_found(self, mocked_get_hosted_domains):
        domain_list = self.passport.get_all_domain_list(self.DOMAIN_FOR_QUERY)
        assert mocked_get_hosted_domains.called
        assert not domain_list
