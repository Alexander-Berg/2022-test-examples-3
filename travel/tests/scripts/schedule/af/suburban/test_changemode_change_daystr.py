# -*- coding: utf-8 -*-
from __future__ import absolute_import

import logging
from xml.etree import ElementTree
from StringIO import StringIO

import pytest
import freezegun

from common.models.schedule import RThread
from travel.rasp.library.python.common23.date import environment
from travel.rasp.admin.scripts.schedule.af_processors import suburban
from travel.rasp.admin.scripts.schedule.af_processors.utils import parse_thread
from tester.factories import create_thread
from tester.testcase import TestCase


@pytest.yield_fixture(scope='module', autouse=True)
def replace_now():
    with freezegun.freeze_time('2015-01-01'):
        yield


change_mode_change_daystr_xml = u"""
<?xml version="1.0" encoding="utf-8"?>
<channel>
  <thread t_type="suburban" thread="thread_uid" changemode="change_daystr" daystr="E"/>
</channel>
""".strip()


class TestChangeDayStrMode(TestCase):
    def test_change_daystr(self):
        thread = create_thread(t_type='suburban', uid=u'thread_uid')

        tree = ElementTree.parse(StringIO(change_mode_change_daystr_xml.encode('utf-8')))
        for thread_el in tree.findall('.//thread'):
            thread = parse_thread(thread_el, default_t_type_code='suburban')

            suburban.process_thread(thread, thread_el, 'test.xml', environment.today())

        thread = RThread.objects.get(uid=u'thread_uid')

        assert thread.template_text == 'E'
