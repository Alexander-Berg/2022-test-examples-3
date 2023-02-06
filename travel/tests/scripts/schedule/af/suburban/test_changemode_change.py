# -*- coding: utf-8 -*-
from __future__ import absolute_import

import logging
from datetime import date
from xml.etree import ElementTree
from StringIO import StringIO

import freezegun
import pytest

from common.apps.facility.models import SuburbanFacility
from common.db.switcher import switcher
from common.models.schedule import Route, TrainSchedulePlan
from travel.rasp.library.python.common23.date import environment
from travel.rasp.admin.scripts.schedule.af_processors import suburban
from travel.rasp.admin.scripts.schedule.af_processors.utils import parse_thread
from tester.testcase import TestCase
from tester.utils.datetime import replace_now


@pytest.yield_fixture(scope='module', autouse=True)
def auto_replace_now():
    with replace_now('2015-01-01'):
        yield


change_mode_insert_xml = u"""
<?xml version="1.0" encoding="utf-8"?>
<channel>
  <thread t_type="suburban" number="3333" startstationparent="000001"
          dates="2015-06-23;2015-06-24" changemode="insert" graph="xxx">
    <stations>
      <station esrcode="000001" stname="1"
               arrival_time="" stop_time=""
               departure_time="12:00" minutes_from_start="0" />
      <station esrcode="000002" stname="2"
               arrival_time="12:40" stop_time="10"
               departure_time="12:50" minutes_from_start="40" />
      <station esrcode="000003" stname="3"
               arrival_time="13:00" stop_time=""
               departure_time="" minutes_from_start="60" />
    </stations>
  </thread>
</channel>
""".strip()


change_mode_change_xml = u"""
<?xml version="1.0" encoding="utf-8"?>
<channel>
  <thread t_type="suburban" number="3333" startstationparent="000001"
          dates="2015-06-23" changemode="change">
    <stations>
      <station esrcode="000001" stname="1"
               arrival_time="" stop_time=""
               departure_time="12:00" minutes_from_start="0" />
      <station esrcode="000002" stname="2"
               arrival_time="12:40" stop_time="10"
               departure_time="12:50" minutes_from_start="40" />
      <station esrcode="000003" stname="3"
               arrival_time="13:00" stop_time=""
               departure_time="" minutes_from_start="60" />
    </stations>
  </thread>
</channel>
""".strip()


class TestChangeMode(TestCase):
    def setUp(self):
        self.schedule_plan = TrainSchedulePlan.objects.create(
            title=u'Тестовый график',
            start_date=date(2015, 5, 1),
            end_date=date(2016, 5, 1),
            code='xxx',
            appendix_type='to'
        )

        # избавляемся от воздействия cache_until_switch
        switcher.data_updated.send(None)

    def insert_thread(self):
        tree = ElementTree.parse(StringIO(change_mode_insert_xml.encode('utf-8')))
        for thread_el in tree.findall('.//thread'):
            thread = parse_thread(thread_el, default_t_type_code='suburban')

            suburban.process_thread(thread, thread_el, 'test.xml', environment.today())

    def change_thread(self):
        tree = ElementTree.parse(StringIO(change_mode_change_xml.encode('utf-8')))
        for thread_el in tree.findall('.//thread'):
            thread = parse_thread(thread_el, default_t_type_code='suburban')

            suburban.process_thread(thread, thread_el, 'test.xml', environment.today())

    def test_insert_and_change(self):
        self.insert_thread()

        route = Route.objects.all()[0]
        basic_thread = route.rthread_set.get(type__code='basic')
        schedule_dates = [date(2015, 6, 23), date(2015, 6, 24)]

        mask = basic_thread.get_mask()
        assert all(mask[d] for d in schedule_dates)

        self.change_thread()

        basic_thread = route.rthread_set.get(type__code='basic')
        change_thread = route.rthread_set.get(type__code='change')
        change_date = date(2015, 6, 23)

        assert not basic_thread.get_mask()[change_date]
        assert change_thread.get_mask()[change_date]

        assert basic_thread.schedule_plan == self.schedule_plan
        assert change_thread.schedule_plan == basic_thread.schedule_plan

    def test_copy_facilities(self):
        velik_facility = SuburbanFacility.objects.create(title_ru='Велик', code='velik')

        self.insert_thread()

        route = Route.objects.all()[0]
        basic_thread = route.rthread_set.get(type__code='basic')

        thread_el = ElementTree.fromstring("""
            <thread t_type="suburban" thread="{}" changemode="change_facilities">
              <facilities>
                <facility_period period_start="2015-01-01" period_end="2015-12-31" weektemplate="1234567" facilities="velik"/>
              </facilities>
            </thread>
            """.format(basic_thread.uid))

        suburban.process_thread(parse_thread(thread_el, default_t_type_code='suburban'),
                                thread_el, 'test.xml', environment.today())

        basic_thread.refresh_from_db()
        basic_thread.suburbanthreadfacility_set.all()[0].facilities.all()[0] == velik_facility

        self.change_thread()

        change_thread = route.rthread_set.get(type__code='change')
        change_thread.suburbanthreadfacility_set.all()[0].facilities.all()[0] == velik_facility


