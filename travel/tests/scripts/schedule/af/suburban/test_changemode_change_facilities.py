# coding: utf-8

from __future__ import absolute_import, unicode_literals

import logging
from datetime import date
from xml.etree import ElementTree

import freezegun
import pytest

from common.apps.facility.models import SuburbanFacility
from common.models.schedule import RThread, RThreadType, Supplier
from travel.rasp.library.python.common23.date import environment
from travel.rasp.admin.scripts.schedule.af_processors import suburban
from travel.rasp.admin.scripts.schedule.af_processors.utils import parse_thread
from tester.factories import create_thread, create_route, create_station
from __tests__.scripts.schedule.af.utils import assert_has_facilities, assert_has_not_facilities


# март 2016
# пн   вт   ср   чт   пт   сб   вс
#       1    2    3    4    5    6
#  7    8    9   10   11   12   13
# 14   15   16   17   18   19   20
# 21   22   23   24   25   26   27
# 28   29   30   31


def make_threads_and_facilities(with_changes=False):
    SuburbanFacility.objects.create(title_ru='Велик', code='velik')
    basic_thread = create_thread(t_type='suburban', uid='basic_uid')
    if with_changes:
        create_thread(t_type='suburban', uid='change_uid', basic_thread=basic_thread, type=RThreadType.CHANGE_ID)

    thread_el = ElementTree.fromstring("""
    <thread t_type="suburban" thread="basic_uid" changemode="change_facilities">
      <facilities>
        <facility_period period_start="2016-03-01" period_end="2016-03-31" weektemplate="1" facilities="velik"/>
      </facilities>
    </thread>
    """)

    suburban.process_thread(parse_thread(thread_el, default_t_type_code='suburban'),
                            thread_el, 'test.xml', environment.today())


@freezegun.freeze_time('2016-03-01')
@pytest.mark.dbuser
def test_apply_changes_to_empty_thread():
    make_threads_and_facilities(with_changes=True)
    velik = SuburbanFacility.objects.get(code='velik')

    thread = RThread.objects.get(uid='basic_uid')
    assert thread.suburbanthreadfacility_set.count() == 1
    assert_has_facilities(thread, [velik], [date(2016, 3, 7), date(2016, 3, 14)])

    change_thread = RThread.objects.get(uid='change_uid')
    assert change_thread.suburbanthreadfacility_set.count() == 1
    assert_has_facilities(change_thread, [velik], [date(2016, 3, 7), date(2016, 3, 14)])


@freezegun.freeze_time('2016-03-01')
@pytest.mark.dbuser
def test_apply_changes_to_filled_thread():
    make_threads_and_facilities(with_changes=True)
    velik = SuburbanFacility.objects.get(code='velik')

    thread_el = ElementTree.fromstring("""
    <thread t_type="suburban" thread="basic_uid" changemode="change_facilities">
      <facilities>
        <facility_period period_start="2016-03-10" period_end="2016-03-20" weektemplate="1" facilities="sofa"/>
      </facilities>
    </thread>
    """)

    suburban.process_thread(parse_thread(thread_el, default_t_type_code='suburban'),
                            thread_el, 'test.xml', environment.today())

    thread = RThread.objects.get(uid='basic_uid')
    assert thread.suburbanthreadfacility_set.count() == 1
    assert_has_facilities(thread, [velik], [date(2016, 3, 7), date(2016, 3, 21), date(2016, 3, 28)])

    change_thread = RThread.objects.get(uid='change_uid')
    assert change_thread.suburbanthreadfacility_set.count() == 1
    assert_has_facilities(change_thread, [velik], [date(2016, 3, 7), date(2016, 3, 21), date(2016, 3, 28)])


@freezegun.freeze_time('2016-03-01')
@pytest.mark.dbuser
def test_apply_changes_by_thread_number_and_station():
    velik = SuburbanFacility.objects.create(title_ru='Велик', code='velik')
    start_station = create_station(__={'codes': {'esr': '123456'}})
    supplier = Supplier.objects.get(code='af')
    route = create_route(route_uid='6001_{}_{}'.format(start_station.id, supplier.id))
    basic_thread = create_thread(t_type='suburban', uid='basic_uid', route=route)
    create_thread(t_type='suburban', uid='change_uid', basic_thread=basic_thread, type=RThreadType.CHANGE_ID,
                  route=route, ordinal_number=2)

    thread_el = ElementTree.fromstring("""
        <thread t_type="suburban" number="6001" startstationparent="123456" changemode="change_facilities">
          <facilities>
            <facility_period period_start="2016-03-01" period_end="2016-03-31" weektemplate="1" facilities="velik"/>
          </facilities>
        </thread>
        """)

    suburban.process_thread(parse_thread(thread_el, default_t_type_code='suburban'),
                            thread_el, 'test.xml', environment.today())

    thread = RThread.objects.get(uid='basic_uid')
    assert thread.suburbanthreadfacility_set.count() == 1
    assert_has_facilities(thread, [velik], [date(2016, 3, 7), date(2016, 3, 14)])

    change_thread = RThread.objects.get(uid='change_uid')
    assert change_thread.suburbanthreadfacility_set.count() == 1
    assert_has_facilities(change_thread, [velik], [date(2016, 3, 7), date(2016, 3, 14)])


@freezegun.freeze_time('2016-03-01')
@pytest.mark.dbuser
def test_fill_old_facilities_if_match():
    make_threads_and_facilities()
    velik = SuburbanFacility.objects.get(code='velik')

    thread_el = ElementTree.fromstring("""
    <thread t_type="suburban" thread="basic_uid" changemode="change_facilities">
      <facilities>
        <facility_period dates="2016-03-08" facilities="velik"/>
        <facility_period dates="2016-03-01" facilities="velik"/>
      </facilities>
    </thread>
    """)

    thread = RThread.objects.get(uid='basic_uid')

    assert thread.suburbanthreadfacility_set.count() == 1
    assert_has_facilities(thread, [velik], [date(2016, 3, 7), date(2016, 3, 14), date(2016, 3, 21), date(2016, 3, 28)])
    assert_has_not_facilities(thread, [velik], [date(2016, 3, 1), date(2016, 3, 8)])

    suburban.process_thread(parse_thread(thread_el, default_t_type_code='suburban'),
                            thread_el, 'test.xml', environment.today())

    assert thread.suburbanthreadfacility_set.count() == 1
    assert_has_facilities(thread, [velik], [date(2016, 3, 7), date(2016, 3, 14), date(2016, 3, 21), date(2016, 3, 28),
                                            date(2016, 3, 1), date(2016, 3, 8)])
