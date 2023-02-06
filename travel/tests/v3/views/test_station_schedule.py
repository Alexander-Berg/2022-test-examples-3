# coding: utf8

from datetime import datetime, time, date

import pytz
from iso8601 import parse_date

from common.apps.facility.models import SuburbanThreadFacility
from common.models.schedule import RTStation
from common.utils.date import MSK_TZ, RunMask
from common.apps.facility.factories import create_suburban_facility

from common.tester.factories import create_station_schedule
from common.tester.testcase import TestCase
from common.tester.utils.datetime import replace_now

from travel.rasp.export.tests.v3.factories import create_station, create_thread
from travel.rasp.export.tests.v3.helpers import api_get_json, test_server_time_dt, check_server_time


class TestStationScheduleBase(TestCase):
    def setUp(self):
        self.esr_code = '123'
        self.station = create_station(__={'codes': {'esr': self.esr_code}})

    def test_direction_exists(self):
        """
        Проверяем, что элемент direction отсутствует, если реального направления нет.
        RASPEXPORT-94
        """
        create_thread(schedule_v1=[[None, 10, self.station],
                                   [11, None]])
        response = api_get_json('/v3/suburban/station_schedule/{}'.format(self.esr_code))
        threads = response['threads']
        assert len(threads) == 1
        assert threads[0].get('direction') is None

    @replace_now(test_server_time_dt)
    def test_server_time(self):
        response = api_get_json('/v3/suburban/station_schedule/{}'.format(self.esr_code))
        check_server_time(test_server_time_dt, response)

    @replace_now(test_server_time_dt)
    def test_segment_departure_utc(self):
        departure_time = time(0, 30)
        params = {
            'date': test_server_time_dt.strftime('%Y-%m-%d'),
        }
        create_thread(
            title='test_thread',
            tz_start_time=departure_time,
            year_days=[test_server_time_dt.date()],
            __={'calculate_noderoute': True},
            schedule_v1=[
                [None, 0, self.station],
                [10, None],

            ],
        )

        response = api_get_json('/v3/suburban/station_schedule_on_date/{}'.format(self.esr_code), params)
        thread = response['threads'][0]
        iso_dt = thread['departure']['time_utc']
        utc_dt = parse_date(iso_dt)
        msk_dt_departure = MSK_TZ.localize(datetime.combine(test_server_time_dt.date(), departure_time))
        assert utc_dt == msk_dt_departure.astimezone(pytz.utc)
        assert iso_dt == '2001-02-01T21:30:00+00:00'

    @replace_now('2016-3-1')
    def test_day_start_end_utc(self):
        create_station(__={'codes': {'esr': '100'}}, time_zone='Europe/Kiev')
        # Проверяем переход с летнего времени на зимнее utc+3 -> utc+2.
        params = {
            'date': '2016-10-30',
        }

        response = api_get_json('/v3/suburban/station_schedule_on_date/{}'.format('100'), params)
        date_time = response['date_time']
        assert date_time['day_start_utc'] == '2016-10-29T21:00:00+00:00'
        assert date_time['day_end_utc'] == '2016-10-30T21:59:59+00:00'
        time_diff = parse_date(date_time['day_end_utc']) - parse_date(date_time['day_start_utc'])
        assert (time_diff.days * 3600. * 24 + time_diff.seconds + 1) / 3600 == 25

        # Проверяем переход с зимнего времени на летнее utc+2 -> utc+3.
        params = {
            'date': '2016-3-27',
        }

        response = api_get_json('/v3/suburban/station_schedule_on_date/{}'.format('100'), params)
        date_time = response['date_time']
        assert date_time['day_start_utc'] == '2016-03-26T22:00:00+00:00'
        assert date_time['day_end_utc'] == '2016-03-27T20:59:59+00:00'
        time_diff = parse_date(date_time['day_end_utc']) - parse_date(date_time['day_start_utc'])
        assert (time_diff.days * 3600. * 24 + time_diff.seconds + 1) / 3600 == 23

    @replace_now(datetime(2001, 2, 2, 23))
    def test_date_today_tomorrow(self):
        station_from = create_station(__={'codes': {'esr': '100'}}, time_zone='Asia/Yekaterinburg')
        station_to = create_station()
        params = {
            'date': 'today',
        }

        create_thread(
            title='test_thread',
            tz_start_time=time(23, 30),
            year_days=[date(2001, 2, 2)],
            __={'calculate_noderoute': True},
            schedule_v1=[
                [None, 0, station_from],
                [50, None, station_to],

            ],
        )

        response = api_get_json('/v3/suburban/station_schedule_on_date/{}'.format(100), params)
        assert response['date_time']['date'] == '2001-02-03'
        thread = response['threads'][0]
        assert thread['departure']['time'] == '2001-02-03T01:30:00+05:00'
        assert thread['departure']['time_utc'] == '2001-02-02T20:30:00+00:00'

        params = {
            'date': 'tomorrow',
        }

        create_thread(
            title='test_thread',
            tz_start_time=time(23, 10),
            year_days=[date(2001, 2, 3)],
            __={'calculate_noderoute': True},
            schedule_v1=[
                [None, 0, station_from],
                [50, None, station_to],

            ],
        )

        response = api_get_json('/v3/suburban/station_schedule_on_date/{}'.format(100), params)
        assert response['date_time']['date'] == '2001-02-04'
        threads = response['threads']
        assert len(threads) == 1
        assert threads[0]['departure']['time'] == '2001-02-04T01:10:00+05:00'
        assert threads[0]['departure']['time_utc'] == '2001-02-03T20:10:00+00:00'

    def test_404_for_hidden_station(self):
        create_station(__={'codes': {'esr': '100'}}, hidden=True)
        response = api_get_json('/v3/suburban/station/{}'.format(100),
                                response_status_code=404)
        assert response['error']['status_code'] == 404

    def test_timezone(self):
        def check_thread(thread, arrival, departure, days):
            if arrival:
                assert thread['arrival']['time'] == arrival
            else:
                assert 'arrival' not in thread

            if departure:
                assert thread['departure']['time'] == departure
            else:
                assert 'departure' not in thread

            assert thread['days'] == days

        def check_call(arrival, departure, days, tz=''):
            response = api_get_json('/v3/suburban/station_schedule/{}?timezone={}'.format(self.esr_code, tz))
            threads = response['threads']
            assert len(threads) == 2
            check_thread(threads[0], arrival, None, days)
            check_thread(threads[1], arrival, departure, days)

        # Проходящая через нашу станцию нитка
        create_thread(
            __={'calculate_noderoute': True},
            tz_start_time=time(13, 20),
            schedule_v1=[[None, 0], [1, 10, self.station], [11, None]],
            translated_days_texts='[{"ru": "previous days"}, {"ru": "today days"}, {"ru": "next days"}]')

        # Проверяем, что в сдвиг дней вычисляется на основании правильного event (departure/arrival).
        # Чтобы добиться использования arrival, нужна нитка с последней остановкой в нашей станции.
        create_thread(
            __={'calculate_noderoute': True},
            tz_start_time=time(13, 20),
            schedule_v1=[[None, 0], [1, None, self.station]],
            translated_days_texts='[{"ru": "previous days"}, {"ru": "today days"}, {"ru": "next days"}]')

        check_call('13:21', '13:30', 'today days')
        check_call('15:21', '15:30', 'today days', pytz.timezone('Asia/Yekaterinburg'))
        check_call('23:21', '23:30', 'previous days', pytz.timezone('Pacific/Samoa'))
        check_call('00:21', '00:30', 'next days', pytz.timezone('Pacific/Kiritimati'))

    def test_stops_by_version(self):
        thread = create_thread(
            __={'calculate_noderoute': True},
            schedule_v1=[[None, 42, self.station], [43, None]])

        create_station_schedule(
            station=self.station,
            route=thread.route,
            rtstation=RTStation.objects.get(thread=thread, station=self.station),
            stops_translations='',
        )

        response = api_get_json('/v3/suburban/station_schedule/{}'.format(self.esr_code))
        threads = response['threads']
        assert threads[0]['stops'] is None

    def test_platforms(self):
        station_from = self.station
        station_to = create_station()

        thread = create_thread(
            __={'calculate_noderoute': True},
            schedule_v1=[
                [None, 10, station_from, {'platform': u'*departure_platform*'}],
                [20, None, station_to]
            ]
        )

        create_station_schedule(
            station=station_from,
            route=thread.route,
            rtstation=RTStation.objects.get(thread=thread, station=station_from)
        )

        response = api_get_json('/v3/suburban/station_schedule/{}'.format(self.esr_code))
        threads = response['threads']
        assert threads[0]['departure']['platform'] == u'*departure_platform*'

    @replace_now(datetime(2017, 12, 13))
    def test_add_facilities_data(self):
        thread = create_thread(
            __={'calculate_noderoute': True},
            schedule_v1=[[None, 42, self.station], [43, None]])

        facility1 = create_suburban_facility(title_ru='wifi')
        facility2 = create_suburban_facility(title_ru='beer')
        thread_facilitiy = SuburbanThreadFacility.objects.create(
            thread=thread,
            year_days=str(RunMask(days=[datetime(2017, 12, 13)])))
        thread_facilitiy.facilities.add(facility1)
        thread_facilitiy.facilities.add(facility2)

        response = api_get_json('/v3/suburban/station_schedule_on_date/{}?date={}'.format(self.esr_code, '2017-12-13'))
        threads = response['threads']
        fac_titles = [f['title'] for f in threads[0]['facilities']]
        assert sorted(fac_titles) == ['beer', 'wifi']
