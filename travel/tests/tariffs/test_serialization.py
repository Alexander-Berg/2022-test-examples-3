# -*- coding: utf-8 -*-

import pytest

from common.tester.factories import create_station
from travel.rasp.library.python.common23.date.environment import now_aware
from travel.rasp.morda_backend.morda_backend.tariffs.serialization import TariffsSegmentSchema
from travel.rasp.morda_backend.morda_backend.tariffs.train.base.models import TrainSegment


@pytest.mark.parametrize('number, show_number, expected_number', (
    ('NUMBER', True, 'NUMBER'),
    ('NUMBER', False, '')
))
@pytest.mark.dbuser
def test_tariffs_segment_schema_number(number, show_number, expected_number):
    segment = TrainSegment()
    segment.number = number
    segment.show_number = show_number
    segment.station_from = create_station()
    segment.station_to = create_station()
    segment.departure = now_aware()
    segment.arrival = now_aware()

    result, errors = TariffsSegmentSchema().dump(segment)
    assert not errors
    assert result['number'] == expected_number
