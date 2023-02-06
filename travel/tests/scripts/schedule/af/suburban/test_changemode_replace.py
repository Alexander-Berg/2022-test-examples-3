# -*- coding: utf-8 -*-
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date, time
from xml.etree import ElementTree
from StringIO import StringIO

import freezegun
import pytest

from common.models.geo import Station
from common.models.schedule import RThread, RThreadType
from travel.rasp.library.python.common23.date import environment

from travel.rasp.admin.lib.unittests.check_thread_mixin import CheckThreadMixin
from travel.rasp.admin.scripts.schedule.af_processors import suburban
from travel.rasp.admin.scripts.schedule.af_processors.utils import parse_thread
from tester.factories import create_thread, create_route
from tester.testcase import TestCase


@pytest.yield_fixture(scope='module', autouse=True)
def replace_now():
    with freezegun.freeze_time('2015-01-01'):
        yield


change_mode_replace_xml_template = """
<?xml version="1.0" encoding="utf-8"?>
<channel>
  <thread t_type="suburban" number="666" {} dates="2015-06-24" changemode="replace">
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


def _get_xml_with_uid(uid):
    uid_prop = 'thread="{}"'.format(uid)
    return change_mode_replace_xml_template.format(uid_prop)


def _get_xml_with_canonical_uid(canonical_uid, thread_date):
    canonical_uid_prop = 'canonical="{0}" threaddate="{1}"'.format(canonical_uid, thread_date)
    return change_mode_replace_xml_template.format(canonical_uid_prop)


create_thread = create_thread.mutate(t_type='suburban')
create_af_thread = create_thread.mutate(supplier='af')


class TestReplaceMode(TestCase, CheckThreadMixin):
    def _make_replaced_thread(self, use_canonical, schedule_v1):
        number = '777' if use_canonical else '666'
        thread = create_thread(
            number=number,
            schedule_v1=schedule_v1,
            type = RThreadType.BASIC_ID,
            year_days=[date(2015, 6, 24)],
        )
        thread.route.route_uid = thread.gen_route_uid(use_start_station=True)
        thread.gen_uid()
        thread.canonical_uid = 'R_{}'.format(thread.uid)
        thread.save()

        create_thread(
            number=number,
            schedule_v1=schedule_v1,
            year_days = [date(2015, 6, 25)],
            type=RThreadType.CHANGE_ID,
            uid='{}_2'.format(thread.uid),
            canonical_uid=thread.canonical_uid
        )

        if not use_canonical:
            change_mode_replace_xml = _get_xml_with_uid(thread.uid)
        else:
            change_mode_replace_xml = _get_xml_with_canonical_uid(thread.canonical_uid, date(2015, 6, 24))

        tree = ElementTree.parse(StringIO(change_mode_replace_xml.encode('utf-8')))
        for thread_el in tree.findall('.//thread'):
            thread = parse_thread(thread_el, default_t_type_code='suburban')
            suburban.process_thread(thread, thread_el, 'test.xml', environment.today())

        return RThread.objects.get(number=number, type_id=RThreadType.BASIC_ID)

    def test_replace_with_equal_path(self):
        station1 = Station.get_by_code('esr', '000001')
        station2 = Station.get_by_code('esr', '000002')
        station3 = Station.get_by_code('esr', '000003')
        schedule_v1 = [
            [None, 0, station1],
            [10, 20, station2],
            [30, None, station3]
        ]

        for use_canonical in [True, False]:
            thread = self._make_replaced_thread(use_canonical, schedule_v1)

            mask = thread.get_mask()
            assert mask[date(2015, 6, 24)]
            assert thread.tz_start_time == time(12, 0)
            self.assertThreadStopsTimes(thread, [
                [None, 0, 'Europe/Moscow'],
                [40, 50, 'Europe/Moscow'],
                [60, None, 'Europe/Moscow'],
            ])

    def test_replace_not_equal_path_but_equal_start_station(self):
        station1 = Station.get_by_code('esr', '000001')
        station2 = Station.get_by_code('esr', '000002')
        Station.get_by_code('esr', '000003')
        schedule_v1 = [
            [None, 0, station1],
            [10, None, station2],
        ]

        for use_canonical in [True, False]:
            thread = self._make_replaced_thread(use_canonical, schedule_v1)

            mask = thread.get_mask()
            assert mask[date(2015, 6, 24)]
            assert thread.tz_start_time == time(12, 0)
            self.assertThreadStopsTimes(thread, [
                [None, 0, 'Europe/Moscow'],
                [40, 50, 'Europe/Moscow'],
                [60, None, 'Europe/Moscow'],
            ])

    def test_replace_not_equal_path_and_not_equal_start_station(self):
        Station.get_by_code('esr', '000001')
        station2 = Station.get_by_code('esr', '000002')
        station3 = Station.get_by_code('esr', '000003')
        schedule_v1 = [
            [None, 0, station2],
            [10, None, station3],
        ]

        for use_canonical in [True, False]:
            thread = self._make_replaced_thread(use_canonical, schedule_v1)

            mask = thread.get_mask()
            assert mask[date(2015, 6, 24)]
            assert thread.tz_start_time == time(12, 0)
            self.assertThreadStopsTimes(thread, [
                [None, 0, 'Europe/Moscow'],
                [40, 50, 'Europe/Moscow'],
                [60, None, 'Europe/Moscow'],
            ])

    def test_replace_not_equal_path_and_not_equal_start_station_with_changes(self):
        self._test_replace_not_equal_path_and_not_equal_start_station_with_changes()

    def test_replace_not_equal_path_and_not_equal_start_station_with_changes_existed_new_route(self):
        station1 = Station.get_by_code('esr', '000001')
        station3 = Station.get_by_code('esr', '000003')

        thread = create_af_thread(hidden_number='another_existed_thread', number='666', schedule_v1=[
            [None, 0, station1],
            [10, None, station3],
        ])
        thread.route.route_uid = thread.gen_route_uid(use_start_station=True)
        thread.gen_uid()
        thread.save()
        thread.route.save()

        self._test_replace_not_equal_path_and_not_equal_start_station_with_changes()

    def _test_replace_not_equal_path_and_not_equal_start_station_with_changes(self):
        station2 = Station.get_by_code('esr', '000002')
        station3 = Station.get_by_code('esr', '000003')

        thread = create_af_thread(number='666', schedule_v1=[
            [None, 0, station2],
            [10, None, station3],
        ])
        thread.route.route_uid = thread.gen_route_uid(use_start_station=True)
        thread.gen_uid()
        thread.save()
        thread.route.save()
        old_route = thread.route

        change_thread = create_af_thread(number='666', schedule_v1=[
            [None, 0, station2],
            [40, None, station3],
        ], type=RThreadType.CHANGE_ID, ordinal_number=2, route=thread.route, basic_thread=thread)

        change_mode_replace_xml = _get_xml_with_uid(thread.uid)
        tree = ElementTree.parse(StringIO(change_mode_replace_xml.encode('utf-8')))
        for thread_el in tree.findall('.//thread'):
            thread = parse_thread(thread_el, default_t_type_code='suburban')

            suburban.process_thread(thread,  thread_el, 'test.xml', environment.today())

        new_thread = RThread.objects.exclude(hidden_number='another_existed_thread').get(number='666', type_id=RThreadType.BASIC_ID)
        change_thread = RThread.objects.get(pk=change_thread.id)

        assert new_thread.route != old_route
        assert change_thread.route == new_thread.route
