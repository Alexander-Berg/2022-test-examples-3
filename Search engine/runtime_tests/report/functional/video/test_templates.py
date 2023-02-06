# -*- coding: utf-8 -*-

import pytest
import json

from report.functional.web.base import BaseFuncTest
from report.const import *

class TestVideo(BaseFuncTest):
    @pytest.mark.parametrize("query_type, url_path, exp_flags, result", [
        (DESKTOP, '/video/search' ,      {},                                '/video3:desktop'),
        (PAD,     '/video/pad/search',   {},                                '/video3:pad'),
        (TOUCH,   '/video/touch/search', {},                                '/video3:phone'),
        (GRANNY,  '/video/search',       {},                                '/video3_granny:desktop'),
        (GRANNY,  '/video/search',       {'video_disable_granny': 1},       '/video3:desktop'),
        (DESKTOP, '/video/preview',      {'video_enable_preview_page': 1},  '/video3:desktop_viewer'),
    ])
    def test_renderer_templates(self, query, query_type, url_path, exp_flags, result):
        query.set_url(url_path)
        query.set_user_agent(USERAGENT_BY_TYPE[query_type])
        query.set_params({'text':TEXT, 'searchid': '2053249', 'web': '0'})
        query.set_flags({'json_template_external': 'by_name', 'json_template': 1})
        query.add_flags(exp_flags)

        resp = self.request(query, sources='TEMPLATES')

        assert resp.source.requests[0].path == result

