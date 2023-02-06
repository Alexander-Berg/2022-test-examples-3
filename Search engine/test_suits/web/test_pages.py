# -*- coding: utf-8 -*-

import pytest

from util.const import HNDL, CTXS
from util.tsoy import TSoY


class TestPages():

    @pytest.mark.parametrize(('page', 'status'), [
        (0,   200),
        (1,   200),
        (2,   200),
        (3,   200),
        (10,  200),
        (15,  200),
        (23,  200),
        (24,  200),
        pytest.param(25,  404, marks=pytest.mark.soy_http('RUNTIMETESTS-96')),
        pytest.param(50,  404, marks=pytest.mark.soy_http('RUNTIMETESTS-96')),
        pytest.param(100, 404, marks=pytest.mark.soy_http('RUNTIMETESTS-96'))
    ])
    @TSoY.yield_test
    def test_pages(self, page, status, query):
        expect_docs = True if status == 200 else False
        if expect_docs:
            query.SetDumpFilter(resp=[CTXS.BLENDER_TEMPLATE_DATA])
        query.SetPath(HNDL.SEARCH)
        query.SetParams({
            'text': 'Keira Knightley',
            'p': str(page),
        })
        query.SetRequireStatus(status)

        resp = yield query

        if expect_docs:
            tmpl = resp.GetCtxs()['template_data']
            assert len(tmpl) != 0 and 'data' in tmpl[0]

            js = tmpl[0]['data']
            assert js['searchdata']['docs']
            assert len(js['searchdata']['docs']) > 0
