# -*- coding: utf-8 -*-

import pytest
import os
import json

from report.const import *
from report.base import BaseReportTest

class TestInternal(BaseReportTest):

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-37047', 'SERP-36068')
    def test_version_json(self, query):
        query.set_internal()
        query.set_url('/v')
        query.set_params({'json': 1})

        resp = self.request(query)
        resp.validate_schema('version.json')

    @pytest.mark.ticket('SERP-37047')
    def test_version_metainfo(self, query):
        query.set_internal()
        query.set_url('/v')

        resp = self.request(query)

        assert "metainfo" in resp.content
