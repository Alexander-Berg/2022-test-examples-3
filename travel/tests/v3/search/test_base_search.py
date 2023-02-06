# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytz
from datetime import date

import pytest
from hamcrest import assert_that, has_properties, contains_inanyorder

from common.tester.factories import create_station, create_settlement
from travel.rasp.api_public.tests.v3.search.helpers import RequestStub, QueryPointsStub, SegmentStub
from travel.rasp.api_public.api_public.v3.search.base_search import BaseSearch


pytestmark = [pytest.mark.dbuser]


def test_base_search():
    city1 = create_settlement()
    city2 = create_settlement()
    station1 = create_station(settlement=city1)
    station2 = create_station(settlement=city2)

    search = BaseSearch(
        RequestStub(),
        {"add_days_mask": False, "result_pytz": pytz.timezone("Europe/Moscow")},
        QueryPointsStub(station1, city2, date(2020, 12, 12))
    )

    assert search.national_version == "ru_RU"
    assert search.add_days_mask is False
    assert search.result_pytz.zone == "Europe/Moscow"

    assert search.point_from.id == station1.id
    assert search.point_to.id == city2.id
    assert search.departure_date == date(2020, 12, 12)

    assert search.used_points == set()
    assert search.segments == []
    assert search.interval_segments == []

    search.add_segment_used_points(SegmentStub(station1, station2))

    assert len(search.used_points) == 4
    assert_that(search.used_points, contains_inanyorder(
        has_properties({"id": city1.id}),
        has_properties({"id": city2.id}),
        has_properties({"id": station1.id}),
        has_properties({"id": station2.id}),
    ))
