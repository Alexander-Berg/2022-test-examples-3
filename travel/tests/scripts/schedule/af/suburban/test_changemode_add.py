# -*- coding: utf-8 -*-
from __future__ import absolute_import

import logging
from datetime import date
from xml.etree import ElementTree
from StringIO import StringIO

import freezegun
import pytest

from common.models.geo import Station
from common.models.schedule import RThread, RThreadType
from travel.rasp.library.python.common23.date import environment
from travel.rasp.admin.scripts.schedule.af_processors import suburban
from travel.rasp.admin.scripts.schedule.af_processors.utils import parse_thread
from tester.factories import create_thread
from tester.testcase import TestCase


@pytest.yield_fixture(scope='module', autouse=True)
def replace_now():
    with freezegun.freeze_time('2015-01-01'):
        yield


change_mode_add_xml = u"""
<?xml version="1.0" encoding="utf-8"?>
<channel>
  <thread t_type="suburban" number="3333" dates="2015-06-24" changemode="add">
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


change_mode_add2_xml = u"""
<?xml version="1.0" encoding="utf-8"?>
<channel>
  <thread t_type="suburban" number="3333" dates="2015-06-25" changemode="add">
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


class TestAddMode(TestCase):
    def test_add(self):
        date_before_add = date(2015, 6, 23)
        first_add_date = date(2015, 6, 24)
        second_add_date = date(2015, 6, 25)

        station1 = Station.get_by_code('esr', '000001')
        station2 = Station.get_by_code('esr', '000002')
        station3 = Station.get_by_code('esr', '000003')

        thread = create_thread(t_type='suburban', number=u'3333', schedule_v1=[
            [None, 0, station1],
            [10, 20, station2],
            [30, None, station3]
        ])
        mask = thread.get_mask()
        mask[first_add_date] = False
        mask[second_add_date] = False
        thread.year_days = str(mask)
        thread.route.route_uid = thread.gen_route_uid()
        thread.gen_uid()
        thread.save()
        thread.route.save()

        mask = thread.get_mask()
        assert mask[date_before_add] and not mask[first_add_date] and not mask[second_add_date]

        tree = ElementTree.parse(StringIO(change_mode_add_xml.encode('utf-8')))
        for thread_el in tree.findall('.//thread'):
            thread = parse_thread(thread_el, default_t_type_code='suburban')

            suburban.process_thread(thread, thread_el, 'test.xml', environment.today())

        thread = RThread.objects.get(number=u'3333', type_id=RThreadType.BASIC_ID)
        mask = thread.get_mask()
        assert mask[date_before_add] and not mask[first_add_date] and not mask[second_add_date]

        add_thread = RThread.objects.get(number=u'3333', type_id=RThreadType.ASSIGNMENT_ID)
        mask = add_thread.get_mask()
        assert not mask[date_before_add] and mask[first_add_date] and not mask[second_add_date]

        tree = ElementTree.parse(StringIO(change_mode_add2_xml.encode('utf-8')))
        for thread_el in tree.findall('.//thread'):
            thread = parse_thread(thread_el, default_t_type_code='suburban')

            suburban.process_thread(thread, thread_el, 'test.xml', environment.today())

        thread = RThread.objects.get(number=u'3333', type_id=RThreadType.BASIC_ID)
        mask = thread.get_mask()
        assert mask[date_before_add] and not mask[first_add_date] and not mask[second_add_date]

        add_thread = RThread.objects.get(number=u'3333', type_id=RThreadType.ASSIGNMENT_ID)
        mask = add_thread.get_mask()
        assert not mask[date_before_add] and mask[first_add_date] and mask[second_add_date]
