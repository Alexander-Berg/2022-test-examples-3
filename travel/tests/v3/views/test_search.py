# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, timedelta, time, date

import mock
import pytest
import pytz
from hamcrest import assert_that, has_entries
from iso8601 import parse_date

from common.apps.suburban_events.factories import ThreadStationStateFactory
from common.models.factories import create_tariff_group, create_tariff_type, create_aeroex_tariff
from common.models.geo import Settlement
from common.tester.factories import create_settlement
from common.tester.utils.datetime import replace_now

from geosearch.views.pointlist import PointList

from travel.rasp.export.tests.v3.factories import create_station, create_thread
from travel.rasp.export.tests.v3.helpers import test_server_time_dt, check_server_time, check_segment_departure_utc, api_get_json


pytestmark = [pytest.mark.dbuser('module'), pytest.mark.mongouser('module')]


class TestSearch(object):
    def test_valid(self):
        settlement = create_settlement()
        params = {
            'city_from': Settlement.MOSCOW_ID,
            'city_to': settlement.id,
        }

        api_get_json('/v3/suburban/search/', params)

    @replace_now(test_server_time_dt)
    def test_server_time(self):
        settlement = create_settlement()
        params = {
            'city_from': Settlement.MOSCOW_ID,
            'city_to': settlement.id,
        }

        response = api_get_json('/v3/suburban/search/', params)
        check_server_time(test_server_time_dt, response)

    @replace_now(test_server_time_dt)
    def test_segment_departure_utc(self):
        station_from, station_to = create_station(__={'codes': {'esr': '100'}}), create_station(
            __={'codes': {'esr': '101'}})
        departure_time = time(0, 30)
        params = {
            'station_from': '100',
            'station_to': '101',
            'date': test_server_time_dt.strftime('%Y-%m-%d'),
        }

        create_thread(
            title='test_thread',
            tz_start_time=departure_time,
            year_days=[test_server_time_dt.date()],
            __={'calculate_noderoute': True},
            schedule_v1=[
                [None, 0, station_from],
                [10, None, station_to],

            ],
        )

        response = api_get_json('/v3/suburban/search_on_date/', params)
        check_segment_departure_utc(departure_time, response)

    @replace_now('2016-3-1')
    def test_day_start_end_utc(self):
        create_station(__={'codes': {'esr': '100'}},  time_zone='Europe/Kiev'), create_station(__={'codes': {'esr': '101'}})

        # Проверяем переход с летнего времени на зимнее utc+3 -> utc+2.
        params = {
            'station_from': '100',
            'station_to': '101',
            'date': '2016-10-30',
        }

        response = api_get_json('/v3/suburban/search_on_date/', params)
        date_time = response['date_time']
        assert date_time['day_start_utc'] == '2016-10-29T21:00:00+00:00'
        assert date_time['day_end_utc'] == '2016-10-30T21:59:59+00:00'
        time_diff = parse_date(date_time['day_end_utc']) - parse_date(date_time['day_start_utc'])
        assert (time_diff.days * 3600. * 24 + time_diff.seconds + 1) / 3600 == 25

        # Проверяем переход с зимнего времени на летнее utc+2 -> utc+3.
        params['date'] = '2016-3-27'

        response = api_get_json('/v3/suburban/search_on_date/', params)
        date_time = response['date_time']
        assert date_time['day_start_utc'] == '2016-03-26T22:00:00+00:00'
        assert date_time['day_end_utc'] == '2016-03-27T20:59:59+00:00'
        time_diff = parse_date(date_time['day_end_utc']) - parse_date(date_time['day_start_utc'])
        assert (time_diff.days * 3600. * 24 + time_diff.seconds + 1) / 3600 == 23

    @replace_now(datetime(2001, 2, 2, 23))
    def test_date_today_tomorrow(self):
        station_from = create_station(__={'codes': {'esr': '100'}}, time_zone='Asia/Yekaterinburg')
        station_to = create_station(__={'codes': {'esr': '101'}})
        params = {
            'station_from': '100',
            'station_to': '101',
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

        response = api_get_json('/v3/suburban/search_on_date/', params)
        date_time = response['date_time']
        assert date_time['date'] == '2001-02-03'
        segment = response['days'][0]['segments'][0]
        assert segment['departure']['time'] == '2001-02-03T01:30:00+05:00'

        params['date'] = 'tomorrow'

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

        response = api_get_json('/v3/suburban/search_on_date/', params)
        date_time = response['date_time']
        assert date_time['date'] == '2001-02-04'
        segments = response['days'][0]['segments']
        assert len(segments) == 1
        assert segments[0]['departure']['time'] == '2001-02-04T01:10:00+05:00'

    @replace_now(datetime(2001, 2, 2, 23))
    def test_date_today_up_to_ahead(self):
        station_from = create_station(__={'codes': {'esr': '100'}}, time_zone='Asia/Yekaterinburg')
        station_to = create_station(__={'codes': {'esr': '101'}})
        params = {
            'date': 'today',
            'days_ahead': '3',
            'tomorrow_upto': '12',
            'station_from': '100',
            'station_to': '101'
        }

        create_thread(
            title='test_thread_1',
            tz_start_time=time(23, 30),
            year_days=[date(2001, 2, 2)],
            __={'calculate_noderoute': True},
            schedule_v1=[
                [None, 0, station_from],
                [50, None, station_to],

            ],
        )
        create_thread(
            title='test_thread_2',
            tz_start_time=time(23, 40),
            year_days=[date(2001, 2, 4)],
            __={'calculate_noderoute': True},
            schedule_v1=[
                [None, 0, station_from],
                [50, None, station_to],

            ],
        )
        create_thread(
            title='test_thread_3',
            tz_start_time=time(23, 50),
            year_days=[date(2001, 2, 5)],
            __={'calculate_noderoute': True},
            schedule_v1=[
                [None, 0, station_from],
                [50, None, station_to],

            ],
        )

        response = api_get_json('/v3/suburban/search_on_date/', params)
        date_time = response['date_time']
        assert date_time['date'] == '2001-02-03'
        days = response['days']

        expected_days = [datetime(2001, 2, 3), datetime(2001, 2, 5), datetime(2001, 2, 6)]

        for day_element, day in zip(days, expected_days):
            assert day_element['date'] == day.strftime('%Y-%m-%d')

            day_utc_start = pytz.timezone('Asia/Yekaterinburg').localize(day).astimezone(pytz.UTC)
            day_utc_end = day_utc_start + timedelta(hours=23, minutes=59)

            assert day_element['day_start_utc'] == '{}T19:00:00+00:00'.format(day_utc_start.strftime('%Y-%m-%d'))
            assert day_element['day_end_utc'] == '{}T18:59:59+00:00'.format(day_utc_end.strftime('%Y-%m-%d'))
            assert len(day_element['segments']) == 1

    def test_from_to_reduce(self):
        station_from = create_station(__={'codes': {'esr': '100'}}, settlement_id=Settlement.MOSCOW_ID)
        station_to = create_station(__={'codes': {'esr': '101'}})
        params = {
            'city_from': Settlement.MOSCOW_ID,
            'station_to': '101',
        }

        create_thread(
            title='test_thread_1',
            tz_start_time=time(23, 30),
            year_days=[date(2001, 2, 2)],
            __={'calculate_noderoute': True},
            schedule_v1=[
                [None, 0, station_from],
                [50, None, station_to],
            ],
        )

        def proc_point_lists(point_list_from, point_list_to, **kwargs):
            return PointList(station_from), point_list_to

        with mock.patch('travel.rasp.export.export.v3.views.utils.process_points_lists', side_effect=proc_point_lists):
            response = api_get_json('/v3/suburban/search/', params)
            segments = response['segments']
            assert len(segments) == 1
            assert response['narrowed_from'] == '100'
            assert response['narrowed_to'] is None

        response = api_get_json('/v3/suburban/search/', params)
        segments = response['segments']
        assert len(segments) == 1
        assert response['narrowed_from'] is None
        assert response['narrowed_to'] is None

    @replace_now('2016-01-10 00:00:00')
    def test_thread_local_start_date(self):
        departure_date = datetime(2016, 1, 10)
        station_to = create_station(__={'codes': {'esr': '101'}})

        create_thread(
            __={'calculate_noderoute': True},
            uid='12345',
            year_days=[departure_date.date()],
            tz_start_time=time(22),
            schedule_v1=[
                [None, 0, create_station(time_zone='Asia/Barnaul', __={'codes': {'esr': '100'}})],
                [500, None, station_to],
            ],
        )

        params = {
            'station_from': '100',
            'station_to': '101',
        }

        response = api_get_json('/v3/suburban/search/', params)
        segment = response['segments'][0]
        assert 'start_time' not in segment['thread']

        params['date'] = '2016-01-11'
        response = api_get_json('/v3/suburban/search_on_date/', params)
        segment = response['days'][0]['segments'][0]
        assert segment['thread']['start_time'] == '2016-01-11T01:00:00+06:00'

    def test_suburban_search_tariffs(self):
        station_from = create_station(__={'codes': {'esr': '100'}})
        station_to = create_station(__={'codes': {'esr': '101'}})

        group = create_tariff_group()
        tariff_type = create_tariff_type(code='type1', category='usual', is_main=True, order=1,
                                         title='tariff1', __={'tariff_groups': [group]})
        create_aeroex_tariff(id=601, station_from=station_from, station_to=station_to, tariff=100, type=tariff_type)

        create_thread(
            title='thread_1',
            __={'calculate_noderoute': True},
            schedule_v1=[
                [None, 0, station_from],
                [50, None, station_to],
            ],
            tariff_type=tariff_type
        )

        params = {'station_from': '100', 'station_to': '101'}
        response = api_get_json('/v3/suburban/search/', params)

        assert 'tariffs' in response
        assert len(response['tariffs']) == 1
        assert_that(response['tariffs'][0], has_entries({
            'category': 'usual',
            'title': 'tariff1',
            'code': 'type1',
            'order': 1,
            'is_main': True,
            'price': has_entries({'currency': 'RUR', 'value': 100}),
            'id': 601,
        }))

        assert 'segments' in response
        assert len(response['segments']) == 1

        segment = response['segments'][0]
        assert 'tariffs_ids' in segment
        assert segment['tariffs_ids'] == [601]

        assert 'tariff' in segment
        assert segment['tariff']['value'] == 100
        assert segment['tariff']['currency'] == 'RUR'

    @replace_now('2018-02-10 13:00:00')
    def test_forecast(self):
        start_dt = datetime(2018, 2, 10, 13)
        stations = [create_station(__={'codes': {'esr': str(code)}}, id=code) for code in range(101, 104)]

        thread = create_thread(
            __={'calculate_noderoute': True},
            number='500',
            tz_start_time=start_dt.time(),
            schedule_v1=[
                [None, 0, stations[0]],
                [20, 25, stations[1]],
                [50, None, stations[2]],
            ],
        )
        rtstations = thread.path

        ThreadStationStateFactory.create_from_rtstation(
            rtstations[1], start_dt,
            departure={
                'dt': start_dt + timedelta(minutes=25),
                'minutes_from': 1,
                'minutes_to': 5,
            },
            arrival={
                'dt': start_dt + timedelta(minutes=20),
                'minutes_from': 5,
                'minutes_to': 10,
            }
        )

        params = {
            'station_from': '102',
            'station_to': '103',
            'date': '2018-02-10'
        }
        response = api_get_json('/v3/suburban/search_on_date/', params)
        segment = response['days'][0]['segments'][0]

        assert_that(segment, has_entries({
            'departure': has_entries({
                'state': {
                    'minutes_from': 1,
                    'minutes_to': 5,
                    'type': 'fact',
                    'key': '500__101___2018-02-10T13:00:00___102___20___25___None___None',
                    'fact_time': '2018-02-10T13:25:00+03:00'
                },
                'arrival_state': {
                    'minutes_from': 5,
                    'minutes_to': 10,
                    'type': 'fact',
                    'key': '500__101___2018-02-10T13:00:00___102___20___25___None___None',
                    'fact_time': '2018-02-10T13:20:00+03:00'
                }
            }),
            'arrival': has_entries({
                'state': {
                    'type': 'undefined',
                    'key': '500__101___2018-02-10T13:00:00___103___50___None___None___None'
                },
            }),
        }))

        params = {
            'station_from': '101',
            'station_to': '102',
            'date': '2018-02-10'
        }
        response = api_get_json('/v3/suburban/search_on_date/', params)
        segment = response['days'][0]['segments'][0]

        assert_that(segment, has_entries({
            'departure': has_entries({
                'state': {
                    'type': 'undefined',
                    'key': '500__101___2018-02-10T13:00:00___101___None___0___None___None'
                },
            }),
            'arrival': has_entries({
                'state': {
                    'minutes_from': 5,
                    'minutes_to': 10,
                    'type': 'fact',
                    'key': '500__101___2018-02-10T13:00:00___102___20___25___None___None',
                    'fact_time': '2018-02-10T13:20:00+03:00'
                },
                'departure_state': {
                    'minutes_from': 1,
                    'minutes_to': 5,
                    'type': 'fact',
                    'key': '500__101___2018-02-10T13:00:00___102___20___25___None___None',
                    'fact_time': '2018-02-10T13:25:00+03:00'
                }
            })
        }))

    # @replace_now('2019-02-28 03:00:00')
    # def test_subscription_allowed(self):
    #     company_66, company_59181 = create_company(id=66), create_company(id=59181)
    #     station_from = create_station(__={'codes': {'esr': '100'}})
    #     station_to = create_station(__={'codes': {'esr': '101'}})
    #     dep_date = date(2019, 2, 28)
    #     params = {'station_from': '100', 'station_to': '101', 'date': dep_date.strftime('%Y-%m-%d')}
    #
    #     response = api_get_json('/v3/suburban/search/', params)
    #     assert response['subscription_allowed'] is False
    #     response = api_get_json('/v3/suburban/search_on_date/', params)
    #     assert response['subscription_allowed'] is False
    #
    #     create_thread(
    #         company=company_59181,
    #         year_days=[dep_date],
    #         schedule_v1=[
    #             [None, 0, station_from],
    #             [10, None, station_to],
    #
    #         ]
    #     )
    #     response = api_get_json('/v3/suburban/search/', params)
    #     assert response['subscription_allowed'] is False
    #     response = api_get_json('/v3/suburban/search_on_date/', params)
    #     assert response['subscription_allowed'] is False
    #
    #     create_thread(
    #         company=company_66,
    #         year_days=[dep_date],
    #         schedule_v1=[
    #             [None, 0, station_from],
    #             [10, None, station_to],
    #
    #         ]
    #     )
    #     response = api_get_json('/v3/suburban/search/', params)
    #     assert response['subscription_allowed'] is True
    #     response = api_get_json('/v3/suburban/search_on_date/', params)
    #     assert response['subscription_allowed'] is True
    #
    # @replace_now('2019-02-28 03:00:00')
    # def test_subscription_allowed_train(self):
    #     station_from = create_station(__={'codes': {'esr': '100'}})
    #     station_to = create_station(__={'codes': {'esr': '101'}})
    #     dep_date = date(2019, 2, 28)
    #     params = {'station_from': '100', 'station_to': '101', 'date': dep_date.strftime('%Y-%m-%d')}
    #     t_subtype = create_transport_subtype(
    #         t_type=TransportSubtype.SUBURBAN_ID,
    #         code='last',
    #         use_in_suburban_search=True
    #     )
    #
    #     create_thread(
    #         year_days=[dep_date],
    #         t_type=TransportType.TRAIN_ID,
    #         t_subtype=t_subtype,
    #         schedule_v1=[
    #             [None, 0, station_from],
    #             [10, None, station_to],
    #         ]
    #     )
    #
    #     clear_cache_until_switch()
    #     response = api_get_json('/v3/suburban/search/', params)
    #     assert response['subscription_allowed'] is False
    #     response = api_get_json('/v3/suburban/search_on_date/', params)
    #     assert response['subscription_allowed'] is False
    #
    #     create_thread(
    #         year_days=[dep_date],
    #         schedule_v1=[
    #             [None, 0, station_from],
    #             [10, None, station_to],
    #
    #         ]
    #     )
    #     response = api_get_json('/v3/suburban/search/', params)
    #     assert response['subscription_allowed'] is True
    #     response = api_get_json('/v3/suburban/search_on_date/', params)
    #     assert response['subscription_allowed'] is True
