# -*- coding: utf-8 -*-
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date
from xml.etree import ElementTree
from StringIO import StringIO

import freezegun
import pytest

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


change_mode_delete_xml_by_uid = """
<?xml version="1.0" encoding="utf-8"?>
<channel>
  <thread t_type="suburban" number="3333" period_start="2015-06-24" changemode="delete" thread="test_uid" />
</channel>
""".strip()


change_mode_delete_xml_by_canonical_uid = """
<?xml version="1.0" encoding="utf-8"?>
<channel>
  <thread t_type="suburban" number="3333" period_start="2015-06-24" changemode="delete" canonical="canonical_uid" threaddate="2015-06-24" />
</channel>
""".strip()


class TestDeleteMode(TestCase):
    def test_insert_and_delete_thread_bu_uid(self):
        date_before_delete = date(2015, 6, 23)
        first_delete_date = date(2015, 6, 24)

        thread = create_thread(t_type='suburban', uid='test_uid')
        mask = thread.get_mask()
        assert mask[date_before_delete] and mask[first_delete_date]

        tree = ElementTree.parse(StringIO(change_mode_delete_xml_by_uid.encode('utf-8')))
        for thread_el in tree.findall('.//thread'):
            thread = parse_thread(thread_el, default_t_type_code='suburban')

            suburban.process_thread(thread, thread_el, 'test.xml', environment.today())

        thread = RThread.objects.get(uid='test_uid')
        mask = thread.get_mask()
        assert not mask[first_delete_date]
        assert mask[date_before_delete]


    def test_insert_and_delete_thread_bu_canonical_uid(self):
        date_before_delete = date(2015, 6, 23)
        first_delete_date = date(2015, 6, 24)

        thread = create_thread(t_type='suburban', uid='test_uid', canonical_uid=u'canonical_uid')
        mask = thread.get_mask()
        assert mask[date_before_delete] and mask[first_delete_date]

        create_thread(t_type='suburban', canonical_uid='canonical_uid', year_days=[date(2015, 6, 25)])

        tree = ElementTree.parse(StringIO(change_mode_delete_xml_by_uid.encode('utf-8')))
        for thread_el in tree.findall('.//thread'):
            thread = parse_thread(thread_el, default_t_type_code='suburban')

            suburban.process_thread(thread, thread_el, 'test.xml', environment.today())

        thread = RThread.objects.get(uid='test_uid')
        mask = thread.get_mask()
        assert not mask[first_delete_date]
        assert mask[date_before_delete]
