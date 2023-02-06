# -*- coding: utf-8 -*-

import pytest
from util.tsoy import TSoY
from util.const import PLATFORM, CTXS
from urllib.parse import urlparse


class TestSitesearch():
    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.ticket('SITESEARCH-3586')
    @pytest.mark.parametrize('params', [
        ({'searchid': 3971480, 'text': ''})
    ])
    @TSoY.yield_test
    def test_empty_query_response(self, query, params):
        query.SetDumpFilter(resp=[CTXS.SITE_SEARCH_TEMPLATE_DATA])
        query.SetParams(params)
        query.SetQueryType(PLATFORM.SITESEARCH)
        query.SetRequireStatus(200)

        resp = yield query

        tmpl = resp.GetCtxs()['template_data']
        assert len(tmpl) != 0 and 'data' in tmpl[0]

        js = tmpl[0]['data']
        assert js['searchdata']['err_code'] == 2

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.parametrize('params, host', [
        ({'searchid': 3971480, 'text': 'аниме'}, 'torm-egan.ru'),
        ({'searchid': 3971490, 'text': 'порно'}, 'ru.wikipedia.org'),
        ({'searchid': 3971491, 'text': 'кошка'}, 'ru.wikipedia.org')
    ])
    @TSoY.yield_test
    def test_non_empty_response_and_url_restriction(self, query, params, host):
        query.SetDumpFilter(resp=[CTXS.SITE_SEARCH_TEMPLATE_DATA])
        query.SetParams(params)
        query.SetQueryType(PLATFORM.SITESEARCH)
        query.SetRequireStatus(200)

        resp = yield query

        tmpl = resp.GetCtxs()['template_data']
        assert len(tmpl) != 0 and 'data' in tmpl[0]

        js = tmpl[0]['data']
        searchdata = js['searchdata']

        assert searchdata
        assert searchdata.get('err_code') is None
        assert len(searchdata['docs']) > 0
        for doc in searchdata['docs']:
            url = urlparse(doc['url'])
            assert url.netloc == host

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.parametrize('params', [
        ({'searchid': 3971491, 'text': 'порно'})
    ])
    @TSoY.yield_test
    def test_family_filter(self, query, params):
        query.SetDumpFilter(resp=[CTXS.SITE_SEARCH_TEMPLATE_DATA])
        query.SetParams(params)
        query.SetQueryType(PLATFORM.SITESEARCH)
        query.SetRequireStatus(200)

        resp = yield query

        tmpl = resp.GetCtxs()['template_data']
        assert len(tmpl) != 0 and 'data' in tmpl[0]

        js = tmpl[0]['data']
        searchdata = js['searchdata']
        assert searchdata
        assert searchdata.get('err_code') == 15
        assert len(searchdata['docs']) == 0

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.ticket('SERP-46282')
    @pytest.mark.parametrize(('code', 'params'), (
        (15, {'searchid': '2045692', 'text': 'персонаж', 'web': '0', 'lr': '213', 'constraintid': '3',
                                'within': '777', 'from_day': '19', 'from_month': '9', 'from_year': '2016', 'to_day': '19',
                                'to_month': '9', 'to_year': '2016'}),
        (2,  {'searchid': '2045692', 'text': '', 'web': '0', 'lr': '213', 'constraintid': '3',
                                'within': '777'}),
    ))
    @TSoY.yield_test
    def test_msg_no_results(self, query, code, params):
        query.SetDumpFilter(resp=[CTXS.SITE_SEARCH_TEMPLATE_DATA])
        query.SetParams(params)
        query.SetQueryType(PLATFORM.SITESEARCH)
        query.SetRequireStatus(200)

        resp = yield query

        tmpl = resp.GetCtxs()['template_data']
        assert len(tmpl) != 0 and 'data' in tmpl[0]

        js = tmpl[0]['data']
        assert js['searchdata']['err_code'] == code
