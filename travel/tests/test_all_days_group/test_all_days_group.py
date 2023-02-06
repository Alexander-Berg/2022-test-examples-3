# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, time, date

import pytest
import six
from django.conf import settings

from common.models.geo import CityMajority, Settlement
from common.tester import factories
from common.tester.transaction_context import transaction_fixture
from route_search.base import PlainSegmentSearch


@pytest.fixture(scope='module')
@transaction_fixture
def schedule(request):
    old_now = getattr(settings, 'ENVIRONMENT_NOW', None)
    settings.ENVIRONMENT_NOW = datetime(2015, 1, 1)

    def fin():
        settings.ENVIRONMENT_NOW = old_now

    request.addfinalizer(fin)

    schedule = {}

    sever_baik = factories.create_settlement(title=u'Северобайкальск', majority=CityMajority.REGION_CAPITAL_ID,
                                             time_zone='Asia/Irkutsk')
    ekat = factories.create_settlement(title=u'Екатеринбург', majority=CityMajority.REGION_CAPITAL_ID,
                                       time_zone='Asia/Yekaterinburg')
    msk = Settlement.objects.get(pk=Settlement.MOSCOW_ID)

    create_station = factories.create_station.mutate(t_type='train', majority='in_tablo')
    sever_baik_st = create_station(settlement=sever_baik, title=u'Северобайкальск')
    ekat_st = create_station(settlement=ekat, title=u'Екатеринбург')
    msk_st = create_station(settlement=msk, title=u'Москва')

    route = factories.create_route(__=None)

    create_thread = factories.create_thread.mutate(number=u'091И', route=route, t_type='train', time_zone='Europe/Moscow',
                                                   __=dict(calculate_noderoute=True),
                                                   schedule_v1=[
                                                       [None,    0, sever_baik_st, {'time_zone': 'Europe/Moscow'}],
                                                       [3763, 3790, ekat_st,       {'time_zone': 'Europe/Moscow'}],
                                                       [5474, None, msk_st,        {'time_zone': 'Europe/Moscow'}],
                                                   ])

    create_thread(tz_start_time="10:00", ordinal_number=1, year_days=[date(2015, 1, 1)], title=u'MyThread1')
    create_thread(tz_start_time="10:00", ordinal_number=2, year_days=[date(2015, 1, 2)], title=u'MyThread1')
    create_thread(tz_start_time="11:00", ordinal_number=3, year_days=[date(2015, 1, 3)], title=u'MyThread1')
    create_thread(tz_start_time="11:00", ordinal_number=4, year_days=[date(2015, 1, 3)], title=u'MyThread2')

    schedule['msk'] = msk
    schedule['ekat'] = ekat

    return schedule


@pytest.mark.dbuser
def test_all_days_group(schedule):
    groups = list(PlainSegmentSearch(
        schedule['ekat'],
        schedule['msk'],
        'train'
    ).all_days_group())

    assert len(groups) == 3

    group = groups[0]
    assert group.number == u'091И'
    assert group.arrival_time == time(5, 14)
    assert group.title == u'MyThread1'
    assert six.text_type(group.mask.format_days_text(lang='ru')) == u'4, 5\xa0января'
    assert len(list(group)) == 2

    group = groups[1]
    assert group.number == u'091И'
    assert group.arrival_time == time(6, 14)
    assert group.title == u'MyThread1'
    assert six.text_type(group.mask.format_days_text(lang='ru')) == u'6\xa0января'
    assert len(list(group)) == 1

    group = groups[2]
    assert group.number == u'091И'
    assert group.arrival_time == time(6, 14)
    assert group.title == u'MyThread2'
    assert six.text_type(group.mask.format_days_text(lang='ru')) == u'6\xa0января'
    assert len(list(group)) == 1


@pytest.mark.dbuser
def test_no_title_grouping(schedule):
    groups = list(PlainSegmentSearch(
        schedule['ekat'],
        schedule['msk'],
        'train'
    ).all_days_group(title_grouping=False))

    assert len(groups) == 2

    group = groups[0]
    assert len(list(group)) == 2
    assert group.title is None
    assert {s.title for s in group.segments} == {u'MyThread1'}

    group = groups[1]
    assert len(list(group)) == 2
    assert group.title is None
    assert {s.title for s in group.segments} == {u'MyThread1', u'MyThread2'}
