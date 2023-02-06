# -*- coding: utf-8 -*-

import pytest
from util.tsoy import TSoY
from util.const import CTXS


class TestSearchResult():
    @pytest.mark.ticket('')
    @TSoY.yield_test
    def test_search_result_misspell(self, query):
        query.SetDumpFilter(resp=[CTXS.MISSPELL])
        query.SetParams({'text': 'rjnbrb'})
        query.SetInternal()
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        ms = ctxs["misspell"][0]
        assert ms["raw_text"]["fixed"] == u"котики"
