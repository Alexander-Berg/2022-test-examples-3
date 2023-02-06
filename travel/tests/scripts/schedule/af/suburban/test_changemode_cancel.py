# coding: utf-8
from __future__ import absolute_import, unicode_literals

import json
from datetime import date
from xml.etree import ElementTree
from io import BytesIO

import freezegun
import httpretty
import pytest

import library.python.resource
from common.models.schedule import Route
from travel.rasp.library.python.common23.date import environment
from travel.rasp.admin.scripts.schedule.af_processors import suburban
from travel.rasp.admin.scripts.schedule.af_processors.utils import parse_thread
from tester.testcase import TestCase


@pytest.yield_fixture(scope='module', autouse=True)
def replace_now():
    with freezegun.freeze_time('2015-01-01'):
        yield


change_mode_insert_xml = """
<?xml version="1.0" encoding="utf-8"?>
<channel>
  <thread t_type="suburban" number="3333" startstationparent="000001"
          dates="2015-06-23;2015-06-24" changemode="insert" weektemplate="D">
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


change_mode_cancel_xml = """
<?xml version="1.0" encoding="utf-8"?>
<channel>
  <thread t_type="suburban" number="3333" startstationparent="000001"
          dates="2015-06-23" changemode="cancel">
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


@pytest.mark.usefixtures('http_allowed')
class TestCancelMode(TestCase):
    @httpretty.activate
    def test_insert_and_cancel(self):
        httpretty.register_uri(
            httpretty.GET,
            uri='https://calendar.yandex.ru/export/holidays.xml?start_date=2014-01-01&end_date=2014-12-31&country_id=225&out_mode=all',  # noqa
            content_type='text/xml; charset=utf-8',
            body=library.python.resource.find('tester/data/yandex_calendar_2014_rus.xml')
        )
        httpretty.register_uri(
            httpretty.GET,
            uri='https://calendar.yandex.ru/export/holidays.xml?start_date=2015-01-01&end_date=2015-12-31&country_id=225&out_mode=all',  # noqa
            content_type='text/xml; charset=utf-8',
            body=library.python.resource.find('tester/data/yandex_calendar_2015_rus.xml')
        )

        tree = ElementTree.parse(BytesIO(change_mode_insert_xml.encode('utf-8')))
        for thread_el in tree.findall('.//thread'):
            thread = parse_thread(thread_el, default_t_type_code='suburban')

            suburban.process_thread(thread, thread_el, 'test.xml', environment.today(),
                                    recount_schedule_on_the_fly=True)

        route = Route.objects.all()[0]

        basic_thread = route.rthread_set.get(type__code='basic')
        schedule_dates = [date(2015, 6, 23), date(2015, 6, 24)]

        mask = basic_thread.get_mask()
        assert all(mask[d] for d in schedule_dates)
        assert json.loads(basic_thread.translated_manual_days_texts)['0']['ru'] == 'ежедневно'
        assert json.loads(basic_thread.translated_except_texts)[1] == ''

        tree = ElementTree.parse(BytesIO(change_mode_cancel_xml.encode('utf-8')))
        for thread_el in tree.findall('.//thread'):
            thread = parse_thread(thread_el, default_t_type_code='suburban')

            suburban.process_thread(thread, thread_el, 'test.xml', environment.today(),
                                    recount_schedule_on_the_fly=True)

        basic_thread = route.rthread_set.get(type__code='basic')
        cancel_thread = route.rthread_set.get(type__code='cancel')
        cancel_date = date(2015, 6, 23)

        assert not basic_thread.get_mask()[cancel_date]
        assert cancel_thread.get_mask()[cancel_date]

        assert json.loads(basic_thread.translated_except_texts)[1] == '23.06'
        assert json.loads(basic_thread.translated_manual_days_texts)['0']['ru'] == 'ежедневно'
