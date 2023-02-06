# -*- coding: utf-8 -*-

import os
import pytest
import datetime

from report.functional.web.base import BaseFuncTest
from report.const import *


@pytest.mark.skipif(True, reason="Установлен 100% флаг disable_sdch_in_report")
@pytest.mark.skipif(os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestSDCH(BaseFuncTest):

    @classmethod
    @pytest.fixture(scope='class', autouse=True)
    def class_setup(cls, setup):
        cls.RETRIES = 1

    @pytest.fixture(scope='function')
    def sdch_dict_path(self, query):
        if not hasattr(self, '_sdch_dict'):
            query.set_flag('enable_sdch')
            query.headers.set_custom_headers({"Accept-Encoding": 'sdch', 'Host': 'yandex.ru'})
            query.set_user_agent(USER_AGENT_TOUCH)
            query.set_url(SEARCH_TOUCH)
            resp = self.json_request(query)

            d = resp.headers['Get-Dictionary'][0]
            self._sdch_dict = d
        return self._sdch_dict

    def test_sdch_dict(self, query, sdch_dict_path):
            query.headers.set_custom_headers({"Accept-Encoding": 'gzip', 'Host': 'yandex.ru'})
            query.set_user_agent(USER_AGENT_TOUCH)
            query.set_url(sdch_dict_path)
            resp = self.json_request(query)

            assert resp.headers['Content-Encoding'] == ['gzip']
            assert resp.headers['Cache-Control'] == ['max-age=2592000']
            assert resp.headers['Expires']
            assert resp.headers['Last-Modified']
            assert resp.headers['ETag']

            sec = int(resp.headers.get_one('Cache-Control').split('=', 1)[1])
            d = resp.headers.get_one('Date')
            format_str = '%a, %d %b %Y %H:%M:%S GMT'
            exp = datetime.datetime.strptime(d, format_str) + datetime.timedelta(seconds=sec)
            assert resp.headers.get_one('Expires') == exp.strftime(format_str)
