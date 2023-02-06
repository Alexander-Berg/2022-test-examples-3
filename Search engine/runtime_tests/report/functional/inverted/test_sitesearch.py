# -*- coding: utf-8 -*-

import os
import pytest
import logging
import json
import time
import urlparse

from report.functional.web.base import BaseFuncTest
from report import const


@pytest.mark.skipif(not os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestSitesearch(BaseFuncTest):
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SITESEARCH-3586')
    @pytest.mark.parametrize('params', [
        ({'searchid': 3971480, 'text': ''})
    ])
    def test_empty_query_response(self, query, params):
        query.set_params(params)
        query.set_query_type(const.SITESEARCH)
        resp = self.json_request(query)

        assert resp.data['searchdata']['err_code'] == 2

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize('params, host', [
        ({'searchid': 3971480, 'text': 'персонаж'}, 'torm-egan.ru'),
        ({'searchid': 3971490, 'text': 'порно'}, 'ru.wikipedia.org'),
        ({'searchid': 3971491, 'text': 'кошка'}, 'ru.wikipedia.org')
    ])
    def test_non_empty_response_and_url_restriction(self, query, params, host):
        query.set_params(params)
        query.set_query_type(const.SITESEARCH)
        resp = self.json_request(query)

        searchdata = resp.data.get('searchdata')
        assert searchdata
        assert searchdata.get('err_code') == None
        assert len(searchdata['docs']) > 0
        for doc in searchdata['docs']:
            url = urlparse.urlparse(doc['url'])
            assert url.netloc == host

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize('params', [
        ({'searchid': 3971491, 'text': 'порно'})
    ])
    def test_family_filter(self, query, params):
        query.set_params(params)
        query.set_query_type(const.SITESEARCH)
        resp = self.json_request(query)

        searchdata = resp.data.get('searchdata')
        assert searchdata
        assert searchdata.get('err_code') == 15
        assert len(searchdata['docs']) == 0
