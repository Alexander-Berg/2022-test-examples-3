# -*- coding: utf-8 -*-

import pytest
from util.tsoy import TSoY
from util.const import TEMPLATE, USER_AGENT, PLATFORM, CTXS


class TestTemplatesGranny():
    @pytest.mark.ticket('SERP-64042')
    @pytest.mark.parametrize(('query_type', 'user_agent', 'params', 'template_name', 'check_flags'), [
        # Use 'granny_exp:phone' template for true 'smart' browsers
        (PLATFORM.SMART, USER_AGENT.OPERA_MINI_7_5, {}, TEMPLATE.GRANNY, {}),
        (PLATFORM.SMART, USER_AGENT.BADA_1_0, {}, TEMPLATE.GRANNY, {}),
        (PLATFORM.SMART, USER_AGENT.FIREFOX_MOBILE_IOS_67, {}, TEMPLATE.GRANNY, {}),
        (PLATFORM.SMART, USER_AGENT.FIREFOX_MOBILE_ANDROID_67, {}, TEMPLATE.GRANNY, {}),

        # Use 'granny_exp:phone' template for manually forced to be 'smart' browsers
        (PLATFORM.TOUCH, USER_AGENT.ANDROID_3_2, {}, TEMPLATE.GRANNY, {'touch_to_granny': '1', 'smart': '1'}),
        (PLATFORM.TOUCH, USER_AGENT.ANDROID_4_2_2, {}, TEMPLATE.GRANNY, {'touch_to_granny': '1', 'smart': '1'}),
        (PLATFORM.TOUCH, USER_AGENT.IPHONE_6_1_4, {}, TEMPLATE.GRANNY, {'touch_to_granny': '1', 'smart': '1'}),
        (PLATFORM.TOUCH, USER_AGENT.IPHONE_7_0, {}, TEMPLATE.GRANNY, {'touch_to_granny': '1', 'smart': '1'}),
        (PLATFORM.TOUCH, USER_AGENT.IPHONE_8_0, {}, TEMPLATE.GRANNY, {'touch_to_granny': '1', 'smart': '1'}),
        (PLATFORM.TOUCH, USER_AGENT.IPHONE_9_3_5, {}, TEMPLATE.GRANNY, {'touch_to_granny': '1', 'smart': '1'}),
        (PLATFORM.TOUCH, USER_AGENT.IPHONE_10_3_1, {}, TEMPLATE.GRANNY, {'touch_to_granny': '1', 'smart': '1'}),
        (PLATFORM.TOUCH, USER_AGENT.WP_10_0, {}, TEMPLATE.GRANNY, {'touch_to_granny': '1', 'smart': '1'}),
        (PLATFORM.TOUCH, USER_AGENT.WP_7_5, {}, TEMPLATE.GRANNY, {'touch_to_granny': '1', 'smart': '1'}),
        (PLATFORM.TOUCH, USER_AGENT.WP_8_0, {}, TEMPLATE.GRANNY, {'touch_to_granny': '1', 'smart': '1'}),
        (PLATFORM.TOUCH, USER_AGENT.WP_8_1, {}, TEMPLATE.GRANNY, {'touch_to_granny': '1', 'smart': '1'}),
        (PLATFORM.TOUCH, USER_AGENT.ANDROID_4_4_2, {}, TEMPLATE.GRANNY, {'touch_to_granny': '1', 'smart': '1'}),
        (PLATFORM.TOUCH, USER_AGENT.UCBROWSER_11, {}, TEMPLATE.GRANNY, {'touch_to_granny': '1', 'smart': '1'}),

        # Use 'web4:phone' template
        (PLATFORM.TOUCH, USER_AGENT.IPHONE_15_3_1, {}, TEMPLATE.WEB4_PHONE, {}),
        (PLATFORM.TOUCH, USER_AGENT.ANDROID_4_4_NO_BROWSER, {}, TEMPLATE.WEB4_PHONE, {}),
        (PLATFORM.TOUCH, USER_AGENT.UCBROWSER_12, {}, TEMPLATE.WEB4_PHONE, {}),
        (PLATFORM.TOUCH, USER_AGENT.FIREFOX_MOBILE_IOS_68, {}, TEMPLATE.WEB4_PHONE, {}),
        (PLATFORM.TOUCH, USER_AGENT.FIREFOX_MOBILE_ANDROID_68, {}, TEMPLATE.WEB4_PHONE, {}),

        # Slow connection (smart)
        (PLATFORM.SMART, USER_AGENT.OPERA_MINI_7_5, {'user_connection': 'slow_connection=1'}, TEMPLATE.GRANNY, {}),
        (PLATFORM.SMART, USER_AGENT.BADA_1_0, {'user_connection': 'slow_connection=1'}, TEMPLATE.GRANNY, {}),
        (PLATFORM.SMART, USER_AGENT.FIREFOX_MOBILE_IOS_67, {'user_connection': 'slow_connection=1'}, TEMPLATE.GRANNY, {}),
        (PLATFORM.SMART, USER_AGENT.FIREFOX_MOBILE_ANDROID_67, {'user_connection': 'slow_connection=1'}, TEMPLATE.GRANNY, {}),
        (PLATFORM.TOUCH, USER_AGENT.ANDROID_3_2, {'user_connection': 'slow_connection=1'}, TEMPLATE.GRANNY, {'touch_to_granny': '1', 'smart': '1'}),
        (PLATFORM.TOUCH, USER_AGENT.ANDROID_4_2_2, {'user_connection': 'slow_connection=1'}, TEMPLATE.GRANNY, {'touch_to_granny': '1', 'smart': '1'}),
        (PLATFORM.TOUCH, USER_AGENT.IPHONE_6_1_4, {'user_connection': 'slow_connection=1'}, TEMPLATE.GRANNY, {'touch_to_granny': '1', 'smart': '1'}),
        (PLATFORM.TOUCH, USER_AGENT.IPHONE_7_0, {'user_connection': 'slow_connection=1'}, TEMPLATE.GRANNY, {'touch_to_granny': '1', 'smart': '1'}),
        (PLATFORM.TOUCH, USER_AGENT.IPHONE_8_0, {'user_connection': 'slow_connection=1'}, TEMPLATE.GRANNY, {'touch_to_granny': '1', 'smart': '1'}),
        (PLATFORM.TOUCH, USER_AGENT.WP_10_0, {'user_connection': 'slow_connection=1'}, TEMPLATE.GRANNY, {'touch_to_granny': '1', 'smart': '1'}),
        (PLATFORM.TOUCH, USER_AGENT.WP_7_5, {'user_connection': 'slow_connection=1'}, TEMPLATE.GRANNY, {'touch_to_granny': '1', 'smart': '1'}),
        (PLATFORM.TOUCH, USER_AGENT.WP_8_0, {'user_connection': 'slow_connection=1'}, TEMPLATE.GRANNY, {'touch_to_granny': '1', 'smart': '1'}),
        (PLATFORM.TOUCH, USER_AGENT.WP_8_1, {'user_connection': 'slow_connection=1'}, TEMPLATE.GRANNY, {'touch_to_granny': '1', 'smart': '1'}),
        (PLATFORM.TOUCH, USER_AGENT.ANDROID_4_4_2, {'user_connection': 'slow_connection=1'}, TEMPLATE.GRANNY, {'touch_to_granny': '1', 'smart': '1'}),
        (PLATFORM.TOUCH, USER_AGENT.UCBROWSER_11, {'user_connection': 'slow_connection=1'}, TEMPLATE.GRANNY, {'touch_to_granny': '1', 'smart': '1'}),

        # Slow connection (web4)
        (PLATFORM.TOUCH, USER_AGENT.FIREFOX_MOBILE_IOS_68, {'user_connection': 'slow_connection=1'}, TEMPLATE.SLOW, {'touch_to_granny': '1', 'serp3_granny_https': '1'}),
        (PLATFORM.TOUCH, USER_AGENT.FIREFOX_MOBILE_ANDROID_68, {'user_connection': 'slow_connection=1'}, TEMPLATE.SLOW, {'touch_to_granny': '1', 'serp3_granny_https': '1'}),
        (PLATFORM.TOUCH, USER_AGENT.IPHONE_9_0, {'user_connection': 'slow_connection=1'}, TEMPLATE.SLOW, {'touch_to_granny': '1', 'serp3_granny_https': '1'}),
        (PLATFORM.TOUCH, USER_AGENT.IPHONE_9_3_5, {'user_connection': 'slow_connection=1'}, TEMPLATE.SLOW, {'touch_to_granny': '1', 'serp3_granny_https': '1'}),
        (PLATFORM.TOUCH, USER_AGENT.IPHONE_15_3_1, {'user_connection': 'slow_connection=1'}, TEMPLATE.SLOW, {'touch_to_granny': '1', 'serp3_granny_https': '1'}),
        (PLATFORM.TOUCH, USER_AGENT.ANDROID_4_4_NO_BROWSER, {'user_connection': 'slow_connection=1'}, TEMPLATE.SLOW, {'touch_to_granny': '1', 'serp3_granny_https': '1'}),
        (PLATFORM.TOUCH, USER_AGENT.UCBROWSER_12, {'user_connection': 'slow_connection=1'}, TEMPLATE.SLOW, {'touch_to_granny': '1', 'serp3_granny_https': '1'}),
    ])
    @TSoY.yield_test
    def test_search_touch_granny_smart(self, query, query_type, user_agent, params, template_name, check_flags):
        query.SetDumpFilter(resp=[CTXS.INIT])
        query.SetQueryType(query_type)
        query.SetUserAgent(user_agent)
        query.SetParams(params)
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert ctxs['device_config'][-1]['template_name'] == template_name
        for (k, v) in check_flags.items():
            assert str(ctxs['flags'][-1]['all'][k]) == str(v), k
