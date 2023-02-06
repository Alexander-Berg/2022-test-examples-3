# coding: utf8
from datetime import date

import pytest
from django.db import connection
from django.test.utils import CaptureQueriesContext

from common.apps.facility.factories import create_suburban_facility
from common.apps.facility.models import SuburbanThreadFacility
from common.apps.facility.shortcuts import fill_suburban_facilities, get_suburban_thread_facilities
from common.models.transport import TransportType
from common.tester.factories import create_thread
from common.utils.date import RunMask


class ScheduleObject(object):
    """Тестовый объект, по интефрейсу подходящий для fill_suburban_facilities. """
    def __init__(self, thread, start_date):
        self.thread = thread
        self.start_date = start_date
        self.suburban_facilities = None


def get_start_date(sched_obj):
    return sched_obj.start_date


@pytest.mark.dbuser
def test_add_facility_query_count():
    lezhanki_facility = create_suburban_facility()
    spalniki_facility = create_suburban_facility()

    def create_segment_with_facility(facility):
        thread = create_thread(t_type=TransportType.SUBURBAN_ID)
        thread_facility = SuburbanThreadFacility.objects.create(year_days=RunMask.ALL_YEAR_DAYS, thread=thread)
        thread_facility.facilities.add(facility)
        segment = ScheduleObject(thread=thread, start_date=date(2016, 3, 10))

        return segment

    segment_lazhanki = create_segment_with_facility(lezhanki_facility)
    segment_spalniki = create_segment_with_facility(spalniki_facility)

    with CaptureQueriesContext(connection) as captured_queries:
        fill_suburban_facilities([segment_lazhanki, segment_spalniki], get_start_date)

        assert segment_lazhanki.suburban_facilities == [lezhanki_facility]
        assert segment_spalniki.suburban_facilities == [spalniki_facility]

    assert len(captured_queries) == 2


@pytest.mark.dbuser
def test_facility_order():
    lezhanki_facility = create_suburban_facility(order=1)
    spalniki_facility = create_suburban_facility(order=2)
    eda_facility = create_suburban_facility(order=3)

    def create_segment_with_facilitis(facilitis):
        thread = create_thread(t_type=TransportType.SUBURBAN_ID)
        thread_facility = SuburbanThreadFacility.objects.create(year_days=RunMask.ALL_YEAR_DAYS, thread=thread)
        for facility in facilitis:
            thread_facility.facilities.add(facility)
        segment = ScheduleObject(thread=thread, start_date=date(2016, 3, 10))

        return segment

    segment = create_segment_with_facilitis((spalniki_facility, lezhanki_facility, eda_facility))

    with CaptureQueriesContext(connection) as captured_queries:
        fill_suburban_facilities([segment], get_start_date)

        assert segment.suburban_facilities == [lezhanki_facility, spalniki_facility, eda_facility]

    assert len(captured_queries) == 2


@pytest.mark.dbuser
def test_empty_facilities():
    segment = ScheduleObject(
        thread=create_thread(t_type=TransportType.SUBURBAN_ID),
        start_date=date(2016, 3, 10))

    fill_suburban_facilities([segment], get_start_date)
    assert segment.suburban_facilities is None


@pytest.mark.dbuser
def test_another_day_facilities():
    facility = create_suburban_facility()
    thread = create_thread(t_type=TransportType.SUBURBAN_ID)
    thread_facility = SuburbanThreadFacility.objects.create(year_days=str(RunMask(days=[date(2016, 3, 10)])),
                                                            thread=thread)
    thread_facility.facilities.add(facility)

    sched_obj = ScheduleObject(thread=thread, start_date=date(2016, 3, 11))
    fill_suburban_facilities([sched_obj], get_start_date)
    assert sched_obj.suburban_facilities is None

    sched_obj = ScheduleObject(thread=thread, start_date=date(2016, 3, 10))
    fill_suburban_facilities([sched_obj], get_start_date)
    assert sched_obj.suburban_facilities == [facility]


@pytest.mark.dbuser
def test_get_suburban_thread_facilities():
    facility1 = create_suburban_facility(order=1)
    facility2 = create_suburban_facility(order=2)
    thread = create_thread(t_type=TransportType.SUBURBAN_ID)
    thread_facility = SuburbanThreadFacility.objects.create(
        year_days=str(RunMask(days=[date(2019, 7, 1)])), thread=thread
    )
    thread_facility.facilities.add(facility2)
    thread_facility.facilities.add(facility1)

    facilities = get_suburban_thread_facilities(thread.id, date(2019, 7, 1))
    assert facilities[0] == facility1
    assert facilities[1] == facility2

    assert get_suburban_thread_facilities(thread.id, date(2019, 7, 2)) is None
