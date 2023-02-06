# -*- coding: utf-8 -*-
from __future__ import absolute_import

import logging
from copy import copy
from datetime import date
from xml.etree import ElementTree
from StringIO import StringIO

import freezegun
import pytest

from common.models.geo import Station
from common.models.schedule import RThread, RThreadType, Supplier
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


change_mode_restore_xml = u"""
<?xml version="1.0" encoding="utf-8"?>
<channel>
  <thread number="6013" startstationparent="000001" supplier="af"
  dates="2015-06-20;2015-06-21;2015-06-22" changemode="restore"/>
</channel>
""".strip()


change_mode_restore_by_uid_xml_template = u"""
<?xml version="1.0" encoding="utf-8"?>
<channel>
  <thread thread="{}" dates="2015-06-20;2015-06-21;2015-06-22" changemode="restore"/>
</channel>
""".strip()


class TestRestoreMode(TestCase):
    def test_restore(self):
        date_before_restore = date(2015, 6, 20)
        first_restore_date = date(2015, 6, 21)
        second_restore_date = date(2015, 6, 22)

        station1 = Station.get_by_code('esr', '000001')
        station2 = Station.get_by_code('esr', '000002')
        station3 = Station.get_by_code('esr', '000003')

        supplier = Supplier.objects.get(code='af')
        thread = create_thread(t_type='suburban', number=u'6013', supplier=supplier, schedule_v1=[
            [None, 0, station1],
            [10, 20, station2],
            [30, None, station3]
        ])
        mask = thread.get_mask()
        mask[first_restore_date] = False
        mask[second_restore_date] = False
        thread.year_days = str(mask)
        thread.route.route_uid = thread.gen_route_uid(use_start_station=True)
        thread.gen_uid()
        thread.save()
        thread.route.save()

        cancel_thread = copy(thread)
        cancel_thread.id = None
        cancel_thread.type = RThreadType.objects.get(id=RThreadType.CANCEL_ID)
        cancel_thread.basic_thread = thread
        cancel_thread.year_days = str(RunMask(days=[first_restore_date]))
        cancel_thread.ordinal_number += 1
        cancel_thread.gen_import_uid()
        cancel_thread.gen_uid()
        cancel_thread.save()

        change_thread = copy(thread)
        change_thread.id = None
        change_thread.type = RThreadType.objects.get(id=RThreadType.CHANGE_ID)
        change_thread.basic_thread = thread
        change_thread.year_days = str(RunMask(days=[second_restore_date]))
        change_thread.ordinal_number += 2
        change_thread.gen_import_uid()
        change_thread.gen_uid()
        change_thread.save()

        tree = ElementTree.parse(StringIO(change_mode_restore_xml.encode('utf-8')))
        for thread_el in tree.findall('.//thread'):
            thread = parse_thread(thread_el, default_t_type_code='suburban')

            suburban.process_thread(thread, thread_el, 'test.xml', environment.today())

        thread = RThread.objects.get(number=u'6013', type_id=RThreadType.BASIC_ID)
        mask = thread.get_mask()
        assert mask[date_before_restore] and mask[first_restore_date] and mask[second_restore_date]

        cancel_thread = RThread.objects.get(number=u'6013', type_id=RThreadType.CANCEL_ID)
        assert cancel_thread.year_days == RunMask.EMPTY_YEAR_DAYS

        change_thread = RThread.objects.get(number=u'6013', type_id=RThreadType.CHANGE_ID)
        assert change_thread.year_days == RunMask.EMPTY_YEAR_DAYS

    def test_restore_by_uid(self):
        date_before_restore = date(2015, 6, 20)
        first_restore_date = date(2015, 6, 21)
        second_restore_date = date(2015, 6, 22)

        thread = create_thread(t_type='suburban', number=u'6013')
        mask = thread.get_mask()
        mask[first_restore_date] = False
        mask[second_restore_date] = False
        thread.year_days = str(mask)
        thread.save()

        cancel_thread = create_thread(
            t_type='suburban', number=u'6013', basic_thread=thread,
            type=RThreadType.objects.get(id=RThreadType.CANCEL_ID),
            year_days=str(RunMask(days=[first_restore_date, second_restore_date]))
        )

        change_mode_restore_by_uid_xml = change_mode_restore_by_uid_xml_template.format(cancel_thread.uid)
        tree = ElementTree.parse(StringIO(change_mode_restore_by_uid_xml.encode('utf-8')))
        for thread_el in tree.findall('.//thread'):
            thread = parse_thread(thread_el, default_t_type_code='suburban')

            suburban.process_thread(thread, thread_el, 'test.xml', environment.today())

        thread = RThread.objects.get(number=u'6013', type_id=RThreadType.BASIC_ID)
        mask = thread.get_mask()
        assert mask[date_before_restore] and mask[first_restore_date] and mask[second_restore_date]

        cancel_thread = RThread.objects.get(number=u'6013', type_id=RThreadType.CANCEL_ID)
        assert cancel_thread.year_days == RunMask.EMPTY_YEAR_DAYS
