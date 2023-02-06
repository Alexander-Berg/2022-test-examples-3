# -*- coding: utf-8 -*-

from __future__ import absolute_import

from datetime import datetime

import mock
from lxml import etree

from travel.avia.library.python.common.models.schedule import RThread, RTStation
from travel.avia.library.python.common.models.geo import Station
from travel.avia.library.python.common.utils.date import MSK_TIMEZONE, environment
import travel.avia.library.python.route_search.transfers
from travel.avia.library.python.route_search.models import ZNodeRoute2
from travel.avia.library.python.route_search.transfers import (
    TransferSegment, find_routes, parse_response, fetch_threads_and_stations,
    fetch_rtstaitons, fill_segments, add_stops, get_variants, Group
)
from travel.avia.library.python.tester.factories import create_settlement, create_station, create_thread
from travel.avia.library.python.tester.mocks import patch_urlopen, set_setting
from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.library.python.tester.utils.datetime import replace_now

ONE_VARIANT_XML = """
<routes>
    <group>
        <variant >
            <route start_date="2016-03-10" thread_id="1" departure_datetime="2016-03-10 20:52" departure_station_id="1" arrival_datetime="2016-03-12 08:38" arrival_station_id="2" ></route>
        </variant>
    </group>
</routes>
""".strip()

ROUTES_XML = """
<routes>
    <group>
        <variant >
            <route start_date="2016-03-10" thread_id="1" departure_datetime="2016-03-10 20:52" departure_station_id="9607404" arrival_datetime="2016-03-12 08:38" arrival_station_id="9602499" ></route>
        </variant>
        <variant >
            <route start_date="2016-03-10" thread_id="2" departure_datetime="2016-03-10 21:32" departure_station_id="9607404" arrival_datetime="2016-03-12 10:00" arrival_station_id="9602499" ></route>
        </variant>
        <variant >
            <route start_date="2016-03-10" thread_id="3" departure_datetime="2016-03-10 03:41" departure_station_id="9607404" arrival_datetime="2016-03-11 06:01" arrival_station_id="2000002" ></route>
            <route start_date="2016-03-11" thread_id="4" departure_datetime="2016-03-11 06:36" departure_station_id="2000002" arrival_datetime="2016-03-18 11:21" arrival_station_id="2006004" ></route>
            <route start_date="2016-03-11" thread_id="5" departure_datetime="2016-03-11 07:30" departure_station_id="2006004" arrival_datetime="2016-03-11 11:30" arrival_station_id="9602494" ></route>
        </variant>
        <variant >
            <route start_date="2016-03-10" thread_id="6" departure_datetime="2016-03-10 15:17" departure_station_id="9607404" arrival_datetime="2016-03-11 17:58" arrival_station_id="2000002" ></route>
            <route start_date="2016-03-11" thread_id="7" departure_datetime="2016-03-11 18:33" departure_station_id="2000002" arrival_datetime="2016-03-18 23:18" arrival_station_id="2006004" ></route>
            <route start_date="2016-03-11" thread_id="8" departure_datetime="2016-03-11 19:30" departure_station_id="2006004" arrival_datetime="2016-03-11 23:30" arrival_station_id="9602494" ></route>
        </variant>
    </group>
</routes>
""".strip()


class TestTransferSegment(TestCase):
    def test_is_valid(self):
        def create_segment(departure=None, arrival=None):
            departure = departure if departure is not None else datetime.now()
            arrival = arrival if arrival is not None else datetime.now()

            el = etree.Element('some_element', {
                'departure_datetime': departure.strftime('%Y-%m-%d %H:%M'),
                'arrival_datetime': arrival.strftime('%Y-%m-%d %H:%M'),
                'start_date': departure.strftime('%Y-%m-%d'),
                'departure_station_id': '1',
                'arrival_station_id': '2',
                'thread_id': '123',
            })

            segment = TransferSegment(el, datetime.now())
            segment.thread = RThread(tz_start_time=departure)
            segment.station_from = Station()
            segment.station_to = Station()
            segment.rtstation_from = RTStation(tz_departure=0, time_zone=MSK_TIMEZONE)
            segment.rtstation_to = RTStation(tz_arrival=20, time_zone=MSK_TIMEZONE)

            return segment

        segment = create_segment()
        assert segment.is_valid()

        segment = create_segment(departure=datetime(2008, 1, 1))
        assert not segment.is_valid()

        segment = create_segment(arrival=datetime(2008, 1, 1))
        assert not segment.is_valid()


class TestFindRoutes(TestCase):
    def test_empty_if_invalid_transport(self):
        point_from = Station()
        point_to = Station()

        for ttype, called in [('helicopter', False), ('plane', True), ('bus', True), ('spaceship', False)]:
            m_parse_response = mock.Mock(wraps=parse_response)
            with mock.patch('travel.avia.library.python.route_search.transfers.parse_response', m_parse_response):
                groups, xml_str = find_routes(point_from, point_to, datetime.now().date(), ttype)
                assert groups == []

                xml = etree.fromstring(xml_str.encode('utf8'))
                assert len(xml.xpath('groups')) == 0

                assert m_parse_response.called == called

    def test_find_routes(self):
        point_from = create_settlement()
        point_to = create_settlement()
        tree = etree.fromstring(ROUTES_XML)
        t_type = ['bus', 'train', 'plain']

        with patch_urlopen(travel.avia.library.python.route_search.transfers, etree.tostring(tree)) as m_urlopen, \
                mock.patch('travel.avia.library.python.route_search.transfers.parse_response', return_value=None) as m_parse, \
                set_setting('PATHFINDER_URL', 'not None'):
            groups, xml_str = find_routes(point_from, point_to, datetime.now().date(), t_type)

            assert groups is None
            assert m_urlopen.called
            m_parse.assert_called_once_with(xml_str)
            assert etree.tostring(tree) == xml_str


class TestFetchFields(TestCase):
    def test_fetch_threads_and_stations(self):
        tree = etree.fromstring(ONE_VARIANT_XML)
        segment = TransferSegment(tree.find('group').find('variant').find('route'), datetime.now())
        st_from = create_station(id=1)
        st_to = create_station(id=2)
        thread = create_thread(uid=1)
        n_segments, th_by_id, st_by_id = fetch_threads_and_stations([segment])
        assert len(n_segments) == 1
        assert n_segments[0].station_from == st_from
        assert n_segments[0].station_to == st_to
        assert n_segments[0].thread == thread

    def test_fetch_rtstaitons(self):
        tree = etree.fromstring(ONE_VARIANT_XML)
        segment = TransferSegment(tree.find('group').find('variant').find('route'), datetime.now())
        station_from = create_station(id=1)
        station_to = create_station(id=2)
        create_thread(uid=1,
                      schedule_v1=[
                          [None, 0, station_from],
                          [10, None, station_to],
                      ])
        n_segments, th_by_id, st_by_id = fetch_threads_and_stations([segment])
        fetch_rtstaitons([segment], th_by_id, st_by_id)

        assert n_segments[0].rtstation_from.station == station_from
        assert n_segments[0].rtstation_to.station == station_to

    def test_fill_segments(self):
        segments = 'segments'
        st_ids = 'st_ids'
        th_ids = 'th_ids'

        def f_t_s(segments):
            return segments, th_ids, st_ids

        with mock.patch('travel.avia.library.python.route_search.transfers.fetch_threads_and_stations', side_effect=f_t_s) as m_f_t_s, \
                mock.patch('travel.avia.library.python.route_search.transfers.fetch_rtstaitons') as m_f_rts:
            fill_segments(segments)
            m_f_t_s.assert_called_once_with(segments)
            m_f_rts.assert_called_once_with(segments, th_ids, st_ids)

    def test_add_stops(self):
        tree = etree.fromstring(ONE_VARIANT_XML)
        segment = TransferSegment(tree.find('group').find('variant').find('route'), datetime.now())
        stops_text = u'остановки'
        station_from = create_station(id=1)
        station_to = create_station(id=2)
        thread = create_thread(uid=1,
                               schedule_v1=[
                                   [None, 0, station_from],
                                   [10, None, station_to],
                               ])

        ZNodeRoute2(
            route_id=thread.route_id,
            thread=thread,
            station_from_id=station_from.id,
            station_to_id=station_to.id,
            stops_translations=stops_text
        ).save()
        n_segments, th_by_id, st_by_id = fetch_threads_and_stations([segment])
        add_stops(n_segments)
        assert len(n_segments) == 1
        assert n_segments[0].stops_translations == stops_text


class TestParseResponse(TestCase):
    def test_parse_response(self):
        tree = etree.fromstring(ROUTES_XML)

        # невалидный xml
        assert [] == parse_response('><')

        with mock.patch('travel.avia.library.python.route_search.transfers.fill_segments') as m_fill_segment, \
                mock.patch('travel.avia.library.python.route_search.transfers.add_stops') as m_add_stops, \
                mock.patch('travel.avia.library.python.route_search.transfers.Group.transfers', return_value=True), \
                mock.patch('travel.avia.library.python.route_search.transfers.Variant.transfers', return_value=True), \
                mock.patch('travel.avia.library.python.route_search.transfers.TransferSegment.is_valid', return_value=True), \
                mock.patch('travel.avia.library.python.route_search.transfers.Variant._add_transfers_info'):

            groups = parse_response(etree.tostring(tree))

            assert m_fill_segment.call_count == 1
            assert m_add_stops.call_count == 1

            assert len(groups) == 1
            assert len(groups[0].variants) == 4
            assert int(groups[0].variants[0].segments[0].thread_uid) == 1
            assert [int(segment.thread_uid) for segment in groups[0].variants[3].segments] == [6, 7, 8]

    @replace_now('2000-01-01 00:00:00')
    def test_get_variants(self):
        point_from = create_station(id=1)
        point_to = create_station(id=2)
        tree = etree.fromstring(ONE_VARIANT_XML)
        t_type = ['bus', 'train', 'plain']
        group = Group(tree.find('group'))
        with mock.patch('travel.avia.library.python.route_search.transfers.find_routes', return_value=([group], None)) as m_find_routes:
            variants = list(get_variants(point_from, point_to, environment.now_aware(), t_type))

            assert len(variants) == 1
            assert variants[0] == group.variants[0]
            m_find_routes.assert_called_once_with(point_from, point_to, environment.now_aware(), t_type)

        with mock.patch.object(travel.avia.library.python.route_search.transfers.Variant, 'transfers') as m_v_transfers:
            m_v_transfers.__get__ = mock.Mock(return_value=[point_from])
            group = Group(tree.find('group'))
            with mock.patch('travel.avia.library.python.route_search.transfers.find_routes', return_value=([group], None)):
                variants = list(get_variants(point_from, point_to, environment.now_aware(), t_type))

                assert len(variants) == 0
