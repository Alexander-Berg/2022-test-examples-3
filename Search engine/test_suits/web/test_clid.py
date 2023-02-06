# -*- coding: utf-8 -*-

import pytest
# from base64 import b64encode
from util.tsoy import TSoY
from util.const import CTXS


class TestClid():
    @pytest.mark.ticket('SERP-55203')
    @pytest.mark.parametrize(('clid', 'removed'), [
        (2302157, True),
        (230215, False),
    ])
    @TSoY.yield_test
    def test_delete_mts_clid(self, query, clid, removed):
        query.SetDumpFilter(resp=[CTXS.INIT])
        query.SetParams({
            "clid": clid,
            "safari_remove_clids": 1
        })
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert len(ctxs['request']) > 0
        if removed:
            assert 'clid' not in ctxs['request'][0]['params']
        else:
            assert int(ctxs['request'][0]['params']['clid'][0]) == clid
        assert int(ctxs['report'][0]["clid"]) == clid

    @pytest.mark.ticket('SERP-55203')
    @pytest.mark.parametrize(('clid', 'new_clid'), [
        (1906723, 2192579),
        (1906724, 2192593),
    ])
    @TSoY.yield_test
    def test_rewrite_clid(self, query, clid, new_clid):
        query.SetDumpFilter(resp=[CTXS.INIT])
        query.SetParams({"clid": clid})
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert len(ctxs['request']) > 0
        assert int(ctxs['request'][0]['params']['clid'][0]) == new_clid
        assert int(ctxs['report'][0]["clid"]) == clid
