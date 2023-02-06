# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from hamcrest import assert_that, has_entries

from common.tester.utils.replace_setting import replace_dynamic_setting
from travel.rasp.export.export.v3.core.settings import get_settings


pytestmark = [pytest.mark.mongouser('module')]


def test_get_settings():
    with replace_dynamic_setting('SUBURBAN_APP_AUTO_UPDATE_INTERVAL', 222), \
            replace_dynamic_setting('UGC_NOTIFICATION_SCHEDULED_TIME_SEND_SECONDS', 11), \
            replace_dynamic_setting('UGC_NOTIFICATION_TIMEOUT_SECONDS', 22), \
            replace_dynamic_setting('SUBURBAN_PROMO_SEARCH', False), \
            replace_dynamic_setting('SUBURBAN_PROMO_FAVORITES', True), \
            replace_dynamic_setting('SUBURBAN_MOVISTA_SELLING_ENABLED', False), \
            replace_dynamic_setting('SUBURBAN_MOVISTA_SELLING_ENABLED_IOS', True), \
            replace_dynamic_setting('SUBURBAN_IM_SELLING_ENABLED', False), \
            replace_dynamic_setting('SUBURBAN_IM_SELLING_ENABLED_IOS', True), \
            replace_dynamic_setting('SUBURBAN_SELLING_ENABLE_GOOGLE_PAY', False), \
            replace_dynamic_setting('SUBURBAN_SELLING_ENABLE_APPLE_PAY', True), \
            replace_dynamic_setting('SUBURBAN_SELLING_ENABLE_PAYMENT_SDK_IOS', True), \
            replace_dynamic_setting('SUBURBAN_POLLING_MAX_ORDERS_COUNT_IN_REQUEST', 20), \
            replace_dynamic_setting('SUBURBAN_POLLING_ORDERS_FIRST_TIME_STEP', 3), \
            replace_dynamic_setting('SUBURBAN_POLLING_ORDERS_EXP_BACKOFF', 1.6), \
            replace_dynamic_setting('SUBURBAN_POLLING_ORDERS_MAX_CALLS_COUNT', 30), \
            replace_dynamic_setting('SUBURBAN_POLLING_ORDERS_MAX_MINUTES_OFFSET', 42), \
            replace_dynamic_setting('SUBURBAN_POLLING_ORDERS_SHOW_SPINNER_TIME', 43), \
            replace_dynamic_setting('SUBURBAN_SELLING_ENABLED', True):

        assert_that(get_settings(), has_entries({
            'auto_update_interval': 222,
            'info_banner': False,
            'ugc_notification_scheduled_time_send_seconds': 11,
            'ugc_notification_timeout_seconds': 22,
            'promo_search': False,
            'promo_favorites': True,
            'drive_integration': False,
            'music_integration': False,
            'aeroex_selling': True,
            'movista_selling': False,
            'movista_selling_ios': True,
            'im_selling': False,
            'im_selling_ios': True,
            'enable_apple_pay': False,
            'apple_pay_enabled': True,
            'enable_google_pay': False,
            'enable_payment_sdk_ios': False,
            'payment_sdk_ios_enabled': True,
            'enable_payment_sdk_android': True,
            'polling_max_orders_count_in_request': 20,
            'polling_orders_first_time_step': 3,
            'polling_orders_exp_backoff': 1.6,
            'polling_orders_max_calls_count': 30,
            'polling_orders_max_minutes_offset': 42,
            'polling_orders_show_spinner_time': 43,
        }))
