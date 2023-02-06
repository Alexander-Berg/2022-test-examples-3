# -*- coding: utf-8 -*-
__author__ = 'melnikoff'

import json

import os
import pytest

from report.const import *
from report.functional.web.base import BaseFuncTest


@pytest.mark.skipif(not os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestGranny(BaseFuncTest):
    @pytest.mark.ticket('SERP-43251')
    def test_granny_banner(self, query):
        query.set_params({'text': 'пластиковые окна'})
        query.set_flags('serp3_granny', 'serp3_granny_https')
        query.set_url(SEARCH)
        query.set_user_agent(USER_AGENT_GRANNY)

        resp = self.json_test(query)
        data = resp.data
        assert data['banner']

    def patch_request_granny(self, query, https, g_https_flag, g_flag, export):
        query.set_flags({'serp3_granny_https': g_https_flag, 'serp3_granny': g_flag})
        if export:
            query.set_internal()
            query.replace_params({'export':'json'})

        query.set_user_agent(USER_AGENT_GRANNY)
        query.set_https(https)

    def base_test_granny(self, query, https, g_https_flag, g_flag, export):
        self.patch_request_granny(query, https, g_https_flag, g_flag, export)

        resp = self.request(query, require_status=None)
        assert not export or resp.data

        return resp

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.ticket('SERP-36585', 'SERP-32482', 'SERP-36784')
    def test_search_granny_https_2(self, query):
        """
        SERP-36585 - [granny] Добавить возможно видеть бабулю по https с флагом
        case: https + serp3_granny_https + serp3_granny => https 200
        case: export - внутренняя ручка, поэтому редиректы отключены
        """
        resp = self.base_test_granny(query, 1, 1, 1, 0)
        assert resp.status == 200

        resp = self.base_test_granny(query, 0, 1, 1, 1)
        assert resp.status == 200

        ctxs = self.json_dump_ctxs(query)
        assert "template_data" in ctxs
        template_data = ctxs["template_data"][0]
        assert "name" in template_data

        assert template_data['name'] == 'granny:desktop'

    @pytest.mark.ticket('SERP-36585', 'SERP-32482', 'SERP-36784')
    def test_search_granny_https_3(self, query):
        """
        SERP-36585 - [granny] Добавить возможно видеть бабулю по https с флагом
        case: http + no serp3_granny_https + serp3_granny => 200
        case: export - внутренняя ручка, поэтому редиректы отключены
        """
        resp = self.base_test_granny(query, 0, 0, 1, 0)
        assert resp.status == 200

    @pytest.mark.ticket('SERP-50272')
    def test_search_granny_https_4(self, query):
        """
        SERP-50272 - [granny] Не редиректить бабулю с HTTPS
        case: https => 200
        case: export - внутренняя ручка, поэтому редиректы отключены
        """
        # def base_test_granny(self, query, https, g_https_flag, g_flag, export):
        resp = self.base_test_granny(query, 1, 0, 0, 0)
        assert resp.status == 200

