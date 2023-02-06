# -*- coding: utf-8 -*-

import pytest
from util.tsoy import TSoY
from util.const import CTXS


class TestSearchProp():
    @pytest.mark.ticket('')
    @TSoY.yield_test
    def test_search_props_personal(self, query):
        query.SetDumpFilter(resp=[CTXS.BLENDER])
        query.SetInternal()
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert 'personalizationFirstTouched' in ctxs['template_data'][-1]['data']
