# -*- coding: utf-8 -*-
from __future__ import absolute_import

import logging
from datetime import date
from xml.etree import ElementTree
from StringIO import StringIO

from common.models.schedule import RThread, Company
from travel.rasp.admin.scripts.schedule.af_processors import common
from travel.rasp.admin.scripts.schedule.af_processors.utils import parse_thread, RZD_COMPANY_ID
from tester.testcase import TestCase


change_mode_insert_xml = u"""
<?xml version="1.0" encoding="utf-8"?>
<channel>
  <thread t_type="suburban" number="3333" startstationparent="000001"
          dates="2015-06-23;2015-06-24" changemode="insert" career="{}">
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


class TestGetCompany(TestCase):
    def test_good_company(self):
        company = Company.objects.create(id=1, title=u'Компания')
        tree = ElementTree.parse(StringIO(change_mode_insert_xml.format(1).encode('utf-8')))
        for thread_el in tree.findall('.//thread'):
            thread = parse_thread(thread_el, default_t_type_code='suburban')

            common.process_thread(thread, 'test.xml')

        thread = RThread.objects.get(number=3333)
        assert thread.company == company

    def test_bad_company(self):
        tree = ElementTree.parse(StringIO(change_mode_insert_xml.format(1).encode('utf-8')))
        for thread_el in tree.findall('.//thread'):
            thread = parse_thread(thread_el, default_t_type_code='suburban')

            common.process_thread(thread, 'test.xml')

        thread = RThread.objects.get(number=3333)
        assert thread.company is None

    def test_empty_company(self):
        tree = ElementTree.parse(StringIO(change_mode_insert_xml.format('').encode('utf-8')))
        for thread_el in tree.findall('.//thread'):
            thread = parse_thread(thread_el, default_t_type_code='suburban')

            common.process_thread(thread, 'test.xml')

        thread = RThread.objects.get(number=3333)
        assert thread.company is None

    def test_default_company(self):
        tree = ElementTree.parse(StringIO(change_mode_insert_xml.format('').replace(' career=""', '').encode('utf-8')))
        for thread_el in tree.findall('.//thread'):
            thread = parse_thread(thread_el, default_t_type_code='suburban')

            common.process_thread(thread, 'test.xml')

        thread = RThread.objects.get(number=3333)
        assert thread.company.id == RZD_COMPANY_ID


