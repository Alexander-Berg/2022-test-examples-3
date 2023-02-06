# coding=utf-8
from __future__ import unicode_literals
from datetime import datetime
from urlparse import urlparse, parse_qs

from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.ticket_daemon_api.jsonrpc.lib.flights import IATAFlight, Variant


class IATAFlightUrlTest(TestCase):
    def test_russian(self):
        url = IATAFlight.url(
            number='русский',
            departure='2017-09-01',
            national_version='ru'
        )

        assert url == (
            'https://mock-travel.yandex.ru/avia/'
            'flights/русский/?when=2017-09-01'
        )

    def test_english(self):
        url = IATAFlight.url(
            number='english',
            departure='2017-09-01',
            national_version='ru'
        )

        assert url == (
            'https://mock-travel.yandex.ru/avia/'
            'flights/english/?when=2017-09-01'
        )

    def test_english_without_departure(self):
        url = IATAFlight.url(
            number='english',
            departure=None,
            national_version='ru'
        )

        assert url == (
            'https://mock-travel.yandex.ru/avia/'
            'flights/english/'
        )

    def test_check_number_with_spaces(self):
        url = IATAFlight.url(
            number='for yOu-123',
            departure=None,
            national_version='ru'
        )

        assert url == (
            'https://mock-travel.yandex.ru/avia/'
            'flights/for-yOu-123/'
        )

    def test_kz(self):
        url = IATAFlight.url(
            number='SU 100',
            departure='2017-09-01',
            national_version='kz'
        )

        assert url == (
            'https://mock-avia.yandex.kz/'
            'flights/SU-100/?when=2017-09-01'
        )

    def test_params(self):
        url = IATAFlight.url(
            number='for yOu-123',
            departure='2018-01-02',
            national_version='ru',
            params={
                'a': 1,
                'b': 'c',
            },
        )

        parsed_url = urlparse(url)
        assert parsed_url.scheme == 'https'
        assert parsed_url.hostname == 'mock-travel.yandex.ru'
        assert parsed_url.path == '/avia/flights/for-yOu-123/'
        self.assertDictEqual(parse_qs(parsed_url.query), {
            'when': ['2018-01-02'],
            'a': ['1'],
            'b': ['c'],
        })

    def test_sirena(self):
        url = IATAFlight.url(
            number='АЯ 183'.encode('utf-8'),
            departure={
                'local': '2020-12-14T10:00:00',
                'offset': 540,
                'tzname': 'Asia/Chita'
            },
            national_version=u'ru',
            params={
                'when': '2020-12-14',
            }
        )

        assert url == (
            'https://mock-travel.yandex.ru/avia/'
            'flights/АЯ-183/?when=2020-12-14'
        )


class VariantMakeTagTest(TestCase):
    def test_russian(self):
        print type(Variant.make_tag(
            forward_tags=['рус1', 'eng1'],
            backward_tags=['рус2', 'eng2'],
            klass='economy',
            partner_code='dohop',
            with_baggage=True,
        ))


class IATAFlightMakeFlightTagTest(TestCase):
    def test_russian(self):
        self.assertEqual(
            IATAFlight.make_flight_tag(
                local_departure=datetime(2017, 9, 1, 10, 0),
                number='русский'
            ),
            '1709011000русский'
        )

    def test_english(self):
        self.assertEqual(
            IATAFlight.make_flight_tag(
                local_departure=datetime(2017, 9, 1, 10, 0),
                number='english'
            ),
            '1709011000english'
        )
