# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import mock
import pytest
import pytz
from hamcrest import assert_that, has_entries, contains_inanyorder

from common.apps.facility.factories import create_suburban_facility
from common.apps.facility.models import SuburbanThreadFacility
from common.data_api.platforms.serialization import PlatformData, PlatformKey, PlatformRecord
from common.data_api.platforms.instance import platforms as platforms_client
from common.models.transport import TransportType, TransportSubtype
from common.utils.date import MSK_TZ, RunMask
from common.tester.factories import create_transport_subtype
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import ReplaceAttr
from common.tester.utils.django_cache import clear_cache_until_switch

from stationschedule.facilities import fill_suburban_facilities

from travel.rasp.export.export.v3.core import helpers
from travel.rasp.export.export.v3.core.helpers import fill_thread_local_start_dt
from travel.rasp.export.export.v3.core.station_schedule import (
    get_station_schedule_on_all_days, get_station_schedule_on_date, common_station_schedule,
    build_thread_data, build_thread_data_on_date, get_facilities_list
)
from travel.rasp.export.tests.v3.factories import create_station, create_thread
from travel.rasp.export.tests.v3.helpers import test_server_time_dt


pytestmark = [pytest.mark.dbripper('module'), pytest.mark.mongouser('module')]


class TestCommonStationSchedule(object):

    @replace_now(test_server_time_dt)
    def test_valid(self):
        esr_code = '123'
        station = create_station(__={'codes': {'esr': esr_code}})
        thread = create_thread(schedule_v1=[[None, 10, station],
                                            [11, None]])
        tz = pytz.timezone('Europe/Moscow')
        station_schedule_data, schedule_routes, now_aware, direction_title_by_code, timezone, next_plan = \
            common_station_schedule(station, esr_code, timezone=tz)
        assert station_schedule_data == {
            'date_time': {
                'server_time': MSK_TZ.localize(test_server_time_dt).astimezone(pytz.utc).isoformat()
            },
            'esr': '123'
        }
        assert len(schedule_routes) == 1
        assert schedule_routes[0].thread == thread
        assert now_aware == MSK_TZ.localize(test_server_time_dt)
        assert direction_title_by_code == {}
        assert timezone == MSK_TZ
        assert next_plan is None

        from stationschedule.type.suburban import SuburbanSchedule
        with mock.patch.object(SuburbanSchedule, 'build', side_effect=SuburbanSchedule.build, autospec=True) as m_build:
            station_schedule_data, schedule_routes, now_aware, direction_title_by_code, timezone, next_plan = \
                common_station_schedule(station, esr_code, timezone=tz, request_date=test_server_time_dt.date())

            assert m_build.call_args_list[0][1] == {'schedule_date': test_server_time_dt.date()}
            assert station_schedule_data == {
                'date_time': {
                    'server_time': MSK_TZ.localize(test_server_time_dt).astimezone(pytz.utc).isoformat()
                },
                'esr': '123'
            }

    @replace_now(test_server_time_dt)
    def test_build_thread_data(self):
        esr_code = '123'
        station = create_station(__={'codes': {'esr': esr_code}})
        create_thread(schedule_v1=[[None, 10, station],
                                   [11, None]],
                      year_days=[test_server_time_dt.date()],
                      uid='*thread_uid*',
                      canonical_uid='*thread_canonical_uid*',
                      number='242')
        station_schedule_data, schedule_routes, now_aware, direction_title_by_code, timezone, next_plan = \
            common_station_schedule(station, esr_code, timezone=None, request_date=test_server_time_dt.date())

        thread_data = build_thread_data(schedule_routes[0], now_aware, direction_title_by_code,
                                        timezone, next_plan)

        assert thread_data == {
            'days': u'только 2\xa0февраля',
            'is_combined': False,
            'title': None,
            'departure': {
                'time': '2001-02-02T00:10:00+03:00'
            },
            'stops': None,
            'number': '242',
            'uid': '*thread_uid*',
            'canonical_uid': '*thread_canonical_uid*',
        }

        thread_data = build_thread_data(schedule_routes[0], now_aware, direction_title_by_code,
                                        timezone, next_plan, time_format='%H:%M')

        assert thread_data == {
            'days': u'только 2\xa0февраля',
            'is_combined': False,
            'title': None,
            'departure': {
                'time': '00:10'
            },
            'stops': None,
            'number': '242',
            'uid': '*thread_uid*',
            'canonical_uid': '*thread_canonical_uid*',
        }

    @replace_now(test_server_time_dt)
    def test_build_thread_data_on_date(self):
        esr_code = '123'
        station = create_station(__={'codes': {'esr': esr_code}})
        thread = create_thread(schedule_v1=[[None, 10, station],
                                            [11, None]],
                               __={'calculate_noderoute': True},
                               year_days=[test_server_time_dt.date()],
                               uid='*thread_uid*',
                               canonical_uid='*thread_canonical_uid*',
                               number='242')

        facility_wifi = create_suburban_facility(title_ru='wifi')
        facility_bike = create_suburban_facility(title_ru='bike')
        thread_facilitiy = SuburbanThreadFacility.objects.create(
            thread=thread,
            year_days=str(RunMask(days=[test_server_time_dt.date()])))
        thread_facilitiy.facilities.add(facility_bike)
        thread_facilitiy.facilities.add(facility_wifi)

        station_schedule_data, schedule_routes, now_aware, direction_title_by_code, timezone, next_plan = \
            common_station_schedule(station, esr_code, timezone=None, request_date=test_server_time_dt.date())

        fill_suburban_facilities(schedule_routes)
        fill_thread_local_start_dt(schedule_routes)

        with mock.patch('travel.rasp.export.export.v3.core.station_schedule.build_thread_data',
                        side_effect=build_thread_data) as m_build_thread_data, \
                mock.patch('travel.rasp.export.export.v3.core.station_schedule.get_facilities_list',
                           side_effect=get_facilities_list) as m_facilities:
            thread_data = build_thread_data_on_date(schedule_routes[0], now_aware, direction_title_by_code,
                                                    timezone, next_plan)
            m_build_thread_data.assert_called_once_with(schedule_routes[0], now_aware, direction_title_by_code,
                                                        timezone, next_plan, dynamic_platforms=None)
            assert set(m_facilities.call_args_list[0][0][0]) == {facility_bike, facility_wifi}

            assert_that(thread_data, has_entries({
                'days': u'только 2\xa0февраля',
                'is_combined': False,
                'title': None,
                'departure': {
                    'time_utc': '2001-02-01T21:10:00+00:00',
                    'time': '2001-02-02T00:10:00+03:00'
                },
                'stops': None,
                'number': u'242',
                'uid': '*thread_uid*',
                'canonical_uid': '*thread_canonical_uid*',
            }))

            assert sorted(thread_data['facilities'], key=lambda x: x['title']) == [
                {'code': facility_bike.code, 'icon': None, 'title': u'bike'},
                {'code': facility_wifi.code, 'icon': None, 'title': u'wifi'}
            ]

    @replace_now(test_server_time_dt)
    def test_station_schedule_on_all_days(self):
        esr_code = '123'
        station = create_station(__={'codes': {'esr': esr_code}})
        create_thread(schedule_v1=[[None, 10, station],
                                   [11, None]],
                      year_days=[test_server_time_dt.date()],
                      uid='*thread_uid*',
                      canonical_uid='*thread_canonical_uid*',
                      number='545')
        tz = pytz.timezone('Europe/Berlin')
        station_schedule_data = get_station_schedule_on_all_days(station, esr_code, timezone=tz)

        assert station_schedule_data == {
            'date_time': {'server_time': '2001-02-02T09:13:14+00:00'},
            'threads': [{
                'days': u'только 1\xa0февраля',
                'is_combined': False,
                'title': None,
                'departure': {'time': '22:10'},
                'stops': None,
                'number': '545',
                'uid': '*thread_uid*',
                'canonical_uid': '*thread_canonical_uid*',
            }],
            'esr': '123'
        }

    @replace_now(test_server_time_dt)
    def test_station_schedule_on_date(self):
        esr_code = '123'
        station = create_station(__={'codes': {'esr': esr_code}})
        create_thread(schedule_v1=[[None, 10, station],
                                   [11, None]],
                      year_days=[test_server_time_dt.date()],
                      uid='*thread_uid*',
                      canonical_uid='*thread_canonical_uid*',
                      number='545')

        station_schedule_data = get_station_schedule_on_date(
            station, esr_code, timezone=None, request_date=test_server_time_dt.date()
        )

        assert station_schedule_data == {
            'date_time': {
                'day_end_utc': '2001-02-02T20:59:59+00:00',
                'day_start_utc': '2001-02-01T21:00:00+00:00',
                'server_time': '2001-02-02T09:13:14+00:00',
                'date': '2001-02-02'
            },
            'threads': [{
                'days': u'только 2\xa0февраля',
                'is_combined': False,
                'title': None,
                'departure': {
                    'time_utc': '2001-02-01T21:10:00+00:00',
                    'time': '2001-02-02T00:10:00+03:00'
                },
                'stops': None,
                'number': u'545',
                'uid': u'*thread_uid*',
                'canonical_uid': '*thread_canonical_uid*',
                'start_time': '2001-02-02T00:10:00+03:00'
            }],
            'esr': '123'}

    @replace_now(test_server_time_dt)
    def test_station_schedule_on_date_platforms(self):
        esr_code = '234'
        station = create_station(__={'codes': {'esr': esr_code}})
        threads = [
            create_thread(
                year_days=[test_server_time_dt.date()],
                uid='*thread_uid0*',
                number='1000',
                schedule_v1=[
                    [None, 10, station],
                    [11, None]
                ],
            ),
            create_thread(
                year_days=[test_server_time_dt.date()],
                uid='*thread_uid1*',
                number='1001',
                schedule_v1=[
                    [None, 15, station, {'platform': 'way1'}],
                    [16, None]
                ],
            ),
            create_thread(
                year_days=[test_server_time_dt.date()],
                uid='*thread_uid2*',
                number='1002',
                schedule_v1=[
                    [None, 20, station],
                    [25, None]
                ],
            ),
        ]

        platforms_client.update([
            PlatformRecord(
                key=PlatformKey(date=test_server_time_dt.date(), station_id=station.id, train_number=threads[0].number),
                data=PlatformData(departure_platform='platform_0')
            ),
            PlatformRecord(
                key=PlatformKey(date=test_server_time_dt.date(), station_id=station.id, train_number=threads[2].number),
                data=PlatformData(departure_platform='platform_2')
            ),
        ])

        station_schedule_data = get_station_schedule_on_date(
            station, esr_code, timezone=None, request_date=test_server_time_dt.date()
        )

        assert_that(station_schedule_data, has_entries({
            'threads': contains_inanyorder(
                has_entries({
                    'departure': has_entries({
                        'time': '2001-02-02T00:10:00+03:00',
                        'platform': 'platform_0',
                    }),
                    'number': u'1000',
                    'uid': u'*thread_uid0*'
                }),
                has_entries({
                    'departure': has_entries({
                        'time': '2001-02-02T00:15:00+03:00',
                        'platform': 'way1',
                    }),
                    'number': u'1001',
                    'uid': u'*thread_uid1*'
                }),
                has_entries({
                    'departure': has_entries({
                        'time': '2001-02-02T00:20:00+03:00',
                        'platform': 'platform_2',
                    }),
                    'number': u'1002',
                    'uid': u'*thread_uid2*'
                }),
            )
        }))

    @replace_now(test_server_time_dt)
    def test_train_station_schedule_on_all_days(self):
        esr_code = '123'
        station_1, station_2 = create_station(__={'codes': {'esr': esr_code}}), create_station()
        dt = datetime(2018, 9, 10, 12)
        t_subtype_code = '_last_'
        t_subtype = create_transport_subtype(
            t_type=TransportSubtype.SUBURBAN_ID,
            code=t_subtype_code,
            use_in_suburban_search=True
        )

        create_thread(
            __={'calculate_noderoute': True},
            year_days=[dt.date()],
            tz_start_time=dt.time(),
            t_type=TransportType.TRAIN_ID,
            number='119A',
            uid='uid119A',
            canonical_uid='canonical_uid_119A',
            t_subtype=t_subtype,
            schedule_v1=[
                [None, 0, station_1],
                [50, None, station_2],
            ]
        )

        clear_cache_until_switch()
        station_schedule_data = get_station_schedule_on_all_days(station_1, esr_code, MSK_TZ)

        assert_that(station_schedule_data, has_entries({
            'date_time': {'server_time': '2001-02-02T09:13:14+00:00'},
            'esr': '123',
            'threads': contains_inanyorder(has_entries({
                'transport': has_entries({'subtype': has_entries({'code': t_subtype_code})}),
                'number': '119A',
                'uid': 'uid119A',
                'canonical_uid': 'canonical_uid_119A',
                'days': 'только 10 сентября',
                'departure': {'time': '12:00'},
            }))
        }))

    @replace_now(test_server_time_dt)
    def test_train_station_schedule_on_date(self):
        esr_code = '123'
        station_1, station_2 = create_station(__={'codes': {'esr': esr_code}}), create_station()
        dt = datetime(2018, 9, 10, 12)
        t_subtype_code = '_last_'
        t_subtype = create_transport_subtype(
            t_type=TransportSubtype.SUBURBAN_ID,
            code=t_subtype_code,
            title_ru='ласточка',
            title_suburban_ru='ласточка(train)',
            use_in_suburban_search=True
        )

        create_thread(
            __={'calculate_noderoute': True},
            year_days=[dt.date()],
            tz_start_time=dt.time(),
            t_type=TransportType.TRAIN_ID,
            number='119A',
            uid='uid119A',
            t_subtype=t_subtype,
            schedule_v1=[
                [None, 0, station_1],
                [50, None, station_2],
            ]
        )

        clear_cache_until_switch()
        station_schedule_data = get_station_schedule_on_date(station_1, esr_code, MSK_TZ, dt.date())

        assert_that(station_schedule_data, has_entries({
            'date_time': {
                'day_end_utc': '2018-09-10T20:59:59+00:00',
                'day_start_utc': '2018-09-09T21:00:00+00:00',
                'server_time': '2001-02-02T09:13:14+00:00',
                'date': '2018-09-10'
            },
            'esr': '123',
            'threads': contains_inanyorder(has_entries({
                'transport': has_entries({
                    'subtype': has_entries({
                        'code': t_subtype_code,
                        'title': 'ласточка(train)'
                    })
                }),
                'number': '119A',
                'uid': 'uid119A',
                'days': 'только 10 сентября',
                'departure': {'time_utc': '2018-09-10T09:00:00+00:00', 'time': '2018-09-10T12:00:00+03:00'},
                'start_time': '2018-09-10T12:00:00+03:00'
            }))
        }))

    def test_train_station_schedule_lastdal(self):
        esr_code = '123'
        station_1, station_2 = create_station(__={'codes': {'esr': esr_code}}), create_station()
        dt = datetime(2018, 9, 10, 12)
        t_subtype_code = '_last_'
        t_subtype = create_transport_subtype(
            t_type=TransportSubtype.SUBURBAN_ID,
            code=t_subtype_code,
            title_ru='ласточка(common)',
            title_suburban_ru='ласточка',
            use_in_suburban_search=True
        )

        create_thread(
            __={'calculate_noderoute': True},
            year_days=[dt.date()],
            tz_start_time=dt.time(),
            t_type=TransportType.TRAIN_ID,
            number='119A',
            uid='uid119A',
            t_subtype=t_subtype,
            schedule_v1=[
                [None, 0, station_1],
                [50, None, station_2],
            ]
        )

        clear_cache_until_switch()
        with ReplaceAttr('TRAIN_SUBTYPES_CODES', {t_subtype_code}, helpers):
            station_schedule_data = get_station_schedule_on_date(station_1, esr_code, MSK_TZ, dt.date())
            assert_that(station_schedule_data, has_entries({
                'threads': contains_inanyorder(has_entries({
                    'transport': has_entries({
                        'subtype': has_entries({
                            'code': t_subtype_code,
                            'title': 'ласточка<br/>(билеты&nbsp;c указанием&nbsp;мест)'
                        })
                    })
                }))
            }))
