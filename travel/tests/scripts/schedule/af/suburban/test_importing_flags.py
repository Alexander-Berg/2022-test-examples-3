# coding: utf-8

from __future__ import unicode_literals

from xml.etree import ElementTree

import freezegun
import pytest
from hamcrest import has_properties, assert_that

from common.models.schedule import RTStation, RThread, RThreadType
from common.models.transport import TransportType
from travel.rasp.library.python.common23.date import environment
from travel.rasp.admin.scripts.schedule.af_processors import suburban
from travel.rasp.admin.scripts.schedule.af_processors.utils import parse_thread
from tester.factories import create_station
from tester.transaction_context import transaction_fixture


@pytest.fixture(autouse=True)
@transaction_fixture
def create_stations(request):
    create_station(__={'codes': {'esr': '100001'}}, t_type=TransportType.TRAIN_ID)
    create_station(__={'codes': {'esr': '100002'}}, t_type=TransportType.TRAIN_ID,
                   is_searchable_from=False, is_searchable_to=False, in_station_schedule=False)


@pytest.mark.dbuser
@freezegun.freeze_time('2015-06-01')
def test_insert():
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

    rtstations = RTStation.objects.filter(thread__number='3333').order_by('id')

    assert_that(rtstations[0], has_properties({
        'in_station_schedule': False,
        'is_searchable_from': True,
        'is_searchable_to': False,
    }))
    assert_that(rtstations[1], has_properties({
        'in_station_schedule': True,
        'is_searchable_from': False,
        'is_searchable_to': False,
    }))


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
                dates="2015-06-25" changemode="change">
          <stations>
            <station esrcode="100001" stname="1"
                     arrival_time="" stop_time=""
                     departure_time="12:00" minutes_from_start="0" />
            <station esrcode="100002" stname="2"
                     arrival_time="12:40" stop_time="10"
                     departure_time="12:50" minutes_from_start="40" in_station_schedule="1" />
          </stations>
        </thread>""")

    suburban.process_thread(parse_thread(thread_el, default_t_type_code='suburban'),
                            thread_el, 'test.xml', environment.today())

    assert RThread.objects.filter(number='3333').count() == 2

    rtstations = RTStation.objects.filter(thread__number='3333',
                                          thread__type_id=RThreadType.BASIC_ID).order_by('id')
    assert_that(rtstations[0], has_properties({
        'in_station_schedule': True,
        'is_searchable_from': True,
        'is_searchable_to': True,
    }))
    assert_that(rtstations[1], has_properties({
        'in_station_schedule': False,
        'is_searchable_from': False,
        'is_searchable_to': False,
    }))

    rtstations = RTStation.objects.filter(thread__number='3333',
                                          thread__type_id=RThreadType.CHANGE_ID).order_by('id')
    assert_that(rtstations[0], has_properties({
        'in_station_schedule': True,
        'is_searchable_from': True,
        'is_searchable_to': True,
    }))
    assert_that(rtstations[1], has_properties({
        'in_station_schedule': True,
        'is_searchable_from': False,
        'is_searchable_to': False,
    }))
