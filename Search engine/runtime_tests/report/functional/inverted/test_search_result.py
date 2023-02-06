# -*- coding: utf-8 -*-

import os
import pytest

from report.functional.web.base import BaseFuncTest
from report.const import *


@pytest.mark.skipif(not os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestTemplates(BaseFuncTest):
    def test_search_result_misspell(self, query):
        query.set_params({'text': 'rjnbrb', "dump": "eventlog", "dump_source_response": "MISSPELL"})
        query.set_internal()

        resp = self.request(query)
        assert resp

        source_resp = self.get_context_from_apphost_eventlog(resp.content, "response", "MISSPELL");
        assert source_resp

        ms = self.get_apphost_type(source_resp, "misspell")
        assert ms

        ms = ms[0]
        assert ms["raw_text"]["fixed"]==u"котики"
