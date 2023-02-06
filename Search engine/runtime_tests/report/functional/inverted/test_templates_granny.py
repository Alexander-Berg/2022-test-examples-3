# -*- coding: utf-8 -*-

import os
import pytest

from report.functional.web.base import BaseFuncTest
from report.const import *

USER_AGENTS_SMART = (
    # Opera Mini/7.5
    'Opera/9.80 (Android; Opera Mini/7.5.33361/31.1448; U; en) Presto/2.8.119 Version/11.1010',

    # BADA
    'Mozilla/5.0 (SAMSUNG; SAMSUNG-GT-S8500/S8500XXJF8; U; Bada/1.0; nl-nl) AppleWebKit/533.1 (KHTML, like Gecko) Dolfin/2.0 Mobile WVGA SMM-MMS/1.2.0 OPN-B',
)

USER_AGENTS_FORCED_SMART = (
    # AndroidOS < 4
    'Mozilla/5.0 (Linux; U; Android 3.2; nl-nl; GT-P6800 Build/HTJ85B) AppleWebKit/534.13 (KHTML, like Gecko) Version/4.0 Safari/534.13',
    # AndroidBrowser < 4.4
    'Mozilla/5.0 (Linux; U; Android 4.2.2; nl-nl; HTC_One_X Build/JDQ39) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30',
    # AndroidBrowser 4.4.2
    'Mozilla/5.0 (Linux; U; Android 4.4.2; de-de; Nexus 5 Build/KOT49H) AppleWebKit/537.16 (KHTML, like Gecko) Version/4.0 Mobile Safari/537.16',

    # UC Browser 11
    'Mozilla/5.0 (Linux; Android 10; M2003J15SC Build/QP1A.190711.020; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/87.0.4280.101 Mobile Safari/537.36 AgentWeb/4.1.3 UCBrowser/11.6.4.950',

    # iOS < 9 (Safari)
    'Mozilla/5.0 (iPhone; CPU iPhone OS 6_1_4 like Mac OS X) AppleWebKit/536.26 (KHTML, like Gecko) Version/6.0 Mobile/10B350 Safari/8536.25',
    'Mozilla/5.0 (iPhone; CPU iPhone OS 7_0 like Mac OS X) AppleWebKit/537.51.1 (KHTML, like Gecko) Version/7.0 Mobile/11A465 Safari/9537.53',
    'Mozilla/5.0 (iPhone; CPU iPhone OS 8_0 like Mac OS X) AppleWebKit/537.51.1 (KHTML, like Gecko) Version/8.0 Mobile/11A465 Safari/9537.53',

    # Mobile MS Edge
    'Mozilla/5.0 (Windows Phone 10.0; Android 6.0.1; NuAns; NEO) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.116 Mobile Safari/537.36 Edge/15.15254',

    # IEMobile <= 11
    'Mozilla/5.0 (compatible; MSIE 9.0; Windows Phone OS 7.5; Trident/5.0; IEMobile/9.0; HTC; Radar C110e)',
    'Mozilla/5.0 (compatible; MSIE 10.0; Windows Phone 8.0; Trident/6.0; IEMobile/10.0; ARM; Touch; HTC; Windows Phone 8X by HTC)',
    'mozilla/5.0 (windows phone 8.1; arm; trident/7.0; touch; rv:11.0; iemobile/11.0; nokia; lumia 520) like gecko'
)

USER_AGENTS_WEB4 = (
    # iPhone 10.3.1
    'Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_1 like Mac OS X) AppleWebKit/603.1.30 (KHTML, like Gecko) Version/10.0 Mobile/14E304 Safari/602.1',

    # Android 4.4+ / no browser
    'Mozilla/5.0 (Linux; Android 4.4; Nexus 5)',

    # UC Browser 12
    'Mozilla/5.0 (Linux; U; Android 4.4.2; en-US; MAX-10 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/57.0.2987.108 UCBrowser/12.5.5.1111 Mobile Safari/537.36'
)

TEMPLATE_SMART = 'granny_exp:phone'
TEMPLATE_SLOW = 'granny_exp:phone'
TEMPLATE_WEB4 = 'web4:phone'


@pytest.mark.skipif(not os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestTemplatesGranny(BaseFuncTest):
    def assert_report_template(self, query, browser, template):
        query.set_user_agent(browser)
        ctxs = self.json_dump_ctxs(query)
        assert ctxs['device_config'][-1]['template_name'] == template

    # Use 'granny_freeze:phone' template for true 'smart' browsers
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-64042')
    def test_search_touch_granny_smart(self, query):
        query.set_query_type(SMART)

        for browser in USER_AGENTS_SMART:
            self.assert_report_template(query, browser, TEMPLATE_SMART)

    # Use 'granny_freeze:phone' template for manually forced to be 'smart' browsers
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-64042')
    def test_search_touch_granny_forced_smart(self, query):
        query.set_query_type(TOUCH)

        for browser in USER_AGENTS_FORCED_SMART:
            self.assert_report_template(query, browser, TEMPLATE_SMART)

            query.set_user_agent(browser)
            resp = self.json_test(query)
            assert str(resp.data['reqdata']['flags']['touch_to_granny']) == '1'
            assert str(resp.data['reqdata']['flags']['app_host']['smart']) == '1'

    # Slow connection
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.skipif(os.environ.get('REPORT_INVERTED') != '1', reason="SERP-67197")
    @pytest.mark.ticket('SERP-64042')
    def test_search_touch_granny_mobile(self, query):
        query.set_query_type(TOUCH)
        query.set_params({
            'user_connection': 'slow_connection=1',
            "init_meta": "move-user_connection-to-init=0",
            "timeout": "999999"
        })

        for browser in USER_AGENTS_WEB4:
            self.assert_report_template(query, browser, TEMPLATE_SLOW) # mobile granny for slow connection

        for browser in USER_AGENTS_FORCED_SMART:
            self.assert_report_template(query, browser, TEMPLATE_SMART) # ignore slow connection

        query.set_query_type(SMART)
        for browser in USER_AGENTS_SMART:
            self.assert_report_template(query, browser, TEMPLATE_SMART) # ignore slow connection

    # Use 'web4:phone' template
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-64042')
    def test_search_touch_web4(self, query):
        query.set_query_type(TOUCH)

        for browser in USER_AGENTS_WEB4:
            self.assert_report_template(query, browser, TEMPLATE_WEB4)
