# -*- coding: utf-8 -*-

import os
import json
import urlparse

import os
import pytest

from report.functional.web.base import BaseFuncTest
from report.const import *


@pytest.mark.skipif(not os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestLogin(BaseFuncTest):
    def test_noauthorization(self, query):
        resp = self.json_test(query)
        assert not resp.data.get('blackbox')

    def test_authorization_mailbox_info(self, query):
        query.set_auth2()
        resp = self.json_test(query)

    @pytest.mark.unstable # TODO make proper logging in
    def test_uid_to_yabs_auth(self, query):
        query.set_auth()
        yabs_setup = self.json_dump_context(query, ['yabs_setup'])
        assert len(yabs_setup) > 0
        for ctx in yabs_setup:
            assert ctx['uid'] == ['132594000']
        #resp = self.request(query)
        #assert resp.source.clients['YABS']['uid'][0] == '132594000'
        #uid = self.base_uid_to_yabs(query)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_uid_to_yabs_noauth(self, query):
        query.set_noauth()
        yabs_setup = self.json_dump_context(query, ['yabs_setup'])
        assert len(yabs_setup) > 0
        for ctx in yabs_setup:
            assert ctx['uid'] == ['0']

    @pytest.mark.ticket('SERP-45268')
    @pytest.mark.skip('')
    def test_login_ok_using_blackbox2(self, query):
        query.set_auth2()
        resp = self.json_request(query)

        passp = resp.data['reqdata']['passport']
        assert passp['logged_in'] == 1
        assert passp['id'] == query.get_passport()['id']
        assert passp['login'] == query.get_passport()['login']
        assert passp['cookieL']['login'] == query.get_passport()['login']

    @pytest.mark.unstable
    @pytest.mark.ticket('SERP-45268')
    def test_login_ok_using_blackbox(self, query):
        query.set_auth()
        resp = self.json_request(query)

        passp = resp.data['reqdata']['passport']
        assert passp['logged_in'] == 1
        assert passp['id'] == '1130000012898159'
        assert passp['login'] == u'robbitter-5151382063@закодированный.домен'
        assert passp['cookieL']['login'] == u'robbitter-5151382063@закодированный.домен'

