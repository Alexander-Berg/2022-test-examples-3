# -*- coding: utf-8 -*-

import pytest
from util.tsoy import TSoY
from util.const import CTXS, HNDL
from util.helpers import Auth


class TestCsp(Auth):

    @TSoY.yield_test
    def test_csp_login_encoding_ok(self, query, blackbox):
        self.SetBlackBoxAuth(query, blackbox)

        # Send Request
        resp = yield query
        csp_header = resp.headers['Content-Security-Policy']

        assert ';report-uri ' in csp_header or ' report-uri ' in csp_header
        for csp in csp_header.split(';'):
            if csp.startswith('report-uri ') or csp.startswith(' report-uri '):
                assert '&yandex_login={}'.format(query.GetPassport()['regname']) in csp


class TestCgiParams(Auth):

    @pytest.mark.ticket('WEBREPORT-818')
    @pytest.mark.parametrize(('cgi_p', 'page'), [
        (None, 0),
        (0, 0),
        (1, 1),
        (3, 3)
    ])
    @TSoY.yield_test
    def test_cgi_p_param(self, query, cgi_p, page):
        query.SetPath(HNDL.SEARCH_SEARCHAPI)
        query.SetDumpFilter(resp=[CTXS.REPORT])

        if cgi_p is not None:
            query.SetParams({'p': cgi_p})

        # Send Request
        resp = yield query
        ctxs = resp.GetCtxs()

        assert ctxs['report'][0]['params']['page'] == page

    @pytest.mark.ticket('WEBREPORT-818')
    @pytest.mark.parametrize(('numdoc', 'expect'), [
        (None, 10),
        (10, 10),
        (20, 20),
        (50, 50),
    ])
    @pytest.mark.parametrize(('cgi_p'), [
        (None),
        (0),
        (1),
        (4)  # Больше 250 документов базовые не ищут
    ])
    @TSoY.yield_test
    def test_cgi_numdoc_param(self, query, cgi_p, numdoc, expect):
        query.SetPath(HNDL.SEARCH)
        query.SetDumpFilter(resp=[CTXS.BLENDER])
        if cgi_p is not None:
            query.SetParams({
                'p': cgi_p,
            })
        if numdoc is not None:
            query.SetParams({
                'numdoc': numdoc,
            })
        else:
            numdoc = 10
        query.SetParams({
            'text': 'Путин',
            'lr': 213
        })

        # Send Request
        resp = yield query
        ctxs = resp.GetCtxs()

        assert len(ctxs['template_data'][0]['data']['searchdata']['docs']) // numdoc == expect // numdoc

    @pytest.mark.ticket('SERP-55203')
    @pytest.mark.parametrize(
        ('clid', 'new_clid'),
        [
            (1906723, 2192579),
            (1906724, 2192593),
        ]
    )
    @TSoY.yield_test
    def test_rewrite_clid(self, query, clid, new_clid):
        query.SetParams({
            "clid": clid
        })
        query.SetDumpFilter(resp=[CTXS.REQUEST, CTXS.REPORT])

        resp = yield query
        ctxs = resp.GetCtxs()

        assert len(ctxs['request']) > 0
        assert int(ctxs['request'][0]['params']['clid'][0]) == new_clid
        assert int(ctxs['report'][0]["clid"]) == clid

    @pytest.mark.ticket('SERP-55203')
    @pytest.mark.parametrize(
        ('clid', 'removed'),
        [
            (2302157, True),
            (230215, False),
        ]
    )
    @TSoY.yield_test
    def test_delete_mts_clid(self, query, clid, removed):
        query.SetParams({"clid": clid})
        query.SetFlags({"safari_remove_clids": 1})
        query.SetDumpFilter(resp=[CTXS.REQUEST, CTXS.REPORT])

        resp = yield query
        ctxs = resp.GetCtxs()

        assert len(ctxs['request']) > 0
        if removed:
            assert 'clid' not in ctxs['request'][0]['params']
        else:
            assert int(ctxs['request'][0]['params']['clid'][0]) == clid
        assert int(ctxs['report'][0]["clid"]) == clid
