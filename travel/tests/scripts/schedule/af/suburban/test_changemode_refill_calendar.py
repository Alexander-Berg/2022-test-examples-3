# -*- coding: utf-8 -*-
from __future__ import absolute_import

import logging
from datetime import date, timedelta
from xml.etree import ElementTree
from StringIO import StringIO

import freezegun
import pytest

from common.models.schedule import RThread, RThreadType
from travel.rasp.library.python.common23.date import environment
from common.utils.date import RunMask
from travel.rasp.admin.scripts.schedule.af_processors import suburban
from travel.rasp.admin.scripts.schedule.af_processors.utils import parse_thread
from tester.factories import create_thread
from tester.testcase import TestCase


@pytest.yield_fixture(scope='module', autouse=True)
def replace_now():
    with freezegun.freeze_time('2015-01-01'):
        yield


change_mode_refill_calendar_xml = u"""
<?xml version="1.0" encoding="utf-8"?>
<channel>
  <thread thread="test_uid" period_start="2015-06-21" period_end="2015-06-25"
  changemode="refill_calendar" calendar_geobase_country_id="225" />
</channel>
""".strip()


class TestRefillCalendar(TestCase):
    def test_refill_calendar(self):
        start_date = date(2015, 6, 21)
        end_date = date(2015, 6, 25)
        odd = [date(2015, 6, 21), date(2015, 6, 23), date(2015, 6, 25)]
        even = [date(2015, 6, 22), date(2015, 6, 24)]

        thread = create_thread(t_type='suburban', uid=u'test_uid', template_code='E')

        tree = ElementTree.parse(StringIO(change_mode_refill_calendar_xml.encode('utf-8')))
        for thread_el in tree.findall('.//thread'):
            thread = parse_thread(thread_el, default_t_type_code='suburban')

            suburban.process_thread(thread, thread_el, 'test.xml', environment.today())

        thread = RThread.objects.get(uid=u'test_uid')
        mask = thread.get_mask()
        assert all([not mask[d] for d in odd])
        assert all([mask[d] for d in even])
        assert mask[start_date - timedelta(days=1)] and mask[end_date + timedelta(days=1)]

    def test_refill_calendar_with_cancel(self):
        start_date = date(2015, 6, 21)
        end_date = date(2015, 6, 25)
        in_dates = [date(2015, 6, 21), date(2015, 6, 22), date(2015, 6, 25)]
        out_dates = [date(2015, 6, 23), date(2015, 6, 24)]
        cancel_dates = RunMask.range(date(2015, 6, 23), date(2015, 6, 24), include_end=True)

        thread = create_thread(t_type='suburban', uid=u'test_uid', template_code='1234567', ordinal_number=1)
        cancel_thread = create_thread(t_type='suburban', uid=u'cancel_uid', ordinal_number=2,
                                      year_days=str(cancel_dates), basic_thread=thread, route=thread.route,
                                      type=RThreadType.CANCEL_ID)

        tree = ElementTree.parse(StringIO(change_mode_refill_calendar_xml.encode('utf-8')))
        for thread_el in tree.findall('.//thread'):
            thread = parse_thread(thread_el, default_t_type_code='suburban')

            suburban.process_thread(thread, thread_el, 'test.xml', environment.today())

        thread = RThread.objects.get(uid=u'test_uid')
        mask = thread.get_mask()
        assert all([not mask[d] for d in out_dates])
        assert all([mask[d] for d in in_dates])
        assert mask[start_date - timedelta(days=1)] and mask[end_date + timedelta(days=1)]
