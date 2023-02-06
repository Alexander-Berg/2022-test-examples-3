# -*- coding: utf-8 -*-

import pytest
from util.tsoy import TSoY
from util.const import TLD, CTXS, HNDL


class TestBanner():

    @pytest.mark.ticket('WEBREPORT-818')
    @pytest.mark.parametrize(('tld', 'lr'), [
        (TLD.RU, 213)
    ])
    @pytest.mark.parametrize(('path', 'text'), [
        (HNDL.SEARCH_ADS, 'пластиковые окна'),
        (HNDL.SEARCH_DIRECT, 'пластиковые окна'),
    ])
    @TSoY.yield_test
    def test_yabs_serp(self, query, path, tld, lr, text):
        query.SetPath(path)
        query.SetDomain(tld)
        query.SetParams({
            'text': text,
            'lr': lr,
            'noredirect': '1',
            'srcskip': 'SAAS_DJ_SETUP',
        })
        query.SetDumpFilter(resp=[CTXS.BLENDER_TEMPLATE_DATA])
        query.SetRequireStatus(200)

        resp = yield query

        tmpl = resp.GetCtxs()['template_data']
        assert len(tmpl) != 0 and 'data' in tmpl[0]

        template_data = tmpl[0]['data']
        assert template_data['searchdata']['docs']
        assert len(template_data['searchdata']['docs']) > 0

    @pytest.mark.ticket('WEBREPORT-818')
    @pytest.mark.parametrize(('tld', 'lr'), [
        (TLD.RU, 213)
    ])
    @pytest.mark.parametrize(('path', 'text'), [
        (HNDL.SEARCH_ADS, 'jryf'),
        (HNDL.SEARCH_DIRECT, 'jryf'),
    ])
    @TSoY.yield_test
    def test_yabs_text(self, query, path, tld, lr, text):
        query.SetDumpFilter(resp=[CTXS.YABS_PROXY])
        query.SetPath(path)
        query.SetDomain(tld)
        query.SetParams({
            'text': text,
            'lr': lr,
            'srcskip': 'SAAS_DJ_SETUP',
        })
        query.SetRequireStatus(200)

        resp = yield query

        result = resp.GetCtxs()['source_setup'][0]
        assert result['text'] == u'окна'
