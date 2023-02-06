# coding=utf-8
import unittest
from datetime import datetime

from travel.avia.api_gateway.application.mapper.mail import MailMapper


class MailMapperDatetimeTestCase(unittest.TestCase):
    def test_get_datetime_to_utc(self):
        self.assertEqual(
            MailMapper.get_datetime(datetime(2019, 6, 1, 11, 22, 33), 'UTC').isoformat(), '2019-06-01T11:22:33+00:00'
        )

    def test_get_datetime_to_europe_moscow(self):
        self.assertEqual(
            MailMapper.get_datetime(datetime(2019, 6, 1, 11, 22, 33), 'Europe/Moscow').isoformat(),
            '2019-06-01T14:22:33+03:00',
        )

    def test_make_aware_datetime_to_utc(self):
        self.assertEqual(
            MailMapper.get_datetime(datetime(2019, 6, 1, 11, 22, 33), 'UTC').isoformat(), '2019-06-01T11:22:33+00:00'
        )

    def test_make_aware_datetime_to_europe_moscow(self):
        self.assertEqual(
            MailMapper.localize_datetime(datetime(2019, 6, 1, 11, 22, 33), 'Europe/Moscow').isoformat(),
            '2019-06-01T11:22:33+03:00',
        )


class MailMapperMappingTestCase(unittest.TestCase):
    def setUp(self):
        self.mapper = MailMapper(avia_frontend_url_ru='avia.yandex.ru/')

    def test_mapping_with_status_info(self):
        source = {
            'flight': {
                'airline_id': 9144,
                'number': 'DP 404',
                'departure_day': '2019-06-20',
                'departure_utc': '2019-06-20 01:20:00',
                'departure_timezone': 'Asia/Yekaterinburg',
                'arrival_utc': '2019-06-20 03:55:00',
                'arrival_timezone': 'Europe/Moscow',
                'airport_from_id': 9600370,
                'airport_to_id': 9600215,
            },
            'airline': {
                'title': 'Победа',
                'iata': 'DP',
                'sirena': 'ДР',
            },
            'airport_from': {
                'title': 'Кольцово123',
                'popularTitle': 'Кольцово123',
                'stationType': {
                    'prefix': 'а/п',
                },
                'iataCode': 'SVX',
                'sirenaCode': 'КЛЦ',
                'settlement': {
                    'country': {
                        'geoId': 225,
                    },
                    'geoId': 54,
                },
            },
            'airport_to': {
                'title': 'Внуково',
                'popularTitle': 'Внуково',
                'stationType': {
                    'prefix': 'а/п',
                },
                'iataCode': 'VKO',
                'sirenaCode': 'ВНК',
                'settlement': {
                    'country': {
                        'geoId': 225,
                    },
                    'geoId': 213,
                },
            },
            'flight_status': {
                'status': 'arrived',
                'departure': '2019-06-20 06:27:00',
                'arrival': '2019-06-20 06:41:00',
            },
        }
        actual = self.mapper.flight_by_departure_date(source)

        expected = {
            't_type': 'plane',
            't_type_id': 2,
            'title': None,
            'plain_title': None,
            'from_phrase': None,
            'from_name_gen': None,
            'terminal_name': None,
            'title_parts': [],
            'link': 'avia.yandex.ru/flights/DP-404/?when=2019-06-20&utm_source=yandex-mail',
            'mobile_link': 'avia.yandex.ru/flights/DP-404/?when=2019-06-20&utm_source=yandex-mail',
            'company_id': 9144,
            'number': 'DP 404',
            'scheduled_departure': '2019-06-20 06:20:00 +0500',
            'departure_tz': 'Asia/Yekaterinburg',
            'scheduled_arrival': '2019-06-20 06:55:00 +0300',
            'arrival_tz': 'Europe/Moscow',
            'from_id': 9600370,
            'to_id': 9600215,
            'company': 'Победа',
            'company_iata': 'DP',
            'company_sirena': 'ДР',
            'from_name': 'Кольцово123',
            'from_name_popular': 'Кольцово123',
            'from_prefix': 'а/п',
            'from_iata': 'SVX',
            'from_sirena': 'КЛЦ',
            'from_country_geoid': 225,
            'from_settlement_geoid': 54,
            'to_name': 'Внуково',
            'to_name_popular': 'Внуково',
            'to_prefix': 'а/п',
            'to_iata': 'VKO',
            'to_sirena': 'ВНК',
            'to_country_geoid': 225,
            'to_settlement_geoid': 213,
            'status': 'arrived',
            'status_title': 'Прилетел',
            'departure': '2019-06-20 06:27:00 +0500',
            'arrival': '2019-06-20 06:41:00 +0300',
        }
        self.assertEqual(expected, actual)

    def test_mapping_without_status_info(self):
        source = {
            'flight': {
                'airline_id': 9144,
                'number': 'DP 404',
                'departure_day': '2019-06-20',
                'departure_utc': '2019-06-20 01:20:00',
                'departure_timezone': 'Asia/Yekaterinburg',
                'arrival_utc': '2019-06-20 03:55:00',
                'arrival_timezone': 'Europe/Moscow',
                'airport_from_id': 9600370,
                'airport_to_id': 9600215,
            },
            'airline': {
                'title': 'Победа',
                'iata': 'DP',
                'sirena': 'ДР',
            },
            'airport_from': {
                'title': 'Кольцово123',
                'popularTitle': 'Кольцово123',
                'stationType': {
                    'prefix': 'а/п',
                },
                'iataCode': 'SVX',
                'sirenaCode': 'КЛЦ',
                'settlement': {
                    'country': {
                        'geoId': 225,
                    },
                    'geoId': 54,
                },
            },
            'airport_to': {
                'title': 'Внуково',
                'popularTitle': 'Внуково',
                'stationType': {
                    'prefix': 'а/п',
                },
                'iataCode': 'VKO',
                'sirenaCode': 'ВНК',
                'settlement': {
                    'country': {
                        'geoId': 225,
                    },
                    'geoId': 213,
                },
            },
            'flight_status': {
                'status': 'unknown',
                'departure': '',
                'arrival': '',
            },
        }
        actual = self.mapper.flight_by_departure_date(source)

        expected = {
            't_type': 'plane',
            't_type_id': 2,
            'title': None,
            'plain_title': None,
            'from_phrase': None,
            'from_name_gen': None,
            'terminal_name': None,
            'title_parts': [],
            'link': 'avia.yandex.ru/flights/DP-404/?when=2019-06-20&utm_source=yandex-mail',
            'mobile_link': 'avia.yandex.ru/flights/DP-404/?when=2019-06-20&utm_source=yandex-mail',
            'company_id': 9144,
            'number': 'DP 404',
            'scheduled_departure': '2019-06-20 06:20:00 +0500',
            'departure_tz': 'Asia/Yekaterinburg',
            'scheduled_arrival': '2019-06-20 06:55:00 +0300',
            'arrival_tz': 'Europe/Moscow',
            'from_id': 9600370,
            'to_id': 9600215,
            'company': 'Победа',
            'company_iata': 'DP',
            'company_sirena': 'ДР',
            'from_name': 'Кольцово123',
            'from_name_popular': 'Кольцово123',
            'from_prefix': 'а/п',
            'from_iata': 'SVX',
            'from_sirena': 'КЛЦ',
            'from_country_geoid': 225,
            'from_settlement_geoid': 54,
            'to_name': 'Внуково',
            'to_name_popular': 'Внуково',
            'to_prefix': 'а/п',
            'to_iata': 'VKO',
            'to_sirena': 'ВНК',
            'to_country_geoid': 225,
            'to_settlement_geoid': 213,
            'status': 'unknown',
            'status_title': 'Информация о статусе недоступна',
            'departure': '2019-06-20 06:20:00 +0500',
            'arrival': '2019-06-20 06:55:00 +0300',
        }
        self.assertEqual(expected, actual)

    def test_mapping_scheduled_utc_previous_day(self):
        source = {
            'flight': {
                'airline_id': 9144,
                'number': 'DP 404',
                'departure_day': '2019-06-20',
                'departure_utc': '2019-06-19 21:20:00',
                'departure_timezone': 'Asia/Yekaterinburg',
                'arrival_utc': '2019-06-19 23:55:00',
                'arrival_timezone': 'Europe/Moscow',
                'airport_from_id': 9600370,
                'airport_to_id': 9600215,
            },
            'airline': {
                'title': 'Победа',
                'iata': 'DP',
                'sirena': 'ДР',
            },
            'airport_from': {
                'title': 'Кольцово123',
                'popularTitle': 'Кольцово123',
                'stationType': {
                    'prefix': 'а/п',
                },
                'iataCode': 'SVX',
                'sirenaCode': 'КЛЦ',
                'settlement': {
                    'country': {
                        'geoId': 225,
                    },
                    'geoId': 54,
                },
            },
            'airport_to': {
                'title': 'Внуково',
                'popularTitle': 'Внуково',
                'stationType': {
                    'prefix': 'а/п',
                },
                'iataCode': 'VKO',
                'sirenaCode': 'ВНК',
                'settlement': {
                    'country': {
                        'geoId': 225,
                    },
                    'geoId': 213,
                },
            },
            'flight_status': {
                'status': 'arrived',
                'departure': '2019-06-20 06:27:00',
                'arrival': '2019-06-20 06:41:00',
            },
        }
        actual = self.mapper.flight_by_departure_date(source)

        expected = {
            't_type': 'plane',
            't_type_id': 2,
            'title': None,
            'plain_title': None,
            'from_phrase': None,
            'from_name_gen': None,
            'terminal_name': None,
            'title_parts': [],
            'link': 'avia.yandex.ru/flights/DP-404/?when=2019-06-20&utm_source=yandex-mail',
            'mobile_link': 'avia.yandex.ru/flights/DP-404/?when=2019-06-20&utm_source=yandex-mail',
            'company_id': 9144,
            'number': 'DP 404',
            'scheduled_departure': '2019-06-20 02:20:00 +0500',
            'departure_tz': 'Asia/Yekaterinburg',
            'scheduled_arrival': '2019-06-20 02:55:00 +0300',
            'arrival_tz': 'Europe/Moscow',
            'from_id': 9600370,
            'to_id': 9600215,
            'company': 'Победа',
            'company_iata': 'DP',
            'company_sirena': 'ДР',
            'from_name': 'Кольцово123',
            'from_name_popular': 'Кольцово123',
            'from_prefix': 'а/п',
            'from_iata': 'SVX',
            'from_sirena': 'КЛЦ',
            'from_country_geoid': 225,
            'from_settlement_geoid': 54,
            'to_name': 'Внуково',
            'to_name_popular': 'Внуково',
            'to_prefix': 'а/п',
            'to_iata': 'VKO',
            'to_sirena': 'ВНК',
            'to_country_geoid': 225,
            'to_settlement_geoid': 213,
            'status': 'arrived',
            'status_title': 'Прилетел',
            'departure': '2019-06-20 06:27:00 +0500',
            'arrival': '2019-06-20 06:41:00 +0300',
        }
        self.assertEqual(expected, actual)
