# -*- coding: utf-8 -*-
import json

from mock import mock
from nose_parameterized import parameterized

from test.base_suit import (
    UserTestCaseMixin,
)
from test.fixtures.users import default_user, user_6, user_7
from test.helpers.stubs.resources import users_info
from test.helpers.stubs.services import NewUAASStub, UAASStub
from test.parallelly.api.disk.base import DiskApiTestCase
from mpfs.common.static import tags


class ExperimentsTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'disk/experiments'

    EXCLUDED_TEST_ID = '217422'

    @classmethod
    def teardown_class(cls):
        users_info.reset_users_info()
        super(ExperimentsTestCase, cls).teardown_class()

    @parameterized.expand([
        ('exclude',
         [{'HANDLER': 'DISK', 'CONTEXT': {'DISK': {'flags': ['disk_should_be_excluded'], 'testid': [EXCLUDED_TEST_ID]}}},
          {'HANDLER': 'DISK', 'CONTEXT': {'DISK': {'flags': ['disk_should_be_untouched'], 'testid': ['123456']}}}],
         [['disk_should_be_untouched'], ['disk_forbidden_video_unlim']],
         {default_user.uid}),
        ('keep_all',
         [{'HANDLER': 'DISK', 'CONTEXT': {'DISK': {'flags': ['disk_should_be_untouched'], 'testid': ['542065']}}},
          {'HANDLER': 'DISK', 'CONTEXT': {'DISK': {'flags': ['disk_should_be_untouched_too'], 'testid': ['123456']}}}],
         [['disk_should_be_untouched'], ['disk_should_be_untouched_too'], ['disk_forbidden_video_unlim']],
         {default_user.uid}),
        ('keep_when_not_excluded_for_uid',
         [{'HANDLER': 'DISK', 'CONTEXT': {'DISK': {'flags': ['disk_should_be_untouched'], 'testid': [EXCLUDED_TEST_ID]}}},
          {'HANDLER': 'DISK', 'CONTEXT': {'DISK': {'flags': ['disk_should_be_untouched_too'], 'testid': ['123456']}}}],
         [['disk_should_be_untouched'], ['disk_should_be_untouched_too'], ['disk_forbidden_video_unlim']],
         {'7788996'}),
    ])
    def test_positive_cases(self, case_name, exps_from_uaas, expected_flags, exclude_for_uids):
        with self.specified_client(scopes=['yadisk:all'], uid=self.uid), \
                 NewUAASStub(experiments=exps_from_uaas), \
                 mock.patch('mpfs.platform.v1.disk.handlers.PLATFORM_EXCLUDE_EXPS', [{'testids': [self.EXCLUDED_TEST_ID],
                                                                                      'for_uids': exclude_for_uids}]):
            resp = self.client.request(self.method, self.url)

        result = json.loads(resp.content)

        assert 'uas_exp_flags' in result
        actual_flags = [item['CONTEXT']['DISK']['flags'] for item in result['uas_exp_flags']]
        assert actual_flags == expected_flags

    @parameterized.expand([
        ([], False),
        (['TELEMOST'], False),
        (['DISK'], True),
        (['TELEMOST', 'MAIL'], False),
        (['DISK', 'TELEMOST', 'MAIL'], True),
        (['TELEMOST', 'DISK', 'MAIL'], True),
        (['TELEMOST', 'MAIL', 'DISK'], True),
    ])
    def test_multi_exps(self, uaas_handlers, exp_exists):
        uaas_tmpls = {
            'DISK': {'HANDLER': 'DISK', 'CONTEXT': {'DISK': {'flags': ['diskflag'], 'testid': ['123']}}},
            'TELEMOST': {'HANDLER': 'TELEMOST', 'CONTEXT': {'TELEMOST': {'flags': ['telemostflag'], 'testid': ['567']}}},
            'MAIL': {'HANDLER': 'MAIL', 'CONTEXT': {'MAIL': {'flags': ['mailflag'], 'testid': ['321']}}},
        }

        with self.specified_client(scopes=['yadisk:all'], uid=user_6.uid), \
             NewUAASStub(experiments=[uaas_tmpls[x] for x in uaas_handlers]):

            resp = self.client.request(self.method, self.url)
            assert resp.status_code == 200

            expected_exps = [(u'diskflag', u'123'), (u'disk_forbidden_video_unlim', u'190458')] if exp_exists else\
                [(u'disk_forbidden_video_unlim', u'190458')]
            expected_response = {u'uas_exp_flags': [
                {u'HANDLER': u'DISK', u'CONTEXT': {u'DISK': {u'flags': [flag], u'testid': [testid]}}}
                for flag, testid in expected_exps
            ]}

            assert json.loads(resp.content) == expected_response

    @parameterized.expand(['has_staff', 'has_telemost'])
    def test_telemost(self, attribute):
        self.create_user(user_6.uid, noemail=True)
        kwargs = {attribute: True}
        users_info.update_info_by_uid(user_6.uid, **kwargs)
        expected_flags = [['disk_uaas'], ['disk_forbidden_video_unlim'], ['disk_telemost']]

        with self.specified_client(scopes=['yadisk:all'], uid=user_6.uid), \
             NewUAASStub(experiments=[{'HANDLER': 'DISK', 'CONTEXT': {'DISK': {'flags': ['disk_uaas'], 'testid': ['123456']}}}]):
            resp = self.client.request(self.method, self.url)

        result = json.loads(resp.content)

        assert 'uas_exp_flags' in result
        actual_flags = [item['CONTEXT']['DISK']['flags'] for item in result['uas_exp_flags']]
        assert actual_flags == expected_flags

    def test_telemost_enable_by_lang_and_testid(self):
        self.create_user(user_6.uid, noemail=True)
        users_info.update_info_by_uid(user_6.uid, language='ru')

        with self.specified_client(scopes=['yadisk:all'], uid=user_6.uid), \
             NewUAASStub(experiments=[{'HANDLER': 'DISK', 'CONTEXT': {'DISK': {'flags': ['disk_telemost_launch'], 'testid': ['250846']}}}]):
            resp = self.client.request(self.method, self.url)

        result = json.loads(resp.content)
        assert 'uas_exp_flags' in result
        actual_flags = [item['CONTEXT']['DISK']['flags'] for item in result['uas_exp_flags']]
        assert ['disk_telemost'] in actual_flags

    def test_telemost_disable_by_lang(self):
        self.create_user(user_6.uid, noemail=True)
        users_info.update_info_by_uid(user_6.uid, language='en')

        with self.specified_client(scopes=['yadisk:all'], uid=user_6.uid), \
             NewUAASStub(experiments=[{'HANDLER': 'DISK', 'CONTEXT': {'DISK': {'flags': ['disk_telemost_launch'], 'testid': ['250846']}}}]):
            resp = self.client.request(self.method, self.url)

        result = json.loads(resp.content)
        assert 'uas_exp_flags' in result
        actual_flags = [item['CONTEXT']['DISK']['flags'] for item in result['uas_exp_flags']]
        assert ['disk_telemost'] not in actual_flags

    def test_telemost_disable_by_flag(self):
        self.create_user(user_6.uid, noemail=True)
        users_info.update_info_by_uid(user_6.uid, language='ru')

        with self.specified_client(scopes=['yadisk:all'], uid=user_6.uid), \
             NewUAASStub(experiments=[]):
            resp = self.client.request(self.method, self.url)

        result = json.loads(resp.content)
        assert 'uas_exp_flags' in result
        actual_flags = [item['CONTEXT']['DISK']['flags'] for item in result['uas_exp_flags']]
        assert ['disk_telemost'] not in actual_flags

    @parameterized.expand([
        (user_7.uid, True),
        (default_user.uid, False)
    ])
    def test_disk_android_videounlim_alert_flag(self, uid, is_flag_in_resp):
        # для этого теста в conftest подкладывается специальный файл
        # в корень директории с конфигам `disk_android_videounlim_alert.uids`
        # он содержит один uid - user_7.uid
        self.create_user(uid, noemail=True)
        with self.specified_client(scopes=['yadisk:all'], uid=uid), \
                 NewUAASStub(experiments=[]):
            resp = self.client.request(self.method, self.url)
        result = json.loads(resp.content)
        assert 'uas_exp_flags' in result
        actual_flags = [item['CONTEXT']['DISK']['flags'] for item in result['uas_exp_flags']]
        print actual_flags
        assert (['disk_android_videounlim_alert'] in actual_flags) is is_flag_in_resp


class TelemostExperimentsTestCaseV1(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'telemost/experiments'

    @parameterized.expand([
        ([], False),
        (['TELEMOST'], True),
        (['DISK'], False),
        (['DISK', 'MAIL'], False),
        (['TELEMOST', 'DISK', 'MAIL'], True),
        (['DISK', 'TELEMOST', 'MAIL'], True),
        (['DISK', 'MAIL', 'TELEMOST'], True),
    ])
    def test_positive_case(self, uaas_handlers, exp_exists):
        uaas_tmpls = {
            'TELEMOST': '{"HANDLER": "TELEMOST", "CONTEXT": {"TELEMOST": {"flags": ["%s"], "testid": ["%s"]}}}',
            'DISK': '{"HANDLER": "DISK", "CONTEXT": {"DISK": {"flags": ["flagname1,flagname2"], "testid": ["123"]}}}',
            'MAIL': '{"HANDLER": "MAIL", "CONTEXT": {"MAIL": {"flags": ["flagname1,flagname2"], "testid": ["321"]}}}',
        }
        uaas_tmpl = '[%s]' % ','.join([uaas_tmpls[x] for x in uaas_handlers])
        with self.specified_client(scopes=['yadisk:all'], uid=user_6.uid), \
             UAASStub(exp_tmpl=uaas_tmpl) as uaas_stub:

            resp = self.client.request(self.method, self.url, headers={'User-AGENT': 'Yandex.Telemost {"os":"android 7.0.0", "id": "asdfghjkl", "device": "phone"}'})
            assert resp.status_code == 200
            assert uaas_stub.request.call_args[1]['relative_url'] == '/telemost/'
            if exp_exists:
                assert resp.content == u'{"uas_exp_flags":[{"HANDLER":"TELEMOST","CONTEXT":{"TELEMOST":{"flags":["flagname1"],"testid":["0"]}}},{"HANDLER":"TELEMOST","CONTEXT":{"TELEMOST":{"flags":["flagname2"],"testid":["1"]}}}]}'
            else:
                assert resp.content == '{"uas_exp_flags":[]}'


class TelemostExperimentsTestCaseV2(TelemostExperimentsTestCaseV1):
    api_version = 'v2'
