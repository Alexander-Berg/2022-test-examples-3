# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date, datetime

import mock
import pytest

from common.apps.facility.factories import create_suburban_facility
from common.apps.facility.models import SuburbanThreadFacility
from common.models.transport import TransportType
from common.tester.factories import create_thread, create_rtstation, create_station
from common.utils.date import RunMask
from stationschedule.facilities import fill_suburban_facilities
from stationschedule.models import ScheduleRoute


@pytest.mark.dbuser
def test_fill_suburban_facilities():
    """ Проверяем работоспособность partial'а в stationschedule.facilities.fill_suburban_facilities """
    facility = create_suburban_facility()
    thread = create_thread(t_type=TransportType.SUBURBAN_ID)
    thread_facility = SuburbanThreadFacility.objects.create(year_days=str(RunMask(days=[date(2016, 3, 10)])),
                                                            thread=thread)
    thread_facility.facilities.add(facility)

    schedule_route = ScheduleRoute(
        mock.Mock(rtstation=create_rtstation(thread=thread, station=create_station())),
        event_dt=None,
        naive_start_dt=datetime(2016, 3, 10),
        arrival_dt=None,
        departure_dt=None)

    fill_suburban_facilities([schedule_route])
    assert schedule_route.suburban_facilities == [facility]
