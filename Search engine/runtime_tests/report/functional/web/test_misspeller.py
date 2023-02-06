# -*- coding: utf-8 -*-

import os
import pytest

from report.functional.web.base import BaseFuncTest
from runtime_tests.report.const import *


@pytest.mark.skipif(os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestMisspeller(BaseFuncTest):
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize(('no_wizard'), [ True, False ])
    def test_misspell_compat(self, query, no_wizard):
        query.set_flag('json_template_external')
        dump_param = "search.app_host.sources"
        query.set_params({'text': 'ьфвщттф', 'json_dump': dump_param})
        if no_wizard:
            query.add_flags({'srcskip': 'BEGEMOT_GRAPH'})

        resp = self.json_request(query)
        contexts = resp.data.get(dump_param)
        assert contexts

        data = get_app_host_context(contexts, source='APP_HOST', ttype='wizard')
        assert len(data) == 1
        #earlier we check ["reqdata"]["wizard_text"]
        assert data[0].get("original_request") == 'madonna'
