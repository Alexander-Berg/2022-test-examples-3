# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest

from common.models.transport import TransportType
from common.tester.factories import create_station, create_thread
from travel.rasp.library.python.common23.date import environment
from travel.rasp.wizards.wizard_lib.cache import ThreadsCache


@pytest.mark.dbuser
def test_thread_predicate():
    departure_station = create_station()
    arrival_station = create_station()
    thread = create_thread(number='1234', t_type='suburban', schedule_v1=[
        [None, 0, departure_station],
        [10, None, arrival_station],
    ])

    suburban_cache = ThreadsCache(TransportType.SUBURBAN_ID)
    with suburban_cache.using_precache():
        assert next(suburban_cache.find_segments(
            departure_station,
            arrival_station,
            environment.now_aware(),
            thread_predicate=None
        ), None).thread.id == thread.id

        assert next(suburban_cache.find_segments(
            departure_station,
            arrival_station,
            environment.now_aware(),
            thread_predicate=lambda thread: thread.number == '1234'
        ), None).thread.id == thread.id

        assert next(suburban_cache.find_segments(
            departure_station,
            arrival_station,
            environment.now_aware(),
            thread_predicate=lambda thread: thread.number != '1234'
        ), None) is None
