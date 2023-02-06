# -*- coding: utf-8 -*-
from __future__ import absolute_import

import logging
from datetime import date
from xml.etree import ElementTree
from StringIO import StringIO

import freezegun
import pytest

from common.models.schedule import RThread, TrainSchedulePlan
from travel.rasp.library.python.common23.date import environment
from travel.rasp.admin.scripts.schedule.af_processors import suburban
from travel.rasp.admin.scripts.schedule.af_processors.utils import parse_thread
from tester.factories import create_thread
from tester.testcase import TestCase


@pytest.yield_fixture(scope='module', autouse=True)
def replace_now():
    with freezegun.freeze_time('2015-01-01'):
        yield


change_mode_change_schedule_plan_xml = u"""
<?xml version="1.0" encoding="utf-8"?>
<channel>
  <thread t_type="suburban" thread="thread_uid" changemode="change_schedule_plan" graph="g14"/>
</channel>
""".strip()


class TestChangeSchedulePlanMode(TestCase):
    def test_change_schedule_plan(self):
        TrainSchedulePlan.objects.create(
            title=u'2014',
            start_date=date(2015, 1, 1),
            end_date=date(2015, 12, 31),
            code='g14',
        )
        thread = create_thread(t_type='suburban', uid=u'thread_uid')

        tree = ElementTree.parse(StringIO(change_mode_change_schedule_plan_xml.encode('utf-8')))
        for thread_el in tree.findall('.//thread'):
            thread = parse_thread(thread_el, default_t_type_code='suburban')

            suburban.process_thread(thread, thread_el, 'test.xml', environment.today())

        thread = RThread.objects.all()[0]

        assert thread.schedule_plan
        assert 'g14' in thread.uid
