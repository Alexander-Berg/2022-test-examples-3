# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import datetime

import pytest
import pytz

from travel.rasp.train_api.wizard_api.helpers.dt import get_dt_from_wizard_dict, get_dt


@pytest.mark.parametrize('value', ('2019-02-14T09:13:00.123+03:00', '2019-02-14T09:13:00+03:00'))
def test_get_dt_from_wizard_dict(value):
    dt_dict = {
        'timezone': 'Europe/Moscow',
        'value': value,
    }

    assert get_dt_from_wizard_dict(dt_dict) == pytz.timezone('Europe/Moscow').localize(datetime(2019, 2, 14, 9, 13))


@pytest.mark.parametrize('timezone', ('Europe/Moscow', pytz.timezone('Europe/Moscow')))
def test_get_dt(timezone):
    assert (
        get_dt('2019-02-14T09:13:00', timezone) == pytz.timezone('Europe/Moscow').localize(datetime(2019, 2, 14, 9, 13))
    )
