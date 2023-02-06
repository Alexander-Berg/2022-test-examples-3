# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import date, timedelta

from common.models.currency import Price
from travel.rasp.wizards.proxy_api.lib.direction.aggregation import aggregate_direction_data
from travel.rasp.wizards.proxy_api.lib.direction.models import AggregatedDirectionData, DirectionData
from travel.rasp.wizards.proxy_api.lib.tests_utils import make_segment
from travel.rasp.wizards.wizard_lib.tests_utils import utc_dt


def test_aggregate_direction_data():
    segments = (
        make_segment(departure_local_dt=utc_dt(2000, 1, 1, 1), arrival_local_dt=utc_dt(2000, 1, 1, 12)),
        make_segment(departure_local_dt=utc_dt(2000, 1, 1, 2), arrival_local_dt=utc_dt(2000, 1, 1, 12)),
    )
    direction_data = DirectionData(segments=segments, total=100)

    assert aggregate_direction_data(direction_data) == AggregatedDirectionData(
        segments=segments,
        total=100,
        found_departure_date=date(2000, 1, 1),
        minimum_duration=timedelta(hours=10),
        minimum_price=None
    )


def test_aggregate_direction_data_minimum_price():
    segments = (
        make_segment(departure_local_dt=utc_dt(2000, 1, 1, 1), arrival_local_dt=utc_dt(2000, 1, 1, 12), price=Price(5)),
        make_segment(departure_local_dt=utc_dt(2000, 1, 1, 2), arrival_local_dt=utc_dt(2000, 1, 1, 12)),
        make_segment(departure_local_dt=utc_dt(2000, 1, 1, 3), arrival_local_dt=utc_dt(2000, 1, 1, 12), price=Price(3)),
    )
    direction_data = DirectionData(segments=segments, total=100)

    assert aggregate_direction_data(direction_data).minimum_price == Price(3)
