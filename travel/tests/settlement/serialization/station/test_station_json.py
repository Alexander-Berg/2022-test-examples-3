# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock

from common.models.geo import Station
from common.tester.factories import create_station
from common.tester.testcase import TestCase
from travel.rasp.morda_backend.morda_backend.settlement.serialization.station import station_json


class TestStationJson(TestCase):
    def test_station_json_bus_ru(self):
        station = create_station(id=2222, t_type='bus', type_choices='schedule')
        status = 'status'

        with mock.patch.object(Station, 'L_popular_title', return_value='popular title') as m_L_popular_title, \
                mock.patch.object(Station, 'is_metro',
                                  return_value='metro', new_callable=mock.PropertyMock) as m_is_metro, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.serialization.station_helpers.station_majority_json',
                           return_value='majority') as m_majority, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.serialization.station_helpers.station_type_json',
                           return_value='station type') as m_station_type, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.serialization.station_helpers.station_aeroexpress_json',
                           return_value='station aeroexpress') as m_station_aeroexpress:

            json = station_json(station, status, 'ru')
            assert json == {
                'id': 2222,
                'title': 'popular title',
                'popular_title': 'popular title',
                't_type': 'bus',
                'is_metro': 'metro',
                'majority': 'majority',
                'station_type': 'station type',
                'status': 'status',
                'aeroexpress': 'station aeroexpress',
                'page_type': 'bus',
                'main_subtype': 'schedule'
            }

        m_L_popular_title.assert_called_with(lang='ru')
        m_is_metro.assert_called_once_with()
        m_majority.assert_called_once_with(station)
        m_station_type.assert_called_once_with(station, 'ru')
        m_station_aeroexpress.assert_called_once_with(station)

    def test_station_json_plane_ua(self):
        station = create_station(id=2222, t_type='plane', type_choices='tablo')
        status = 'status'

        with mock.patch.object(Station, 'L_popular_title', return_value='popular title') as m_L_popular_title, \
                mock.patch.object(Station, 'is_metro',
                                  return_value='metro', new_callable=mock.PropertyMock) as m_is_metro, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.serialization.station_helpers.station_majority_json',
                           return_value='majority') as m_majority, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.serialization.station_helpers.station_type_json',
                           return_value='station type') as m_station_type, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.serialization.station_helpers.station_aeroexpress_json',
                           return_value='station aeroexpress') as m_station_aeroexpress:

            json = station_json(station, status, 'uk')
            assert json == {
                'id': 2222,
                'title': 'popular title',
                'popular_title': 'popular title',
                't_type': 'plane',
                'is_metro': 'metro',
                'majority': 'majority',
                'station_type': 'station type',
                'status': 'status',
                'aeroexpress': 'station aeroexpress',
                'page_type': 'plane',
                'main_subtype': 'plane'
            }

        m_L_popular_title.assert_called_with(lang='uk')
        m_is_metro.assert_called_once_with()
        m_majority.assert_called_once_with(station)
        m_station_type.assert_called_once_with(station, 'uk')
        m_station_aeroexpress.assert_called_once_with(station)
