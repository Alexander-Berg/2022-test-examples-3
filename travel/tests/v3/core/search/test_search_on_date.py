# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest
from datetime import time, timedelta, datetime
from hamcrest import assert_that, contains_inanyorder, has_entries

from common.apps.facility.factories import create_suburban_facility
from common.apps.facility.models import SuburbanThreadFacility
from common.apps.info_center.models import Info
from common.apps.suburban_events.factories import ThreadStationStateFactory
from common.dynamic_settings.default import conf
from common.data_api.platforms.instance import platforms as platforms_client
from common.data_api.platforms.serialization import PlatformData, PlatformKey, PlatformRecord
from common.models.factories import create_info, create_aeroex_tariff, create_tariff_group, create_tariff_type
from common.models.schedule import RThreadType
from common.models.transport import TransportType, TransportSubtypeColor
from common.tester.factories import create_transport_subtype
from travel.rasp.library.python.common23.tester.utils.datetime import replace_now
from travel.rasp.library.python.common23.tester.utils.hamcrest import has_only_entries
from travel.rasp.library.python.common23.tester.utils.replace_setting import replace_setting
from travel.rasp.library.python.common23.date import environment
from common.utils.date import MSK_TZ, RunMask
from common.utils.text import NBSP

from travel.rasp.export.export.v3.core.search import search_on_date, TransfersMode
from travel.rasp.export.tests.v3.factories import create_station, create_thread
from travel.rasp.export.tests.v3.helpers import create_request

from route_search.transfers import transfers
from route_search.factories import create_transfer_variant


pytestmark = [pytest.mark.dbuser('module'), pytest.mark.mongouser('module')]


class TestSearchOnDate(object):
    @replace_now('2015-01-01 02:13:18')
    @replace_setting('MEDIA_URL', 'https://static/')
    def test_search_on_date(self):
        """
        Мега-тест, проверяющий наличие всех возможных элементов выдачи, с минимальным количеством моков.
        """

        departure_day = environment.today()
        departure_time = time(14, 20)
        departure_dt = datetime.combine(departure_day, departure_time)

        station1 = create_station(id=100, __={'codes': {'esr': 'esr1'}})
        station2 = create_station(id=200, __={'codes': {'esr': 'esr2'}})
        station3 = create_station(id=300, __={'codes': {'esr': 'esr3'}}, title='пересадка тут')
        station4 = create_station(id=400)

        subtype = create_transport_subtype(
            t_type=TransportType.SUBURBAN_ID,
            code='megatransport',
            title_ru='мегатранспорт',
            color=TransportSubtypeColor.objects.create(color='#000000', code='sooo_black'),
        )

        group = create_tariff_group()
        group2 = create_tariff_group()
        uber_tariff_type = create_tariff_type(code='uber_tariff', title='uber', description_ru='uber description',
                                              category='usual', __={'tariff_groups': [group]}, order=1)
        mega_tariff_type = create_tariff_type(code='mega_tariff', title='mega', description_ru='mega description',
                                              category='usual', __={'tariff_groups': [group]}, order=2)
        express_tariff_type = create_tariff_type(code='express_tariff', title='express', description_ru='exp description',
                                                 category='usual', __={'tariff_groups': [group2]}, order=50)

        # попадает в выдачу, в два дня
        thread2 = create_thread(
            __={
                'calculate_noderoute': True,
                'stops_translations': {'ru': 'то тут, то там'},
            },
            uid='thread_uid2',
            canonical_uid='R_canonical_uid2',
            number='thread_number2blabla',  # clean_number очистит это до '2'
            title='thread_title2',
            title_short='thread_title_short2',
            is_manual_title=False,  # чтобы использовался title_short
            year_days=[departure_day, departure_day + timedelta(days=1)],
            tz_start_time=departure_time,
            schedule_v1=[
                [None, 0, station1],
                [50, None, station2],
            ],
            type=RThreadType.CHANGE_ID,
            tariff_type=mega_tariff_type
        )

        rts2_1, rts2_2 = thread2.path
        rts2_1.platform = 'платформа2_1'
        rts2_1.save()

        # попадает в выдачу, в один день
        thread3 = create_thread(
            __={'calculate_noderoute': True},
            uid='thread_uid3',
            canonical_uid='R_canonical_uid3',
            title='thread_title3',
            number='3333',
            year_days=[departure_day],
            tz_start_time=departure_time.replace(minute=departure_time.minute + 10),
            schedule_v1=[
                [None, 0, station3],
                [10, 20, station1],
                [140, 150, station2],
                [160, None, station4],
            ],
            translated_days_texts='[{}, {"ru": "я здесь, я там, я всегда"}, {}, {}]',
            translated_except_texts='["", "01.05, 08.05", "", ""]',
            t_subtype=subtype,
            express_type='express',
            tariff_type=express_tariff_type
        )
        rts3_3 = thread3.path[2]
        rts3_3.platform = 'платформа3_3'
        rts3_3.save()
        rts3_2 = thread3.path[1]

        # попадает в пересадки
        thread1 = create_thread(
            __={'calculate_noderoute': True, 'stops_translations': {'ru': 'остановки для thread1'}},
            uid='thread_uid1',
            canonical_uid='R_canonical_uid1',
            title='thread_title1',
            title_short='thread_title_short1',
            number='1111',
            year_days=[departure_day + timedelta(days=1)],
            tz_start_time=departure_time.replace(minute=departure_time.minute - 10),
            schedule_v1=[
                [None, 0, station1],
                [50, None, station3],
            ],
            tariff_type=uber_tariff_type,
        )

        # попадает в пересадки
        thread4 = create_thread(
            __={'calculate_noderoute': True},
            uid='thread_uid4',
            canonical_uid='R_canonical_uid4',
            title='thread_title4',
            number='4444',
            year_days=[departure_day + timedelta(days=1)],
            tz_start_time=departure_time.replace(hour=departure_time.hour + 2),
            schedule_v1=[
                [None, 0, station3],
                [23, None, station2],
            ],
            tariff_type=uber_tariff_type,
        )
        rts4_2 = thread4.path[1]
        rts4_2.platform = 'платформа4_2'
        rts4_2.save()

        # попадает в выдачу третьего дня за счет tomorrow_upto=4
        create_thread(
            __={'calculate_noderoute': True},
            uid='thread_uid5',
            canonical_uid='R_canonical_uid5',
            year_days=[departure_day + timedelta(days=2)],
            tz_start_time=time(3, 20),
            schedule_v1=[
                [None, 0, station1],
                [50, None, station2],
            ],
        )

        # Переопределяем динамические платформы
        platforms_client.update([
            PlatformRecord(
                key=PlatformKey(date=d, station_id=rts2_1.station.id, train_number=thread2.number),
                data=PlatformData(departure_platform='платформа2_1а')
            ) for d in RunMask(thread2.year_days, today=environment.today()).dates()
        ] + [
            PlatformRecord(
                key=PlatformKey(date=d, station_id=rts3_3.station.id, train_number=thread3.number),
                data=PlatformData(arrival_platform='платформа3_3а')
            ) for d in RunMask(thread3.year_days, today=environment.today()).dates()
        ])

        facility_wifi = create_suburban_facility(title_ru='вайфайчег', code='wifi', icon='wifi.svg')
        facility_bike = create_suburban_facility(title_ru='байк', code='bike')

        thread_facilitiy3 = SuburbanThreadFacility.objects.create(
            thread=thread3,
            year_days=str(RunMask(days=[departure_day]))
        )
        thread_facilitiy3.facilities.add(facility_bike)
        thread_facilitiy3.facilities.add(facility_wifi)

        thread_facilitiy1 = SuburbanThreadFacility.objects.create(
            thread=thread1,
            year_days=str(RunMask(days=[departure_day + timedelta(days=1)]))
        )
        thread_facilitiy1.facilities.add(facility_bike)

        create_info(
            uuid=6501510990188215577,
            title='Мой Тизер',
            text='длинный текстище',
            text_short='короткий текстик',
            url='https://ya.ru',
            services=[Info.Service.MOBILE_APPS],
            stations=[station1],
            info_type=Info.Type.NORMAL,
        )
        create_info(services=[Info.Service.WEB], stations=[station1])  # не показывается
        create_info(services=[Info.Service.MOBILE_APPS], stations=[station3])  # не показывается

        ThreadStationStateFactory.create_from_rtstation(
            rts2_1, departure_dt,
            departure={
                'dt': datetime(2015, 1, 1, 14, 30),
                'type': u'fact',
                'minutes_from': 5,
                'minutes_to': 105,
            }
        )
        ThreadStationStateFactory.create_from_rtstation(
            rts2_2, departure_dt,
            arrival={
                'type': u'possible_delay',
                'minutes_from': 2,
                'minutes_to': 5,
            },
        )
        ThreadStationStateFactory.create_from_rtstation(
            rts2_2, departure_dt + timedelta(days=1),
            arrival={
                'type': u'possible_delay',
                'minutes_from': 6,
                'minutes_to': 7,
            },
        )
        ThreadStationStateFactory.create_from_rtstation(
            rts4_2, datetime.combine(departure_day + timedelta(days=1), thread4.tz_start_time),
            arrival={
                'type': u'possible_delay',
                'minutes_from': 10,
                'minutes_to': 20,
            },
        )
        ThreadStationStateFactory.create_from_rtstation(
            rts3_2, datetime.combine(departure_day, thread3.tz_start_time),
            departure={
                'type': u'possible_delay',
                'minutes_from': 111,
                'minutes_to': 222,
            },
            arrival={
                'type': u'possible_delay',
                'minutes_from': 333,
                'minutes_to': 444,
            },
        )
        expected_rts2_1_key = 'thread_number2blabla__100___2015-01-01T14:20:00___100___None___0___None___None'
        expected_rts2_2_key = 'thread_number2blabla__100___2015-01-01T14:20:00___200___50___None___None___None'
        expected_rts2_2_key_day2 = 'thread_number2blabla__100___2015-01-02T14:20:00___200___50___None___None___None'
        expected_rts2_1_key_day2 = 'thread_number2blabla__100___2015-01-02T14:20:00___100___None___0___None___None'
        expected_rts4_2_key_day2 = '4444__300___2015-01-02T16:20:00___200___23___None___None___None'
        expected_rts4_3_key_day2 = '4444__300___2015-01-02T16:20:00___300___None___0___None___None'
        expected_rts3_2_key = '3333__300___2015-01-01T14:30:00___100___10___20___None___None'
        expected_rts3_3_key = '3333__300___2015-01-01T14:30:00___200___140___150___None___None'

        create_aeroex_tariff(
            type=mega_tariff_type, id=901,
            station_from=station1, station_to=station2,
            currency='RUR', tariff=100500,
        )
        create_aeroex_tariff(
            type=express_tariff_type, id=902,
            station_from=station1, station_to=station2,
            currency='USD', tariff=666.55500001,
        )
        create_aeroex_tariff(
            type=uber_tariff_type, id=903,
            station_from=station3, station_to=station2,
            currency='BYN', tariff=123,
        )
        create_aeroex_tariff(
            type=uber_tariff_type, id=904,
            station_from=station1, station_to=station3,
            currency='BYN', tariff=456,
        )

        transfer_variants = {
            create_transfer_variant(routes=[
                {
                    'station_from': station1,
                    'station_to': station3,
                    'thread': thread1,
                    'departure_datetime': '2015-01-02 14:10',
                    'arrival_datetime': '2015-01-02 15:00',
                    'start_date': '2015-01-02',
                },
                {
                    'station_from': station3,
                    'station_to': station2,
                    'thread': thread4,
                    'departure_datetime': '2015-01-02 16:20',
                    'arrival_datetime': '2015-01-02 16:43',
                    'start_date': '2015-01-02',
                },
            ]),
        }

        with mock.patch.object(transfers, 'get_transfer_variants') as m_get_transfer_variants:
            m_get_transfer_variants.return_value = transfer_variants
            result = search_on_date(
                request=create_request(),
                point_from=station1,
                point_to=station2,
                point_from_reduced=False,
                point_to_reduced=True,
                timezone=MSK_TZ,
                departure_date=departure_day,
                days_ahead=2,
                tomorrow_upto=4,
                transfers_mode=TransfersMode.ALL,
            )

            assert len(m_get_transfer_variants.call_args_list) == 1
            assert m_get_transfer_variants.call_args_list[0] == mock.call(
                station1, station2, departure_day,
                [TransportType.objects.get(id=TransportType.SUBURBAN_ID).code]
            )

        assert 'narrowed_from' in result
        assert result['narrowed_from'] is None
        assert 'narrowed_to' in result
        assert result['narrowed_to'] == 'esr2'

        assert 'date_time' in result
        assert_that(result['date_time'], has_only_entries({
            'date': '2015-01-01',
            'server_time': '2014-12-31T23:13:18+00:00',
            'day_start_utc': '2014-12-31T21:00:00+00:00',
            'day_end_utc': '2015-01-01T20:59:59+00:00',
            'days_ahead': 2,
        }))

        assert 'teasers' in result
        assert len(result['teasers']) == 1
        assert_that(result['teasers'][0], has_only_entries({
            'title': 'Мой Тизер',
            'content': 'длинный текстище',
            'mobile_content': 'короткий текстик',
            'url': 'https://ya.ru',
            'image_url': None,
            'id': 6501510990188215577,
            'selected': True,
        }))

        assert 'sup_tags' in result
        assert_that(result['sup_tags'], has_only_entries({
            'suburban_city': [],
            'suburban_station': contains_inanyorder('esr1', 'esr2'),
            'suburban_direction': []
        }))

        assert 'settings' in result
        assert_that(result['settings'], has_entries({
            'auto_update_interval': conf.SUBURBAN_APP_AUTO_UPDATE_INTERVAL,
            'info_banner': conf.SUBURBAN_INFO_BANNER,
            'ugc_notification_scheduled_time_send_seconds': conf.UGC_NOTIFICATION_SCHEDULED_TIME_SEND_SECONDS,
            'ugc_notification_timeout_seconds': conf.UGC_NOTIFICATION_TIMEOUT_SECONDS,
            'ugc_station_near_distance_metres': conf.UGC_STATION_NEAR_DISTANCE_METRES,
            'promo_search': conf.SUBURBAN_PROMO_SEARCH,
            'promo_favorites': conf.SUBURBAN_PROMO_FAVORITES,
            'promo_station': conf.SUBURBAN_PROMO_STATION,
            'drive_integration': conf.SUBURBAN_DRIVE_INTEGRATION,
            'music_integration': conf.SUBURBAN_MUSIC_INTEGRATION,
            'aeroex_selling': conf.SUBURBAN_AEROEX_SELLING_ENABLED,
            'movista_selling': conf.SUBURBAN_MOVISTA_SELLING_ENABLED,
            'im_selling': conf.SUBURBAN_IM_SELLING_ENABLED,
            'polling_max_orders_count_in_request': conf.SUBURBAN_POLLING_MAX_ORDERS_COUNT_IN_REQUEST,
            'polling_orders_first_time_step': conf.SUBURBAN_POLLING_ORDERS_FIRST_TIME_STEP,
            'polling_orders_exp_backoff': conf.SUBURBAN_POLLING_ORDERS_EXP_BACKOFF,
            'polling_orders_max_calls_count': conf.SUBURBAN_POLLING_ORDERS_MAX_CALLS_COUNT,
            'polling_orders_max_minutes_offset': conf.SUBURBAN_POLLING_ORDERS_MAX_MINUTES_OFFSET,
            'polling_orders_show_spinner_time': conf.SUBURBAN_POLLING_ORDERS_SHOW_SPINNER_TIME,
        }))

        # assert 'subscription_allowed' in result
        # assert result['subscription_allowed'] is True

        assert 'days' in result
        assert len(result['days']) == 3

        day = result['days'][0]
        assert day['date'] == '2015-01-01'
        assert day['day_start_utc'] == '2014-12-31T21:00:00+00:00'
        assert day['day_end_utc'] == '2015-01-01T20:59:59+00:00'
        assert len(day['segments']) == 2

        segment = day['segments'][0]
        assert_that(segment['thread'], has_only_entries({
            'title': 'thread_title2',
            'title_short': 'thread_title_short2',
            'uid': 'thread_uid2',
            'canonical_uid': 'R_canonical_uid2',
            'number': '2',
            'start_time': '2015-01-01T14:20:00+03:00',
            'type': 'update',
        }))
        assert_that(segment['departure'], has_only_entries({
            'time': '2015-01-01T14:20:00+03:00',
            'time_utc': '2015-01-01T11:20:00+00:00',
            'station': 'esr1',
            'platform': 'платформа2_1а',
            'state': {
                'type': 'fact',
                'key': expected_rts2_1_key,
                'fact_time': '2015-01-01T14:30:00+03:00',
                'minutes_from': 5,
                'minutes_to': 105,
            },
        }))
        assert_that(segment['arrival'], has_only_entries({
            'time': '2015-01-01T15:10:00+03:00',
            'time_utc': '2015-01-01T12:10:00+00:00',
            'station': 'esr2',
            'state': {
                'type': 'possible_delay',
                'key': expected_rts2_2_key,
                'minutes_from': 2,
                'minutes_to': 5
            },
        }))
        assert segment['duration'] == 50
        assert segment['days'] == 'только 1, 2 января'
        assert segment['stops'] == 'то тут, то там'
        assert_that(segment['tariff'], has_only_entries({
            'currency': 'RUR',
            'value': 100500,
        }))
        assert segment['tariffs_ids'] == [901]

        segment = day['segments'][1]
        assert_that(segment['thread'], has_only_entries({
            'title': 'thread_title3',
            'title_short': 'thread_title3',
            'uid': 'thread_uid3',
            'canonical_uid': 'R_canonical_uid3',
            'number': '3333',
            'start_time': '2015-01-01T14:30:00+03:00',
            'transport': {
                'express_type': 'express',
                'subtype': {
                    'code': 'megatransport',
                    'title': 'мегатранспорт',
                    'color': '#000000',
                }
            },
            'facilities': [
                {
                    'code': 'wifi',
                    'icon': 'https://static/wifi.svg',
                    'title': 'вайфайчег'
                },
                {
                    'code': 'bike',
                    'icon': None,
                    'title': 'байк'
                }
            ],
        }))
        assert_that(segment['departure'], has_only_entries({
            'time': '2015-01-01T14:50:00+03:00',
            'time_utc': '2015-01-01T11:50:00+00:00',
            'station': 'esr1',
            'state': {
                'type': 'possible_delay',
                'key': expected_rts3_2_key,
                'minutes_from': 111,
                'minutes_to': 222
            },
            'arrival_state': {
                'type': 'possible_delay',
                'key': expected_rts3_2_key,
                'minutes_from': 333,
                'minutes_to': 444
            }
        }))
        assert_that(segment['arrival'], has_only_entries({
            'time': '2015-01-01T16:50:00+03:00',
            'time_utc': '2015-01-01T13:50:00+00:00',
            'station': 'esr2',
            'platform': 'платформа3_3а',
            'state': {
                'type': 'undefined',
                'key': expected_rts3_3_key
            },
            'departure_state': {
                'type': 'undefined',
                'key': expected_rts3_3_key
            }
        }))
        assert segment['duration'] == 120
        assert segment['days'] == 'я здесь, я там, я всегда'
        assert segment['except'] == '1, 8' + NBSP + 'мая'
        assert segment['stops'] is None
        assert_that(segment['tariff'], has_only_entries({
            'currency': 'USD',
            'value': 666.56,
        }))
        assert segment['tariffs_ids'] == [902]

        day = result['days'][1]
        assert day['date'] == '2015-01-02'
        assert day['day_start_utc'] == '2015-01-01T21:00:00+00:00'
        assert day['day_end_utc'] == '2015-01-02T20:59:59+00:00'
        assert len(day['segments']) == 2

        transfer = day['segments'][0]
        assert transfer['transfer_points'] == [{'title': 'пересадка тут'}]
        assert transfer['duration'] == 153
        assert transfer['is_transfer'] is True
        assert_that(transfer['tariff'], has_only_entries({
            'currency': 'BYN',
            'value': 579,
        }))
        assert len(transfer['segments']) == 2

        segment = transfer['segments'][0]
        assert_that(segment['thread'], has_only_entries({
            'title': 'thread_title1',
            'title_short': 'thread_title1',
            'uid': 'thread_uid1',
            'canonical_uid': 'R_canonical_uid1',
            'number': '1111',
            'start_time': '2015-01-02T14:10:00+03:00',
            'facilities': [
                {
                    'code': 'bike',
                    'icon': None,
                    'title': 'байк'
                },
            ],
        }))
        assert_that(segment['departure'], has_only_entries({
            'time': '2015-01-02T14:10:00+03:00',
            'time_utc': '2015-01-02T11:10:00+00:00',
            'station': 'esr1',
        }))
        assert_that(segment['arrival'], has_only_entries({
            'time': '2015-01-02T15:00:00+03:00',
            'time_utc': '2015-01-02T12:00:00+00:00',
            'station': 'esr3',
        }))
        assert segment['duration'] == 50
        assert segment['days'] == 'только 2 января'
        assert segment['stops'] == 'остановки для thread1'
        assert_that(segment['tariff'], has_only_entries({
            'currency': 'BYN',
            'value': 456,
        }))
        assert segment['tariffs_ids'] == [904]

        segment = transfer['segments'][1]
        assert_that(segment['thread'], has_only_entries({
            'title': 'thread_title4',
            'title_short': 'thread_title4',
            'uid': 'thread_uid4',
            'canonical_uid': 'R_canonical_uid4',
            'number': '4444',
            'start_time': '2015-01-02T16:20:00+03:00',
        }))
        assert_that(segment['departure'], has_only_entries({
            'time': '2015-01-02T16:20:00+03:00',
            'time_utc': '2015-01-02T13:20:00+00:00',
            'station': 'esr3',
            'state': {
                'key': expected_rts4_3_key_day2,
                'type': 'undefined'
            }
        }))
        assert_that(segment['arrival'], has_only_entries({
            'time': '2015-01-02T16:43:00+03:00',
            'time_utc': '2015-01-02T13:43:00+00:00',
            'station': 'esr2',
            'platform': 'платформа4_2',
            'state': {
                'type': 'possible_delay',
                'key': expected_rts4_2_key_day2,
                'minutes_from': 10,
                'minutes_to': 20
            },
        }))
        assert segment['duration'] == 23
        assert segment['days'] == 'только 2 января'
        assert segment['stops'] is None
        assert_that(segment['tariff'], has_only_entries({
            'currency': 'BYN',
            'value': 123,
        }))
        assert segment['tariffs_ids'] == [903]

        segment = day['segments'][1]
        assert_that(segment['thread'], has_only_entries({
            'title': 'thread_title2',
            'title_short': 'thread_title_short2',
            'uid': 'thread_uid2',
            'canonical_uid': 'R_canonical_uid2',
            'number': '2',
            'start_time': '2015-01-02T14:20:00+03:00',
            'type': 'update',
        }))
        assert_that(segment['departure'], has_only_entries({
            'time': '2015-01-02T14:20:00+03:00',
            'time_utc': '2015-01-02T11:20:00+00:00',
            'station': 'esr1',
            'platform': 'платформа2_1а',
            'state': {
                'key': expected_rts2_1_key_day2,
                'type': 'undefined'
            }
        }))
        assert_that(segment['arrival'], has_only_entries({
            'time': '2015-01-02T15:10:00+03:00',
            'time_utc': '2015-01-02T12:10:00+00:00',
            'station': 'esr2',
            'state': {
                'type': 'possible_delay',
                'key': expected_rts2_2_key_day2,
                'minutes_from': 6,
                'minutes_to': 7,
            },
        }))
        assert segment['duration'] == 50
        assert segment['days'] == 'только 1, 2 января'
        assert segment['stops'] == 'то тут, то там'
        assert_that(segment['tariff'], has_only_entries({
            'currency': 'RUR',
            'value': 100500,
        }))
        assert segment['tariffs_ids'] == [901]

        day = result['days'][2]
        assert day['date'] == '2015-01-03'
        assert day['day_start_utc'] == '2015-01-02T21:00:00+00:00'
        assert day['day_end_utc'] == '2015-01-03T20:59:59+00:00'
        assert len(day['segments']) == 1

        segment = day['segments'][0]
        assert_that(segment['thread'], has_only_entries({
            'title_short': None,
            'title': None,
            'start_time': '2015-01-03T03:20:00+03:00',
            'uid': 'thread_uid5',
            'canonical_uid': 'R_canonical_uid5',
            'number': None
        }))
        assert_that(segment['departure'], has_only_entries({
            'time_utc': '2015-01-03T00:20:00+00:00',
            'station': 'esr1',
            'time': '2015-01-03T03:20:00+03:00'
        }))
        assert_that(segment['arrival'], has_only_entries({
            'time_utc': '2015-01-03T01:10:00+00:00',
            'station': 'esr2',
            'time': '2015-01-03T04:10:00+03:00'
        }))
        assert segment['duration'] == 50
        assert segment['days'] == 'только 3 января'
        assert segment['stops'] is None
        assert segment['tariffs_ids'] == []

        assert 'tariffs' in result
        assert len(result['tariffs']) == 4
        assert_that(result['tariffs'], contains_inanyorder(
            has_only_entries({
                'category': 'usual',
                'title': 'mega',
                'code': 'mega_tariff',
                'description': 'mega description',
                'url': '',
                'order': 2,
                'is_main': False,
                'price': has_only_entries({'currency': 'RUR', 'value': 100500}),
                'id': 901,
            }),
            has_only_entries({
                'category': 'usual',
                'title': 'express',
                'code': 'express_tariff',
                'description': 'exp description',
                'url': '',
                'order': 50,
                'is_main': False,
                'price': has_only_entries({'currency': 'USD', 'value': 666.56}),
                'id': 902,
            }),
            has_only_entries({
                'category': 'usual',
                'title': 'uber',
                'code': 'uber_tariff',
                'description': 'uber description',
                'url': '',
                'order': 1,
                'is_main': False,
                'price': has_only_entries({'currency': 'BYN', 'value': 123}),
                'id': 903,
            }),
            has_only_entries({
                'category': 'usual',
                'title': 'uber',
                'code': 'uber_tariff',
                'description': 'uber description',
                'url': '',
                'order': 1,
                'is_main': False,
                'price': has_only_entries({'currency': 'BYN', 'value': 456}),
                'id': 904,
            }),
        ))
