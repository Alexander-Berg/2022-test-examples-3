# -*- coding: utf-8 -*-

import pytest
from util.tsoy import TSoY
from util.const import CTXS


class TestMisspeller():
    @pytest.mark.ticket('')
    @TSoY.yield_test
    def test_misspell_compat(self, query):
        query.SetDumpFilter(resp=[CTXS.WIZARD])
        query.SetParams({
            'text': 'ьфвщттф'
        })
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert ctxs['wizard'][-1]['original_request'] == 'madonna'
