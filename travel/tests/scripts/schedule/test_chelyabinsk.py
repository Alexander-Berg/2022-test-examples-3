# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
from hamcrest import assert_that, contains, contains_inanyorder, has_entries
from xml.etree import ElementTree

from common.cysix.builder import ChannelBlock, GroupBlock

from travel.rasp.rasp_scripts.scripts.schedule.chelyabinsk import (
    build_thread_stoppoints, build_group_threads, ChelyabinskBusClient, build_schedule
)


def test_stoppoints():
    stop1 = {
        'Point': 'st1',
        'DistanceFromTheFirstStop': 0,
        'ArrivalTime': '0001-01-01T21:10:00',
        'DepartureTime': '0001-01-01T21:20:00'
    }

    stop2 = {
        'Point': 'st2',
        'DistanceFromTheFirstStop': 50,
        'ArrivalTime': '0001-01-01T21:30:00',
        'DepartureTime': '0001-01-01T21:40:00',
        'TicketPrice': '300'
    }

    stop3 = {
        'Point': 'st3',
        'DistanceFromTheFirstStop': 120,
        'ArrivalTime': '0001-01-01T22:20:00',
        'DepartureTime': '0001-01-01T22:40:00',
        'TicketPrice': '400'
    }

    channel_block = ChannelBlock('bus', timezone='local')
    group_block = GroupBlock(channel_block, title='a', code='2', t_type='bus')
    fare = group_block.add_fare('1')
    stoppoints = build_thread_stoppoints(group_block, [stop1, stop2, stop3], fare)

    assert len(stoppoints) == 3

    stoppoint0 = stoppoints[0]
    stoppoint1 = stoppoints[1]
    stoppoint2 = stoppoints[2]

    assert stoppoint0['station'].title == 'st1'
    assert stoppoint0['distance'] == 0
    assert stoppoint0['arrival'] == ''
    assert stoppoint0['departure'] == '21:20:00'

    assert stoppoint1['station'].title == 'st2'
    assert stoppoint1['distance'] == 50
    assert stoppoint1['arrival'] == '21:30:00'
    assert stoppoint1['departure'] == '21:40:00'

    assert stoppoint2['station'].title == 'st3'
    assert stoppoint2['distance'] == 120
    assert stoppoint2['arrival'] == '22:20:00'
    assert stoppoint2['departure'] == '22:40:00'


def test_same_thread_two_days():
    thread = {
        'Flight': '11.12.2020 12:50:00  Челябинск Юность - Миасс  511/Э',
        'RoutingNumber': '511',
        'ModelBus': 'BMW',
        'AutotransportCompany': 'cmp1',
        'TrackList': [
            {
                'Point': 'st1',
                'DistanceFromTheFirstStop': 0,
                'ArrivalTime': '0001-01-01T21:10:00',
                'DepartureTime': '0001-01-01T21:20:00'
            },
            {
                'Point': 'st2',
                'DistanceFromTheFirstStop': 50,
                'ArrivalTime': '0001-01-01T21:30:00',
                'DepartureTime': '0001-01-01T21:40:00',
            }
        ]
    }

    channel_block = ChannelBlock('bus', timezone='local')
    group_block = GroupBlock(channel_block, title='a', code='2', t_type='bus')
    schedule = [
        ('2020-12-10', [thread]),
        ('2020-12-11', [thread])
    ]

    threads = build_group_threads(schedule, set(), group_block)

    assert len(threads) == 1
    thread = threads[0]

    assert_that(thread['dates'], contains_inanyorder('2020-12-10', '2020-12-11'))
    assert thread['title'] == 'Челябинск Юность - Миасс  511/Э'
    assert thread['number'] == '511'
    assert thread['carrier'].title == 'cmp1'
    assert thread['vehicle'].title == 'BMW'


def test_threads():
    track_list = [
        {
            'Point': 'st1',
            'DistanceFromTheFirstStop': 0,
            'ArrivalTime': '0001-01-01T21:10:00',
            'DepartureTime': '0001-01-01T21:20:00'
        },
        {
            'Point': 'st2',
            'DistanceFromTheFirstStop': 50,
            'ArrivalTime': '0001-01-01T21:30:00',
            'DepartureTime': '0001-01-01T21:40:00',
        }
    ]

    thread0 = {
        'Flight': '11.12.2020 12:50:00  Челябинск Юность - Миасс  511/Э',
        'RoutingNumber': '511',
        'ModelBus': 'BMW',
        'AutotransportCompany': 'cmp1',
        'TrackList': track_list
    }

    thread1 = {
        'Flight': '11.12.2020 12:50:00  Челябинск - Нижний Тагил  244',
        'RoutingNumber': '244',
        'ModelBus': 'Volvo',
        'AutotransportCompany': 'cmp1',
        'TrackList': track_list
    }

    thread2 = {
        'Flight': '11.12.2020 12:50:00  Челябинск - Екатеринбург  404',
        'RoutingNumber': '404',
        'ModelBus': None,
        'AutotransportCompany': None,
        'TrackList': track_list
    }

    channel_block = ChannelBlock('bus', timezone='local')
    group_block = GroupBlock(channel_block, title='a', code='2', t_type='bus')
    schedule = [
        ('2020-12-10', [thread0, thread1, thread2])
    ]

    threads = build_group_threads(schedule, set(), group_block)

    assert len(threads) == 3

    thread = threads[0]
    assert_that(thread['dates'], contains('2020-12-10'))
    assert thread['title'] == 'Челябинск Юность - Миасс  511/Э'
    assert thread['number'] == '511'
    assert thread['carrier'].title == 'cmp1'
    assert thread['vehicle'].title == 'BMW'
    assert len(thread['stoppoints']) == 2

    thread = threads[1]
    assert_that(thread['dates'], contains('2020-12-10'))
    assert thread['title'] == 'Челябинск - Нижний Тагил  244'
    assert thread['number'] == '244'
    assert thread['carrier'].title == 'cmp1'
    assert thread['vehicle'].title == 'Volvo'
    assert len(thread['stoppoints']) == 2

    thread = threads[2]
    assert_that(thread['dates'], contains('2020-12-10'))
    assert thread['title'] == 'Челябинск - Екатеринбург  404'
    assert thread['number'] == '404'
    assert 'carrier' not in thread
    assert 'vehicle' not in thread
    assert len(thread['stoppoints']) == 2


def test_schedule():
    thread0 = {
        'Flight': '11.12.2020 12:50:00  Челябинск Юность - Миасс  511/Э',
        'RoutingNumber': '511',
        'ModelBus': 'BMW',
        'AutotransportCompany': 'cmp1',
        'TrackList': [
            {
                'Point': 'st1',
                'DistanceFromTheFirstStop': 0,
                'ArrivalTime': '0001-01-01T21:10:00',
                'DepartureTime': '0001-01-01T21:20:00'
            },
            {
                'Point': 'st2',
                'DistanceFromTheFirstStop': 50,
                'ArrivalTime': '0001-01-01T21:30:00',
                'DepartureTime': '0001-01-01T21:40:00',
                'TicketPrice': '300'
            },
            {
                'Point': 'st3',
                'DistanceFromTheFirstStop': 120,
                'ArrivalTime': '0001-01-01T22:20:00',
                'DepartureTime': '0001-01-01T22:40:00',
                'TicketPrice': '400'
            }
        ]
    }

    thread1 = {
        'Flight': '11.12.2020 12:50:00  Челябинск - Нижний Тагил  244',
        'RoutingNumber': '244',
        'ModelBus': 'Volvo',
        'AutotransportCompany': 'cmp1',
        'TrackList': [
            {
                'Point': 'st1',
                'DistanceFromTheFirstStop': 0,
                'ArrivalTime': '0001-01-01T21:10:00',
                'DepartureTime': '0001-01-01T21:20:00'
            },
            {
                'Point': 'st2',
                'DistanceFromTheFirstStop': 50,
                'ArrivalTime': '0001-01-01T21:30:00',
                'DepartureTime': '0001-01-01T21:40:00',
                'TicketPrice': '300'
            },
        ]
    }

    thread2 = {
        'Flight': '11.12.2020 12:50:00  Челябинск - Екатеринбург  404',
        'RoutingNumber': '404',
        'ModelBus': 'PAZ',
        'AutotransportCompany': 'cmp1',
        'TrackList': [
            {
                'Point': 'st1',
                'DistanceFromTheFirstStop': 0,
                'ArrivalTime': '0001-01-01T21:10:00',
                'DepartureTime': '0001-01-01T21:20:00'
            },
            {
                'Point': None,
                'DistanceFromTheFirstStop': 50,
                'ArrivalTime': '0001-01-01T21:30:00',
                'DepartureTime': '0001-01-01T21:40:00',
                'TicketPrice': '300'
            }
        ]
    }

    client = ChelyabinskBusClient('http://localhost:80', 'us', 'pas')

    buses = [{'id': '2', 'name': 'bus_name'}]
    schedule = [
        ('2020-12-24', [thread0, thread1, thread2]),
        ('2020-12-25', [thread0])
    ]

    with mock.patch.object(ChelyabinskBusClient, 'get_buses', return_value=buses):
        with mock.patch.object(ChelyabinskBusClient, 'get_schedule', return_value=schedule):
            with mock.patch('travel.rasp.rasp_scripts.scripts.schedule.chelyabinsk.title_hash', return_value='hash'):

                schedule_xml = build_schedule(client, 1)
                schedule = ElementTree.fromstring(schedule_xml.encode('utf-8'))

                # dictionaries
                vehicles = [v.get('title') for v in schedule.findall('group/vehicles/vehicle')]
                carriers = [c.get('title') for c in schedule.findall('group/carriers/carrier')]
                stations = [s.get('title') for s in schedule.findall('group/stations/station')]

                assert_that(vehicles, contains_inanyorder('BMW', 'Volvo'))
                assert_that(carriers, contains('cmp1'))
                assert_that(stations, contains_inanyorder('st1', 'st2', 'st3'))

                # prices
                fares = schedule.findall('group/fares/fare')
                prices = [p.get('price') for p in schedule.findall('group/fares/fare/price')]

                assert len(fares) == 3
                assert_that(prices, contains_inanyorder('300', '300', '400'))

                # threads
                threads = schedule.findall('group/threads/thread')
                assert len(threads) == 2

                thread = schedule.find('group/threads/thread')
                assert_that(thread.attrib, has_entries({
                    'vehicle_title': 'BMW',
                    'title': u'Челябинск Юность - Миасс  511/Э',
                    'vehicle_code': 'hash',
                    'fare_code': '#auto_generated_code#_1',
                    'carrier_title': 'cmp1',
                    'carrier_code': 'hash',
                    'number': '511'
                }))

                schedule_days = [s.get('days') for s in thread.findall('schedules/schedule')]
                assert_that(schedule_days, contains_inanyorder('2020-12-24', '2020-12-25'))

                stoppoints = [s.attrib for s in thread.findall('stoppoints/stoppoint')]
                assert_that(stoppoints, contains_inanyorder(
                    has_entries({
                        'arrival_time': '',
                        'station_title': 'st1',
                        'station_code': 'hash',
                        'departure_time': '21:20:00',
                        'distance': '0'
                    }),
                    has_entries({
                        'arrival_time': '21:30:00',
                        'station_title': 'st2',
                        'station_code': 'hash',
                        'departure_time': '21:40:00',
                        'distance': '50'
                    }),
                    has_entries({
                        'arrival_time': '22:20:00',
                        'station_title': 'st3',
                        'station_code': 'hash',
                        'departure_time': '22:40:00',
                        'distance': '120'
                    })
                ))
