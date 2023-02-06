# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, timedelta, time

import pytest
import pytz
from mock import Mock, call, patch

from common.models.geo import Country, Region
from common.models.schedule import RThread, RTStation
from common.models.transport import TransportType
from common.tester.factories import create_rthread_segment,  create_station, create_country, create_region
from common.tester.utils.datetime import replace_now
from travel.rasp.library.python.common23.date import environment
from route_search.models import IntervalRThreadSegment

from travel.rasp.morda_backend.morda_backend.search.search.data_layer.backend import (
    calculate_days_by_tz, is_kaliningrad_train
)


def _id(val):
    if isinstance(val, basestring):
        return val


def _key(ttype_id, number):
    return u'{}.{}'.format(ttype_id, number),


@pytest.mark.dbuser
def test_calculate_days_by_tz():
    """
    Должен возвращать словарь с ключами - временными зонами и значением
    - словарем с описанием дней курсирования.
    """
    m_calc_days_shift = Mock(return_value=42)
    m_days_text_dict = Mock(side_effect=[
        {
            'days_text': '1',
            'except_days_text': 'one',
        },
        {
            'days_text': '2',
        },
        {
            'days_text': '3',
            'cancel': True
        }
    ])

    timezones = map(pytz.timezone, ['Europe/Moscow', 'Africa/Asmara', 'America/Phoenix'])
    next_plan = 'plan'

    with patch.object(RThread, 'L_days_text_dict', m_days_text_dict),\
            patch.object(RTStation, 'calc_days_shift', m_calc_days_shift),\
            replace_now('2016-01-01 0:00:00') as now:
        segment = create_rthread_segment(departure=environment.now_aware(), arrival=environment.now_aware())
        result = calculate_days_by_tz(segment, timezones, next_plan)
        assert result == {
            'Europe/Moscow': {
                'days_text': '1',
                'except_days_text': 'one',
            },
            'Africa/Asmara': {
                'days_text': '2',
            },
            'America/Phoenix': {
                'days_text': '3',
                'cancel': True
            }
        }

        assert m_calc_days_shift.call_count == 3
        m_calc_days_shift.assert_has_calls([
            call(event='departure', start_date=segment.calculated_start_date, event_tz=tz)
            for tz in timezones
        ])

        assert m_days_text_dict.call_count == 3
        m_days_text_dict.assert_has_calls([
            call(
                shift=42, thread_start_date=now.date(), next_plan=next_plan, show_days=True,
                force_recalculate_days_text=False
            )
            for _ in timezones
        ])


LATEST_DT = datetime.utcnow()
calculate_latest_fixtures = [
    (
        u'should return None if `context.nearest` and no segments',
        {'nearest': True},
        [],
        None
    ),
    (
        u'should return None if `context.nearest` and all segments are interval',
        {'nearest': True},
        [IntervalRThreadSegment(), IntervalRThreadSegment()],
        None
    ),
    (
        u'should return departure of the latest segment if `context.nearest`',
        {'nearest': True},
        [{'departure': LATEST_DT}],
        LATEST_DT
    ),
    (
        u'should return `when` + 4hours if `when` given',
        {'when': LATEST_DT},
        [{'departure': LATEST_DT}],
        datetime.combine(LATEST_DT, time()) + timedelta(days=1, hours=4)
    ),
    (
        u'all days search should return None',
        {},
        [],
        None
    ),
]


@pytest.mark.dbuser
def test_mark_kaliningrad_train():
    russia = Country.objects.get(id=Country.RUSSIA_ID)
    ukraine = create_country(id=Country.UKRAINE_ID)
    kaliningrad_region = create_region(id=Region.KALININGRAD_REGION_ID, country=russia)
    moskow_region = create_region(id=Region.MOSCOW_REGION_ID, country=russia)
    kaliningrad_station = create_station(region=kaliningrad_region, country=russia)
    moskow_station = create_station(region=moskow_region, country=russia)
    ukraine_station = create_station(country=ukraine)

    segment = create_rthread_segment(t_type=TransportType.objects.get(id=TransportType.TRAIN_ID),
                                     station_from=kaliningrad_station, station_to=moskow_station)
    assert is_kaliningrad_train(segment)

    segment = create_rthread_segment(t_type=TransportType.objects.get(id=TransportType.TRAIN_ID),
                                     station_from=moskow_station, station_to=kaliningrad_station)
    assert is_kaliningrad_train(segment)

    segment = create_rthread_segment(t_type=TransportType.objects.get(id=TransportType.TRAIN_ID),
                                     station_from=kaliningrad_station, station_to=kaliningrad_station)
    assert not is_kaliningrad_train(segment)

    segment = create_rthread_segment(t_type=TransportType.objects.get(id=TransportType.TRAIN_ID),
                                     station_from=kaliningrad_station, station_to=ukraine_station)
    assert not is_kaliningrad_train(segment)
