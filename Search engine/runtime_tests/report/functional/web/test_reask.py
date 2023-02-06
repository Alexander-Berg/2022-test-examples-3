# -*- coding: utf-8 -*-

import os
import pytest
import json
import urllib
import re

from report.functional.web.base import BaseFuncTest
from report.const import *


@pytest.mark.skipif(os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestReask(BaseFuncTest):
    # SERP-57212
    @pytest.mark.skip(reason='Docs are now comming from VIDEO for the original request')
    @pytest.mark.parametrize( ("flag"), [
        {'skip_reask': 1},
        {'skip_reask': 0}
    ])
    def test_skip_reask(self, query, flag):
        query.set_flags(flag)
        query.set_params({'text': '"пушкин онегин путин достоевский лермонтов долллар"'})
        resp = self.json_request(query, require_status=200)
        size = len(resp.data['searchdata']['docs'])
        assert (not bool(size)) == flag['skip_reask']
