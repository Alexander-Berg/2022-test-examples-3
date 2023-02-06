# coding: utf-8

from __future__ import unicode_literals, absolute_import

from datetime import date
from xml.etree import ElementTree
from io import BytesIO

import freezegun
import pytest
from tester.testcase import TestCase

from common.models.schedule import RThread
from travel.rasp.admin.scripts.schedule.af_processors import common
from travel.rasp.admin.scripts.schedule.af_processors.utils import parse_thread


@pytest.yield_fixture(scope='module', autouse=True)
def replace_now():
    with freezegun.freeze_time('2015-01-01'):
        yield


change_mode_insert_xml = """
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


class TestInsertMode(TestCase):
    def test_insert_and_insert(self):
        tree = ElementTree.parse(BytesIO(change_mode_insert_xml.encode('utf-8')))
        for thread_el in tree.findall('.//thread'):
            thread = parse_thread(thread_el, default_t_type_code='suburban')
            common.process_thread(thread, 'test.xml')

        thread = RThread.objects.get(type__code='basic')
        schedule_dates = [date(2015, 6, 23), date(2015, 6, 24)]
        mask = thread.get_mask()
        assert all(mask[d] for d in schedule_dates)
        assert thread.import_uid == '3333_1101_4-1-82f9757801af73154b74519e81cb3994-e86fb5aceafe6f653f62cbcb3ba3ac47', (
            'Поменялась логика генерации import_uid! '
            'Правьте данный assert, только если логика генерации import_uid была изменена сознательно.'
        )

        original_import_uid = thread.import_uid
        assert original_import_uid == thread.gen_import_uid(), 'При перегенерации import_uid должен сохранятся'
