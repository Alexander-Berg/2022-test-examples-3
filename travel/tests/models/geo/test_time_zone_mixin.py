# -*- coding=utf-8 -*-

from datetime import datetime

from mock import patch, Mock
from pytz import timezone

from travel.avia.library.python.common.models.geo import TimeZoneMixin
from travel.avia.library.python.common.utils.date import MSK_TZ
from travel.avia.library.python.tester.testcase import TestCase


class TestTimeZoneMixin(TestCase):
    def test_localize(self):
        ekb_tz = timezone('Asia/Yekaterinburg')
        samoa_tz = timezone('Pacific/Samoa')

        with patch.object(TimeZoneMixin, 'pytz') as mock_tzm:
            mock_tzm.__get__ = Mock(return_value=ekb_tz)
            tzm = TimeZoneMixin()
            dt_msk = datetime(2000, 1, 2)
            dt_samoa = datetime(2000, 2, 2)
            dt_tz_msk = MSK_TZ.localize(dt_msk)
            dt_tz_samoa = samoa_tz.localize(dt_samoa)

            assert tzm.localize(dt_msk) == MSK_TZ.localize(dt_msk)
            assert tzm.localize(dt_tz_msk) == dt_tz_msk.astimezone(ekb_tz)

            assert tzm.localize(loc=dt_samoa) == ekb_tz.localize(dt_samoa)
            assert tzm.localize(loc=dt_tz_samoa) == dt_tz_samoa.astimezone(ekb_tz)

            assert tzm.localize(dt_msk, dt_samoa) == MSK_TZ.localize(dt_msk)
            assert tzm.localize(dt_tz_msk, dt_tz_samoa) == dt_tz_msk.astimezone(ekb_tz)

    def test_pytz(self):
        ekb_tz = 'Asia/Yekaterinburg'
        invalid_tz = 'invalid'

        with patch('travel.avia.library.python.common.models_utils.geo.get_timezone_from_point', side_effect=[ekb_tz, invalid_tz]):
            tzm = TimeZoneMixin()
            assert tzm.pytz == timezone(ekb_tz)
            tzm = TimeZoneMixin()
            assert tzm.pytz == MSK_TZ
