# -*- coding: utf-8 -*-
from __future__ import absolute_import

import logging
from datetime import date
from xml.etree import ElementTree
from StringIO import StringIO

import freezegun
import pytest

from common.models.geo import Station
from common.models.schedule import Route, RThread
from common.models.transport import TransportType, TransportSubtype
from travel.rasp.library.python.common23.date import environment
from travel.rasp.admin.scripts.schedule.af_processors import suburban
from travel.rasp.admin.scripts.schedule.af_processors.utils import parse_thread
from tester.testcase import TestCase
from tester.factories import create_thread
from __tests__.scripts.schedule.af.utils import process_xml_thread


@pytest.yield_fixture(scope='module', autouse=True)
def replace_now():
    with freezegun.freeze_time('2015-01-01'):
        yield


change_mode_insert_xml = u"""
<?xml version="1.0" encoding="utf-8"?>
<channel>
  <thread t_type="suburban" number="3333" startstationparent="000001"
          dates="2015-06-23;2015-06-24" changemode="insert">
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


change_mode_insert2_xml = u"""
<?xml version="1.0" encoding="utf-8"?>
<channel>
  <thread t_type="suburban" number="3333" startstationparent="000001"
          dates="2015-06-25" changemode="insert">
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


class TestInsertMode(TestCase):
    def test_insert_and_insert(self):
        tree = ElementTree.parse(StringIO(change_mode_insert_xml.encode('utf-8')))
        for thread_el in tree.findall('.//thread'):
            thread = parse_thread(thread_el, default_t_type_code='suburban')

            suburban.process_thread(thread, thread_el, 'test.xml', environment.today())

        tree = ElementTree.parse(StringIO(change_mode_insert2_xml.encode('utf-8')))
        for thread_el in tree.findall('.//thread'):
            thread = parse_thread(thread_el, default_t_type_code='suburban')

            suburban.process_thread(thread, thread_el, 'test.xml', environment.today())

        route = Route.objects.all()[0]

        basic_thread = route.rthread_set.get(type__code='basic')
        schedule_dates = [date(2015, 6, 23), date(2015, 6, 24), date(2015, 6, 25)]

        mask = basic_thread.get_mask()
        assert all(mask[d] for d in schedule_dates)

    def test_insert_into_existed_route_with_different_thread(self):
        station1 = Station.get_by_code('esr', '000001')
        station2 = Station.get_by_code('esr', '000002')
        station3 = Station.get_by_code('esr', '000003')

        thread = create_thread(supplier='af', t_type='suburban', number=u'3333', schedule_v1=[
            [None, 0, station1],
            [20, 30, station2],
            [40, None, station3],
        ])
        thread.route.route_uid = thread.gen_route_uid(use_start_station=True)
        thread.gen_uid()
        thread.save()
        thread.route.save()

        tree = ElementTree.parse(StringIO(change_mode_insert_xml.encode('utf-8')))
        for thread_el in tree.findall('.//thread'):
            thread = parse_thread(thread_el, default_t_type_code='suburban')

            suburban.process_thread(thread, thread_el, 'test.xml', environment.today())

        assert thread.route.rthread_set.count() == 2

    def test_subtype(self):
        thread_xml = """
  <thread t_type="suburban" number="3333" startstationparent="000001"
          dates="2015-06-25" changemode="insert" express_subtype="vihr">
    <stations>
      <station esrcode="000001" stname="1"
               arrival_time="" stop_time=""
               departure_time="12:00" minutes_from_start="0" />
      <station esrcode="000002" stname="2"
               arrival_time="12:40" stop_time="10"
               departure_time="12:50" minutes_from_start="40" />
    </stations>
  </thread>
        """.strip()

        t_subtype = TransportSubtype.objects.create(t_type_id=TransportType.SUBURBAN_ID, code='vihr',
                                                    title_ru=u'Вихрь')

        process_xml_thread(thread_xml)
        thread = RThread.objects.get(number='3333')
        assert thread.t_subtype == t_subtype

