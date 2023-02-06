# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock

from common.models_utils.geo import TimeZoneMixin
from common.tester.factories import create_settlement, create_station, create_country
from common.tester.testcase import TestCase
from travel.rasp.morda_backend.morda_backend.search.parse_context.point import get_point_timezone


class TestGetPointTimezone(TestCase):
    def test_settlement(self):
        settlement = create_settlement()
        with mock.patch.object(TimeZoneMixin, 'get_tz_name', return_value='Asia/Omsk') as m_get_tz_name:
            assert get_point_timezone(settlement) == 'Asia/Omsk'

        m_get_tz_name.assert_called_once_with()

    def test_station(self):
        station = create_station()
        with mock.patch.object(TimeZoneMixin, 'get_tz_name', return_value='Asia/Omsk') as m_get_tz_name:
            assert get_point_timezone(station) == 'Asia/Omsk'

        m_get_tz_name.assert_called_once_with()

    def test_country(self):
        country = create_country()
        assert get_point_timezone(country) is None
