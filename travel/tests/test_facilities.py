# coding: utf8
from datetime import date

import pytest

from common.apps.facility.factories import create_suburban_facility
from common.apps.facility.models import SuburbanThreadFacility
from common.models.transport import TransportType
from common.tester.factories import create_thread, create_rthread_segment
from common.utils.date import RunMask
from route_search.facilities import fill_suburban_facilities


@pytest.mark.dbuser
def test_fill_suburban_facilities():
    """
    Проверяем работоспособность partial'а в route_search.facilities.fill_suburban_facilities
    """
    facility = create_suburban_facility()
    thread = create_thread(t_type=TransportType.SUBURBAN_ID)
    thread_facility = SuburbanThreadFacility.objects.create(year_days=str(RunMask(days=[date(2016, 3, 10)])),
                                                            thread=thread)
    thread_facility.facilities.add(facility)
    segment = create_rthread_segment(thread=thread, calculated_start_date=date(2016, 3, 10))
    fill_suburban_facilities([segment])
    assert segment.suburban_facilities == [facility]
