# -*- coding: utf-8 -*-

import os
import pytest
import brotli

from report.functional.web.base import BaseFuncTest
from report.const import *

@pytest.mark.skipif(not os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestBrotli(BaseFuncTest):
    @pytest.fixture(scope='function')
    def sdch_dict(self, query):
        if not hasattr(self, '_sdch_dict'):
            query.set_flags({ 'enable_sdch': 1, 'output_in_app_host': 0, 'sdch_from_sandbox': 1 })
            query.headers.set_custom_headers({"Accept-Encoding": 'sdch', 'Host': 'yandex.ru'})
            query.set_user_agent(USER_AGENT_TOUCH)
            query.set_url(SEARCH_TOUCH)
            resp = self.json_request(query)

            d = resp.headers['Get-Dictionary'][0].replace('/search/sdch/', '')
            d = d.replace('.dict', '')
            self._sdch_dict = d
        return self._sdch_dict

    def test_brotli(self, query):
        query.set_flag('enable_brotli')
        query.headers.set_custom_headers({"Accept-Encoding": 'gzip, deflate, br', 'Host': 'yandex.ru'})
        query.set_user_agent(USER_AGENT_TOUCH)
        query.set_url(SEARCH_TOUCH)
        resp = self.json_request(query)

        assert resp.headers['Content-Encoding'] == ['br']
        brotli.decompress(resp.content)

    def test_brotli_unsupported(self, query):
        query.set_flag('enable_brotli')
        query.headers.set_custom_headers({"Accept-Encoding": 'gzip, deflate', 'Host': 'yandex.ru'})
        query.set_user_agent(USER_AGENT_TOUCH)
        query.set_url(SEARCH_TOUCH)
        resp = self.json_request(query)

        assert 'br' not in resp.headers['Content-Encoding']

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    def test_sdch_over_br(self, query, sdch_dict):
        query.set_flags({ 'enable_sdch': 1, 'enable_brotli': 1 })
        query.set_user_agent(USER_AGENT_TOUCH)
        query.set_url(SEARCH_TOUCH)
        query.headers.set_custom_headers({"Accept-Encoding": 'sdch, br, gzip, deflate', "Avail-Dictionary": sdch_dict, 'Host': 'yandex.ru'})
        resp = self.json_request(query)

        assert resp.headers['Content-Encoding'] == ['sdch, br']
