# coding=utf-8
from logging import Logger
from typing import cast
from mock import Mock

from travel.avia.library.python.common.models.schedule import Company
from travel.avia.library.python.common.models.transport import TransportType
from travel.avia.library.python.iata_correction import IATACorrector
from travel.avia.library.python.tester.factories import create_company
from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.admin.lib.airline_popular_score_updater import AirlinePopularScoreUpdater
from travel.avia.admin.lib.flight_frequency_collector import FlightFrequencyCollector
from travel.avia.admin.lib.yt_helpers import YtTableProvider


class TestAirlinePopularScoreUpdater(TestCase):
    def setUp(self):
        self._fake_flight_frequency_collector = Mock()
        self._fake_yt_table_provider = Mock()
        self._fake_iata_corrector = Mock()
        self._logger = Mock()

        self._updater = AirlinePopularScoreUpdater(
            flight_frequency_collector=cast(FlightFrequencyCollector, self._fake_flight_frequency_collector),
            yt_table_provider=cast(YtTableProvider, self._fake_yt_table_provider),
            iata_corrector=cast(IATACorrector, self._fake_iata_corrector),
            logger=cast(Logger, self._logger)
        )

    def _check_score(self, company, ru=0, ua=0, kz=0, com=0, tr=0, total=0):
        # type: (Company, int, int, int, int, int, int) -> None

        company = Company.objects.get(id=company.id)
        assert company.popular_score_for_ru == ru
        assert company.popular_score_for_ua == ua
        assert company.popular_score_for_kz == kz
        assert company.popular_score_for_com == com
        assert company.popular_score_for_tr == tr
        assert company.popular_score == total

    def test_with_russia_company(self):
        russian_company = create_company(
            t_type_id=TransportType.PLANE_ID,
        )

        self._fake_flight_frequency_collector.collect_to_list = Mock(
            return_value=[
                {
                    'number': 'кодкомпании 1234',
                    'ru': 100,
                    'ua': 200
                },
                {
                    'number': 'кодкомпании 5678',
                    'ru': 10,
                    'ua': 20
                }
            ]
        )
        self._fake_iata_corrector.flight_numbers_to_carriers = Mock(
            return_value={
                u'кодкомпании 1234': russian_company.id,
                u'кодкомпании 5678': russian_company.id,
            }
        )

        russian_company = Company.objects.get(id=russian_company.id)

        self._updater.update(30)

        self._check_score(
            company=russian_company,
            ru=110,
            ua=220,
            total=330
        )

    def test_with_english_company(self):
        russian_company = create_company(
            t_type_id=TransportType.PLANE_ID,
        )
        self._fake_flight_frequency_collector.collect_to_list = Mock(
            return_value=[
                {
                    'number': 'SU 1234',
                    'ru': 100,
                    'ua': 200
                },
                {
                    'number': 'SU 5678',
                    'ru': 10,
                    'ua': 20
                }
            ]
        )
        self._fake_iata_corrector.flight_numbers_to_carriers = Mock(
            return_value={
                u'SU 1234': russian_company.id,
                u'SU 5678': russian_company.id,
            }
        )

        self._updater.update(30)

        self._check_score(
            company=russian_company,
            ru=110,
            ua=220,
            total=330
        )

    def test_with_two_company(self):
        company = create_company(
            t_type_id=TransportType.PLANE_ID,
            slug='some'

        )
        another_company = create_company(
            t_type_id=TransportType.PLANE_ID,
            slug='another'

        )
        self._fake_flight_frequency_collector.collect_to_list = Mock(
            return_value=[
                {
                    'number': 'company 1234',
                    'ru': 100,
                    'ua': 200
                },
                {
                    'number': 'another_company 5678',
                    'ru': 10,
                    'ua': 20
                }
            ]
        )
        self._fake_iata_corrector.flight_numbers_to_carriers = Mock(
            return_value={
                u'company 1234': company.id,
                u'another_company 5678': another_company.id,
            }
        )

        self._updater.update(30)

        self._check_score(
            company=company,
            ru=100,
            ua=200,
            total=300
        )
        self._check_score(
            company=another_company,
            ru=10,
            ua=20,
            total=30
        )

    def test_unknown_company(self):
        self._fake_flight_frequency_collector.collect_to_list = Mock(
            return_value=[
                {
                    'number': 'unknown 1234',
                    'ru': 100,
                    'ua': 200
                }
            ]
        )
        self._fake_iata_corrector.flight_numbers_to_carriers = Mock(
            return_value={},
        )

        self._updater.update(30)

    def test_unknown_national_version(self):
        company = create_company(
            t_type_id=TransportType.PLANE_ID
        )
        self._fake_flight_frequency_collector.collect_to_list = Mock(
            return_value=[
                {
                    'number': 'unknown 1234',
                    'zzz': 100
                }
            ]
        )
        self._fake_iata_corrector.flight_numbers_to_carriers = Mock(
            return_value={
                u'unknown 1234': company.id,
            }
        )

        self._updater.update(30)

        self._check_score(company)
