# -*- coding: utf-8 -*-

import zlib
import json
from copy import deepcopy

import os
import pytest

from report.const import *
from report.proto import meta_pb2
from runtime_tests.report.base import Query

from report.functional.web.base import BaseFuncTest

@pytest.mark.skipif(not os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestBanner(BaseFuncTest):
    @classmethod
    @pytest.fixture(scope='function')
    def upper_resp(self):
        if not hasattr(self, '_upper_resp'):
            self._upper_resp = self.get_hamster_upper('пластиковые окна')
        return deepcopy(self._upper_resp)

    def init_default_request(self, query):
        query.set_host(RU)
        query.set_params({'text': 'пластиковые окна', 'lr': '213', 'noredirect': '1'})
        resp = self.json_request(query)
        assert resp.data['searchdata']['docs']
        assert len(resp.data['searchdata']['docs']) > 0

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_banner_serp(self, query):
        query.set_url('/search/ads')
        self.init_default_request(query)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_search_direct_serp(self, query):
        query.set_url('/search/direct')
        self.init_default_request(query)

    def init_text_check_request(self, query):
        APP_HOST_SOURCE = "search.app_host.sources.(_.name eq 'APP_HOST')"
        query.set_host(RU)
        query.set_params({'text': 'jryf', 'lr': '213', 'json_dump': APP_HOST_SOURCE})
        resp = self.json_request(query)
        assert resp.data[APP_HOST_SOURCE]
        for source in resp.data[APP_HOST_SOURCE]:
            if 'results' not in source:
                continue
            for result in source['results']:
                if '__oname' in result and result['__oname'] == 'YABS_PROXY_SETUP':
                    assert result['text'] == u'окна'

    @pytest.mark.skipif(True, reason="https://st.yandex-team.ru/WEBREPORT-893")
    def test_yabs_text(self, query):
        query.set_url('/search/ads')
        self.init_text_check_request(query)

    @pytest.mark.skipif(True, reason="https://st.yandex-team.ru/WEBREPORT-893")
    def test_search_direct_text(self, query):
        query.set_url('/search/direct')
        self.init_text_check_request(query)

