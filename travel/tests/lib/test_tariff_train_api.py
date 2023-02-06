# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime
from urlparse import urlparse, parse_qs

import pytz
from django.test import override_settings
from hamcrest import assert_that, has_properties, contains, all_of, starts_with, contains_string, has_entries
from mock import Mock, call, mock

from common.models.currency import Price
from common.tester.factories import create_station
from common.tester.testcase import TestCase
from travel.rasp.touch.touch.core.lib.train_number import TrainNumberParser, TrainNumberReverser
from travel.rasp.touch.touch.core.lib.train_tariff_api_client import TrainTariffApiClient, TrainTariffResult, make_full_train_order_url
from travel.rasp.touch.touch.core.lib.train_tariff_key_map_fabric import TrainTariffKeyMapFabric


@override_settings(TRAINS_FRONT_URL='https://trains.example.com')
class TestTrainTariffApiClient(TestCase):
    def setUp(self):
        self.fake_response = Mock()
        self.fake_transport = Mock()
        self.fake_transport.get = Mock(return_value=self.fake_response)
        self.fake_logger = Mock()
        self.client = TrainTariffApiClient(
            host='cool_host',
            transport=self.fake_transport,
            train_tariff_key_map_fabric=TrainTariffKeyMapFabric(
                train_number_parser=TrainNumberParser(),
                train_number_reverser=TrainNumberReverser()
            ),
            logger=self.fake_logger
        )
        self.from_station = create_station()
        self.to_station = create_station()
        self.rates = {'RUR': 1}
        self.departure_dt = pytz.UTC.localize(datetime(2018, 9, 1, 14, 45))

    def test_querying(self):
        self.fake_response.json = Mock(return_value={
            'segments': [],
            'querying': True
        })

        result = self.client.find_tariffs_for(
            from_station=self.from_station,
            to_station=self.to_station,
            departure_dt=self.departure_dt,
            number='80X',
            national_version='ru',
            rates=self.rates,
            init_query=True
        )

        assert self.fake_logger.exception.call_count == 0
        assert result == TrainTariffResult(
            records=[],
            train_order_url_owner='unknown',
            is_electronic_ticket=False,
            querying=True
        )

    def test_can_not_find(self):
        self.fake_response.json = Mock(return_value={
            'segments': [],
            'querying': False
        })

        result = self.client.find_tariffs_for(
            from_station=self.from_station,
            to_station=self.to_station,
            departure_dt=self.departure_dt,
            number='80X',
            national_version='ru',
            rates=self.rates,
            init_query=True
        )

        assert self.fake_logger.exception.call_count == 0
        assert result == TrainTariffResult(
            records=[],
            train_order_url_owner='unknown',
            is_electronic_ticket=False,
            querying=False
        )

    def _make_tariff(self, price_value, seats=10, lower_seats=2, upper_seats=8,
                     several_prices=True, train_url_owner='train'):
        return {
            "trainOrderUrl": "/cool/order/url/{}".format(price_value),
            "trainOrderUrlOwner": train_url_owner,
            "price": {
                "currency": "RUB",
                "value": price_value
            },
            "seats": seats,
            "lowerSeats": lower_seats,
            "upperSeats": upper_seats,
            "severalPrices": several_prices
        }

    def _make_segment(self, key, from_station_id, to_station_id, tariffs_by_code, is_electronic_ticket=True):
        return {
            "stationFrom": {
                "id": from_station_id,
            },
            "stationTo": {
                "id": to_station_id,
            },
            "tariffs": {
                "classes": tariffs_by_code,
                "electronicTicket": is_electronic_ticket
            },
            "key": key
        }

    def _make_price(self, value):
        p = Price(value, 'RUR')
        p.base_value = value

        return p

    def test_querying_and_train_is_not_fit(self):
        additional_segment = self._make_segment(
            'another_thread_uid',
            self.from_station.id,
            self.to_station.id,
            {
                'common': self._make_tariff(1000)
            }
        )
        additional_segment['thread'] = None

        self.fake_response.json = Mock(return_value={
            'segments': [
                additional_segment,
                self._make_segment(
                    "another key",
                    self.from_station.id,
                    self.to_station.id,
                    {
                        'common': self._make_tariff(1000)
                    }
                ),
                self._make_segment(
                    "train 80X 201809091_1445",
                    self.from_station.id + self.to_station.id,
                    self.to_station.id,
                    {
                        'common': self._make_tariff(2000)
                    }
                ),
                self._make_segment(
                    "train 80X 201809091_1445",
                    self.from_station.id,
                    self.from_station.id + self.to_station.id,
                    {
                        'common': self._make_tariff(3000)
                    }
                )
            ],
            'querying': True
        })

        result = self.client.find_tariffs_for(
            from_station=self.from_station,
            to_station=self.to_station,
            departure_dt=self.departure_dt,
            number='8X0X',
            national_version='ru',
            rates=self.rates,
            init_query=True
        )
        assert self.fake_logger.info.call_args_list == [
            call(
                mock.ANY,
                self.from_station.id, self.to_station.id, '2018-09-01', u'ru'
            ),
            call(
                 mock.ANY,
                 4, True, self.from_station.id, self.to_station.id, '2018-09-01', u'ru'
            ),
            call(
                mock.ANY,
                'train 8X0X 20180901_12, train 8X0X 20180901_14, train 8X0X 20180901_1445',
                self.from_station.id, self.to_station.id
            ),
        ]
        assert self.fake_logger.exception.call_count == 0
        assert result == TrainTariffResult(
            records=[],
            train_order_url_owner='unknown',
            is_electronic_ticket=False,
            querying=True
        )

    def test_querying_and_train_is_fit(self):
        self.fake_response.json = Mock(return_value={
            'segments': [
                self._make_segment(
                    'train 8X0X 20180901_12',
                    self.from_station.id,
                    self.to_station.id,
                    {
                        'common': self._make_tariff(1000)
                    }
                )
            ],
            'querying': True
        })

        result = self.client.find_tariffs_for(
            from_station=self.from_station,
            to_station=self.to_station,
            departure_dt=self.departure_dt,
            number='8X0X',
            national_version='ru',
            rates=self.rates,
            init_query=True
        )

        assert self.fake_logger.info.call_args_list == [
            call(
                mock.ANY,
                self.from_station.id, self.to_station.id, '2018-09-01', u'ru'),
            call(
                 mock.ANY,
                 1, True, self.from_station.id, self.to_station.id, '2018-09-01', u'ru'
            ),
            call(
                mock.ANY,
                'train 8X0X 20180901_12, train 8X0X 20180901_14, train 8X0X 20180901_1445',
                self.from_station.id, self.to_station.id
            ),
        ]
        assert self.fake_logger.exception.call_count == 0
        assert_that(result, has_properties(
            records=contains(
                has_properties(
                    coach_type='common',
                    coach_title='общий',
                    train_order_url=all_of(
                        starts_with('https://trains.example.com/cool/order/url/1000?'),
                        contains_string('utm_medium=thread_button'),
                        contains_string('utm_source=rasp'),
                    ),
                    train_order_url_owner='train',
                    price=self._make_price(1000),
                    seats=10,
                    lower_seats=2,
                    upper_seats=8,
                    several_prices=True
                ),
            ),
            train_order_url_owner='train',
            is_electronic_ticket=True,
            querying=False
        ))

    def test_is_not_querying_and_train_is_fit(self):
        self.fake_response.json = Mock(return_value={
            'segments': [
                self._make_segment(
                    'train 8X0X 20180901_1445',
                    self.from_station.id,
                    self.to_station.id,
                    {
                        'common': self._make_tariff(1000)
                    }
                )
            ],
            'querying': False
        })

        result = self.client.find_tariffs_for(
            from_station=self.from_station,
            to_station=self.to_station,
            departure_dt=self.departure_dt,
            number='8X0X',
            national_version='ru',
            rates=self.rates,
            init_query=True
        )

        assert self.fake_logger.info.call_args_list == [
            call(
                mock.ANY,
                self.from_station.id, self.to_station.id, '2018-09-01', u'ru'),
            call(
                 mock.ANY,
                 1, False, self.from_station.id, self.to_station.id, '2018-09-01', u'ru'
            ),
            call(
                mock.ANY,
                'train 8X0X 20180901_12, train 8X0X 20180901_14, train 8X0X 20180901_1445',
                self.from_station.id, self.to_station.id
            ),
        ]
        assert self.fake_logger.exception.call_count == 0
        assert_that(result, has_properties(
            records=contains(
                has_properties(
                    coach_type='common',
                    coach_title='общий',
                    train_order_url=all_of(
                        starts_with('https://trains.example.com/cool/order/url/1000?'),
                        contains_string('utm_medium=thread_button'),
                        contains_string('utm_source=rasp'),
                    ),
                    train_order_url_owner="train",
                    price=self._make_price(1000),
                    seats=10,
                    lower_seats=2,
                    upper_seats=8,
                    several_prices=True
                ),
            ),
            train_order_url_owner='train',
            is_electronic_ticket=True,
            querying=False
        ))

    def test_querying_and_two_train_are_fit(self):
        self.fake_response.json = Mock(return_value={
            'segments': [
                self._make_segment(
                    'train 8X0X 20180901_12',
                    self.from_station.id,
                    self.to_station.id,
                    {
                        'common': self._make_tariff(1000)
                    }
                ),
                self._make_segment(
                    'train 8X0X 20180901_14',
                    self.from_station.id,
                    self.to_station.id,
                    {
                        'common': self._make_tariff(2000)
                    }
                ),
            ],
            'querying': False
        })

        result = self.client.find_tariffs_for(
            from_station=self.from_station,
            to_station=self.to_station,
            departure_dt=self.departure_dt,
            number='8X0X',
            national_version='ru',
            rates=self.rates,
            init_query=True
        )

        assert self.fake_logger.info.call_args_list == [
            call(
                mock.ANY,
                self.from_station.id, self.to_station.id, '2018-09-01', u'ru'
            ),
            call(
                 mock.ANY,
                 2, False, self.from_station.id, self.to_station.id, '2018-09-01', u'ru'
            ),
            call(
                mock.ANY,
                'train 8X0X 20180901_12, train 8X0X 20180901_14, train 8X0X 20180901_1445',
                self.from_station.id, self.to_station.id
            ),
        ]
        assert self.fake_logger.exception.call_count == 0
        assert_that(result, has_properties(
            records=contains(
                has_properties(
                    coach_type='common',
                    coach_title='общий',
                    train_order_url=all_of(
                        starts_with('https://trains.example.com/cool/order/url/1000?'),
                        contains_string('utm_medium=thread_button'),
                        contains_string('utm_source=rasp'),
                    ),
                    train_order_url_owner='train',
                    price=self._make_price(1000),
                    seats=10,
                    lower_seats=2,
                    upper_seats=8,
                    several_prices=True,
                ),
            ),
            train_order_url_owner='train',
            is_electronic_ticket=True,
            querying=False,
        ))


@override_settings(TRAINS_FRONT_URL='https://trains.example.com')
class TestMakeFullTrainOrderUrl(TestCase):
    def test_full_url_without_parameters(self):
        url = make_full_train_order_url('http://bus.example.com/one/two/')

        parsed_url = urlparse(url)
        params = parse_qs(parsed_url.query)

        assert_that(parsed_url, has_properties(
            path='/one/two/',
            hostname='bus.example.com',
            scheme='http',
        ))
        assert_that(params, has_entries(
            utm_medium=['thread_button'],
            utm_source=['rasp'],
        ))

    def test_full_url_with_parameters(self):
        url = make_full_train_order_url('http://bus.example.com/one/two/?a=aaa&b=bb')

        parsed_url = urlparse(url)
        params = parse_qs(parsed_url.query)

        assert_that(parsed_url, has_properties(
            path='/one/two/',
            hostname='bus.example.com',
            scheme='http',
        ))
        assert_that(params, has_entries(
            utm_medium=['thread_button'],
            utm_source=['rasp'],
            a=['aaa'],
            b=['bb'],
        ))

    def test_short_url_without_parameters(self):
        url = make_full_train_order_url('/one/two/')

        parsed_url = urlparse(url)
        params = parse_qs(parsed_url.query)

        assert_that(parsed_url, has_properties(
            path='/one/two/',
            hostname='trains.example.com',
            scheme='https',
        ))
        assert_that(params, has_entries(
            utm_medium=['thread_button'],
            utm_source=['rasp'],
        ))

    def test_short_url_with_parameters(self):
        url = make_full_train_order_url('/one/two/?a=aaa&b=bb')

        parsed_url = urlparse(url)
        params = parse_qs(parsed_url.query)

        assert_that(parsed_url, has_properties(
            path='/one/two/',
            hostname='trains.example.com',
            scheme='https',
        ))
        assert_that(params, has_entries(
            utm_medium=['thread_button'],
            utm_source=['rasp'],
            a=['aaa'],
            b=['bb'],
        ))
