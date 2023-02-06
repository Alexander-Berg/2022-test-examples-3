# -*- coding: utf-8 -*-
import pytz
from datetime import datetime, timedelta
from mock import Mock

from travel.avia.library.python.common.models.transport import TransportType
from travel.avia.library.python.common.utils import environment

from travel.avia.library.python.tester.factories import (
    create_partner, create_dohop_vendor, create_airport, create_avia_currency,
    create_station, create_company, create_aviacompany
)
from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon.ticket_daemon.lib.yt_loggers.variants_logger2 import VariantsLogger2
from travel.avia.ticket_daemon.ticket_daemon.lib.yt_loggers.abstract_yt_logger import IObjectLogger
from travel.avia.ticket_daemon.ticket_daemon.lib.baggage import Baggage, SourceInt
from travel.avia.ticket_daemon.ticket_daemon.daemon_tester import (
    create_query, create_variant, create_flight
)


class VariantLogger2Test(TestCase):
    def setUp(self):
        reset_all_caches()
        self.RUR = create_avia_currency()
        self.USD = create_avia_currency(
            title='dollar', code='USD', iso_code='USD'
        )

        self.query_partner = create_partner(
            code='query_partner'
        )
        self.partner = create_partner(
            code='dohop'
        )
        # For test when dohop_id != partner.id
        self.vendor = create_dohop_vendor(
            dohop_id=self.partner.id + 1
        )

        self._station_1 = create_airport('iata1')
        self._station_2 = create_airport('iata2')
        self._station_3 = create_airport('iata3')

        fake_environment = Mock(environment)
        fake_environment.unixtime = Mock(
            return_value=123
        )
        fake_logger = Mock(IObjectLogger)
        self.actual_logger_calls = []
        fake_logger.log = Mock(side_effect=self.actual_logger_calls.append)

        self.variants_logger = VariantsLogger2(
            yt_logger=fake_logger,
            environment=fake_environment,
            logger=Mock()
        )

    def test_empty(self):
        self.variants_logger._log(
            query=create_query(),
            partner=self.partner,
            partner_variants=[]
        )

        assert len(self.actual_logger_calls) == 0

    def test_common_data(self):
        query = create_query(
            when='2018-01-01',
            return_date='2019-01-01',
            passengers='3_4_5'
        )
        v = create_variant(
            query=query,
            partner=self.partner,
            forward_flights=[
                create_flight(station_from=self._station_1, station_to=self._station_2),
                create_flight(station_from=self._station_2, station_to=self._station_3)
            ]
        )

        self.variants_logger._log(
            query=query,
            partner=self.query_partner,
            partner_variants=[v]
        )

        assert len(self.actual_logger_calls) == 1

        actual = self.actual_logger_calls[0]
        assert actual['query_id'] == query.id
        assert actual['init_id'] == query.id
        assert actual['adults'] == 3
        assert actual['children'] == 4
        assert actual['infants'] == 5
        assert actual['class_id'] == 1
        assert datetime.utcfromtimestamp(actual['forward_date']) == datetime(2018, 1, 1)
        assert datetime.utcfromtimestamp(actual['backward_date']) == datetime(2019, 1, 1)
        assert actual['national_version_id'] == 1
        assert actual['service_id'] == 1
        assert actual['from_settlement_id'] == query.point_from.id
        assert actual['from_airport_id'] is None
        assert actual['to_settlement_id'] == query.point_to.id
        assert actual['to_airport_id'] is None
        assert actual['partner_id'] == self.partner.id
        assert actual['vendor_id'] is None
        assert actual['unixtime'] == 123

    def test_one_way_query(self):
        query = create_query(
            when='2018-01-01',
            passengers='3_4_5'
        )
        v = create_variant(
            query=query,
            partner=self.partner,
            forward_flights=[
                create_flight(station_from=self._station_1,
                              station_to=self._station_2),
                create_flight(station_from=self._station_2,
                              station_to=self._station_3)
            ]
        )

        self.variants_logger._log(
            query=query,
            partner=self.query_partner,
            partner_variants=[v]
        )

        assert len(self.actual_logger_calls) == 1
        actual = self.actual_logger_calls[0]
        assert actual['backward_date'] is None

    def test_from_airport_with_settlement(self):
        query = create_query(
            when='2018-01-01',
            from_is_settlement=False,
            passengers='3_4_5'
        )
        v = create_variant(
            query=query,
            partner=self.partner,
            forward_flights=[
                create_flight(station_from=self._station_1,
                              station_to=self._station_2),
                create_flight(station_from=self._station_2,
                              station_to=self._station_3)
            ]
        )

        self.variants_logger._log(
            query=query,
            partner=self.query_partner,
            partner_variants=[v]
        )

        assert len(self.actual_logger_calls) == 1
        actual = self.actual_logger_calls[0]

        assert actual['from_settlement_id'] == query.point_from.settlement_id
        assert actual['from_airport_id'] == query.point_from.id

    def test_from_airport_without_settlement(self):
        query = create_query(
            when='2018-01-01',
            from_is_settlement=False,
            passengers='3_4_5',
            attach_from_settlement=False
        )
        v = create_variant(
            query=query,
            partner=self.partner,
            forward_flights=[
                create_flight(station_from=self._station_1,
                              station_to=self._station_2),
                create_flight(station_from=self._station_2,
                              station_to=self._station_3)
            ]
        )

        self.variants_logger._log(
            query=query,
            partner=self.query_partner,
            partner_variants=[v]
        )

        assert len(self.actual_logger_calls) == 1
        actual = self.actual_logger_calls[0]

        assert actual['from_settlement_id'] is None
        assert actual['from_airport_id'] == query.point_from.id

    def test_dohop_partner(self):
        query = create_query(
            when='2018-01-01',
            from_is_settlement=False,
            passengers='3_4_5',
            attach_from_settlement=False
        )
        v = create_variant(
            query=query,
            partner=self.vendor,
            forward_flights=[
                create_flight(station_from=self._station_1,
                              station_to=self._station_2),
                create_flight(station_from=self._station_2,
                              station_to=self._station_3)
            ]
        )

        self.variants_logger._log(
            query=query,
            partner=self.query_partner,
            partner_variants=[v]
        )

        assert len(self.actual_logger_calls) == 1
        actual = self.actual_logger_calls[0]

        assert actual['partner_id'] == self.partner.id
        assert actual['vendor_id'] == self.vendor.id

    def test_variant_data(self):
        query = create_query(
            when='2018-01-01',
            return_date='2018-01-02',
            passengers='3_4_5'
        )
        v = create_variant(
            price=100.15,
            currency='USD',
            national_price=200.99,
            national_currency='RUR',
            query=query,
            partner=self.partner,
            forward_flights=[
                create_flight(station_from=self._station_1,
                              station_to=self._station_2,
                              local_departure=datetime(2017, 9, 1, 1),
                              delta=60),
                create_flight(station_from=self._station_2,
                              station_to=self._station_3,
                              local_departure=datetime(2017, 9, 1, 2),
                              delta=120
                              )
            ],
            backward_flights=[
                create_flight(station_from=self._station_3,
                              station_to=self._station_1,
                              local_departure=datetime(2017, 10, 1, 2),
                              delta=240
                              )
            ],
            charter=True,
        )

        self.variants_logger._log(
            query=query,
            partner=self.query_partner,
            partner_variants=[v]
        )

        assert len(self.actual_logger_calls) == 1
        actual = self.actual_logger_calls[0]

        assert actual['partner_id'] == self.partner.id
        assert actual['vendor_id'] is None
        assert actual['original_price'] == 10015
        assert actual['original_currency_id'] == self.USD.id
        assert actual['national_price'] == 20099
        assert actual['national_currency_id'] == self.RUR.id
        assert len(actual['forward_segments']) == 2
        assert len(actual['backward_segments']) == 1
        assert actual['only_planes'] is True
        assert actual['with_baggage'] is False
        assert actual['forward_count_transfers'] == 1
        assert actual['backward_count_transfers'] == 0
        assert actual['is_charter'] is True

        assert actual['forward_duration'] == 180 * 60
        assert actual['backward_duration'] == 240 * 60

    def test_train_segment(self):
        query = create_query(
            when='2018-01-01'
        )

        train_station = create_station(
            t_type=TransportType.TRAIN_ID
        )
        v = create_variant(
            query=query,
            partner=self.partner,
            forward_flights=[
                create_flight(station_from=self._station_1,
                              station_to=train_station)
            ]
        )

        self.variants_logger._log(
            query=query,
            partner=self.query_partner,
            partner_variants=[v]
        )

        assert len(self.actual_logger_calls) == 1
        actual = self.actual_logger_calls[0]

        assert actual['only_planes'] is False

    def test_with_baggage(self):
        query = create_query(
            when='2018-01-01'
        )

        train_station = create_station(
            t_type=TransportType.TRAIN_ID
        )
        with_baggage_variant = create_variant(
            query=query,
            partner=self.partner,
            forward_flights=[
                create_flight(
                    station_from=self._station_1,
                    station_to=train_station,
                    baggage=Baggage(
                        SourceInt.create(2, 'db'),
                        SourceInt.create(10, 'db'),
                        included=SourceInt.create(1, 'db')
                    )
                )
            ]
        )

        without_baggage_variant = create_variant(
            query=query,
            partner=self.partner,
            forward_flights=[
                create_flight(
                    station_from=self._station_1,
                    station_to=train_station,
                    baggage=Baggage.from_db(included=False)
                )
            ]
        )

        self.variants_logger._log(
            query=query,
            partner=self.query_partner,
            partner_variants=[with_baggage_variant, without_baggage_variant]
        )

        assert len(self.actual_logger_calls) == 2
        assert self.actual_logger_calls[0]['with_baggage'] is True
        assert self.actual_logger_calls[0]['forward_segments'][0]['baggage'] == '1d2d10d'
        assert self.actual_logger_calls[1]['with_baggage'] is False
        assert self.actual_logger_calls[1]['forward_segments'][0]['baggage'] == '0dNN'

    def test_segment_data(self):
        query = create_query(
            when='2018-01-01'
        )

        company = create_company(t_type_id=TransportType.PLANE_ID, iata='zzz')
        avia_company = create_aviacompany(rasp_company=company)

        flight = create_flight(
            station_from=self._station_1,
            station_to=self._station_2,
            local_departure=datetime(2017, 1, 1, 12),
            delta=60,
            baggage=Baggage(
                SourceInt.create(2, 'db'),
                SourceInt.create(10, 'db'),
                included=SourceInt.create(1, 'db')
            ),
            company=company,
            avia_company=avia_company,
            tz_from=pytz.timezone('Asia/Vladivostok'),
            tz_to=pytz.timezone('Asia/Vladivostok'),
            fare_code='fare_code',
            fare_family='fare_family',
        )

        v = create_variant(
            query=query,
            partner=self.partner,
            forward_flights=[
                flight
            ]
        )

        self.variants_logger._log(
            query=query,
            partner=self.query_partner,
            partner_variants=[v]
        )

        assert len(self.actual_logger_calls) == 1
        actual = self.actual_logger_calls[0]
        s = actual['forward_segments'][0]

        offset = timedelta(hours=10)
        assert s['route'] == flight.number
        assert s['company_id'] == company.id
        assert s['airline_id'] == avia_company.rasp_company_id
        assert s['departure_station_id'] == self._station_1.id
        assert s['departure_station_transport_type_id'] == TransportType.PLANE_ID
        assert s['arrival_station_id'] == self._station_2.id
        assert s['arrival_station_transport_type_id'] == TransportType.PLANE_ID
        assert datetime.utcfromtimestamp(s['departure_time']) + offset == datetime(2017, 1, 1, 12)
        assert datetime.utcfromtimestamp(s['arrival_time']) + offset == datetime(2017, 1, 1, 13)
        assert s['departure_offset'] == offset.total_seconds()
        assert s['arrival_offset'] == offset.total_seconds()
        assert s['baggage'] == '1d2d10d'
        assert s['fare_code'] == 'fare_code'
        assert s['fare_family'] == 'fare_family'

    def test_selfconnect_variant(self):
        query = create_query(
            when='2018-01-01',
            return_date='2019-01-01',
            passengers='3_4_5'
        )
        v = create_variant(
            query=query,
            partner=self.partner,
            forward_flights=[
                create_flight(station_from=self._station_1, station_to=self._station_2),
                create_flight(station_from=self._station_2, station_to=self._station_3, selfconnect=True)
            ]
        )

        self.variants_logger._log(
            query=query,
            partner=self.query_partner,
            partner_variants=[v]
        )
        assert len(self.actual_logger_calls) == 1

        actual = self.actual_logger_calls[0]
        self.assertTrue(actual['selfconnect'])
        self.assertFalse(actual['forward_segments'][0]['selfconnect'])
        self.assertTrue(actual['forward_segments'][1]['selfconnect'])
