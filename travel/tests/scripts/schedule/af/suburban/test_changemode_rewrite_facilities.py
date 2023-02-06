# coding: utf-8

from __future__ import absolute_import, unicode_literals

import logging
from datetime import date
from xml.etree import ElementTree

import freezegun
import mock
import pytest
from django.db import IntegrityError

from common.apps.facility.models import SuburbanFacility, SuburbanThreadFacility
from common.models.schedule import RThread, RThreadType, Supplier
from travel.rasp.library.python.common23.date import environment
from common.utils.date import RunMask
from travel.rasp.admin.scripts.schedule.af_processors import suburban
from travel.rasp.admin.scripts.schedule.af_processors.utils import parse_thread
from tester.factories import create_thread, create_station, create_route, create_supplier
from travel.rasp.admin.scripts.schedule.af_processors.suburban.utils import facility as facility_utils
from travel.rasp.admin.scripts.schedule.af_processors.suburban.utils.facility import CHECK_INTERSECTION_WARNING


# март 2016
# пн   вт   ср   чт   пт   сб   вс
#       1    2    3    4    5    6
#  7    8    9   10   11   12   13
# 14   15   16   17   18   19   20
# 21   22   23   24   25   26   27
# 28   29   30   31
from __tests__.scripts.schedule.af.utils import assert_has_facilities


def get_thread_facility(thread_facilities, facilities):
    for thread_facility in thread_facilities:
        if set(facilities) == set(thread_facility.facilities.all()):
            return thread_facility


@freezegun.freeze_time('2016-03-01')
@pytest.mark.dbuser
def test_weektemplate_rewrite():
    thread_el = ElementTree.fromstring("""
    <thread t_type="suburban" thread="thread_uid" changemode="rewrite_facilities">
      <facilities>
        <facility_period period_start="2016-03-01" period_end="2016-03-31" weektemplate="1"
                         facilities="velociped,sofa"/>
      </facilities>
    </thread>
    """)

    create_thread(t_type='suburban', uid='thread_uid')
    velosiped = SuburbanFacility.objects.create(title_ru='Велик', code='velociped')
    sofa = SuburbanFacility.objects.create(title_ru='Софа', code='sofa')

    suburban.process_thread(parse_thread(thread_el, default_t_type_code='suburban'),
                            thread_el, 'test.xml', environment.today())

    thread = RThread.objects.get(uid='thread_uid')

    assert thread.suburbanthreadfacility_set.count() == 1
    assert_has_facilities(thread, [velosiped, sofa], [date(2016, 3, 7), date(2016, 3, 14)])


@freezegun.freeze_time('2016-03-01')
@pytest.mark.dbuser
def test_dates_rewrite():
    thread_el = ElementTree.fromstring("""
    <thread t_type="suburban" thread="thread_uid" changemode="rewrite_facilities">
      <facilities>
        <facility_period period_start="2016-03-01" period_end="2016-03-31" dates="2016-03-02"
                         facilities="velociped, sofa"/>
      </facilities>
    </thread>
    """)

    create_thread(t_type='suburban', uid='thread_uid')
    velosiped = SuburbanFacility.objects.create(title_ru='Велик', code='velociped')
    sofa = SuburbanFacility.objects.create(title_ru='Софа', code='sofa')

    suburban.process_thread(parse_thread(thread_el, default_t_type_code='suburban'),
                            thread_el, 'test.xml', environment.today())

    thread = RThread.objects.get(uid='thread_uid')

    assert thread.suburbanthreadfacility_set.count() == 1
    assert_has_facilities(thread, [velosiped, sofa], [date(2016, 3, 2)])


@freezegun.freeze_time('2016-03-01')
@pytest.mark.dbuser
def test_dates_rewrite():
    thread_el = ElementTree.fromstring("""
    <thread t_type="suburban" thread="thread_uid" changemode="rewrite_facilities">
      <facilities>
        <facility_period period_start="2016-03-01" period_end="2016-03-31" dates="2016-03-02"
                         facilities="velociped, sofa"/>
      </facilities>
    </thread>
    """)

    create_thread(t_type='suburban', uid='thread_uid')
    velosiped = SuburbanFacility.objects.create(title_ru='Велик', code='velociped')
    sofa = SuburbanFacility.objects.create(title_ru='Софа', code='sofa')

    suburban.process_thread(parse_thread(thread_el, default_t_type_code='suburban'),
                            thread_el, 'test.xml', environment.today())

    thread = RThread.objects.get(uid='thread_uid')

    assert thread.suburbanthreadfacility_set.count() == 1
    assert_has_facilities(thread, [velosiped, sofa], [date(2016, 3, 2)])


@freezegun.freeze_time('2016-03-01')
@pytest.mark.dbuser
@mock.patch.object(facility_utils.log, 'warning')
def test_not_match_warning(m_log_warning):
    thread_el = ElementTree.fromstring("""
    <thread t_type="suburban" thread="thread_uid" changemode="rewrite_facilities">
      <facilities>
        <facility_period period_start="2016-03-01" period_end="2016-03-31" dates="2016-03-02;2016-03-03;2016-03-04"
                         facilities="sofa"/>
      </facilities>
    </thread>
    """)

    create_thread(t_type='suburban', uid='thread_uid', year_days=[date(2016, 03, 02)])
    SuburbanFacility.objects.create(title_ru='Софа', code='sofa')

    suburban.process_thread(parse_thread(thread_el, default_t_type_code='suburban'),
                            thread_el, 'test.xml', environment.today())

    m_log_warning.assert_called_once_with(CHECK_INTERSECTION_WARNING, '2016-03-03, 2016-03-04')


@freezegun.freeze_time('2016-03-01')
@pytest.mark.dbuser
@mock.patch.object(facility_utils.log, 'warning')
def test_not_match_warning_with_changes(m_log_warning):
    thread_el = ElementTree.fromstring("""
    <thread t_type="suburban" thread="thread_uid" changemode="rewrite_facilities">
      <facilities>
        <facility_period period_start="2016-03-01" period_end="2016-03-31" dates="2016-03-02;2016-03-03;2016-03-04"
                         facilities="sofa"/>
      </facilities>
    </thread>
    """)

    basic_thread = create_thread(t_type='suburban', uid='thread_uid', year_days=[date(2016, 03, 02)])
    create_thread(t_type='suburban', uid='thread_uid_2', year_days=[date(2016, 03, 03)], basic_thread=basic_thread,
                  type=RThreadType.CHANGE_ID)
    SuburbanFacility.objects.create(title_ru='Софа', code='sofa')

    suburban.process_thread(parse_thread(thread_el, default_t_type_code='suburban'),
                            thread_el, 'test.xml', environment.today())

    m_log_warning.assert_called_once_with(CHECK_INTERSECTION_WARNING, '2016-03-04')


@freezegun.freeze_time('2016-03-01')
@pytest.mark.dbuser
def test_rewrite_with_changes():
    thread_el = ElementTree.fromstring("""
    <thread t_type="suburban" thread="thread_uid" changemode="rewrite_facilities">
      <facilities>
        <facility_period period_start="2016-03-01" period_end="2016-03-31" dates="2016-03-02;2016-03-03"
                         facilities="sofa"/>
      </facilities>
    </thread>
    """)

    basic_thread = create_thread(t_type='suburban', uid='thread_uid', year_days=[date(2016, 03, 02)])
    create_thread(t_type='suburban', uid='thread_uid_2', year_days=[date(2016, 03, 03)], basic_thread=basic_thread,
                  type=RThreadType.CHANGE_ID)
    sofa = SuburbanFacility.objects.create(title_ru='Софа', code='sofa')

    suburban.process_thread(parse_thread(thread_el, default_t_type_code='suburban'),
                            thread_el, 'test.xml', environment.today())

    basic_thread = RThread.objects.get(uid='thread_uid')
    change_thread = RThread.objects.get(uid='thread_uid_2')

    assert_has_facilities(basic_thread, [sofa], [date(2016, 3, 2), date(2016, 3, 3)])
    assert_has_facilities(change_thread, [sofa], [date(2016, 3, 2), date(2016, 3, 3)])


@freezegun.freeze_time('2016-03-01')
@pytest.mark.dbuser
def test_clear_old_facilities():
    thread_el = ElementTree.fromstring("""
    <thread t_type="suburban" thread="thread_uid" changemode="rewrite_facilities">
      <facilities>
        <facility_period period_start="2016-03-01" period_end="2016-03-31" dates="2016-03-02;2016-03-03"
                         facilities="sofa"/>
      </facilities>
    </thread>
    """)

    basic_thread = create_thread(t_type='suburban', uid='thread_uid', year_days=[date(2016, 03, 02)])
    change_thread = create_thread(t_type='suburban', uid='thread_uid_2', year_days=[date(2016, 03, 03)],
                                  basic_thread=basic_thread, type=RThreadType.CHANGE_ID)
    sofa = SuburbanFacility.objects.create(title_ru='Софа', code='sofa')
    velic = SuburbanFacility.objects.create(title_ru='Велик', code='velic')
    SuburbanThreadFacility.objects.create(thread=basic_thread, year_days=RunMask.ALL_YEAR_DAYS).facilities.add(velic)
    SuburbanThreadFacility.objects.create(thread=change_thread, year_days=RunMask.ALL_YEAR_DAYS).facilities.add(velic)

    suburban.process_thread(parse_thread(thread_el, default_t_type_code='suburban'),
                            thread_el, 'test.xml', environment.today())

    basic_thread = RThread.objects.get(uid='thread_uid')
    change_thread = RThread.objects.get(uid='thread_uid_2')

    assert basic_thread.suburbanthreadfacility_set.count() == 1
    assert_has_facilities(basic_thread, [sofa], [date(2016, 3, 2), date(2016, 3, 3)])

    assert change_thread.suburbanthreadfacility_set.count() == 1
    assert_has_facilities(change_thread, [sofa], [date(2016, 3, 2), date(2016, 3, 3)])


@freezegun.freeze_time('2016-03-01')
@pytest.mark.dbuser
def test_rewrite_by_number_and_station():
    thread_el = ElementTree.fromstring("""
    <thread t_type="suburban" number="6001" startstationparent="123456" changemode="rewrite_facilities">
      <facilities>
        <facility_period period_start="2016-03-01" period_end="2016-03-31" dates="2016-03-02;2016-03-03"
                         facilities="sofa"/>
      </facilities>
    </thread>
    """)

    start_station = create_station(__={'codes': {'esr': '123456'}})
    supplier = Supplier.objects.get(code='af')
    route = create_route(route_uid='6001_{}_{}'.format(start_station.id, supplier.id))
    basic_thread = create_thread(t_type='suburban', uid='thread_uid', year_days=[date(2016, 03, 02)],
                                 route=route, supplier=supplier)
    create_thread(t_type='suburban', uid='thread_uid_2', year_days=[date(2016, 03, 03)], basic_thread=basic_thread,
                  type=RThreadType.CHANGE_ID)
    sofa = SuburbanFacility.objects.create(title_ru='Софа', code='sofa')

    suburban.process_thread(parse_thread(thread_el, default_t_type_code='suburban'),
                            thread_el, 'test.xml', environment.today())

    basic_thread = RThread.objects.get(uid='thread_uid')
    change_thread = RThread.objects.get(uid='thread_uid_2')

    assert_has_facilities(basic_thread, [sofa], [date(2016, 3, 2), date(2016, 3, 3)])
    assert_has_facilities(change_thread, [sofa], [date(2016, 3, 2), date(2016, 3, 3)])

@freezegun.freeze_time('2016-03-01')
@pytest.mark.dbuser
def test_exception_by_adding_second_facility():
    thread_el = ElementTree.fromstring("""
    <thread t_type="suburban" thread="thread_uid" changemode="rewrite_facilities">
      <facilities>
        <facility_period period_start="2016-03-01" period_end="2016-03-31" dates="2016-03-02"
                         facilities="velociped"/>
      </facilities>
    </thread>
    """)

    thread = create_thread(t_type='suburban', uid='thread_uid')
    velosiped = SuburbanFacility.objects.create(title_ru='Велик', code='velociped')
    suburban.process_thread(parse_thread(thread_el, default_t_type_code='suburban'),
                            thread_el, 'test.xml', environment.today())

    with pytest.raises(IntegrityError):
        SuburbanThreadFacility.objects.create(
            thread=thread, year_days=RunMask.ALL_YEAR_DAYS
        ).facilities.add(velosiped)
