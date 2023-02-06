# coding: utf8
import json
from collections import defaultdict
from datetime import datetime, timedelta, time, date
from functools import partial

import mock
import pytest
import pytz
from django.http import Http404
from django.test.client import Client
from iso8601 import parse_date
from lxml import etree
from xml.etree import ElementTree
from hamcrest import assert_that, has_entries

from common.apps.facility.models import SuburbanThreadFacility
from common.utils.date import RunMask
from common.models.geo import Settlement
from common.models.tariffs import AeroexTariff, TariffType
from common.models.transport import TransportType, TransportSubtype
from common.models.schedule import RTStation, RThread
from common.views.currency import CurrencyInfo
from common.views.tariffs import StaticTariffs
from common.utils.date import MSK_TZ
from common.apps.facility.factories import create_suburban_facility
from common.models.factories import create_aeroex_tariff

from stationschedule.models import ZTablo2

from common.tester.testcase import TestCase
from common.tester.factories import (
    create_settlement, create_station, create_rthread_segment, create_thread, create_station_schedule,
    create_train_schedule_plan, create_transport_subtype, create_transport_subtype_color,
)
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_setting

from travel.rasp.export.export.views.in_xml import (
    build_segment_el, add_departure_delays_data, station_schedule_base, common_search, thread_base,
    fill_transport_type, fill_days_and_except_texts, get_station_by_esr_or_404, log_search, add_facilities_data
)
from travel.rasp.export.export.views.tariffs import add_suburban_tariffs, create_tariff_info, build_tariffs_by_stations, AuxTariffs
from travel.rasp.export.tests.old_versions.views import create_request


create_station = create_station.mutate(t_type="suburban")
create_thread = create_thread.mutate(t_type="suburban")
create_rthread_segment = partial(create_rthread_segment)


def create_currency_info(selected=None, rates=None):
    rates = {} if rates is None else rates
    return CurrencyInfo(selected, None, None, None, rates, [], [])


def check_server_time(msk_dt, xml_tree):
    iso_dt = xml_tree.xpath("/response/server_time")[0].text
    utc_dt = parse_date(iso_dt)
    assert utc_dt == MSK_TZ.localize(msk_dt).astimezone(pytz.utc)


def check_segment_departure_utc(departure_time, xml_tree):
    segment = xml_tree.xpath('/response/trip/segment')[0]
    iso_dt = segment.attrib['departure_utc']
    utc_dt = parse_date(iso_dt)
    msk_dt_departure = MSK_TZ.localize(datetime.combine(test_server_time_dt.date(), departure_time))
    assert utc_dt == msk_dt_departure.astimezone(pytz.utc)
    assert iso_dt == '2001-02-01 21:30:00+00:00'


test_server_time_dt = datetime(2001, 2, 2, 12, 13, 14)


class TestAddDepartureDelaysData(TestCase):
    def test_valid(self):
        now = datetime.now()
        ztablo = ZTablo2(
            departure=now,
            real_departure=now + timedelta(minutes=42, days=1),
            comment=u'Атака шушпанчиков',
            departure_cancelled=False,
        )

        xml_element = ElementTree.Element('tag')
        add_departure_delays_data(xml_element, ztablo)

        assert xml_element.attrib['delay_minutes'] == '1482'
        assert xml_element.attrib['delay_comment'] == u"Атака шушпанчиков"
        assert xml_element.attrib['canceled'] == 'false'

        ztablo.departure_cancelled = True
        ztablo.departure = None
        add_departure_delays_data(xml_element, ztablo)
        assert xml_element.attrib['canceled'] == 'true'
        assert xml_element.attrib['delay_minutes'] == ''

        add_departure_delays_data(xml_element, None)
        assert xml_element.attrib['canceled'] == 'false'
        assert xml_element.attrib['delay_minutes'] == ''
        assert xml_element.attrib['delay_comment'] == ''


class TestAddFacilitiesData(TestCase):

    @replace_setting('MEDIA_URL', 'https://static/')
    def test_valid(self):
        lezhanki_facility = create_suburban_facility(title_ru=u'Лежанки', code='lezhanki')
        spalniki_facility = create_suburban_facility(title_ru=u'Спальники', code='spalniki')
        spalniki_facility.icon.name = u'some/file/path.svg'
        spalniki_facility.save()

        xml_el = ElementTree.Element('el')
        add_facilities_data(xml_el, [lezhanki_facility, spalniki_facility])

        fac1, fac2 = xml_el.findall('facilities/facility')

        assert len(fac1.attrib) == 3
        assert fac1.attrib['title'] == u'Лежанки'
        assert fac1.attrib['code'] == u'lezhanki'
        assert not fac1.attrib['icon']

        assert len(fac2.attrib) == 3
        assert fac2.attrib['title'] == u'Спальники'
        assert fac2.attrib['title'] == u'Спальники'
        assert fac2.attrib['icon'] == u'https://static/some/file/path.svg'
        assert fac2.attrib['code'] == u'spalniki'


def patch_add_departure_delays_data(el_data):
    m_add_delays_data = mock.Mock()
    m_add_delays_data.side_effect = lambda el, *args, **kwargs: el.attrib.update(el_data)
    return mock.patch('travel.rasp.export.export.views.in_xml.add_departure_delays_data', m_add_delays_data)


class TestSearch(TestCase):
    def test_valid(self):
        settlement = create_settlement()
        params = {
            'city_from': Settlement.MOSCOW_ID,
            'city_to': settlement.id,
        }

        response = Client().get('/export/suburban/search/', params)
        etree.fromstring(response.content)

    @replace_now(test_server_time_dt)
    def test_server_time(self):
        settlement = create_settlement()
        params = {
            'city_from': Settlement.MOSCOW_ID,
            'city_to': settlement.id,
            'sub_version': '2.1'
        }

        response = Client().get('/export/v2/suburban/search/', params)
        xml_tree = etree.fromstring(response.content)
        check_server_time(test_server_time_dt, xml_tree)

    @replace_now(test_server_time_dt)
    def test_segment_departure_utc(self):
        station_from, station_to = create_station(__={'codes': {'esr': '100'}}), create_station(
            __={'codes': {'esr': '101'}})
        departure_time = time(0, 30)
        params = {
            'station_from': '100',
            'station_to': '101',
            'date': test_server_time_dt.strftime('%Y-%m-%d'),
            'sub_version': '2.1'
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

        response = Client().get('/export/v2/suburban/search/', params)
        xml_tree = etree.fromstring(response.content)
        check_segment_departure_utc(departure_time, xml_tree)

    def test_day_start_end_utc(self):
        create_station(__={'codes': {'esr': '100'}},  time_zone='Europe/Kiev'), create_station(__={'codes': {'esr': '101'}})

        # Проверяем переход с летнего времени на зимнее utc+3 -> utc+2.
        params = {
            'station_from': '100',
            'station_to': '101',
            'date': '2016-10-30',
            'sub_version': '2.1'
        }

        response = Client().get('/export/v2/suburban/search/', params)
        xml_tree = etree.fromstring(response.content)
        trip = xml_tree.xpath('/response/trip')[0]
        assert trip.attrib['day_start_utc'] == '2016-10-29 21:00:00+00:00'
        assert trip.attrib['day_end_utc'] == '2016-10-30 21:59:59+00:00'
        time_diff = parse_date(trip.attrib['day_end_utc']) - parse_date(trip.attrib['day_start_utc'])
        assert (time_diff.days * 3600. * 24 + time_diff.seconds + 1) / 3600 == 25

        # Проверяем переход с зимнего времени на летнее utc+2 -> utc+3.
        params['date'] = '2016-3-27'

        response = Client().get('/export/v2/suburban/search/', params)
        xml_tree = etree.fromstring(response.content)
        trip = xml_tree.xpath('/response/trip')[0]
        assert trip.attrib['day_start_utc'] == '2016-03-26 22:00:00+00:00'
        assert trip.attrib['day_end_utc'] == '2016-03-27 20:59:59+00:00'
        time_diff = parse_date(trip.attrib['day_end_utc']) - parse_date(trip.attrib['day_start_utc'])
        assert (time_diff.days * 3600. * 24 + time_diff.seconds + 1) / 3600 == 23

    @replace_now(datetime(2001, 2, 2, 23))
    def test_date_today_tomorrow(self):
        station_from = create_station(__={'codes': {'esr': '100'}}, time_zone='Asia/Yekaterinburg')
        station_to = create_station(__={'codes': {'esr': '101'}})
        params = {
            'station_from': '100',
            'station_to': '101',
            'date': 'today',
            'sub_version': '2.1'
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

        response = Client().get('/export/v2/suburban/search/', params)
        xml_tree = etree.fromstring(response.content)
        trip = xml_tree.xpath('/response/trip')[0]
        assert trip.attrib['date'] == '2001-02-03'
        segment = xml_tree.xpath('/response/trip/segment')[0]
        assert segment.attrib['departure'] == '2001-02-03 01:30'

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

        response = Client().get('/export/v2/suburban/search/', params)
        xml_tree = etree.fromstring(response.content)
        trip = xml_tree.xpath('/response/trip')[0]
        assert trip.attrib['date'] == '2001-02-04'
        segments = xml_tree.xpath('/response/trip/segment')
        assert len(segments) == 1
        assert segments[0].attrib['departure'] == '2001-02-04 01:10'

    @replace_now(datetime(2001, 2, 2, 23))
    def test_date_today_up_to_ahead(self):
        station_from = create_station(__={'codes': {'esr': '100'}}, time_zone='Asia/Yekaterinburg')
        station_to = create_station(__={'codes': {'esr': '101'}})
        params = {
            'date': 'today',
            'days_ahead': '3',
            'tomorrow_upto': '12',
            'station_from': '100',
            'station_to': '101',
            'sub_version': '2.1'
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

        response = Client().get('/export/v2/suburban/search/', params)
        xml_tree = etree.fromstring(response.content)
        trip = xml_tree.xpath('/response/trip')[0]
        assert trip.attrib['date'] == '2001-02-03'
        segments = xml_tree.xpath('/response/trip/day/segment')
        assert len(segments) == 3
        days = xml_tree.xpath('/response/trip/day')
        assert [day.attrib['date'] for day in days] == ['2001-02-03', '2001-02-05', '2001-02-06']


class TestTrip(TestCase):
    @replace_now(test_server_time_dt)
    def test_server_time(self):
        create_station(__={'codes': {'esr': '100'}}), create_station(__={'codes': {'esr': '101'}})

        response = Client().get('/export/v2/suburban/trip/{}/{}/'.format(100, 101), {'sub_version': '2.1'})
        xml_tree = etree.fromstring(response.content)
        check_server_time(test_server_time_dt, xml_tree)

    @replace_now(test_server_time_dt)
    def test_segment_departure_utc(self):
        station_from, station_to = create_station(__={'codes': {'esr': '100'}}), create_station(__={'codes': {'esr': '101'}})
        departure_time = time(0, 30)
        params = {
            'date': test_server_time_dt.strftime('%Y-%m-%d'),
            'sub_version': '2.1'
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

        response = Client().get('/export/v2/suburban/trip/{}/{}/'.format(100, 101), params)
        xml_tree = etree.fromstring(response.content)
        check_segment_departure_utc(departure_time, xml_tree)

    def test_day_start_end_utc(self):
        create_station(__={'codes': {'esr': '100'}}, time_zone='Europe/Kiev'), create_station(__={'codes': {'esr': '101'}})
        # Проверяем переход с летнего времени на зимнее utc+3 -> utc+2.
        params = {
            'date': '2016-10-30',
            'sub_version': '2.1'
        }

        response = Client().get('/export/v2/suburban/trip/{}/{}/'.format(100, 101), params)
        xml_tree = etree.fromstring(response.content)
        trip = xml_tree.xpath('/response/trip')[0]
        assert trip.attrib['day_start_utc'] == '2016-10-29 21:00:00+00:00'
        assert trip.attrib['day_end_utc'] == '2016-10-30 21:59:59+00:00'
        time_diff = parse_date(trip.attrib['day_end_utc']) - parse_date(trip.attrib['day_start_utc'])
        assert (time_diff.days * 3600. * 24 + time_diff.seconds + 1) / 3600 == 25

        # Проверяем переход с зимнего времени на летнее utc+2 -> utc+3.
        params = {
            'date': '2016-3-27',
            'sub_version': '2.1'
        }

        response = Client().get('/export/v2/suburban/trip/{}/{}/'.format(100, 101), params)
        xml_tree = etree.fromstring(response.content)
        trip = xml_tree.xpath('/response/trip')[0]
        assert trip.attrib['day_start_utc'] == '2016-03-26 22:00:00+00:00'
        assert trip.attrib['day_end_utc'] == '2016-03-27 20:59:59+00:00'
        time_diff = parse_date(trip.attrib['day_end_utc']) - parse_date(trip.attrib['day_start_utc'])
        assert (time_diff.days * 3600. * 24 + time_diff.seconds + 1) / 3600 == 23

    @replace_now(datetime(2001, 2, 2, 23))
    def test_date_today_tomorrow(self):
        station_from = create_station(__={'codes': {'esr': '100'}}, time_zone='Asia/Yekaterinburg')
        station_to = create_station(__={'codes': {'esr': '101'}})
        params = {
            'date': 'today',
            'sub_version': '2.1'
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

        response = Client().get('/export/v2/suburban/trip/{}/{}/'.format(100, 101), params)
        xml_tree = etree.fromstring(response.content)
        trip = xml_tree.xpath('/response/trip')[0]
        assert trip.attrib['date'] == '2001-02-03'
        segment = xml_tree.xpath('/response/trip/segment')[0]
        assert segment.attrib['departure'] == '2001-02-03 01:30'

        params = {
            'date': 'tomorrow',
            'sub_version': '2.1'
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

        response = Client().get('/export/v2/suburban/trip/{}/{}/'.format(100, 101), params)
        xml_tree = etree.fromstring(response.content)
        trip = xml_tree.xpath('/response/trip')[0]
        assert trip.attrib['date'] == '2001-02-04'
        segments = xml_tree.xpath('/response/trip/segment')
        assert len(segments) == 1
        assert segments[0].attrib['departure'] == '2001-02-04 01:10'

    @replace_now(datetime(2001, 2, 2, 23))
    def test_date_today_up_to_ahead(self):
        station_from = create_station(__={'codes': {'esr': '100'}}, time_zone='Asia/Yekaterinburg')
        station_to = create_station(__={'codes': {'esr': '101'}})
        params = {
            'date': 'today',
            'days_ahead': '3',
            'tomorrow_upto': '12',
            'sub_version': '2.1'
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

        response = Client().get('/export/v2/suburban/trip/{}/{}/'.format(100, 101), params)
        xml_tree = etree.fromstring(response.content)
        trip = xml_tree.xpath('/response/trip')[0]
        assert trip.attrib['date'] == '2001-02-03'
        segments = xml_tree.xpath('/response/trip/day/segment')
        assert len(segments) == 3
        days = xml_tree.xpath('/response/trip/day')
        assert [day.attrib['date'] for day in days] == ['2001-02-03', '2001-02-05', '2001-02-06']

    def test_404_for_hidden_station(self):
        create_station(__={'codes': {'esr': '100'}}, hidden=True)
        create_station(__={'codes': {'esr': '101'}})

        response = Client().get('/export/v2/suburban/trip/{}/{}/'.format(100, 101), {})
        assert response.status_code == 404


class TestBuildSegmentEl(TestCase):
    def test_tariff_currency(self):
        segment = create_rthread_segment()
        for currency, tariff, expected_currency, expected_tariff in [('RUR', 11.2345, 'RUR', '11.23'),
                                                                     ('BYR', 1200.0, 'BYR', '1200'),
                                                                     (None, 1200, 'RUR', '1200.00')]:
            base_tariff = AeroexTariff(tariff=tariff, currency=currency)
            segment.tariff_info = create_tariff_info(base_tariff)
            segment.aux_tariffs = AuxTariffs(base=base_tariff)
            xml_element = build_segment_el(segment, '%H:%M', datetime.now(), None)

            assert xml_element.attrib['tariff'] == expected_tariff
            assert xml_element.attrib['currency'] == expected_currency

    def test_add_delays(self):
        ztablo = ZTablo2()
        segment = create_rthread_segment()
        segment.departure_z_tablo = ztablo

        with patch_add_departure_delays_data({'blabla': 42}) as m_add_delays_data:
            xml_element = build_segment_el(segment, '%H:%M', datetime.now(), None, add_delays=True)
            assert len(m_add_delays_data.call_args_list) == 1
            assert m_add_delays_data.call_args_list[0][0][1] == ztablo
            assert xml_element.attrib['blabla'] == 42

            m_add_delays_data.reset_mock()
            build_segment_el(segment, '%H:%M', datetime.now(), None, add_delays=False)
            assert len(m_add_delays_data.call_args_list) == 0

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

        def check_call(tz, departure, arrival, days):
            el = build_segment_el(segment, '%H:%M', now, None, timezone=tz)
            assert el.attrib['arrival'] == arrival
            assert el.attrib['departure'] == departure
            assert el.attrib['days'] == days

        check_call(None, '13:20', '14:42', 'today days')
        check_call(pytz.timezone('Asia/Yekaterinburg'), '15:20', '16:42', 'today days')
        check_call(pytz.timezone('Pacific/Samoa'), '23:20', '00:42', 'previous days')
        check_call(pytz.timezone('Pacific/Kiritimati'), '00:20', '01:42', 'next days')

    def test_stops_by_version(self):
        segment = create_rthread_segment(stops_translations='')

        el = build_segment_el(segment, '%H:%M', datetime.now(), None)
        assert el.attrib['stops'] == u'остановки по списку'

        el = build_segment_el(segment, '%H:%M', datetime.now(), None, version='2')
        assert el.attrib['stops'] == u''

    def test_add_facilities(self):
        with mock.patch('travel.rasp.export.export.views.in_xml.add_facilities_data') as m_add_facilities_data:
            segment = create_rthread_segment()

            build_segment_el(segment, '%H:%M', datetime.now(), None, version='1')
            assert not m_add_facilities_data.call_args_list

            segment = create_rthread_segment()
            segment.suburban_facilities = 123
            xml_el = build_segment_el(segment, '%H:%M', datetime.now(), None, version='2')
            assert len(m_add_facilities_data.call_args_list) == 1
            assert m_add_facilities_data.call_args_list[0][0] == (xml_el, 123)


class TestCommonSearch(TestCase):
    def test_add_delays(self):
        with mock.patch('travel.rasp.export.export.views.in_xml.add_z_tablos_to_segments') as m_add_z_tablos_to_segments:
            request = create_request()
            station_from = create_station()
            station_to = create_station()

            common_search(request, station_from, station_to, add_delays=True)
            assert len(m_add_z_tablos_to_segments.call_args_list) == 1

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

        def check(request, segments_path, departure_format, threads_num):
            trip_el, _ = common_search(request, station1, station2)
            segments = trip_el.findall(segments_path)
            assert len(segments) == threads_num
            departures = [s.attrib['departure'] for s in segments]
            assert len(set(departures)) == threads_num
            assert [datetime.strptime(dep, departure_format) for dep in departures]
            assert departures == sorted(departures)

        check(create_request(), 'segment', '%H:%M', len(threads))
        check(
            create_request('/?days_ahead=2&date={}'.format(departure_day.strftime('%Y-%m-%d'))),
            'day/segment',
            '%Y-%m-%d %H:%M',
            len(threads),
        )
        check(
            create_request('/?date={}'.format(departure_day.strftime('%Y-%m-%d'))),
            'segment',
            '%Y-%m-%d %H:%M',
            len(threads) - 1,  # на текущий день только 2 нитки
        )

    def test_log_search_called(self):
        with mock.patch('travel.rasp.export.export.views.in_xml.find_suburban') as m_find_suburban, \
                mock.patch('travel.rasp.export.export.views.in_xml.log_search') as m_log_search:

            departure = MSK_TZ.localize(datetime(2015, 10, 8))
            arrival = MSK_TZ.localize(datetime(2015, 10, 8))
            segments = [
                create_rthread_segment(departure=departure, arrival=arrival),
                create_rthread_segment(departure=departure, arrival=arrival),
            ]
            m_find_suburban.return_value = segments
            point_from, point_to = create_settlement(), create_station()
            request = create_request('/bla?date=2015-10-08')

            with replace_setting('USERS_SEARCH_LOG', None):
                common_search(request, point_from, point_to)
                assert m_log_search.call_count == 0

            with replace_setting('USERS_SEARCH_LOG', 'somepath'):
                common_search(request, point_from, point_to)

                # сегменты могут быть получены в разном порядке
                try:
                    m_log_search.assert_called_once_with(request, point_from, point_to, date(2015, 10, 8), segments)
                except AssertionError:
                    rev_segments = list(reversed(segments))
                    m_log_search.assert_called_once_with(request, point_from, point_to, date(2015, 10, 8), rev_segments)

    def test_add_facilities(self):
        with mock.patch('travel.rasp.export.export.views.in_xml.fill_segments_suburban_facilities') as m_fill_facilities, \
                mock.patch('travel.rasp.export.export.views.in_xml.find_suburban') as m_find_suburban:

            segments = [
                create_rthread_segment(
                    departure=MSK_TZ.localize(datetime(2017, 12, 13)),
                    arrival=MSK_TZ.localize(datetime(2017, 12, 13))
                )
                for _ in range(3)
            ]

            m_find_suburban.return_value = segments
            station_from, station_to = create_station(), create_station()

            request = create_request('/?date=2017-11-12')
            common_search(request, station_from, station_to, version='1')
            assert not m_fill_facilities.call_args_list

            request = create_request('/?date={}'.format('2017-12-13'))
            common_search(request, station_from, station_to, version='2')
            assert set(m_fill_facilities.call_args_list[0][0][0]) == set(segments)


class TestBuildTariffsByStations(TestCase):
    def test_valid(self):
        station1, station2, station3 = (
            create_station(
                __={'codes': {'esr': 'esrcode{}'.format(i)}}
            )
            for i in range(1, 4)
        )

        create_rthread_segment(station_from=station1, station_to=station2)
        create_rthread_segment(station_from=station3, station_to=station1)

        tariff1 = AeroexTariff(tariff=1.1, type=TariffType(code='code1'))
        tariff2 = AeroexTariff(tariff=2.2, type=TariffType(code='code2'))

        tariffs_by_stations = {
            (station1, station2): {
                'code1': tariff1,
                'code2': tariff2,
            },
            (station3, station1): {
                'code2': tariff2,
            }
        }

        xml_element = build_tariffs_by_stations(tariffs_by_stations)
        assert xml_element.tag == 'tariffs_by_stations'

        xml = ElementTree.ElementTree(xml_element)
        segments = xml.findall('tariffs')
        assert len(segments) == 2

        tariffs_els = xml.findall('tariffs[@station_from="esrcode1"][@station_to="esrcode2"]/tariff[@tariff="1.10"]')
        assert len(tariffs_els) == 1

        tariffs_els = xml.findall('tariffs[@station_from="esrcode1"][@station_to="esrcode2"]/tariff[@tariff="2.20"]')
        assert len(tariffs_els) == 1

        tariffs_els = xml.findall('tariffs[@station_from="esrcode3"][@station_to="esrcode1"]/tariff[@tariff="2.20"]')
        assert len(tariffs_els) == 1


class TestAddSuburbanTariffs(TestCase):
    def test_valid(self):
        station_from_1 = create_station(title='from1')
        station_from_2 = create_station(title='from2')
        station_to_1 = create_station(title='to1')
        station_to_2 = create_station(title='to2')

        tariff_type1 = TariffType.objects.create(code='test1', category='season_ticket')
        tariff_type2 = TariffType.objects.create(code='test2', category='season_ticket')

        create_aeroex_tariff(station_from=station_from_1, station_to=station_to_1, type=tariff_type1),
        create_aeroex_tariff(station_from=station_from_1, station_to=station_to_1, type=tariff_type2),
        create_aeroex_tariff(station_from=station_from_1, station_to=station_to_2, tariff=2),
        create_aeroex_tariff(station_from=station_from_2, station_to=station_to_1, tariff=3),
        create_aeroex_tariff(station_from=station_from_2, station_to=station_to_2, tariff=4),

        station_pairs = [
            (station_from_1, station_to_1),
            (station_from_1, station_to_2),
            (station_from_2, station_to_1),
            (station_from_2, station_to_2),
        ]

        segments = []
        for station_from, station_to in station_pairs:
            thread = create_thread()
            segment = create_rthread_segment(
                thread=thread,
                station_from=station_from,
                station_to=station_to,
            )
            segments.append(segment)

        tariffs_by_stations = add_suburban_tariffs(segments)
        assert sorted(tariffs_by_stations) == sorted(station_pairs)

        def check_pair(station_pair, tariff_value):
            tariffs = tariffs_by_stations[station_pair]
            assert len(tariffs) == 1
            _, tariff = tariffs.popitem()
            assert tariff.tariff == tariff_value

        check_pair((station_from_1, station_to_2), 2)
        check_pair((station_from_2, station_to_1), 3)
        check_pair((station_from_2, station_to_2), 4)

        tariffs_1_1 = tariffs_by_stations[(station_from_1, station_to_1)]
        assert sorted(tariffs_1_1) == ['test1', 'test2']

    def test_id_max_tariff(self):
        station_from_1 = create_station(title='from1')
        station_to_1 = create_station(title='to1')

        tariff_type_1 = TariffType.objects.create(code='test_1', category=TariffType.SEASON_TICKET_CATEGORY)
        create_aeroex_tariff(station_from=station_from_1, station_to=station_to_1, type=tariff_type_1, tariff=2, id=42)

        thread = create_thread()
        segment = create_rthread_segment(
            thread=thread,
            station_from=station_from_1,
            station_to=station_to_1
        )

        tariffs_by_stations = add_suburban_tariffs([segment])
        tariffs = tariffs_by_stations[(station_from_1, station_to_1)]
        assert tariffs['test_1'].tariff == 2

        create_aeroex_tariff(station_from=station_to_1, station_to=station_from_1, type=tariff_type_1,
                             tariff=10, reverse=True, id=43)
        tariffs_by_stations = add_suburban_tariffs([segment])
        tariffs = tariffs_by_stations[(station_from_1, station_to_1)]
        assert tariffs['test_1'].tariff == 10
        assert tariffs['test_1'].id == 43

        tariff_type_2 = TariffType.objects.create(code='test_2', category=TariffType.SEASON_TICKET_CATEGORY)
        create_aeroex_tariff(station_from=station_from_1, station_to=station_to_1, type=tariff_type_2,
                             precalc=True, tariff=15, reverse=True, id=142)
        create_aeroex_tariff(station_from=station_to_1, station_to=station_from_1, type=tariff_type_2,
                             precalc=True, tariff=5, reverse=True, id=143)
        tariffs_by_stations = add_suburban_tariffs([segment])
        tariffs = tariffs_by_stations[(station_from_1, station_to_1)]
        assert tariffs['test_2'].tariff == 5
        assert tariffs['test_2'].id == 143

        create_aeroex_tariff(station_from=station_to_1, station_to=station_from_1, type=tariff_type_2,
                             precalc=False, tariff=99, reverse=True, id=200)
        tariffs_by_stations = add_suburban_tariffs([segment])
        tariffs = tariffs_by_stations[(station_from_1, station_to_1)]
        assert tariffs['test_2'].tariff == 99
        assert tariffs['test_2'].id == 200

        create_aeroex_tariff(station_from=station_from_1, station_to=station_to_1, type=tariff_type_2,
                             precalc=False, tariff=9, reverse=True, id=201)
        tariffs_by_stations = add_suburban_tariffs([segment])
        tariffs = tariffs_by_stations[(station_from_1, station_to_1)]
        assert tariffs['test_2'].tariff == 9
        assert tariffs['test_2'].id == 201


class TestCreateTariffInfo(TestCase):
    def test_valid(self):
        data = [
            [66.8, None, 1],
            [66.8, 'RUR', 1],
            [66.8, 'TRY', 2],
        ]

        for value, currency, rate in data:
            tariff = AeroexTariff(tariff=value)

            result = create_tariff_info(tariff)

            assert isinstance(result, StaticTariffs)
            assert result.places[0].tariff.value == value
            assert result.places[0].tariff.currency == 'RUR'


class TestStationScheduleBase(TestCase):
    def setUp(self):
        self.esr_code = '123'
        self.station = create_station(__={'codes': {'esr': self.esr_code}})

    def test_add_delays(self):
        minutes = 12
        date = datetime(2015, 07, 10)

        thread = create_thread(
            schedule_v1=[
                [None, minutes, self.station],
                [minutes + 1, None],
            ],
        )
        rtstation = RTStation.objects.get(thread=thread, station=self.station)

        ztablo = ZTablo2.objects.create(
            station=self.station, thread=thread, rtstation=rtstation,
            original_departure=date + timedelta(minutes=minutes)
        )

        request = create_request('/export/v2/suburban/station/{}?date={}'.format(
            self.esr_code,
            date.strftime('%Y-%m-%d')
        ))

        with patch_add_departure_delays_data({'blabla': 42}) as m_add_delays_data:
            station_el, teasers_el = station_schedule_base(request, self.esr_code, add_delays=True)

            assert len(m_add_delays_data.call_args_list) == 1
            assert m_add_delays_data.call_args_list[0][0][1] == ztablo

            xml = ElementTree.ElementTree(station_el)
            thread_els = xml.findall('thread')
            assert len(thread_els) == 1

    def test_direction_exists(self):
        """
        Проверяем, что элемент direction отсутствует, если реального направления нет.
        RASPEXPORT-94
        """
        create_thread(schedule_v1=[[None, 10, self.station], [11, None]])
        station_el, teasers_el = station_schedule_base(create_request(), self.esr_code)
        xml = ElementTree.ElementTree(station_el)
        thread_els = xml.findall('thread')
        assert len(thread_els) == 1
        assert 'direction' not in thread_els[0].attrib

    def test_timezone(self):
        def check_thread(thread_el, arrival, departure, days):
            if arrival:
                assert thread_el.attrib['arrival'] == arrival
            else:
                assert 'arrival' not in thread_el.attrib

            if departure:
                assert thread_el.attrib['departure'] == departure
            else:
                assert 'departure' not in thread_el.attrib

            assert thread_el.attrib['days'] == days

        def check_call(tz, arrival, departure, days):
            url = '/export/v2/suburban/station/{}'.format(self.esr_code)
            if tz:
                url += '?timezone={}'.format(tz)

            request = create_request(url)
            station_el, teasers_el = station_schedule_base(request, self.esr_code)
            xml = ElementTree.ElementTree(station_el)
            threads_el = xml.findall('thread')
            assert len(threads_el) == 2
            check_thread(threads_el[0], arrival, None, days)
            check_thread(threads_el[1], arrival, departure, days)

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

        check_call(None, '13:21', '13:30', 'today days')
        check_call(pytz.timezone('Asia/Yekaterinburg'), '15:21', '15:30', 'today days')
        check_call(pytz.timezone('Pacific/Samoa'), '23:21', '23:30', 'previous days')
        check_call(pytz.timezone('Pacific/Kiritimati'), '00:21', '00:30', 'next days')

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

        station_el, teasers_el = station_schedule_base(create_request(), self.esr_code)
        xml = ElementTree.ElementTree(station_el)
        assert xml.findall('thread')[0].attrib['stops'] == u'остановки по списку'

        station_el, teasers_el = station_schedule_base(create_request(), self.esr_code, version='2')
        xml = ElementTree.ElementTree(station_el)
        assert xml.findall('thread')[0].attrib['stops'] == u''

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

        station_el, teasers_el = station_schedule_base(create_request(), self.esr_code)
        xml = ElementTree.ElementTree(station_el)

        with pytest.raises(KeyError):
            assert xml.findall('thread')[0].attrib['departure_platform']

        station_el, teasers_el = station_schedule_base(create_request(), self.esr_code, version='2')
        xml = ElementTree.ElementTree(station_el)
        assert xml.findall('thread')[0].attrib['departure_platform'] == u'*departure_platform*'

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

        def get_result(*args, **kwargs):
            station_el, teasers_el = station_schedule_base(*args, **kwargs)
            return ElementTree.ElementTree(station_el)

        xml = get_result(create_request('/?date={}'.format('2017-12-13')), self.esr_code, version='1')
        assert not xml.findall('thread/facilities')

        xml = get_result(create_request(), self.esr_code, version='2')
        assert not xml.findall('thread/facilities')

        request = create_request('/?date={}'.format('2017-12-13'))
        station_el = get_result(request, self.esr_code, version='2')
        xml = ElementTree.ElementTree(station_el)
        fac_titles = [f.attrib['title'] for f in xml.findall('thread/facilities/facility')]
        assert sorted(fac_titles) == ['beer', 'wifi']

    @replace_now(test_server_time_dt)
    def test_server_time(self):
        response = Client().get('/export/v2/suburban/station/{}'.format(self.esr_code), {'sub_version': '2.1'})
        xml_tree = etree.fromstring(response.content)
        check_server_time(test_server_time_dt, xml_tree)

    @replace_now(test_server_time_dt)
    def test_segment_departure_utc(self):
        departure_time = time(0, 30)
        params = {
            'date': test_server_time_dt.strftime('%Y-%m-%d'),
            'sub_version': '2.1'
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

        response = Client().get('/export/v2/suburban/station/{}'.format(self.esr_code), params)
        xml_tree = etree.fromstring(response.content)
        thread = xml_tree.xpath('/response/station/thread')[0]
        iso_dt = thread.attrib['departure_utc']
        utc_dt = parse_date(iso_dt)
        msk_dt_departure = MSK_TZ.localize(datetime.combine(test_server_time_dt.date(), departure_time))
        assert utc_dt == msk_dt_departure.astimezone(pytz.utc)
        assert iso_dt == '2001-02-01 21:30:00+00:00'

    def test_day_start_end_utc(self):
        create_station(__={'codes': {'esr': '100'}}, time_zone='Europe/Kiev')
        # Проверяем переход с летнего времени на зимнее utc+3 -> utc+2.
        params = {
            'date': '2016-10-30',
            'sub_version': '2.1'
        }

        response = Client().get('/export/v2/suburban/station/{}'.format('100'), params)
        xml_tree = etree.fromstring(response.content)
        station = xml_tree.xpath('/response/station')[0]
        assert station.attrib['day_start_utc'] == '2016-10-29 21:00:00+00:00'
        assert station.attrib['day_end_utc'] == '2016-10-30 21:59:59+00:00'
        time_diff = parse_date(station.attrib['day_end_utc']) - parse_date(station.attrib['day_start_utc'])
        assert (time_diff.days * 3600. * 24 + time_diff.seconds + 1) / 3600 == 25

        # Проверяем переход с зимнего времени на летнее utc+2 -> utc+3.
        params = {
            'date': '2016-3-27',
            'sub_version': '2.1'
        }

        response = Client().get('/export/v2/suburban/station/{}'.format('100'), params)
        xml_tree = etree.fromstring(response.content)
        station = xml_tree.xpath('/response/station')[0]
        assert station.attrib['day_start_utc'] == '2016-03-26 22:00:00+00:00'
        assert station.attrib['day_end_utc'] == '2016-03-27 20:59:59+00:00'
        time_diff = parse_date(station.attrib['day_end_utc']) - parse_date(station.attrib['day_start_utc'])
        assert (time_diff.days * 3600. * 24 + time_diff.seconds + 1) / 3600 == 23

    @replace_now(datetime(2001, 2, 2, 23))
    def test_date_today_tomorrow(self):
        station_from = create_station(__={'codes': {'esr': '100'}}, time_zone='Asia/Yekaterinburg')
        station_to = create_station()
        params = {
            'date': 'today',
            'sub_version': '2.1'
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

        response = Client().get('/export/v2/suburban/station/{}'.format(100), params)
        xml_tree = etree.fromstring(response.content)
        station = xml_tree.xpath('/response/station')[0]
        assert station.attrib['date'] == '2001-02-03'
        thread = xml_tree.xpath('/response/station/thread')[0]
        assert thread.attrib['departure'] == '01:30'
        assert thread.attrib['departure_utc'] == '2001-02-02 20:30:00+00:00'

        params = {
            'date': 'tomorrow',
            'sub_version': '2.1'
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

        response = Client().get('/export/v2/suburban/station/{}'.format(100), params)
        xml_tree = etree.fromstring(response.content)
        station = xml_tree.xpath('/response/station')[0]
        assert station.attrib['date'] == '2001-02-04'
        threads = xml_tree.xpath('/response/station/thread')
        assert len(threads) == 1
        assert threads[0].attrib['departure'] == '01:10'
        assert threads[0].attrib['departure_utc'] == '2001-02-03 20:10:00+00:00'

    def test_404_for_hidden_station(self):
        create_station(__={'codes': {'esr': '100'}}, hidden=True)

        response = Client().get('/export/v2/suburban/station/{}'.format(100), {})
        assert response.status_code == 404


class TestThreadBase(TestCase):
    def test_add_delays(self):
        request = create_request()
        thread = create_thread(
            schedule_v1=[
                [None, 0],
                [31, 42],
                [50, None],
            ]
        )
        rtstation = thread.path.select_related('station')[1]

        now = datetime.now()
        today = datetime(now.year, now.month, now.day)
        ztablo = ZTablo2.objects.create(
            thread=thread, rtstation=rtstation, station_id=rtstation.station_id,
            original_departure=today + timedelta(minutes=42),
            real_departure=today + timedelta(minutes=45),
        )

        with patch_add_departure_delays_data({'blabla': 42}) as m_add_delays_data:
            thread_el, teasers_el = thread_base(request, thread.uid, add_delays=True)

            assert len(m_add_delays_data.call_args_list) == 1
            assert m_add_delays_data.call_args_list[0][0][1] == ztablo

            xml = ElementTree.ElementTree(thread_el)
            thread_els = xml.findall('rtstation')
            assert len(thread_els) == 3
            assert thread_els[1].attrib['blabla'] == 42

    def test_stops(self):
        station = create_station()
        thread = create_thread(
            __={'calculate_noderoute': True},
            schedule_v1=[[None, 0, station], [0, None]])
        rtstation = RTStation.objects.get(thread=thread, station=station)

        thread_el, teasers_el = thread_base(create_request(), thread.uid)
        assert thread_el.attrib['stops'] == u''

        create_station_schedule(
            station=rtstation.station,
            route=thread.route,
            thread=thread,
            rtstation=rtstation,
            stops_translations='')

        thread_el, teasers_el = thread_base(create_request(), thread.uid)
        assert thread_el.attrib['stops'] == u'остановки по списку'

        thread_el, teasers_el = thread_base(create_request(), thread.uid, version='2')
        assert thread_el.attrib['stops'] == u''

    def test_train_schedule_plan(self):
        plan = create_train_schedule_plan(start_date=datetime.now() + timedelta(days=2))
        current_plan, next_plan = plan.get_current_and_next(datetime.now().date())

        thread = create_thread(schedule_plan=plan, translated_days_texts=u'[{}, {"ru": "ежедневно"}]')
        thread_el, teasers_el = thread_base(create_request(), thread.uid)
        assert thread_el.get('days') == u'ежедневно ' + next_plan.L_appendix()


class TestFillTransportType(TestCase):
    def test_valid(self):
        def create_el():
            return ElementTree.Element('el', field='value')

        el = create_el()
        thread = create_thread(express_type='not_express')
        fill_transport_type(el, thread)
        assert el.attrib['field'] == 'value'
        assert not el.attrib.get('type')

        el = create_el()
        thread = create_thread(express_type='aeroexpress')
        fill_transport_type(el, thread)
        assert el.attrib['field'] == 'value'
        assert el.attrib['type'] == 'aeroexpress'
        assert not el.findall('transport_subtype')

        el = create_el()
        thread = create_thread(express_type='express')
        fill_transport_type(el, thread)
        assert el.attrib['field'] == 'value'
        assert el.attrib['type'] == 'express'
        assert not el.findall('transport_subtype')

        el = create_el()
        color = create_transport_subtype_color(code=0, color='#ABCDEF')
        thread = create_thread(
            t_subtype=create_transport_subtype(t_type_id=TransportType.PLANE_ID, code='bla', title_ru=u'Бла', color=color)
        )
        fill_transport_type(el, thread)
        assert el.attrib['field'] == 'value'
        assert not el.findall('transport_subtype')

        el = create_el()
        fill_transport_type(el, thread, add_subtype=True)
        assert el.attrib['field'] == 'value'
        subtypes = el.findall('transport_subtype')
        assert len(subtypes) == 1

        subtype = subtypes[0]
        assert subtype.attrib['code'] == 'bla'
        assert subtype.attrib['title'] == u'Бла'
        assert subtype.attrib['color'] == '#ABCDEF'

        # при невыставленном цвете отдаем пустой цвет
        thread.t_subtype.color = None
        el = create_el()
        fill_transport_type(el, thread, add_subtype=True)
        assert el.findall('transport_subtype')[0].attrib['color'] == ''

        # если подтип - дефолтный для suburban, то не отдаем его
        thread.t_subtype = TransportSubtype.objects.get(id=TransportSubtype.SUBURBAN_ID)
        el = create_el()
        fill_transport_type(el, thread, add_subtype=True)
        assert not el.findall('transport_subtype')


class TestFillDaysAndExceptTexts(TestCase):
    def test_valid(self):
        m_days_text = mock.Mock(side_effect=[
            {
                'days_text': 'stay awhile',
                'except_days_text': 'and listen',
            },
            {
                'days_text': 'stay awhile2',
            }
        ])

        el1 = ElementTree.Element('el', field='value')
        el2 = ElementTree.Element('el', field='value2')
        thread = create_thread()
        today, shift, next_plan = 'a', 'b', 'c'

        with mock.patch.object(RThread, 'L_days_text_dict', m_days_text):
            fill_days_and_except_texts(today, el1, thread, shift, next_plan)
            assert len(m_days_text.call_args_list) == 1
            assert m_days_text.call_args_list[0][1] == {
                'shift': shift,
                'thread_start_date': today,
                'next_plan': next_plan,
                'show_days': True,
            }

            fill_days_and_except_texts(today, el2, thread, shift, next_plan)

        assert el1.attrib['field'] == 'value'
        assert el1.attrib['days'] == 'stay awhile'
        assert el1.attrib['except'] == 'and listen'

        assert el2.attrib['field'] == 'value2'
        assert el2.attrib['days'] == 'stay awhile2'
        assert 'except' not in el2.attrib

    def test_old_days_format_called(self):
        el = ElementTree.Element('el')
        thread = create_thread()
        today, shift, next_plan = 'a', 'b', 'c'

        with mock.patch('travel.rasp.export.export.views.in_xml.fill_days_and_except_texts_old') as m_old_days:
            fill_days_and_except_texts(today, el, thread, shift, next_plan, old_days_format=True)

            assert len(m_old_days.call_args_list) == 1
            assert m_old_days.call_args_list[0][0] == (today, el, thread, shift, next_plan)
            assert m_old_days.call_args_list[0][1] == {}

    def test_add_days_mask(self):
        today = datetime(2015, 03, 10)
        shift = 5
        thread = create_thread(
            year_days=[datetime(2015, 2, 11), datetime(2015, 3, 22)]
        )

        el = ElementTree.Element('el')
        fill_days_and_except_texts(today, el, thread, shift, add_days_mask=True)
        masks = el.findall('schedule/mask')
        assert len(masks) == 12

        result_masks = defaultdict(dict)
        for mask_el in masks:
            result_masks[mask_el.attrib['year']][mask_el.attrib['month']] = list(mask_el.attrib['days'])

        expected_masks = {
            '2015': {str(i): ['0'] * 31 for i in range(3, 13)},
            '2016': {str(i): ['0'] * 31 for i in range(1, 3)},
        }
        expected_masks['2016']['2'][15] = '1'
        expected_masks['2015']['3'][26] = '1'

        assert result_masks == expected_masks

        el = ElementTree.Element('el')
        fill_days_and_except_texts(today, el, thread, shift)
        masks = el.findall('schedule/mask')
        assert len(masks) == 0


class TestStationByEsr(TestCase):
    def test_get_station_by_esr_or_404(self):
        """Проверяем получение станции по коду esr."""
        stations = [
            create_station(__={'codes': {'esr': 'esr_0'}}),
            create_station(__={'codes': {'esr': 'esr_1'}})
        ]

        assert get_station_by_esr_or_404('esr_0') == stations[0]
        assert get_station_by_esr_or_404('esr_1') == stations[1]

        with pytest.raises(Http404):
            get_station_by_esr_or_404('not exist')


class TestLogSearch(TestCase):
    def test_valid(self):
        request = create_request(
            '?uuid=123',
            attribs={'NATIONAL_VERSION': 'ua'},
            headers={
                'user-agent': 'Android app: 3.20(320)',
                'referer': 'http://example.com'
            })

        station_from = create_station(id=42)
        settlement_to = create_settlement(id=43)

        dt = datetime(2015, 3, 12)
        create_rthread_segment()

        segments = [create_rthread_segment(), create_rthread_segment()]
        with mock.patch('travel.rasp.export.export.views.in_xml.users_search_log') as m_search_log, \
             replace_setting('USERS_SEARCH_LOG', '/somepath'):

            log_search(request, station_from, settlement_to, dt, segments)
            calls = m_search_log.log.call_args_list
            assert len(calls) == 1
            assert calls[0][0][0] == request
            logged_data = calls[0][0][1]

            suburban_code = TransportType.objects.get(id=TransportType.SUBURBAN_ID).code
            assert_that(logged_data, has_entries({
                'from_id': 's42',
                'to_id': 'c43',
                'user_type': 'Android app: 3.20(320)',
                'service': 'export',
                'transport_type': suburban_code,
                'when': '2015-03-12',
                't_type_counts': json.dumps({suburban_code: 2}, separators=(',', ':')),
                'national_version': 'ua',
                'tskv_format': 'rasp-users-search-log',
                'referer': 'http://example.com',
                'device_uuid': '123',
            }))

    def test_empty_user_type_skipped(self):
        request = create_request(headers={'user-agent': 'unknown agent'})
        with mock.patch('travel.rasp.export.export.views.in_xml.users_search_log') as m_search_log, \
                replace_setting('USERS_SEARCH_LOG', '/somepath'):

            log_search(request, None, None, None, [])

            assert m_search_log.call_count == 0
