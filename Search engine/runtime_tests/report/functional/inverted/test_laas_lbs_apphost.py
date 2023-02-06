# -*- coding: utf-8 -*-

import os
import re
import pytest
import json

from report.functional.web.base import BaseFuncTest
from report.const import *


@pytest.mark.skipif(not os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestLaasLbsApphost(BaseFuncTest):

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-65360')
    def test_laas_lbs_rdat(self, query):
        query.set_query_type(TOUCH)
        query.add_flags({'laas_lbs_apphost': 1})
        query.headers.set_forward_for(IP[RU_IRKUTSK])
        query.headers.set_custom_headers({'X-Region-City-Id': REGION[RU_MOSCOW]})

        rdat = self.json_dump_request(query, 'rdat')

        assert 'headers' in rdat.keys()
        headers = rdat['headers']
        assert 'X-LaaS-Answered' in headers.keys()
        assert headers['X-LaaS-Answered'] == '1'

        x_region = 0
        for key in headers:
            if key.startswith('X-Region'):
                x_region += 1
        assert x_region > 2

        assert headers['X-Region-City-Id'] != REGION[RU_MOSCOW]

        assert rdat['user_region']['id'] != REGION[RU_MOSCOW]

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-65360')
    def test_laas_lbs_replaced_headers(self, query):
        query.set_query_type(TOUCH)
        query.add_flags({'laas_lbs_apphost': 1})
        query.replace_params({'ui': 'webmobileapp.yandex'})
        query.headers.set_forward_for(IP[RU_IRKUTSK])
        query.headers.set_custom_headers({
            'X-Region-City-Id': REGION[RU_MOSCOW],
            'X-Region-Id':      REGION[RU_MOSCOW]
        })

        search_props = self.json_dump_request(query, 'search_props.REPORT')
        props = search_props[0]['properties']

    # check lbs is working
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-65361')
    @pytest.mark.parametrize('cellid, region', [
        ('401,01,4466781,6182,-85', 190)
    ])
    def test_cellid(self, query, cellid, region):
        query.set_query_type(TOUCH)
        query.add_flags({'laas_lbs_apphost': 1})
        query.set_params({ 'text': 'test', 'lr': 213, 'cellid': cellid })
        query.headers.set_forward_for(IP[RU_MOSCOW])

        rdat = self.json_dump_request(query, 'rdat')

        assert 'headers' in rdat.keys()
        headers = rdat['headers']
        assert 'X-LaaS-Answered' in headers.keys()
        assert headers['X-LaaS-Answered'] == '1'

        assert rdat['user_region_no_lr']['id'] == region

