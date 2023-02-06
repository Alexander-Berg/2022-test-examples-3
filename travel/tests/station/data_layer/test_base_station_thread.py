# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date, time

import mock
import pytest
import pytz
from hamcrest import has_entries, assert_that

from common.models.transport import TransportType
from common.tester.factories import (
    create_company, create_station, create_thread, create_transport_subtype, create_train_schedule_plan
)
from common.tester.utils.datetime import replace_now
from common.models.schedule import RThread
from stationschedule.views import station_schedule
from travel.rasp.morda_backend.morda_backend.station.data_layer.base_station_thread import StationRaspDBThread


pytestmark = [pytest.mark.dbuser]
create_thread = create_thread.mutate(__={'calculate_noderoute': True})


def _make_thread(station, event, date=None):
    if date:
        schedule = station_schedule(station, event=event, schedule_date=date, t_type_code='suburban')
        is_all_days = False
    else:
        schedule = station_schedule(station, event=event, t_type_code='suburban')
        is_all_days = True
    route = list(schedule.schedule_routes)[0]
    st_for_page = mock.Mock(
        page_context=mock.Mock(event=event, is_all_days=is_all_days),
        language='ru', country='ru',
        companies_by_ids={}
    )
    next_plan = create_train_schedule_plan()
    return StationRaspDBThread(route, st_for_page, next_plan)


@replace_now('2020-02-01')
def test_station_rasp_db_thread():
    tz = pytz.timezone('Etc/GMT-5')
    station0 = create_station(id=200, t_type=TransportType.TRAIN_ID, time_zone=tz)
    station1 = create_station(id=201, t_type=TransportType.TRAIN_ID, time_zone=tz)
    station2 = create_station(id=202, t_type=TransportType.TRAIN_ID)

    t_subtype = create_transport_subtype(
        t_type=TransportType.SUBURBAN_ID, code='vedro', title_ru='Ведро<br/>люкс -&nbsp;перелюкс')
    company = create_company(id=1000)

    create_thread(
        t_type=TransportType.SUBURBAN_ID,
        canonical_uid='canonicalUid',
        title='Станция0 - Станция2',
        number='222x',
        comment='ехать можно',
        express_type='express',
        t_subtype=t_subtype,
        company=company,
        year_days=[date(2020, 1, 29), date(2020, 2, 1), date(2020, 2, 3)],
        time_zone='Etc/GMT-3',
        tz_start_time=time(21, 0),
        schedule_v1=[[None, 0, station0], [50, 70, station1], [120, None, station2]]
    )

    thread = _make_thread(station1, 'departure', date(2020, 2, 2))

    assert thread.canonical_uid == 'canonicalUid'
    assert thread.departure_from.isoformat() == '2020-02-02T00:10:00+05:00'
    assert thread.event_date_and_time['time'] == '00:10'
    assert thread.event_date_and_time['datetime'].isoformat() == '2020-02-02T00:10:00+05:00'
    assert thread.t_type == 'suburban'
    assert thread.title == 'Станция0 - Станция2'
    assert thread.number == '222x'
    assert thread.comment == 'ехать можно'
    assert thread.company_id == 1000
    assert_that(thread.transport_subtype, has_entries({
        'code': 'vedro',
        'title': 'Ведро люкс - перелюкс'
    }))

    thread = _make_thread(station1, 'arrival', date(2020, 2, 1))

    assert thread.canonical_uid == 'canonicalUid'
    assert thread.departure == date(2020, 2, 1)
    assert thread.event_date_and_time['time'] == '23:50'
    assert thread.event_date_and_time['datetime'].isoformat() == '2020-02-01T23:50:00+05:00'
    assert thread.t_type == 'suburban'
    assert thread.title == 'Станция0 - Станция2'
    assert_that(thread.transport_subtype, has_entries({
        'code': 'vedro',
        'title': 'Ведро люкс - перелюкс'
    }))

    m_days_text = mock.Mock(side_effect=[
        {
            'days_text': 'run_days',
            'except_days_text': 'except_days'
        }
    ])
    with mock.patch.object(RThread, 'L_days_text_dict', m_days_text):
        thread = _make_thread(station1, 'departure')

        assert thread.canonical_uid == 'canonicalUid'
        assert thread.departure_from.isoformat() == '2020-02-02T00:10:00+05:00'
        assert thread.event_date_and_time['time'] == '00:10'
        assert 'datetime' not in thread.event_date_and_time
        assert thread.t_type == 'suburban'
        assert thread.title == 'Станция0 - Станция2'
        assert_that(thread.transport_subtype, has_entries({
            'code': 'vedro',
            'title': 'Ведро люкс - перелюкс'
        }))
        assert thread.days_text == 'только 30 января, 2, 4 февраля'
        assert thread.run_days_text == 'run_days'
        assert thread.except_days_text == 'except_days'

    thread = _make_thread(station1, 'arrival')

    assert thread.canonical_uid == 'canonicalUid'
    assert thread.departure == date(2020, 2, 1)
    assert thread.event_date_and_time['time'] == '23:50'
    assert 'datetime' not in thread.event_date_and_time
    assert thread.t_type == 'suburban'
    assert thread.title == 'Станция0 - Станция2'

    assert thread.days_text == 'только 29 января, 1, 3 февраля'
    assert thread.run_days_text == 'только 29 января, 1, 3 февраля'
    assert not hasattr(thread, 'except_days_text')
