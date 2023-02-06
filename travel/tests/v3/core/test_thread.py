# coding: utf8

import mock
import pytest
from datetime import date, datetime, time, timedelta

from common.apps.suburban_events.api import EventStateType
from common.apps.suburban_events.factories import ThreadStationStateFactory
from common.data_api.platforms.instance import platforms as platforms_client
from common.data_api.platforms.serialization import PlatformData, PlatformKey, PlatformRecord
from common.models.transport import TransportType
from common.tester.factories import create_transport_subtype
from common.tester.utils.datetime import replace_now

from travel.rasp.export.export.v3.core.thread import (
    build_thread_rtstations, get_thread_on_date, get_thread_on_all_days, base_thread
)
from travel.rasp.export.tests.v3.factories import create_station, create_thread


pytestmark = [pytest.mark.dbuser('module'), pytest.mark.mongouser('module')]


class TestThreadCore(object):

    @replace_now(datetime(2001, 2, 3))
    def test_build_thread_rtstations(self):
        station_from = create_station(title='42', __={'codes': {'esr': '100'}})
        station_to = create_station(title='43', popular_title_ru='popular_43')
        station_mid = create_station(title='mid')
        thread_start_date = date(2001, 2, 3)
        thread = create_thread(
            tz_start_time=time(15, 10),
            year_days=[thread_start_date],
            number='123',
            __={'calculate_noderoute': True},
            schedule_v1=[[None, 2, station_from],
                         [10, 10, station_mid, {'platform': 'platform_mid'}],
                         [20, None, station_to, {'platform': 'platform_43'}]])
        platforms_client.update([
            PlatformRecord(
                key=PlatformKey(date=thread_start_date, station_id=station_from.id, train_number=thread.number),
                data=PlatformData(departure_platform='platform_1')
            )
        ])

        naive_start_dt = datetime.combine(thread_start_date, thread.tz_start_time)
        aware_start_dt = thread.pytz.localize(naive_start_dt)

        rtstations = build_thread_rtstations(thread.path, naive_start_dt, aware_start_dt, train_number=thread.number)
        assert len(rtstations) == 3
        assert rtstations[0] == {'is_combined': False,
                                 'title': '42',
                                 'departure_local': '2001-02-03T15:12:00+03:00',
                                 'departure': 2,
                                 'platform': 'platform_1',
                                 'esr': '100'}
        assert rtstations[1] == {'is_combined': False,
                                 'title': 'mid',
                                 'no_stop': True}
        assert rtstations[2] == {'arrival': 20,
                                 'is_combined': False,
                                 'title': '43',
                                 'arrival_local': '2001-02-03T15:30:00+03:00',
                                 'popular_title': 'popular_43',
                                 'platform': 'platform_43'}

        rtstations = build_thread_rtstations(thread.path, naive_start_dt, aware_start_dt, time_format='%H:%M')
        assert rtstations[0] == {'is_combined': False,
                                 'title': '42',
                                 'departure_local': '15:12',
                                 'departure': 2,
                                 'esr': '100'}

    @replace_now(datetime(2001, 2, 3))
    def test_get_thread_on_date(self):
        station_from = create_station(title='42')
        station_to = create_station(title='43')
        thread_start_dt = datetime(2001, 2, 3)
        thread = create_thread(
            canonical_uid='*thread_canonical_uid*',
            tz_start_time=time(15, 10),
            year_days=[thread_start_dt],
            number='1111/2222',
            __={'calculate_noderoute': True},
            schedule_v1=[[None, 2, station_from],
                         [20, None, station_to]])
        with mock.patch('travel.rasp.export.export.v3.core.thread.build_thread_rtstations',
                        return_value=mock.sentinel.mock_rtstations) as m_build_thread_rtstations:
            thread_data = get_thread_on_date(thread, thread.pytz.localize(thread_start_dt))
            assert thread_data == {'is_combined': False,
                                   'title': None,
                                   'canonical_uid': '*thread_canonical_uid*',
                                   'number': '1111/2222',
                                   'start_time': '2001-02-03T15:10:00+03:00',
                                   'stations': mock.sentinel.mock_rtstations,
                                   'start_time_msk': '2001-02-03T15:10:00+03:00',
                                   'days': u'только 3\xa0февраля',
                                   'stops': None}
            start_dt = datetime.combine(thread_start_dt.date(), thread.tz_start_time)
            rtstations = list(thread.path.select_related('station'))
            m_build_thread_rtstations.assert_called_once_with(
                rtstations, start_dt, thread.pytz.localize(start_dt), station_states={}, train_number='1111/2222')

            thread.number = '123'
            thread.title = 'thread_title'
            thread.save()

            thread_data = get_thread_on_date(thread, thread.pytz.localize(thread_start_dt))
            assert thread_data == {'is_combined': False,
                                   'title': 'thread_title',
                                   'number': '123',
                                   'canonical_uid': '*thread_canonical_uid*',
                                   'start_time': '2001-02-03T15:10:00+03:00',
                                   'stations': mock.sentinel.mock_rtstations,
                                   'start_time_msk': '2001-02-03T15:10:00+03:00',
                                   'days': u'только 3\xa0февраля',
                                   'stops': None}

    @replace_now(datetime(2001, 2, 5))
    def test_get_thread_on_all_days(self):
        station_from = create_station(title='42')
        station_to = create_station(title='43')
        thread_start_dt = datetime(2001, 2, 10)
        thread = create_thread(
            canonical_uid='*thread_canonical_uid*',
            tz_start_time=time(15, 10),
            year_days=[date(2001, 2, 4), thread_start_dt],
            __={'calculate_noderoute': True},
            schedule_v1=[[None, 2, station_from],
                         [20, None, station_to]])
        with mock.patch('travel.rasp.export.export.v3.core.thread.build_thread_rtstations',
                        return_value=mock.sentinel.mock_rtstations) as m_build_thread_rtstations:
            thread_data = get_thread_on_all_days(thread)
            assert thread_data == {'is_combined': False,
                                   'title': None,
                                   'canonical_uid': '*thread_canonical_uid*',
                                   'number': None,
                                   'start_time': '15:10',
                                   'stations': mock.sentinel.mock_rtstations,
                                   'start_time_msk': '15:10',
                                   'days': u'только 4, 10\xa0февраля',
                                   'stops': None}
            start_dt = datetime.combine(thread_start_dt.date(), thread.tz_start_time)
            rtstations = list(thread.path.select_related('station'))
            m_build_thread_rtstations.assert_called_once_with(
                rtstations, start_dt, thread.pytz.localize(start_dt), '%H:%M'
            )

    @replace_now(datetime(2001, 2, 3))
    def test_base_thread(self):
        station_from = create_station(title='42')
        station_to = create_station(title='43')
        thread_start_dt = datetime(2001, 2, 3)
        thread = create_thread(
            canonical_uid='*thread_canonical_uid*',
            number='242',
            title='242_title',
            express_type='aeroexpress',
            t_subtype=create_transport_subtype(t_type_id=TransportType.SUBURBAN_ID, code='bla', title_ru=u'sub_t_242'),
            is_combined=True,
            tz_start_time=time(15, 10),
            year_days=[thread_start_dt],
            __={'calculate_noderoute': True},
            schedule_v1=[[None, 2, station_from],
                         [20, None, station_to]])

        first_rtstation = thread.path[0]
        start_dt = datetime.combine(thread_start_dt.date(), thread.tz_start_time)
        thread_data = base_thread(thread, start_dt, first_rtstation)
        assert thread_data == {'days': u'только 3\xa0февраля',
                               'stops': None,
                               'is_combined': True,
                               'canonical_uid': '*thread_canonical_uid*',
                               'number': '242',
                               'start_time': '2001-02-03T15:10:00+03:00',
                               'start_time_msk': '2001-02-03T15:10:00+03:00',
                               'title': '242_title',
                               'transport': {'express_type': 'aeroexpress',
                                             'subtype': {'code': 'bla',
                                                         'color': None,
                                                         'title': u'sub_t_242'}}}

        thread_data = base_thread(thread, start_dt, first_rtstation, time_format='%H:%M')
        assert thread_data == {'days': u'только 3\xa0февраля',
                               'stops': None,
                               'is_combined': True,
                               'canonical_uid': '*thread_canonical_uid*',
                               'number': '242',
                               'start_time': '15:10',
                               'start_time_msk': '15:10',
                               'title': '242_title',
                               'transport': {'express_type': 'aeroexpress',
                                             'subtype': {'code': 'bla',
                                                         'color': None,
                                                         'title': u'sub_t_242'}}}

    @replace_now(datetime(2021, 4, 19))
    def test_get_thread_on_date_with_cancels(self):
        stations = [create_station(title='station_{}'.format(i)) for i in range(2)]
        thread_start_dt = datetime(2021, 4, 19, 15, 10)
        thread_uid = 'thread_uid'
        thread = create_thread(
            uid=thread_uid,
            tz_start_time=thread_start_dt.time(),
            year_days=[thread_start_dt.date()],
            schedule_v1=[
                [None, 5, stations[0]],
                [10, None, stations[1]]
            ])

        rtstations = list(thread.path)
        ThreadStationStateFactory.create_from_rtstation(
            rts=rtstations[0],
            thread_start_date=thread_start_dt,
            departure={
                'dt': thread_start_dt + timedelta(minutes=5),
                'type': EventStateType.CANCELLED,
                'thread_uid': thread_uid,
                'minutes_from': None,
                'minutes_to': None
            },
            passed_several_times=False
        )
        ThreadStationStateFactory.create_from_rtstation(
            rts=rtstations[1],
            thread_start_date=thread_start_dt,
            arrival={
                'dt': thread_start_dt + timedelta(minutes=10),
                'type': EventStateType.CANCELLED,
                'thread_uid': thread_uid,
                'minutes_from': None,
                'minutes_to': None
            },
            passed_several_times=False
        )

        thread_data = get_thread_on_date(thread, thread.pytz.localize(thread_start_dt), disable_cancels=True)
        assert thread_data['stations'][0]['state']['departure']['type'] == EventStateType.POSSIBLE_DELAY
        assert thread_data['stations'][1]['state']['arrival']['type'] == EventStateType.POSSIBLE_DELAY

        thread_data = get_thread_on_date(thread, thread.pytz.localize(thread_start_dt), disable_cancels=False)
        assert thread_data['stations'][0]['state']['departure']['type'] == EventStateType.CANCELLED
        assert thread_data['stations'][1]['state']['arrival']['type'] == EventStateType.CANCELLED
