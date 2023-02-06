# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, time, date, timedelta
from itertools import chain

import mock
import pytest
import pytz
from hamcrest import assert_that, has_entries, contains_inanyorder
from iso8601 import parse_date

from common.apps.info_center.models import Info
from common.apps.suburban_events.api import EventStateType
from common.apps.suburban_events.factories import ThreadStationStateFactory
from common.models.factories import create_external_direction, create_info, create_aeroex_tariff
from common.models.schedule import Company, TrainPurchaseNumber
from common.models.tariffs import TariffType, TariffTypeCode, SuburbanSellingFlow
from common.models.transport import TransportType, TransportSubtype
from common.models_abstract.schedule import ExpressType
from common.tester.factories import (
    create_rthread_segment, create_settlement, create_suburban_zone, create_transport_subtype, create_company
)
from common.tester.utils.datetime import replace_now
from common.tester.utils.django_cache import clear_cache_until_switch
from common.tester.utils.replace_setting import replace_setting, replace_dynamic_setting, ReplaceAttr
from common.utils.date import MSK_TZ

from route_search.factories import create_transfer_variant

from travel.rasp.export.export.v3.core import helpers
from travel.rasp.export.export.v3.core import search
from travel.rasp.export.export.v3.core.helpers import fill_thread_local_start_dt
from travel.rasp.export.export.v3.core.search import (
    TransfersMode, build_segment_data, build_segments_subscription, common_search,
    get_segments_subscription_objects, log, search_on_all_days, search_on_date
)
from travel.rasp.export.export.v3.core.tariffs import train_tariffs
from travel.rasp.export.export.v3.selling.suburban import SELLING_V3

from travel.rasp.export.tests.v3.factories import create_station, create_thread
from travel.rasp.export.tests.v3.helpers import create_request, MOCK_TRAIN_RESPONSE
from travel.rasp.export.tests.v3.selling.factories import set_suburban_selling_response


pytestmark = [pytest.mark.dbuser('module'), pytest.mark.mongouser('module')]


class TestCommonSearch(object):
    def test_log_search_called(self):
        with mock.patch('travel.rasp.export.export.v3.core.search.log_search') as m_log_search:
            departure = MSK_TZ.localize(datetime(2015, 10, 8))
            arrival = MSK_TZ.localize(datetime(2015, 10, 8))
            segments = [
                create_rthread_segment(departure=departure, arrival=arrival, start_date=date(2015, 10, 8)),
                create_rthread_segment(departure=departure, arrival=arrival, start_date=date(2015, 10, 8)),
            ]
            point_from, point_to = create_settlement(), create_station()
            request = create_request('/bla?date=2015-10-08')

            with replace_setting('USERS_SEARCH_LOG', None):
                common_search(request, segments, point_from, point_to, False, False, True)
                assert m_log_search.call_count == 0

            with replace_setting('USERS_SEARCH_LOG', 'somepath'):
                common_search(request, segments, point_from, point_to, False, False, True, date_=date(2015, 10, 8))

                # сегменты могут быть получены в разном порядке
                try:
                    m_log_search.assert_called_once_with(
                        log, request, point_from, point_to, date(2015, 10, 8), segments
                    )
                except AssertionError:
                    rev_segments = list(reversed(segments))
                    m_log_search.assert_called_once_with(
                        log, request, point_from, point_to, date(2015, 10, 8), rev_segments
                    )

    @replace_now(datetime(2015, 10, 8, 21))
    def test_necessary_fields(self):
        departure = MSK_TZ.localize(datetime(2015, 10, 8))
        arrival = MSK_TZ.localize(datetime(2015, 10, 8))
        segments = [
            create_rthread_segment(departure=departure, arrival=arrival, start_date=date(2015, 10, 8)),
        ]
        point_from, point_to = create_settlement(), create_station()
        request = create_request()

        with mock.patch('travel.rasp.export.export.v3.core.search.get_search_teasers', return_value=mock.sentinel.teasers), \
                replace_dynamic_setting('SUBURBAN_APP_AUTO_UPDATE_INTERVAL', 333), \
                replace_dynamic_setting('UGC_NOTIFICATION_SCHEDULED_TIME_SEND_SECONDS', 11), \
                replace_dynamic_setting('UGC_NOTIFICATION_TIMEOUT_SECONDS', 22), \
                replace_dynamic_setting('UGC_STATION_NEAR_DISTANCE_METRES', 33), \
                replace_dynamic_setting('SUBURBAN_PROMO_SEARCH', False), \
                replace_dynamic_setting('SUBURBAN_PROMO_FAVORITES', False), \
                replace_dynamic_setting('SUBURBAN_PROMO_STATION', True):

            search_dict = common_search(request, segments, point_from, point_to, False, False, False)
            assert_that(search_dict, has_entries({
                'date_time': has_entries({
                    'server_time': '2015-10-08T18:00:00+00:00'
                }),
                'narrowed_from': None,
                'narrowed_to': None,
                'settings': has_entries({
                    'auto_update_interval': 333,
                    'info_banner': False,
                    'ugc_notification_scheduled_time_send_seconds': 11,
                    'ugc_notification_timeout_seconds': 22,
                    'ugc_station_near_distance_metres': 33,
                    'promo_search': False,
                    'promo_favorites': False,
                    'promo_station': True,
                    'drive_integration': False,
                    'music_integration': False,
                    'aeroex_selling': True,
                    'movista_selling': True,
                    'im_selling': True
                }),
                'subscription_allowed': False,
                'tariffs': []
            }))


class TestSearchOnDate(object):
    def test_add_facilities(self):
        with mock.patch('travel.rasp.export.export.v3.core.search.fill_segments_suburban_facilities') as m_fill_facilities, \
                mock.patch('travel.rasp.export.export.v3.core.search.find_suburban_and_train') as m_find_segments:

            segments = [
                create_rthread_segment(
                    departure=MSK_TZ.localize(datetime(2017, 11, 13, 12, _)),
                    arrival=MSK_TZ.localize(datetime(2017, 11, 13)),
                    start_date=date(2017, 11, 3)
                )
                for _ in range(3)
            ]

            m_find_segments.return_value = segments
            station_from, station_to = create_station(), create_station()

            request = create_request('/?date=2017-11-14')
            search_on_date(request, station_from, station_to, False, False, MSK_TZ, datetime(2017, 11, 14), 1, 0)
            assert m_fill_facilities.call_args_list[0][0][0] == []

            request = create_request('/?date={}'.format('2017-11-13'))
            search_on_date(request, station_from, station_to, False, False, MSK_TZ, datetime(2017, 11, 13), 1, 0)
            assert m_fill_facilities.call_args_list[1][0][0] == segments

    def test_segments_sort(self):
        station1, station2 = create_station(), create_station()

        create_thread_with_schedule = create_thread.mutate(
            __={'calculate_noderoute': True},
            schedule_v1=[
                [None, 0, station1],
                [50, None, station2],
            ],
        )

        departure_day = datetime.today()
        departure_time = time(14, 20)
        threads = [
            create_thread_with_schedule(
                year_days=[departure_day + timedelta(days=1)],
                tz_start_time=departure_time.replace(minute=departure_time.minute - 10),
            ),
            create_thread_with_schedule(
                year_days=[departure_day],
                tz_start_time=departure_time,
            ),
            create_thread_with_schedule(
                year_days=[departure_day],
                tz_start_time=departure_time.replace(minute=departure_time.minute + 10),
            ),
        ]

        def check(request, days_ahead, threads_num):
            search_dict = search_on_date(request, station1, station2, False, False, MSK_TZ, departure_day, days_ahead, 0)
            segments = list(chain(*(d['segments'] for d in search_dict['days'])))
            assert len(segments) == threads_num
            departures = [s['departure']['time'] for s in segments]
            assert len(set(departures)) == threads_num
            assert [parse_date(dep) for dep in departures]
            assert departures == sorted(departures)

        check(
            create_request(),
            2,
            len(threads),
        )
        check(
            create_request(),
            1,
            len(threads) - 1,  # на текущий день только 2 нитки
        )

    def test_subscriptions(self):
        station_from, station_to = create_station(), create_station()

        with mock.patch.object(search, 'build_segments_subscription') as m_subs, \
             mock.patch.object(search, 'find_segments_on_date') as m_find_segments:

            segments = [create_rthread_segment(start_date=datetime.now()) for _ in range(3)]
            m_find_segments.return_value = segments
            m_subs.return_value = mock.sentinel.subs

            request = create_request('/?date=2017-11-14')
            res = search_on_date(request, station_from, station_to, False, False, MSK_TZ, datetime.now(), 1, 0)
            m_subs.assert_called_once_with(segments)
            assert res['sup_tags'] is mock.sentinel.subs

    def test_teasers(self):
        station_from, station_to = create_station(), create_station()
        create_info(text='cont_1', text_short='', services=[Info.Service.MOBILE_APPS], stations=[station_from]),
        create_info(
            text='cont_3', text_short='mobile_cont_3', services=[Info.Service.WEB], stations=[station_from]
        )
        info1 = create_info(
            services=[Info.Service.MOBILE_APPS],
            stations=[station_from],
            text='cont_2',
            text_short='Очень тизер',
            title='Тизер же!',
            url='http://moar.teasers.ru',
            uuid=123,
        )

        request = create_request('/?date=2015-11-14')
        res = search_on_date(request, station_from, station_to, False, False, MSK_TZ, datetime.now(), 1, 0)
        assert res['teasers'] == [
            {
                'title': u'Тизер же!',
                'mobile_content': u'Очень тизер',
                'url': 'http://moar.teasers.ru',
                'selected': True,
                'content': u'cont_2',
                'image_url': None,
                'id': info1.uuid
            }
        ]

    def test_teasers_mocked(self):
        departure = MSK_TZ.localize(datetime(2015, 10, 8, 11))
        arrival = MSK_TZ.localize(datetime(2015, 10, 8, 12))
        segments = [
            create_rthread_segment(departure=departure, arrival=arrival, start_date=date(2015, 10, 8)),
            create_rthread_segment(departure=departure, arrival=arrival, start_date=date(2015, 10, 8)),
        ]
        point_from, point_to = create_settlement(), create_station()

        with mock.patch.object(search, 'find_segments_on_date', mock.Mock(return_value=segments)), \
             mock.patch('travel.rasp.export.export.v3.core.search.get_search_teasers', return_value=mock.sentinel.teasers) as m_teasers:

            request = create_request('/?date=2015-11-14')
            res = search_on_date(
                request, point_from, point_to, False, False, MSK_TZ, datetime.now(), 1, 0,
                national_version='aaa',
                lang='bbb',
            )
            assert res['teasers'] == mock.sentinel.teasers
            m_teasers_call = m_teasers.call_args_list[0]
            call = mock.call(
                segments, point_from, point_to,
                national_version='aaa',
                lang='bbb',
            )
            assert m_teasers_call == call

    def test_transfers_mocked(self):
        point_from, point_to = create_station(), create_station()
        departure = MSK_TZ.localize(datetime(2015, 10, 8))
        arrival = MSK_TZ.localize(datetime(2015, 10, 8))
        today = datetime(2015, 10, 8)

        segments = [
            create_rthread_segment(departure=departure, arrival=arrival, start_date=date(2015, 10, 8)),
            create_rthread_segment(departure=departure, arrival=arrival, start_date=date(2015, 10, 8)),
        ]

        variant1 = create_transfer_variant([{'departure_station_id': 20}])
        variant2 = create_transfer_variant([{'departure_station_id': i} for i in range(3)])
        variants = [variant1, variant2]
        variants_segments = list(chain.from_iterable(v.segments for v in variants))

        with mock.patch.object(search, 'find_segments_on_date', mock.Mock(return_value=segments)), \
                mock.patch.object(search, 'common_search', wraps=search.common_search) as m_common_search, \
                mock.patch.object(search, 'build_segments_on_date', wraps=search.build_segments_on_date) as m_build, \
                mock.patch.object(search, '_get_segments_states', wraps=search._get_segments_states) as m_get_states, \
                mock.patch.object(
                    search, 'fill_segments_suburban_facilities',
                    wraps=search.fill_segments_suburban_facilities
                ) as m_fill_facilities, \
                mock.patch.object(search, 'get_search_teasers', wraps=search.get_search_teasers) as m_get_search_teasers, \
                mock.patch.object(search, '_get_transfers') as m_get_transfers, \
                mock.patch.object(search, 'build_segments_subscription') as m_build_segments_subscription:

            m_get_transfers.return_value = variants
            m_get_states.return_value = {'aaa': 123}

            search_on_date(create_request(), point_from, point_to, False, False, MSK_TZ, datetime.now(), 1, 0)
            assert not m_get_transfers.call_args_list
            assert m_common_search.call_args_list[0][0][1] == segments
            assert m_build.call_args_list[0][0][0] == segments
            assert m_get_search_teasers.call_args_list[0][0][0] == segments

            search_on_date(
                create_request(), point_from, point_to, False, False, MSK_TZ, today, 1, 0,
                transfers_mode=TransfersMode.ALL
            )
            assert m_get_transfers.call_args_list[0] == mock.call(point_from, point_to, today)
            assert m_common_search.call_args_list[1][0][1] == segments + variants_segments
            assert m_build.call_args_list[1][0][0] == segments + variants
            assert m_get_states.call_args_list[1][0][0] == segments + variants_segments
            assert m_build.call_args_list[1][1]['segments_states'] == m_get_states.return_value
            assert m_fill_facilities.call_args_list[1][0][0] == segments + variants_segments
            assert m_get_search_teasers.call_args_list[1][0][0] == segments
            assert m_build_segments_subscription.call_args_list[1][0][0] == segments

    @replace_setting('SUBURBAN_SELLING_URL', 'https://sellingurl.net')
    def test_suburban_selling_info(self, httpretty):
        selling_tariffs = [
            {
                'provider': 'movista',
                'tariffs': [
                    {
                        'partner': 'cppk', 'id': 1, 'price': 56.12,
                        'valid_from': '2020-10-24T00:00:00+03:00',
                        'valid_until': '2020-10-25T03:00:00+03:00',
                    }
                ]
            },
            {
                'provider': 'aeroexpress',
                'tariffs': [
                    {
                        'partner': 'aeroexpress', 'id': 2, 'menu_id': 80,
                        'valid_from': '2020-10-23T00:00:00+03:00',
                        'valid_until': '2020-11-25T00:00:00+03:00',
                    },
                    {
                        'partner': 'aeroexpress', 'id': 3, 'menu_id': 82,
                        'valid_from': '2020-10-23T00:00:00+03:00',
                        'valid_until': '2020-11-25T00:00:00+03:00'
                    }
                ]
            }
        ]

        keys = [
            {
                'key': {
                    'date': '2020-10-24',
                    'station_from': 42,
                    'station_to': 43,
                    'company': Company.CPPK_ID,
                    'tariff_type': 'etrain',
                },
                'tariff_ids': [1],
                'provider': 'movista'
            },
            {
                'key': {
                    'date': '2020-10-24',
                    'station_from': 42,
                    'station_to': 43,
                    'company': Company.AEROEXPRESS_ID,
                    'tariff_type': 'aeroexpress',
                },
                'tariff_ids': [2, 3],
                'provider': 'aeroexpress'
            }
        ]

        set_suburban_selling_response(httpretty, selling_tariffs, keys)

        dt = datetime(2020, 10, 24)
        station1 = create_station(id=42)
        station2 = create_station(id=43)
        cppk_company = create_company(id=Company.CPPK_ID)
        aeroexpress_company = create_company(id=Company.AEROEXPRESS_ID)

        usual_tariff = TariffType.objects.get(code=TariffTypeCode.USUAL)
        create_aeroex_tariff(type=usual_tariff, station_from=station1, station_to=station2)

        aeroex_tariff = TariffType.objects.get(code=TariffTypeCode.AEROEXPRESS)
        create_aeroex_tariff(type=aeroex_tariff, station_from=station1, station_to=station2)

        create_thread_on_date = create_thread.mutate(
            __={'calculate_noderoute': True},
            year_days=[dt.date()],
            schedule_v1=[[None, 0, station1], [50, None, station2]]
        )

        create_thread_on_date(
            company=cppk_company,
            express_type=ExpressType.COMMON,
            tariff_type=usual_tariff,

        )
        create_thread_on_date(
            company=aeroexpress_company,
            express_type=ExpressType.AEROEXPRESS,
            tariff_type=aeroex_tariff,
        )

        search_dict = search_on_date(
            create_request(), station1, station2, False, False, MSK_TZ, dt.date(), 1, 0,
            selling_version=SELLING_V3, selling_flows=[SuburbanSellingFlow.AEROEXPRESS, SuburbanSellingFlow.VALIDATOR]
        )

        assert 'selling_tariffs' in search_dict
        selling_tariffs = search_dict['selling_tariffs']
        segments = search_dict['days'][0]['segments']

        assert len(selling_tariffs) == 3
        assert_that(selling_tariffs, contains_inanyorder(
            has_entries({'partner': 'cppk', 'provider': 'movista', 'id': 1, 'price': 56.12}),
            has_entries({'partner': 'aeroexpress', 'provider': 'aeroexpress', 'id': 2, 'menu_id': 80}),
            has_entries({'partner': 'aeroexpress', 'provider': 'aeroexpress', 'id': 3, 'menu_id': 82}),
        ))

        assert len(segments) == 2
        assert_that(segments, contains_inanyorder(
            has_entries({'selling_tariffs_ids': [1]}),
            has_entries({'selling_tariffs_ids': contains_inanyorder(2, 3)})
        ))

        assert 'selling_partners' in search_dict
        selling_partners = search_dict['selling_partners']

        assert_that(selling_partners, contains_inanyorder(
            has_entries({
                'code': 'cppk',
                'provider': 'movista',
                'ogrn': 111,
                'title': 'ЦППК',
                'address': 'дом',
                'work_time': 'всегда'
            }),
            has_entries({
                'code': 'aeroexpress',
                'provider': 'aeroexpress',
                'ogrn': 222,
                'title': 'Аэроэкспресс',
                'address': 'улица',
                'work_time': 'иногда'
            })
        ))

    def test_train_selling_info(self):
        station_1, station_2 = create_station(id=666), create_station(id=667)
        dt = datetime(2018, 9, 10, 12)
        t_subtype = create_transport_subtype(t_type=TransportSubtype.SUBURBAN_ID, has_train_tariffs=True)

        thread_1 = create_thread(
            __={'calculate_noderoute': True},
            year_days=[dt.date()],
            tz_start_time=dt.time(),
            t_subtype=t_subtype,
            schedule_v1=[
                [None, 0, station_1],
                [50, None, station_2],
            ]
        )
        thread_2 = create_thread(
            __={'calculate_noderoute': True},
            year_days=[dt.date()],
            tz_start_time=(dt + timedelta(hours=3)).time(),
            t_subtype=t_subtype,
            schedule_v1=[
                [None, 0, station_1],
                [50, None, station_2],
            ]
        )

        segment_1_keys = [
            'train 119A 20180910_10',
            'train 119A 20180910_12',
            'train 120A 20180910_10',
            'train 120A 20180910_12'
        ]

        segment_2_keys = [
            'train 222Я 20180910_14',
            'train 222Я 20180910_16',
            'train 221Я 20180910_14',
            'train 221Я 20180910_16'
        ]

        TrainPurchaseNumber.objects.create(thread=thread_1, number='119A')
        TrainPurchaseNumber.objects.create(thread=thread_2, number='222Я')

        with mock.patch('travel.rasp.export.export.v3.selling.train_api.get_train_data', return_value=MOCK_TRAIN_RESPONSE) as m_tariffs, \
                mock.patch.object(train_tariffs, 'get_train_wizard_data'), \
                replace_setting('ALIGN_SEARCH_SEGMENT_KEYS', True), \
                replace_setting('TRAIN_ORDER_DOMAIN', 'touch.train.ru'):
            search_dict = search_on_date(
                create_request(), station_1, station_2, False, False, MSK_TZ, dt.date(), 1, 0,
                selling_version=SELLING_V3, selling_flows=[SuburbanSellingFlow.VALIDATOR]
            )
            segments = search_dict['days'][0]['segments']
            m_tariffs.assert_called_once_with(
                {
                    'date': dt.date(),
                    'point_to': 's667',
                    'point_from': 's666'
                },
                'https://testing.train-api.rasp.internal.yandex.net/ru/api/segments/train-tariffs/'
            )

            assert search_dict['train_tariffs_polling'] is False
            assert_that(segments, contains_inanyorder(
                has_entries({
                    'selling_info': {
                        'type': 'train',
                        'tariffs': [
                            {
                                'seats': 40,
                                'class_name': 'СВ',
                                'currency': 'RUB',
                                'order_url': 'touch.train.ru/order/?coachType=suite&utm_source=suburbans',
                                'class': 'suite',
                                'value': 10000
                            },
                            {
                                'seats': 150,
                                'class_name': 'сидячие',
                                'currency': 'RUB',
                                'order_url': 'touch.train.ru/order/?coachType=sitting&utm_source=suburbans',
                                'class': 'sitting',
                                'value': 59.31
                            }
                        ]
                    },
                    'train_keys': segment_1_keys
                }),
                has_entries({
                    'selling_info': {
                        'type': 'train',
                        'tariffs': [{
                            'seats': 10,
                            'class_name': 'сидячие',
                            'currency': 'RUB',
                            'order_url': 'touch.train.ru/order/?coachType=sitting&utm_source=suburbans',
                            'class': 'sitting',
                            'value': 1500
                        }]
                    },
                    'train_keys': segment_2_keys
                })
            ))

        train_data = {
            'segments': [],
            'querying': True
        }
        with mock.patch('travel.rasp.export.export.v3.selling.train_api.get_train_data', return_value=train_data), \
             mock.patch.object(train_tariffs, 'get_train_wizard_data'), \
                replace_setting('ALIGN_SEARCH_SEGMENT_KEYS', True):
            search_dict = search_on_date(
                create_request(), station_1, station_2, False, False, MSK_TZ, dt.date(), 1, 0,
                selling_version=SELLING_V3, selling_flows=[SuburbanSellingFlow.VALIDATOR]
            )
            assert search_dict['train_tariffs_polling'] is True
            segments = search_dict['days'][0]['segments']
            assert all(segment.get('selling_info') is None for segment in segments)
            assert_that(segments, contains_inanyorder(
                has_entries({
                    'train_keys': segment_1_keys
                }),
                has_entries({
                    'train_keys': segment_2_keys
                })
            ))

    def test_train_tariffs(self):
        station_1, station_2 = create_station(id=666), create_station(id=667)
        dt = datetime(2018, 9, 10, 12)
        t_subtype = create_transport_subtype(t_type=TransportSubtype.SUBURBAN_ID, has_train_tariffs=True)

        thread_1 = create_thread(
            __={'calculate_noderoute': True},
            year_days=[dt.date()],
            tz_start_time=dt.time(),
            t_subtype=t_subtype,
            schedule_v1=[
                [None, 0, station_1],
                [50, None, station_2],
            ]
        )
        thread_2 = create_thread(
            __={'calculate_noderoute': True},
            year_days=[dt.date()],
            tz_start_time=(dt + timedelta(hours=3)).time(),
            t_subtype=t_subtype,
            schedule_v1=[
                [None, 0, station_1],
                [40, None, station_2],
            ]
        )

        TrainPurchaseNumber.objects.create(thread=thread_1, number='119A')
        TrainPurchaseNumber.objects.create(thread=thread_2, number='222Я')

        with mock.patch.object(train_tariffs, 'get_train_wizard_data', autospec=True) as m_search, \
                mock.patch('travel.rasp.export.export.v3.selling.train_api.get_train_data'):

            m_search.return_value = {
                'segments': [
                    {
                        'places': {'records': [
                            {
                                'count': 100,
                                'price': {
                                    'currency': 'RUB',
                                    'value': '1800.05'
                                },
                                'coach_type': 'sitting'
                            },
                            {
                                'count': 13,
                                'price': {
                                    'currency': 'RUB',
                                    'value': '2700.10'
                                },
                                'coach_type': 'platzkarte'
                            }
                        ]},
                        'train': {'number': '119A'},
                        'departure': {
                            'station': {'key': 's666'},
                            'local_datetime': {'value': '2018-09-10T12:00:00+03:00'}
                        },
                        'arrival': {
                            'station': {'key': 's667'},
                            'local_datetime': {'value': '2018-09-10T12:50:00+03:00'}
                        }
                    },
                    {
                        'places': {'records': [{
                            'count': 13,
                            'price': {
                                'currency': 'RUB',
                                'value': '1400.00'
                            },
                            'coach_type': 'sitting'
                        }]},
                        'train': {'number': '222Я'},
                        'departure': {
                            'station': {'key': 's666'},
                            'local_datetime': {'value': '2018-09-10T15:00:00+03:00'}
                        },
                        'arrival': {
                            'station': {'key': 's667'},
                            'local_datetime': {'value': '2018-09-10T15:40:00+03:00'}
                        }
                    }
                ]
            }

            search_dict = search_on_date(
                create_request(), station_1, station_2, False, False, MSK_TZ, dt.date(), 1, 0,
                selling_version=SELLING_V3, selling_flows=[SuburbanSellingFlow.VALIDATOR]
            )
            segments = search_dict['days'][0]['segments']
            assert_that(segments, contains_inanyorder(
                has_entries({
                    'train_tariffs': [
                        {
                            'class_name': 'сидячие',
                            'currency': 'RUB',
                            'class': 'sitting',
                            'value': 1800,
                            'seats': 100
                        },
                        {
                            'class_name': 'плацкарт',
                            'currency': 'RUB',
                            'class': 'platzkarte',
                            'value': 2700,
                            'seats': 13
                        }
                    ]
                }),
                has_entries({
                    'train_tariffs': [{
                        'class_name': 'сидячие',
                        'currency': 'RUB',
                        'class': 'sitting',
                        'value': 1400.0,
                        'seats': 13
                    }]
                })
            ))

    def test_suburb_train(self):
        station_1, station_2 = create_station(id=666), create_station(id=667)
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
        create_thread(
            __={'calculate_noderoute': True},
            year_days=[dt.date()],
            tz_start_time=(dt + timedelta(hours=3)).time(),
            t_type=TransportType.TRAIN_ID,
            schedule_v1=[
                [None, 0, station_1],
                [40, None, station_2],
            ]
        )

        with mock.patch('travel.rasp.export.export.v3.selling.train_api.get_train_data', return_value=MOCK_TRAIN_RESPONSE), \
                mock.patch.object(train_tariffs, 'get_train_wizard_data', autospec=True) as m_saas_search, \
                replace_setting('ALIGN_SEARCH_SEGMENT_KEYS', True), \
                replace_setting('TRAIN_ORDER_DOMAIN', 'touch.train.ru'):
            m_saas_search.return_value = {
                'segments': [
                    {
                        'places': {'records': [
                            {
                                'count': 150,
                                'price': {
                                    'currency': 'RUB',
                                    'value': '2859'
                                },
                                'coach_type': 'sitting'
                            }
                        ]},
                        'train': {'number': '119A'},
                        'departure': {
                            'station': {'key': 's666'},
                            'local_datetime': {'value': '2018-09-10T12:00:00+03:00'}
                        },
                        'arrival': {
                            'station': {'key': 's667'},
                            'local_datetime': {'value': '2018-09-10T12:50:00+03:00'}
                        }
                    }
                ]
            }

            clear_cache_until_switch()
            search_dict = search_on_date(
                create_request(), station_1, station_2, False, False, MSK_TZ, dt.date(), 1, 0,
                selling_version=SELLING_V3, selling_flows=[SuburbanSellingFlow.VALIDATOR]
            )
        assert_that(search_dict['days'][0]['segments'], contains_inanyorder(
            has_entries({
                'thread': has_entries({
                    'transport': has_entries({
                        'subtype': has_entries({
                            'code': t_subtype_code,
                            'title': 'ласточка(train)'
                        })
                    }),
                    'number': '119A',
                    'uid': 'uid119A',
                    'start_time': '2018-09-10T12:00:00+03:00'
                }),
                'duration': 50,
                'departure': {
                    'time_utc': '2018-09-10T09:00:00+00:00',
                    'station': None,
                    'time': '2018-09-10T12:00:00+03:00'
                },
                'arrival': {
                    'time_utc': '2018-09-10T09:50:00+00:00',
                    'station': None,
                    'time': '2018-09-10T12:50:00+03:00'
                },
                'days': 'только 10 сентября',
                'selling_info': {
                    'type': 'train',
                    'tariffs': [
                        {
                            'seats': 40,
                            'class_name': 'СВ',
                            'currency': 'RUB',
                            'order_url': 'touch.train.ru/order/?coachType=suite&utm_source=suburbans',
                            'class': 'suite',
                            'value': 10000
                        },
                        {
                            'seats': 150,
                            'class_name': 'сидячие',
                            'currency': 'RUB',
                            'order_url': 'touch.train.ru/order/?coachType=sitting&utm_source=suburbans',
                            'class': 'sitting',
                            'value': 59.31
                        }
                    ]
                },
                'train_tariffs': [{
                    'class_name': 'сидячие',
                    'currency': 'RUB',
                    'class': 'sitting',
                    'value': 2859.0,
                    'seats': 150
                }],
                'train_keys': [
                    'train 119A 20180910_10',
                    'train 119A 20180910_12',
                    'train 120A 20180910_10',
                    'train 120A 20180910_12'
                ]
            })
        ))


class TestSearchOnAllDays(object):
    def test_segments_sort(self):
        station1, station2 = create_station(), create_station()

        create_thread_with_schedule = create_thread.mutate(
            __={'calculate_noderoute': True},
            schedule_v1=[
                [None, 0, station1],
                [50, None, station2],
            ],
        )

        departure_day = datetime.today()
        departure_time = time(14, 20)
        threads = [
            create_thread_with_schedule(
                year_days=[departure_day + timedelta(days=1)],
                tz_start_time=departure_time.replace(minute=departure_time.minute - 10),
            ),
            create_thread_with_schedule(
                year_days=[departure_day],
                tz_start_time=departure_time,
            ),
            create_thread_with_schedule(
                year_days=[departure_day],
                tz_start_time=departure_time.replace(minute=departure_time.minute + 10),
            ),
        ]

        request = create_request()
        search_dict = search_on_all_days(request, station1, station2, False, False, MSK_TZ)
        segments = search_dict['segments']
        assert len(segments) == len(threads)
        departures = [datetime.strptime(s['departure']['time'], '%H:%M') for s in segments]
        assert len(set(departures)) == len(threads)
        assert departures == sorted(departures)

    def test_teasers(self):
        station_from, station_to = create_station(), create_station()
        create_info(text='cont_1', text_short='', services=[Info.Service.MOBILE_APPS], stations=[station_from]),
        create_info(
            text='cont_3', text_short='mobile_cont_3', services=[Info.Service.WEB], stations=[station_from]
        )
        info1 = create_info(
            services=[Info.Service.MOBILE_APPS],
            stations=[station_from],
            text='cont_2',
            text_short='Очень тизер',
            title='Тизер же!',
            url='http://moar.teasers.ru',
            uuid=123,
        )

        request = create_request()
        res = search_on_all_days(request, station_from, station_to, False, False, MSK_TZ)
        assert res['teasers'] == [
            {
                'title': u'Тизер же!',
                'mobile_content': u'Очень тизер',
                'url': 'http://moar.teasers.ru',
                'selected': True,
                'content': u'cont_2',
                'image_url': None,
                'id': info1.uuid
            }
        ]

    def test_teasers_mocked(self):
        departure = MSK_TZ.localize(datetime(2015, 10, 8, 11))
        arrival = MSK_TZ.localize(datetime(2015, 10, 8, 12))
        segments = [
            create_rthread_segment(departure=departure, arrival=arrival, start_date=date(2015, 10, 8)),
            create_rthread_segment(departure=departure, arrival=arrival, start_date=date(2015, 10, 8)),
            create_rthread_segment(departure=departure, arrival=arrival, start_date=date(2015, 10, 8)),
        ]
        point_from, point_to = create_settlement(), create_station()

        with mock.patch.object(search, 'find_segments_on_all_days', mock.Mock(return_value=segments)), \
             mock.patch('travel.rasp.export.export.v3.core.search.get_search_teasers', return_value=mock.sentinel.teasers) as m_teasers:

            request = create_request('')
            res = search_on_all_days(
                request, point_from, point_to, False, False, MSK_TZ,
                national_version='aaa',
                lang='bbb',
            )
            assert res['teasers'] == mock.sentinel.teasers

            m_teasers_call = m_teasers.call_args_list[0]
            assert set(m_teasers_call[0][0]) == set(segments)
            assert m_teasers_call[0][1] == point_from
            assert m_teasers_call[0][2] == point_to
            assert m_teasers_call[1] == {"national_version": "aaa", "lang": "bbb"}

    def test_suburb_train(self):
        station_1, station_2 = create_station(id=666), create_station(id=667)
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
            t_subtype=t_subtype,
            schedule_v1=[
                [None, 0, station_1],
                [50, None, station_2],
            ]
        )
        create_thread(
            __={'calculate_noderoute': True},
            year_days=[dt.date()],
            tz_start_time=(dt + timedelta(hours=3)).time(),
            t_type=TransportType.TRAIN_ID,
            schedule_v1=[
                [None, 0, station_1],
                [40, None, station_2],
            ]
        )

        clear_cache_until_switch()
        search_dict = search_on_all_days(create_request(), station_1, station_2, False, False, MSK_TZ)
        assert_that(search_dict['segments'], contains_inanyorder(
            has_entries({
                'thread': has_entries({
                    'transport': has_entries({
                        'express_type': 'express',
                        'subtype': has_entries({
                            'code': t_subtype_code,
                            'title': 'ласточка(train)',
                        })
                    }),
                    'number': '119A'
                })
            })
        ))

    def test_suburb_train_lastdal(self):
        station_1, station_2 = create_station(id=666), create_station(id=667)
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
            t_subtype=t_subtype,
            schedule_v1=[
                [None, 0, station_1],
                [50, None, station_2],
            ]
        )

        with ReplaceAttr('TRAIN_SUBTYPES_CODES', {t_subtype_code}, helpers):
            clear_cache_until_switch()
            search_dict = search_on_all_days(create_request(), station_1, station_2, False, False, MSK_TZ)
            assert_that(search_dict['segments'], contains_inanyorder(
                has_entries({
                    'thread': has_entries({
                        'transport': has_entries({
                            'express_type': 'express',
                            'subtype': has_entries({
                                'code': t_subtype_code,
                                'title': 'ласточка<br/>(билеты&nbsp;c указанием&nbsp;мест)'
                            })}),
                        'number': '119A'
                    })
                })
            ))


class TestBuildSegmentData(object):

    @replace_now(datetime(2015, 10, 8, 21))
    def test_build_segment_data(self):
        departure = MSK_TZ.localize(datetime(2015, 10, 8, 10))
        arrival = MSK_TZ.localize(datetime(2015, 10, 8, 12))
        station_from = create_station(title='42', __={'codes': {'esr': '100'}})
        station_to = create_station(title='43')
        thread = create_thread(uid='thread_uid',
                               canonical_uid='R_canonical_uid',
                               schedule_v1=[[None, 2, station_from],
                                            [20, None, station_to,
                                             {'platform': 'platform_43'}]])
        segment = create_rthread_segment(departure=departure, arrival=arrival, thread=thread, start_date=date(2015, 10, 8))
        fill_thread_local_start_dt([segment])
        today = date(2015, 10, 8)

        with mock.patch('travel.rasp.export.export.v3.core.search.get_thread_type',
                        return_value=mock.sentinel.thread_type), \
                mock.patch('travel.rasp.export.export.v3.core.search.get_transport_type',
                           return_value=mock.sentinel.transport), \
                mock.patch('travel.rasp.export.export.v3.core.search.get_facilities_list',
                           return_value=mock.sentinel.facilities), \
                mock.patch('travel.rasp.export.export.v3.core.search.get_segment_tariff',
                           return_value=mock.sentinel.tariff), \
                mock.patch('travel.rasp.export.export.v3.core.search.get_days_and_except_texts',
                           return_value=('__days__', '__except__')):
            segment_dict = build_segment_data(segment, today, None)
            assert segment_dict == {'arrival': {'platform': 'platform_43',
                                                'station': None,
                                                'time': '2015-10-08T12:00:00+03:00'},
                                    'days': '__days__',
                                    'stops': None,
                                    'thread': {'title_short': None,
                                               'uid': 'thread_uid',
                                               'canonical_uid': 'R_canonical_uid',
                                               'title': None,
                                               'number': None,
                                               'facilities': mock.sentinel.facilities,
                                               'type': mock.sentinel.thread_type,
                                               'transport': mock.sentinel.transport},
                                    'except': '__except__',
                                    'departure': {'station': None,
                                                  'time': '2015-10-08T10:00:00+03:00'},
                                    'tariff': mock.sentinel.tariff,
                                    'duration': 120}

        with mock.patch('travel.rasp.export.export.v3.core.search.get_days_and_except_texts', return_value=('', '')):
            segment_dict = build_segment_data(segment, today, None)
            assert segment_dict == {'arrival': {'platform': 'platform_43',
                                                'station': None,
                                                'time': '2015-10-08T12:00:00+03:00'},
                                    'stops': None,
                                    'thread': {'title_short': None,
                                               'uid': 'thread_uid',
                                               'canonical_uid': 'R_canonical_uid',
                                               'title': None,
                                               'number': None},
                                    'departure': {'station': None,
                                                  'time': '2015-10-08T10:00:00+03:00'},
                                    'duration': 120}

    @replace_now(datetime(2015, 5, 4))
    def test_timezone(self):
        now = MSK_TZ.localize(datetime(2015, 5, 4))
        segment = create_rthread_segment(
            departure=now + timedelta(hours=13, minutes=20),
            arrival=now + timedelta(hours=14, minutes=42),
            thread=create_thread(
                tz_start_time=time(13, 20),
                translated_days_texts='[{"ru": "previous days"}, {"ru": "today days"}, {"ru": "next days"}]',
            ),
            start_date=now.date())
        fill_thread_local_start_dt([segment])

        def check_call(tz, departure, arrival, days):
            segment_data = build_segment_data(segment, now.date, None, timezone=tz, time_format='%H:%M')
            assert segment_data['arrival']['time'] == arrival
            assert segment_data['departure']['time'] == departure
            assert segment_data['days'] == days

        check_call(None, '13:20', '14:42', 'today days')
        check_call(pytz.timezone('Asia/Yekaterinburg'), '15:20', '16:42', 'today days')
        check_call(pytz.timezone('Pacific/Samoa'), '23:20', '00:42', 'previous days')
        check_call(pytz.timezone('Pacific/Kiritimati'), '00:20', '01:42', 'next days')


class TestGetSegmentsSubscriptionObjects(object):
    def test_valid(self):
        settlement1, settlement2 = create_settlement(), create_settlement()
        direction = create_external_direction()

        station1 = create_station(settlement=settlement1, __={'ext_directions': [direction]})
        station2 = create_station(settlement=settlement2, __={'ext_directions': [direction]})
        station3 = create_station()

        seg1 = create_rthread_segment(station_from=station1, station_to=station2)
        seg1_2 = create_rthread_segment(station_from=station1, station_to=station2)
        seg2 = create_rthread_segment(station_from=station3, station_to=station1)
        stations_subscr, settlements_subscr, dirs_subscr, dirs_pairs_subscr = get_segments_subscription_objects(
            [seg1, seg1_2, seg2]
        )

        assert stations_subscr == {station1, station2, station3}
        assert settlements_subscr == {settlement1, settlement2}
        assert dirs_subscr == {direction}
        assert dirs_pairs_subscr == set()

    def test_directions(self):
        direction1 = create_external_direction()
        direction2 = create_external_direction()
        direction3 = create_external_direction()

        station1 = create_station(__={'ext_directions': [direction1]})
        station2 = create_station(__={'ext_directions': [direction1, direction3]})
        station3 = create_station(__={'ext_directions': [direction2, direction3]})

        seg1 = create_rthread_segment(station_from=station1, station_to=station2)
        seg2 = create_rthread_segment(station_from=station3, station_to=station1)
        stations_subscr, settlements_subscr, dirs_subscr, dirs_pairs_subscr = get_segments_subscription_objects(
            [seg1, seg2]
        )

        assert dirs_subscr == {direction1}
        assert dirs_pairs_subscr == set()

    def test_directions_pairs(self):
        settlement = create_settlement()
        zone1 = create_suburban_zone(settlement_id=settlement.id)
        zone2 = create_suburban_zone(settlement_id=settlement.id)
        zone3 = create_suburban_zone(settlement_id=settlement.id)

        direction1 = create_external_direction(suburban_zone=zone1, id=1)
        direction2 = create_external_direction(suburban_zone=zone1, id=2)
        direction3 = create_external_direction(suburban_zone=zone1, id=3)
        direction4 = create_external_direction(suburban_zone=zone2, id=4)
        direction5 = create_external_direction(suburban_zone=zone3, id=5)
        direction6 = create_external_direction(suburban_zone=None, id=6)
        direction7 = create_external_direction(suburban_zone=None, id=7)

        station1 = create_station(__={'ext_directions': [direction1, direction4, direction6]})
        station2 = create_station(__={'ext_directions': [direction2, direction5]})
        station3 = create_station(__={'ext_directions': [direction2, direction3, direction5, direction7]})

        seg1 = create_rthread_segment(station_from=station1, station_to=station2)
        seg2 = create_rthread_segment(station_from=station3, station_to=station1)
        _, _, dirs_subscr, dirs_pairs_subscr = get_segments_subscription_objects([seg1, seg2])

        assert dirs_subscr == set()
        assert dirs_pairs_subscr == {(direction1, direction2), (direction1, direction3)}


class TestBuildSegmentsSubscriptionResult(object):
    def test_valid(self):
        settlement1, settlement2 = create_settlement(id=15), create_settlement(id=16)
        direction = create_external_direction(id=8)

        station1 = create_station(settlement=settlement1, __={'ext_directions': [direction], 'codes': {'esr': '42'}})
        station2 = create_station(settlement=settlement2, __={'ext_directions': [direction], 'codes': {'esr': '44'}})
        station3 = create_station(__={'codes': {'esr': '46'}})

        seg1 = create_rthread_segment(station_from=station1, station_to=station2)
        seg1_2 = create_rthread_segment(station_from=station1, station_to=station2)
        seg2 = create_rthread_segment(station_from=station3, station_to=station1)

        subscr_dict = build_segments_subscription([seg1, seg1_2, seg2])

        assert_that(subscr_dict.keys(), contains_inanyorder('suburban_station', 'suburban_city', 'suburban_direction'))
        assert_that(subscr_dict, has_entries({
            'suburban_station': contains_inanyorder('42', '44', '46'),
            'suburban_city': contains_inanyorder('15', '16'),
            'suburban_direction': contains_inanyorder('8'),
        }))

    def test_direction_pairs(self):
        zone1 = create_suburban_zone(settlement_id=create_settlement().id)
        direction1 = create_external_direction(suburban_zone=zone1, id=44)
        direction2 = create_external_direction(suburban_zone=zone1, id=42)
        direction3 = create_external_direction(suburban_zone=zone1, id=43)

        station1 = create_station(__={'ext_directions': [direction1, direction3]})
        station2 = create_station(__={'ext_directions': [direction2]})

        seg1 = create_rthread_segment(station_from=station1, station_to=station2)
        subscr_dict = build_segments_subscription([seg1])
        assert_that(subscr_dict['suburban_direction'], contains_inanyorder('42+44', '42+43'))


class TestSearchOnDateWithCancels(object):
    @replace_now(datetime(2021, 4, 19))
    def test_disable_cancels(self):
        stations = [create_station(popular_title_ru_genitive="station_{}".format(i)) for i in range(3)]
        thread_start_dt = datetime(2021, 4, 19, 15, 30)
        thread_uid = 'thread_uid'
        thread = create_thread(
            year_days=[thread_start_dt.date()],
            tz_start_time=thread_start_dt.time(),
            uid=thread_uid,
            schedule_v1=[
                [None, 0, stations[0]],
                [10, 15, stations[1]],
                [20, None, stations[2]]
            ],
            __={'calculate_noderoute': True}
        )
        rtstations = list(thread.path)

        ThreadStationStateFactory.create_from_rtstation(
            rts=rtstations[0],
            thread_start_date=thread_start_dt,
            departure={
                'dt': thread_start_dt,
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
                'dt': thread_start_dt+timedelta(minutes=10),
                'type': EventStateType.CANCELLED,
                'thread_uid': thread_uid,
            },
            departure={
                'dt': thread_start_dt+timedelta(minutes=15),
                'type': EventStateType.CANCELLED,
                'thread_uid': thread_uid,
            },
            passed_several_times=False
        )

        ThreadStationStateFactory.create_from_rtstation(
            rts=rtstations[2],
            thread_start_date=thread_start_dt,
            arrival={
                'dt': thread_start_dt + timedelta(minutes=20),
                'type': EventStateType.CANCELLED,
                'thread_uid': thread_uid,
            },
            passed_several_times=False
        )

        res = search_on_date(
            create_request(), stations[0], stations[-1], False, False,
            MSK_TZ, thread_start_dt.date(), 1, 0, disable_cancels=True
        )

        assert len(res['days']) == 1
        assert len(res['days'][0]['segments']) == 1
        segment = res['days'][0]['segments'][0]
        assert segment['arrival']['state']['type'] == EventStateType.POSSIBLE_DELAY
        assert segment['departure']['state']['type'] == EventStateType.POSSIBLE_DELAY

        assert 'cancelled' not in segment['thread']
        assert 'cancelled_segments' not in segment['thread']

        res = search_on_date(
            create_request(), stations[0], stations[-1], False, False,
            MSK_TZ, thread_start_dt.date(), 1, 0, disable_cancels=False
        )

        assert len(res['days']) == 1
        assert len(res['days'][0]['segments']) == 1
        segment = res['days'][0]['segments'][0]
        assert segment['arrival']['state']['type'] == EventStateType.CANCELLED
        assert segment['departure']['state']['type'] == EventStateType.CANCELLED
        assert segment['thread']['cancelled'] is True

        assert len(segment['thread']['cancelled_segments']) == 1
        cancelled_segment = segment['thread']['cancelled_segments'][0]
        assert cancelled_segment['from_title_genitive'] == stations[0].L_popular_title(case='genitive')
        assert cancelled_segment['to_title_genitive'] == stations[2].L_popular_title(case='genitive')

    @replace_now(datetime(2021, 4, 19))
    def test_partial_cancels(self):
        stations = [create_station(popular_title_ru_genitive="station_{}".format(i)) for i in range(5)]
        thread_start_dts = [
            datetime(2021, 4, 19, 10, 30) + timedelta(hours=i) for i in range(3)
        ]
        thread_uids = ['thread_uid_{}'.format(i) for i in range(3)]

        def _create_thread(i, schedule):
            return create_thread(
                year_days=[thread_start_dts[i].date()],
                tz_start_time=thread_start_dts[i].time(),
                uid=thread_uids[i],
                schedule_v1=schedule,
                __={'calculate_noderoute': True}
        )
        threads = [
            _create_thread(0, [
                [None, 0, stations[0]],
                [10, 15, stations[1]],
                [20, None, stations[2]]
            ]),
            _create_thread(1, [
                [None, 0, stations[1]],
                [10, 15, stations[2]],
                [20, None, stations[3]]
            ]),
            _create_thread(2, [
                [None, 0, stations[1]],
                [10, 15, stations[2]],
                [20, None, stations[4]]
            ])
        ]
        rtstations = [list(threads[i].path)for i in range(3)]

        # thread 0
        ThreadStationStateFactory.create_from_rtstation(
            rts=rtstations[0][0],
            thread_start_date=thread_start_dts[0],
            departure={
                'dt': thread_start_dts[0],
                'type': EventStateType.CANCELLED,
                'thread_uid': thread_uids[0],
            },
            passed_several_times=False
        )
        ThreadStationStateFactory.create_from_rtstation(
            rts=rtstations[0][1],
            thread_start_date=thread_start_dts[0],
            arrival={
                'dt': thread_start_dts[0] + timedelta(minutes=10),
                'type': EventStateType.CANCELLED,
                'thread_uid': thread_uids[0],
            },
            departure={
                'dt': thread_start_dts[0] + timedelta(minutes=15),
                'type': EventStateType.CANCELLED,
                'thread_uid': thread_uids[0],
            },
            passed_several_times=False
        )
        ThreadStationStateFactory.create_from_rtstation(
            rts=rtstations[0][2],
            thread_start_date=thread_start_dts[0],
            arrival={
                'dt': thread_start_dts[0] + timedelta(minutes=20),
                'type': EventStateType.CANCELLED,
                'thread_uid': thread_uids[0],
            },
            passed_several_times=False
        )

        # thread 1
        ThreadStationStateFactory.create_from_rtstation(
            rts=rtstations[1][0],
            thread_start_date=thread_start_dts[1],
            departure={
                'dt': thread_start_dts[1],
                'type': EventStateType.CANCELLED,
                'thread_uid': thread_uids[1],
            },
            passed_several_times=False
        )
        ThreadStationStateFactory.create_from_rtstation(
            rts=rtstations[1][1],
            thread_start_date=thread_start_dts[1],
            arrival={
                'dt': thread_start_dts[1] + timedelta(minutes=10),
                'type': EventStateType.CANCELLED,
                'thread_uid': thread_uids[1],
            },
            departure={
                'dt': thread_start_dts[1] + timedelta(minutes=15),
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': thread_uids[1],
            },
            passed_several_times=False
        )
        ThreadStationStateFactory.create_from_rtstation(
            rts=rtstations[1][2],
            thread_start_date=thread_start_dts[1],
            arrival={
                'dt': thread_start_dts[1] + timedelta(minutes=20),
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': thread_uids[1],
            },
            passed_several_times=False
        )

        # thread 2
        ThreadStationStateFactory.create_from_rtstation(
            rts=rtstations[2][0],
            thread_start_date=thread_start_dts[2],
            departure={
                'dt': thread_start_dts[2],
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': thread_uids[2],
            },
            passed_several_times=False
        )
        ThreadStationStateFactory.create_from_rtstation(
            rts=rtstations[2][1],
            thread_start_date=thread_start_dts[2],
            arrival={
                'dt': thread_start_dts[2] + timedelta(minutes=10),
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': thread_uids[2],
            },
            departure={
                'dt': thread_start_dts[2] + timedelta(minutes=15),
                'type': EventStateType.CANCELLED,
                'thread_uid': thread_uids[2],
            },
            passed_several_times=False
        )
        ThreadStationStateFactory.create_from_rtstation(
            rts=rtstations[2][2],
            thread_start_date=thread_start_dts[2],
            arrival={
                'dt': thread_start_dts[2] + timedelta(minutes=20),
                'type': EventStateType.CANCELLED,
                'thread_uid': thread_uids[2],
            },
            passed_several_times=False
        )

        res = search_on_date(
            create_request(), stations[1], stations[2], False, False,
            MSK_TZ, thread_start_dts[0].date(), 1, 0, disable_cancels=False
        )
        segments = res['days'][0]['segments']
        for segment in segments:
            thread_id = thread_uids.index(segment['thread']['uid'])
            if thread_id == 0:
                assert segment['arrival']['state']['type'] == EventStateType.CANCELLED
                assert segment['departure']['state']['type'] == EventStateType.CANCELLED
                assert segment['thread']['cancelled'] is True
                cancelled_segment = segment['thread']['cancelled_segments'][0]
                assert cancelled_segment['from_title_genitive'] == stations[0].L_popular_title(case='genitive')
                assert cancelled_segment['to_title_genitive'] == stations[2].L_popular_title(case='genitive')
            elif thread_id == 1:
                assert segment['arrival']['state']['type'] == EventStateType.CANCELLED
                assert segment['departure']['state']['type'] == EventStateType.CANCELLED
                assert segment['thread']['cancelled'] is False
                cancelled_segment = segment['thread']['cancelled_segments'][0]
                assert cancelled_segment['from_title_genitive'] == stations[1].L_popular_title(case='genitive')
                assert cancelled_segment['to_title_genitive'] == stations[2].L_popular_title(case='genitive')
            elif thread_id == 2:
                assert segment['arrival']['state']['type'] == EventStateType.POSSIBLE_DELAY
                assert segment['departure']['state']['type'] == EventStateType.POSSIBLE_DELAY
                assert 'cancelled' not in segment['thread']
                assert 'cancelled_segments' not in segment['thread']
