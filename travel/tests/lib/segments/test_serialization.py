# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import date

import pytest

from common.tester.factories import create_station, create_transport_subtype
from travel.rasp.wizards.proxy_api.lib.segments.serialization import dump_segments
from travel.rasp.wizards.proxy_api.lib.tests_utils import make_segment
from travel.rasp.wizards.wizard_lib.serialization.thread_express_type import ThreadExpressType
from travel.rasp.wizards.wizard_lib.tests_utils import utc_dt


@pytest.mark.dbuser
def test_dump_segments():
    departure_station = create_station(title='departure_station_title', popular_title='departure_station_popular_title')
    arrival_station = create_station(title='arrival_station_title', popular_title='arrival_station_popular_title')
    transport_subtype = create_transport_subtype(t_type='suburban', title_ru='Подтип')

    assert dump_segments(
        (
            make_segment(
                departure_station=departure_station,
                arrival_station=arrival_station,
                departure_local_dt=utc_dt(2000, 1, 1, 1),
                arrival_local_dt=utc_dt(2000, 1, 1, 12),
                thread=dict(
                    number='some_number',
                    title='some_title',
                    start_date=date(2000, 1, 1),
                    transport_subtype=transport_subtype,
                    express_type=ThreadExpressType.EXPRESS,
                ),
            ),
            make_segment(
                departure_station=departure_station,
                arrival_station=arrival_station,
                departure_local_dt=utc_dt(2000, 1, 2, 1),
                arrival_local_dt=utc_dt(2000, 1, 2, 12),
                thread=dict(
                    number='some_number',
                    title='some_title',
                    start_date=date(2000, 2, 1),
                    transport_subtype=None,
                    express_type=None,
                ),
            ),
        )
    ) == (
        {
            'arrival': '2000-01-01 12:00:00 +0000',
            'arrival_station': 'arrival_station_popular_title',
            'departure': '2000-01-01 01:00:00 +0000',
            'departure_station': 'departure_station_popular_title',
            'duration': 660.0,
            'express': True,
            'from_station': 'от departure_station_popular_title',
            'to_station': 'до arrival_station_popular_title',
            'number': 'some_number',
            'title': 'some_title',
            'transport_subtype_title': 'Подтип',
        },
        {
            'arrival': '2000-01-02 12:00:00 +0000',
            'arrival_station': 'arrival_station_popular_title',
            'departure': '2000-01-02 01:00:00 +0000',
            'departure_station': 'departure_station_popular_title',
            'duration': 660.0,
            'from_station': 'от departure_station_popular_title',
            'to_station': 'до arrival_station_popular_title',
            'number': 'some_number',
            'title': 'some_title',
            'transport_subtype_title': None,
        },
    )
