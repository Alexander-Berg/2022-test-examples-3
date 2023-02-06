# coding: utf-8

from __future__ import unicode_literals

from xml.etree import ElementTree

import freezegun
import pytest

from common.models.schedule import RThread, RThreadType
from common.models.transport import TransportType
from travel.rasp.library.python.common23.date import environment
from common.utils.title_generator import DASH
from travel.rasp.admin.scripts.schedule.af_processors import suburban
from travel.rasp.admin.scripts.schedule.af_processors.af_thread_parser import AfThreadParser
from travel.rasp.admin.scripts.schedule.af_processors.utils import parse_thread
from tester.factories import create_station
from tester.transaction_context import transaction_fixture


@pytest.fixture(autouse=True)
@transaction_fixture
def create_stations(request):
    create_station(__={'codes': {'esr': '100001'}}, t_type=TransportType.TRAIN_ID,
                   title='Откуда', title_ru='Откуда', title_uk='УкОткуда')
    create_station(__={'codes': {'esr': '100002'}}, t_type=TransportType.TRAIN_ID,
                   title='Куда', title_ru='Куда', title_uk='УкКуда')


@pytest.mark.dbuser
@freezegun.freeze_time('2015-06-01')
def test_insert_no_manual_title():
    thread_el = ElementTree.fromstring("""
    <thread t_type="suburban" number="3333" startstationparent="000001"
            dates="2015-06-25" changemode="insert">
      <stations>
        <station esrcode="100001" stname="1"
                 arrival_time="" stop_time=""
                 departure_time="12:00" minutes_from_start="0" in_station_schedule="0" is_searchable_to="0" />
        <station esrcode="100002" stname="2"
                 arrival_time="12:40" stop_time="10"
                 departure_time="12:50" minutes_from_start="40" in_station_schedule="1" />
      </stations>
    </thread>""")

    suburban.process_thread(parse_thread(thread_el, default_t_type_code='suburban'),
                            thread_el, 'test.xml', environment.today())

    thread = RThread.objects.get(number='3333')
    assert not thread.is_manual_title
    assert thread.L_title() == 'Откуда {} Куда'.format(DASH)


@pytest.mark.dbuser
@freezegun.freeze_time('2015-06-01')
def test_insert_manual_title():
    thread_el = ElementTree.fromstring("""
    <thread t_type="suburban" number="3333" startstationparent="000001"
            dates="2015-06-25" changemode="insert" is_manual_title="1" title="Ручное название">
      <stations>
        <station esrcode="100001" stname="1"
                 arrival_time="" stop_time=""
                 departure_time="12:00" minutes_from_start="0" in_station_schedule="0" is_searchable_to="0" />
        <station esrcode="100002" stname="2"
                 arrival_time="12:40" stop_time="10"
                 departure_time="12:50" minutes_from_start="40" in_station_schedule="1" />
      </stations>
    </thread>""".encode('utf-8'))

    suburban.process_thread(parse_thread(thread_el, default_t_type_code='suburban'),
                            thread_el, 'test.xml', environment.today())

    thread = RThread.objects.get(number='3333')
    assert thread.is_manual_title
    assert thread.L_title() == 'Ручное название'
    assert thread.L_title(lang='uk') == 'УкОткуда {} УкКуда'.format(DASH)


@pytest.mark.dbuser
@freezegun.freeze_time('2015-06-01')
@pytest.mark.parametrize('title_attr', AfThreadParser.TITLE_ATTRS)
def test_copy_title_attrs(title_attr):
    thread_el = ElementTree.fromstring("""
    <thread t_type="suburban" number="3333" startstationparent="000001"
            dates="2015-06-25" changemode="insert" is_manual_title="1" {0}="название-{0}">
      <stations>
        <station esrcode="100001" stname="1"
                 arrival_time="" stop_time=""
                 departure_time="12:00" minutes_from_start="0" in_station_schedule="0" is_searchable_to="0" />
        <station esrcode="100002" stname="2"
                 arrival_time="12:40" stop_time="10"
                 departure_time="12:50" minutes_from_start="40" in_station_schedule="1" />
      </stations>
    </thread>""".format(title_attr).encode('utf-8'))

    thread = parse_thread(thread_el, default_t_type_code='suburban')
    assert thread.is_manual_title
    assert getattr(thread, title_attr) == "название-{}".format(title_attr)


@pytest.mark.dbuser
@freezegun.freeze_time('2015-06-01')
def test_change():
    thread_el = ElementTree.fromstring("""
    <thread t_type="suburban" number="3333" startstationparent="100001"
            dates="2015-06-25;2015-06-26" changemode="insert">
      <stations>
        <station esrcode="100001" stname="1"
                 arrival_time="" stop_time=""
                 departure_time="12:00" minutes_from_start="0" />
        <station esrcode="100002" stname="2"
                 arrival_time="12:40" stop_time="10"
                 departure_time="12:50" minutes_from_start="40" />
      </stations>
    </thread>""")

    suburban.process_thread(parse_thread(thread_el, default_t_type_code='suburban'),
                            thread_el, 'test.xml', environment.today())

    thread_el = ElementTree.fromstring("""
        <thread t_type="suburban" number="3333" startstationparent="100001"
                dates="2015-06-25" changemode="change" is_manual_title="1" title="Название 1">
          <stations>
            <station esrcode="100001" stname="1"
                     arrival_time="" stop_time=""
                     departure_time="12:00" minutes_from_start="0" />
            <station esrcode="100002" stname="2"
                     arrival_time="12:40" stop_time="10"
                     departure_time="12:50" minutes_from_start="40" in_station_schedule="1" />
          </stations>
        </thread>""".encode('utf-8'))

    suburban.process_thread(parse_thread(thread_el, default_t_type_code='suburban'),
                            thread_el, 'test.xml', environment.today())

    thread = RThread.objects.get(number='3333', type_id=RThreadType.CHANGE_ID)
    assert thread.is_manual_title
    assert thread.title == 'Название 1'
