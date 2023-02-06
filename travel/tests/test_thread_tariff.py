# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date, datetime, time

import pytest
import pytz

from travel.rasp.library.python.common23.date.run_mask import RunMask
from travel.rasp.library.python.common23.models.tariffs.tester.factories import create_thread_tariff


EKB_TZ = pytz.timezone('Asia/Yekaterinburg')


@pytest.mark.dbuser
@pytest.mark.parametrize('tariff_params, expected_local_departure', (
    ({'time_from': time(5, 10)}, datetime(2000, 1, 1, 5, 10)),
    ({'time_from': time(12, 10), 'time_zone_from': EKB_TZ}, datetime(2000, 1, 1, 10, 10)),
    ({'time_from': time(1, 10), 'time_zone_from': EKB_TZ}, datetime(2000, 1, 1, 23, 10)),
    ({'time_from': time(23, 10), 'station_from': {'time_zone': EKB_TZ}}, datetime(2000, 1, 1, 1, 10)),
    ({'year_days_from': RunMask.EMPTY_YEAR_DAYS}, None),
    (
        {'time_from': time(1, 10), 'year_days_from': '01' + '0' * (RunMask.MASK_LENGTH - 2)},
        None
    ),
    (
        {'time_from': time(1, 10), 'time_zone_from': EKB_TZ, 'year_days_from': '01' + '0' * (RunMask.MASK_LENGTH - 2)},
        datetime(2000, 1, 1, 23, 10)
    ),
))
def test_get_local_departure(tariff_params, expected_local_departure):
    tariff = create_thread_tariff(**tariff_params)
    local_departure = tariff.get_local_departure(date(2000, 1, 1))

    if local_departure is None:
        assert expected_local_departure is None
    else:
        assert local_departure.replace(tzinfo=None) == expected_local_departure
