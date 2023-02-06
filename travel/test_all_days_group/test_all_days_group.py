# -*- coding: utf-8 -*-

from datetime import datetime, time, date

import pytest
from django.conf import settings

from travel.avia.library.python.common.models.geo import CityMajority, Settlement
from travel.avia.library.python.route_search.base import PlainSegmentSearch
from travel.avia.library.python.tester import factories
from travel.avia.library.python.tester.transaction_context import transaction_fixture


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
    """
    У расписаний месяца полные:
    https://a.yandex-team.ru/arc/trunk/arcadia/travel/rasp/library/python/common/utils/date.py?rev=6342411#L775

    У нас сокращенные:
    https://a.yandex-team.ru/arc/trunk/arcadia/travel/avia/library/python/common/utils/date.py?rev=6782382#L811
    """
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
    assert unicode(group.mask.format_days_text(lang='ru')) == u'4, 5\xa0янв'
    assert len(list(group)) == 2

    group = groups[1]
    assert group.number == u'091И'
    assert group.arrival_time == time(6, 14)
    assert group.title == u'MyThread1'
    assert unicode(group.mask.format_days_text(lang='ru')) == u'6\xa0янв'
    assert len(list(group)) == 1

    group = groups[2]
    assert group.number == u'091И'
    assert group.arrival_time == time(6, 14)
    assert group.title == u'MyThread2'
    assert unicode(group.mask.format_days_text(lang='ru')) == u'6\xa0янв'
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
