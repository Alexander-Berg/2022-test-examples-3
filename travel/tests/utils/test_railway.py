# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import pytest

from common.importinfo.models import Express2Country
from common.models.geo import Country
from common.tester.factories import create_station, create_settlement
from common.utils.railway import get_railway_tz_by_express_code, get_railway_tz_by_point


@pytest.mark.dbuser
def test_get_railway_tz_by_express_code():
    Express2Country.objects.create(code_re=r'20\d+', country_id=Country.RUSSIA_ID, time_zone='Asia/Yekaterinburg')

    assert get_railway_tz_by_express_code('200000').zone == 'Asia/Yekaterinburg'
    assert get_railway_tz_by_express_code('100000') is None


@pytest.mark.dbuser
def test_get_railway_tz_by_point():
    Express2Country.objects.create(code_re=r'20\d+', country_id=Country.RUSSIA_ID, time_zone='Asia/Yekaterinburg')
    station = create_station(__={'codes': {'express': '200000'}})

    assert get_railway_tz_by_point(station).zone == 'Asia/Yekaterinburg'

    station = create_station(country=Country.RUSSIA_ID)
    assert get_railway_tz_by_point(station).zone == 'Europe/Moscow'

    station = create_station(__={'codes': {'express': '100000'}}, country=Country.RUSSIA_ID)
    assert get_railway_tz_by_point(station).zone == 'Europe/Moscow'

    settlement = create_settlement(country=Country.RUSSIA_ID)
    assert get_railway_tz_by_point(settlement).zone == 'Europe/Moscow'

    station = create_station(country=None, settlement=None)
    assert get_railway_tz_by_point(station).zone == 'Europe/Moscow'

    assert get_railway_tz_by_point(Country.objects.get(pk=Country.RUSSIA_ID)).zone == 'Europe/Moscow'
