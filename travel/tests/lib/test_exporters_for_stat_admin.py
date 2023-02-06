import requests
from mock import Mock, patch

from travel.avia.library.python.common.models.geo import Station2Settlement
from travel.avia.library.python.common.models.transport import TransportType
from travel.avia.admin.lib.exporters_for_stat_admin import AviaStatsSettlementExporter, AviaStatsAirportExporter
from travel.avia.library.python.tester.factories import create_settlement, create_station
from travel.avia.library.python.tester.testcase import TestCase


class TestAviaStatsSettlementExporter(TestCase):
    def setUp(self):
        super(TestAviaStatsSettlementExporter, self).setUp()

        self._settlement_exporter = AviaStatsSettlementExporter(Mock())

    def test_empty(self):
        export_result = {
            'status': 'ok',
            'data': {}
        }

        response = Mock()
        response.json = Mock(return_value=export_result)

        with patch.object(requests, 'post', return_value=response) as mock_method:
            self._settlement_exporter.export()
            send_to_stat_data = mock_method.call_args_list[0][1]['json']
            assert send_to_stat_data == []

    def test_raise_http_error(self):
        with patch.object(requests, 'post', side_effect=Exception('Boom!')):
            try:
                self._settlement_exporter.export()
                raise Exception('must throw exception')
            except Exception as e:
                assert e.message == 'Boom!'

    def test_return_strange_status(self):
        export_result = {
            'status': 'error'
        }

        response = Mock()
        response.json = Mock(return_value=export_result)

        with patch.object(requests, 'post', return_value=response):
            try:
                self._settlement_exporter.export()
                raise Exception('must throw exception')
            except Exception as e:
                assert isinstance(e, AssertionError)

    def test_all_variants(self):
        settlement_with_airport = create_settlement(title='settlement_with_airport')
        create_station(
            settlement_id=settlement_with_airport.id,
            t_type=TransportType.PLANE_ID
        )

        other_settlement_with_airport = create_settlement(title='other_settlement_with_airport')
        create_station(
            settlement_id=other_settlement_with_airport.id,
            t_type=TransportType.PLANE_ID
        )

        settlement_with_s2s_airport = create_settlement(title='settlement_with_s2s_airport')
        airport = create_station(t_type=TransportType.PLANE_ID)
        Station2Settlement.objects.create(
            settlement_id=settlement_with_s2s_airport.id,
            station_id=airport.id
        )

        other_settlement_with_s2s_airport = create_settlement(title='other_settlement_with_s2s_airport')
        airport = create_station(t_type=TransportType.PLANE_ID)
        Station2Settlement.objects.create(
            settlement_id=other_settlement_with_s2s_airport.id,
            station_id=airport.id
        )

        settlement_with_train_station = create_settlement(title='settlement_with_train_station')
        create_station(settlement_id=settlement_with_train_station.id, t_type=TransportType.TRAIN_ID)

        other_settlement_with_s2s_train = create_settlement(title='other_settlement_with_s2s_train')
        train = create_station(t_type=TransportType.TRAIN_ID)
        Station2Settlement.objects.create(
            settlement_id=other_settlement_with_s2s_train.id,
            station_id=train.id
        )

        export_result = {
            'status': 'ok',
            'data': {}
        }

        response = Mock()
        response.json = Mock(return_value=export_result)

        with patch.object(requests, 'post', return_value=response) as mock_method:
            self._settlement_exporter.export()
            send_to_stat_data = mock_method.call_args_list[0][1]['json']

            assert sorted(send_to_stat_data) == sorted([
                {'id': settlement_with_airport.id, 'title': settlement_with_airport.title},
                {'id': other_settlement_with_airport.id, 'title': other_settlement_with_airport.title},
                {'id': settlement_with_s2s_airport.id, 'title': settlement_with_s2s_airport.title},
                {'id': other_settlement_with_s2s_airport.id, 'title': other_settlement_with_s2s_airport.title},
            ])


class TestAviaStatsAirportExporter(TestCase):
    def setUp(self):
        super(TestAviaStatsAirportExporter, self).setUp()

        self._airport_exporter = AviaStatsAirportExporter(Mock())

    def test_empty(self):
        export_result = {
            'status': 'ok',
            'data': {}
        }

        response = Mock()
        response.json = Mock(return_value=export_result)

        with patch.object(requests, 'post', return_value=response) as mock_method:
            self._airport_exporter.export()
            send_to_stat_data = mock_method.call_args_list[0][1]['json']
            assert send_to_stat_data == []

    def test_raise_http_error(self):
        with patch.object(requests, 'post', side_effect=Exception('Boom!')):
            try:
                self._airport_exporter.export()
                raise Exception('must throw exception')
            except Exception as e:
                assert e.message == 'Boom!'

    def test_return_strange_status(self):
        export_result = {
            'status': 'error'
        }

        response = Mock()
        response.json = Mock(return_value=export_result)

        with patch.object(requests, 'post', return_value=response):
            try:
                self._airport_exporter.export()
                raise Exception('must throw exception')
            except Exception as e:
                assert isinstance(e, AssertionError)

    def test_all_variants(self):
        airport = create_station(
            title='airport',
            t_type=TransportType.PLANE_ID
        )
        other_airport = create_station(
            title='other_airport',
            t_type=TransportType.PLANE_ID
        )
        create_station(
            title='train_station',
            t_type=TransportType.TRAIN_ID
        )
        create_station(
            title='other_train_station',
            t_type=TransportType.TRAIN_ID
        )

        export_result = {
            'status': 'ok',
            'data': {}
        }

        response = Mock()
        response.json = Mock(return_value=export_result)

        with patch.object(requests, 'post', return_value=response) as mock_method:
            self._airport_exporter.export()
            send_to_stat_data = mock_method.call_args_list[0][1]['json']

            assert sorted(send_to_stat_data) == sorted([
                {'id': airport.id, 'title': airport.title},
                {'id': other_airport.id, 'title': other_airport.title},
            ])
