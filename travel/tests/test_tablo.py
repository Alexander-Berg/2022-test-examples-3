# coding: utf-8

from datetime import datetime, timedelta

import pytest

from common.models.transport import TransportType
from common.tester.factories import get_model_factory, create_rthread_segment, create_thread
from common.utils.date import FuzzyDateTime
from route_search.tablo import add_z_tablos_to_segments
from route_search.tests.utils import has_stationschedule


@has_stationschedule
@pytest.mark.dbuser
@pytest.mark.parametrize('t_type_id, expected_has_tablo', (
    (TransportType.PLANE_ID, True),
    (TransportType.HELICOPTER_ID, True),
    (TransportType.TRAIN_ID, False),
    (TransportType.SUBURBAN_ID, False),
    (TransportType.BUS_ID, False),
    (TransportType.WATER_ID, False),
))
def test_add_z_tablos_to_segments(t_type_id, expected_has_tablo):
    from stationschedule.models import ZTablo2
    create_ztablo2 = get_model_factory(ZTablo2)
    segments = []
    # убираем микросекунды, так как объекты ZTablo2 сохраняются в базу и теряют их,
    # а сегменты передаются в ф-цию как есть => ключи табло и сегментов не совпадают
    dtnow = datetime.now().replace(microsecond=0)
    for shift in range(5):
        departure_dt = dtnow + timedelta(shift)
        arrival_dt = dtnow + timedelta(shift + 1)
        segment = create_rthread_segment(
            thread=create_thread(t_type=t_type_id),
            departure=FuzzyDateTime(departure_dt),
            arrival=FuzzyDateTime(arrival_dt)
        )
        create_ztablo2(
            original_departure=departure_dt,
            station=segment.station_from,
            thread=segment.thread
        )
        create_ztablo2(
            original_arrival=arrival_dt,
            station=segment.station_to,
            thread=segment.thread
        )
        segments.append(segment)
    add_z_tablos_to_segments(segments)

    for segment in segments:
        if expected_has_tablo:
            assert isinstance(segment.arrival_z_tablo, ZTablo2)
            assert isinstance(segment.departure_z_tablo, ZTablo2)
        else:
            assert segment.arrival_z_tablo is None
            assert segment.departure_z_tablo is None
