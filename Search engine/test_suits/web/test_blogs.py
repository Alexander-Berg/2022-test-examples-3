# -*- coding: utf-8 -*-

import base64
import pytest
from util.tsoy import TSoY
from util.const import HNDL, CTXS, USER_AGENT
from search.begemot.rules.src_setup.web.proto.result_pb2 import TSrcSetupAdjustWebProtoResult


class TestBlogs():
    @pytest.mark.ticket("SERP-40011")
    @pytest.mark.parametrize(("path", "params"), [
        (HNDL.BLOGS_SEARCH, {"text": "котики facebook"})
    ])
    @TSoY.yield_test
    def test_blogs_granny(self, query, path, params):
        query.SetUserAgent(USER_AGENT.GRANNY)
        query.SetPath(path)
        query.SetParams(params)
        query.SetDumpFilter(resp=[CTXS.INIT])

        resp = yield query
        dc = resp.GetCtxs()["device_config"][0]
        expect = {
            "device": "desktop",
            "device_modifier": "",
            "template_name": "web4:desktop",
            "template_path": "v8:web4:desktop",
            "type": "device_config",
            "v2": "v2"
        }
        assert expect == dc

    @pytest.mark.ticket("SEARCH-11506")
    @TSoY.yield_test
    def test_blogs_resp(self, query):
        query.SetPath(HNDL.BLOGS_SEARCH)
        query.SetDumpFilter(resp=[CTXS.WIZARDRY_WEB_SETUP])

        resp = yield query
        data = resp.GetCtxs()["web_setup"][0]["binary"]
        web_setup = TSrcSetupAdjustWebProtoResult()
        web_setup.ParseFromString(base64.b64decode(data))
        for text in web_setup.Result.SourceRequest.Request:
            got = text
            break
        assert got.find(" ppbhost:\"1\"::") != -1
