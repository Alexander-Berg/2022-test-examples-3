# -*- coding: utf-8 -*-

import pytest
# from base64 import b64encode
from util.tsoy import TSoY
from util.const import USER_AGENT, HNDL


class TestBrotli():
    @pytest.mark.ticket('')
    @TSoY.yield_test
    def test_brotli(self, query):
        query.SetFlags({'enable_brotli': 1})
        query.SetHeaders({
            "Accept-Encoding": 'gzip, deflate, br'
        })
        query.SetUserAgent(USER_AGENT.TOUCH)
        query.SetPath(HNDL.SEARCH_TOUCH)
        query.SetRequireStatus(200)

        resp = yield query

        assert 'br' in resp.headers.get('Content-Encoding')
        # brotli.decompress(resp.content)

    @TSoY.yield_test
    def test_brotli_unsupported(self, query):
        query.SetFlags({'enable_brotli': 1})
        query.SetHeaders({
            "Accept-Encoding": 'gzip, deflate'
        })
        query.SetUserAgent(USER_AGENT.TOUCH)
        query.SetPath(HNDL.SEARCH_TOUCH)
        query.SetRequireStatus(200)

        resp = yield query

        assert 'br' not in resp.headers.get('Content-Encoding')
