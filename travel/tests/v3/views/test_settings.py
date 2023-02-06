# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from hamcrest import assert_that, has_entries

from common.tester.utils.replace_setting import replace_dynamic_setting
from travel.rasp.export.tests.v3.helpers import create_request
from travel.rasp.export.export.v3.views.settings import SettingsView


pytestmark = [pytest.mark.mongouser('module')]


def test_settings_view():
    request = create_request()

    with replace_dynamic_setting('SUBURBAN_APP_AUTO_UPDATE_INTERVAL', 111), \
            replace_dynamic_setting('UGC_NOTIFICATION_SCHEDULED_TIME_SEND_SECONDS', 11), \
            replace_dynamic_setting('UGC_NOTIFICATION_TIMEOUT_SECONDS', 22), \
            replace_dynamic_setting('UGC_STATION_NEAR_DISTANCE_METRES', 33), \
            replace_dynamic_setting('SUBURBAN_PROMO_SEARCH', False), \
            replace_dynamic_setting('SUBURBAN_PROMO_FAVORITES', False), \
            replace_dynamic_setting('SUBURBAN_INFO_BANNER', True), \
            replace_dynamic_setting('SUBURBAN_PROMO_STATION', True), \
            replace_dynamic_setting('SUBURBAN_DRIVE_INTEGRATION', True), \
            replace_dynamic_setting('SUBURBAN_MUSIC_INTEGRATION', True), \
            replace_dynamic_setting('SUBURBAN_AEROEX_SELLING_ENABLED', False), \
            replace_dynamic_setting('SUBURBAN_POLLING_MAX_ORDERS_COUNT_IN_REQUEST', 20), \
            replace_dynamic_setting('SUBURBAN_POLLING_ORDERS_FIRST_TIME_STEP', 3), \
            replace_dynamic_setting('SUBURBAN_POLLING_ORDERS_EXP_BACKOFF', 1.6), \
            replace_dynamic_setting('SUBURBAN_POLLING_ORDERS_MAX_CALLS_COUNT', 30), \
            replace_dynamic_setting('SUBURBAN_POLLING_ORDERS_MAX_MINUTES_OFFSET', 42), \
            replace_dynamic_setting('SUBURBAN_POLLING_ORDERS_SHOW_SPINNER_TIME', 43):
        response = SettingsView().handle(request)

    assert_that(response, has_entries({
        'auto_update_interval': 111,
        'info_banner': True,
        'ugc_notification_scheduled_time_send_seconds': 11,
        'ugc_notification_timeout_seconds': 22,
        'ugc_station_near_distance_metres': 33,
        'promo_search': False,
        'promo_favorites': False,
        'promo_station': True,
        'drive_integration': True,
        'music_integration': True,
        'aeroex_selling': False,
        'movista_selling': True,
        'im_selling': True,
        'polling_max_orders_count_in_request': 20,
        'polling_orders_first_time_step': 3,
        'polling_orders_exp_backoff': 1.6,
        'polling_orders_max_calls_count': 30,
        'polling_orders_max_minutes_offset': 42,
        'polling_orders_show_spinner_time': 43,
    }))
