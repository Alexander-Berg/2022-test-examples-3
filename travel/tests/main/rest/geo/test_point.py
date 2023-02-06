# -*- coding: utf-8 -*-
from __future__ import absolute_import

import logging
from mock import Mock
from typing import cast

from travel.avia.backend.main.rest.geo.point import PointView, PointSchema
from travel.avia.backend.main.rest.helpers import NotFoundError
from travel.avia.backend.repository.country import CountryRepository, CountryModel
from travel.avia.backend.repository.region import RegionRepository, RegionModel
from travel.avia.backend.repository.settlement import SettlementRepository, SettlementModel
from travel.avia.backend.repository.station import StationRepository, StationModel
from travel.avia.backend.repository.translations import TranslatedTitleRepository
from travel.avia.library.python.common.models.transport import TransportType
from travel.avia.library.python.tester.testcase import TestCase


class PointViewTest(TestCase):
    def setUp(self):
        self._fake_translated_repository = cast(TranslatedTitleRepository, Mock())
        self._fake_translated_repository.get = Mock(
            return_value='TEST'
        )
        self._fake_country_repository = cast(CountryRepository, Mock())
        self._fake_region_repository = cast(RegionRepository, Mock())
        self._fake_settlement_repository = cast(SettlementRepository, Mock())
        self._fake_station_repository = cast(StationRepository, Mock())
        self._view = PointView(
            form=PointSchema(),
            country_repository=self._fake_country_repository,
            region_repository=self._fake_region_repository,
            settlement_repository=self._fake_settlement_repository,
            station_repository=self._fake_station_repository,
            logger=cast(logging.Logger, Mock()),
        )

        self._test_settlement = SettlementModel(
            translated_title_repository=self._fake_translated_repository,
            pk=77,
            title_id=1,
            iata='IATA',
            sirena='СИРЕНА',
            geo_id=777,
            country_id=7777,
            region_id=7,
            is_disputed_territory=False,
            majority_id=1,
            pytz=None,
            utcoffset=1,
            latitude=1,
            longitude=2,
        )
        self._fake_settlement_repository.get = Mock(
            return_value=self._test_settlement
        )
        self._expected_settlement = {
            'countryId': self._test_settlement.country_id,
            'phraseFrom': 'TEST',
            'urlTitle': 'TEST',
            'title': 'TEST',
            'phraseIn': 'TEST',
            'iata': self._test_settlement.iata,
            'sirena': self._test_settlement.sirena,
            'phraseTo': 'TEST',
            'id': self._test_settlement.pk,
            'geoId': self._test_settlement.geo_id,
            'latitude': self._test_settlement.latitude,
            'longitude': self._test_settlement.longitude,
        }

        self._test_station = StationModel(
            translated_title_repository=self._fake_translated_repository,
            pk=33,
            title_id=1,
            popular_title_id=1,
            iata='IATA_S',
            sirena='SIRENA_S',
            settlement_id=77,
            country_id=7777,
            region_id=7,
            longitude=2,
            latitude=1,
            time_zone=None,
            time_zone_utc_offset=None,
            station_type_id=None,
            transport_type=TransportType.PLANE_ID,
        )
        self._fake_station_repository.get = Mock(
            return_value=self._test_station
        )
        self._expected_station = {
            'title': 'TEST',
            'popularTitle': 'TEST',
            'phraseFrom': 'TEST',
            'urlTitle': 'TEST',
            'phraseIn': 'TEST',
            'iataCode': 'IATA_S',
            'sirenaCode': 'SIRENA_S',
            'phraseTo': 'TEST',
            'id': self._test_station.pk,
            'transportType': TransportType.PLANE_ID,
            'regionId': self._test_station.region_id,
            'countryId': self._test_station.country_id,
            'latitude': self._test_station.latitude,
            'longitude': self._test_station.longitude,
        }

        self._test_country = CountryModel(
            translated_title_repository=self._fake_translated_repository,
            pk=1,
            title_id=1,
            geo_id=1,
            code='TT',
        )
        self._fake_country_repository.get = Mock(
            return_value=self._test_country
        )
        self._expected_country = {
            'code': self._test_country.code,
            'geoId': self._test_country.geo_id,
            'id': self._test_country.pk,
            'title': 'TEST',
        }

        self._test_region = RegionModel(
            translated_title_repository=self._fake_translated_repository,
            pk=2,
            title_id=2,
            geo_id=2,
            country_id=1,
        )
        self._fake_region_repository.get = Mock(
            return_value=self._test_region
        )
        self._expected_region = {
            'geoId': self._test_region.geo_id,
            'id': self._test_region.pk,
            'title': 'TEST',
        }

    def test_settlement_by_key(self):
        test_key, test_lang = 'c77', 'en'

        expected = {
            'settlement': self._expected_settlement,
            'station': None,
            'region': self._expected_region,
            'country': self._expected_country,
        }

        actual = self._view._process({
            'key': test_key,
            'lang': test_lang,
        })
        assert actual == expected

    def test_settlement_by_geo_id(self):
        test_geo_id, test_lang = 777, 'en'

        self._fake_settlement_repository.get_by_geo_id = Mock(
            return_value=self._test_settlement
        )

        expected = {
            'settlement': self._expected_settlement,
            'station': None,
            'region': self._expected_region,
            'country': self._expected_country,
        }

        actual = self._view._process({
            'geo_id': test_geo_id,
            'lang': test_lang,
        })
        assert actual == expected

    def test_unknown_settlement(self):
        test_key, test_lang = 'c223232323', 'ru'

        data = {
            'key': test_key,
            'lang': test_lang,
        }

        self._fake_settlement_repository.get = Mock(
            return_value=None
        )

        self.assertRaises(NotFoundError, self._view._process, data)

    def test_station(self):
        test_key, test_lang = 's33', 'en'

        expected = {
            'settlement': self._expected_settlement,
            'station': self._expected_station,
            'region': self._expected_region,
            'country': self._expected_country,
        }

        actual = self._view._process({
            'key': test_key,
            'lang': test_lang,
        })

        assert expected == actual

    def test_wrong_id_type(self):
        test_key, test_lang = 's0o0o', 'ru'

        data = {
            'key': test_key,
            'lang': test_lang,
        }

        assert self._view._unsafe_process(data).status_code == 400

    def test_unknown_station(self):
        test_key, test_lang = 's93030', 'ru'

        data = {
            'key': test_key,
            'lang': test_lang,
        }

        self._fake_station_repository.get = Mock(
            return_value=None
        )

        self.assertRaises(NotFoundError, self._view._process, data)

    def test_wrong_type_point_key(self):
        test_key, test_lang = 'l225', 'ru'

        actual = self._view._unsafe_process({
            'key': test_key,
            'lang': test_lang,
        })

        assert actual.status_code == 400

    def test_empty_key(self):
        actual = self._view._unsafe_process({
            'lang': 'ru',
        })

        assert actual.status_code == 400

    def test_both_key_and_geo_id(self):
        actual = self._view._unsafe_process({
            'lang': 'ru',
            'geo_id': 228,
            'key': 'c1337'
        })

        assert actual.status_code == 400

    def test_unknown_geo_id(self):
        test_geo_id, test_lang = 1488, 'ru'

        data = {
            'geo_id': test_geo_id,
            'lang': test_lang,
        }

        self._fake_settlement_repository.get_by_geo_id = Mock(
            return_value=None
        )

        self.assertRaises(NotFoundError, self._view._process, data)

    def test_region_no_station(self):
        # no station
        self._fake_station_repository.get = Mock(
            return_value=None
        )

        assert self._view._process({'key': 'c1', 'lang': 'en'}) == {
            'settlement': self._expected_settlement,
            'station': None,
            'region': self._expected_region,
            'country': self._expected_country,
        }

        # no settlement.region_id
        self._test_settlement.region_id = None

        assert self._view._process({'key': 'c1', 'lang': 'en'}) == {
            'settlement': self._expected_settlement,
            'station': None,
            'region': None,
            'country': self._expected_country,
        }

    def test_region_no_settlement(self):
        self._fake_settlement_repository.get = Mock(
            return_value=None
        )

        assert self._view._process({'key': 's1', 'lang': 'en'}) == {
            'settlement': None,
            'station': self._expected_station,
            'region': self._expected_region,
            'country': self._expected_country,
        }

        # no station.region_id
        self._test_station.region_id = None
        self._expected_station['regionId'] = None

        assert self._view._process({'key': 's1', 'lang': 'en'}) == {
            'settlement': None,
            'station': self._expected_station,
            'region': None,
            'country': self._expected_country,
        }

    def test_region(self):
        assert self._view._process({'key': 's1', 'lang': 'en'}) == {
            'settlement': self._expected_settlement,
            'station': self._expected_station,
            'region': self._expected_region,
            'country': self._expected_country,
        }

        # no settlement.region_id
        self._test_settlement.region_id = None

        assert self._view._process({'key': 's1', 'lang': 'en'}) == {
            'settlement': self._expected_settlement,
            'station': self._expected_station,
            'region': self._expected_region,
            'country': self._expected_country,
        }

        # no station.region_id
        self._test_station.region_id = None
        self._test_settlement.region_id = 1
        self._expected_station['regionId'] = None

        assert self._view._process({'key': 's1', 'lang': 'en'}) == {
            'settlement': self._expected_settlement,
            'station': self._expected_station,
            'region': self._expected_region,
            'country': self._expected_country,
        }

        # no station.region_id, no station.region_id
        self._test_station.region_id = None
        self._test_settlement.region_id = None

        assert self._view._process({'key': 's1', 'lang': 'en'}) == {
            'settlement': self._expected_settlement,
            'station': self._expected_station,
            'region': None,
            'country': self._expected_country,
        }

    def test_country_no_station(self):
        # no station
        self._fake_station_repository.get = Mock(
            return_value=None
        )

        assert self._view._process({'key': 'c1', 'lang': 'en'}) == {
            'settlement': self._expected_settlement,
            'station': None,
            'region': self._expected_region,
            'country': self._expected_country,
        }

        # no settlement.region_id
        self._test_settlement.country_id = None
        self._expected_settlement['countryId'] = None

        assert self._view._process({'key': 'c1', 'lang': 'en'}) == {
            'settlement': self._expected_settlement,
            'station': None,
            'region': self._expected_region,
            'country': None,
        }

    def test_country_no_settlement(self):
        self._fake_settlement_repository.get = Mock(
            return_value=None
        )

        assert self._view._process({'key': 's1', 'lang': 'en'}) == {
            'settlement': None,
            'station': self._expected_station,
            'region': self._expected_region,
            'country': self._expected_country,
        }

        # no station.region_id
        self._test_station.country_id = None
        self._expected_station['countryId'] = None

        assert self._view._process({'key': 's1', 'lang': 'en'}) == {
            'settlement': None,
            'station': self._expected_station,
            'region': self._expected_region,
            'country': None,
        }

    def test_country(self):
        assert self._view._process({'key': 's1', 'lang': 'en'}) == {
            'settlement': self._expected_settlement,
            'station': self._expected_station,
            'region': self._expected_region,
            'country': self._expected_country,
        }

        # no settlement.region_id
        self._test_settlement.country_id = None
        self._expected_settlement['countryId'] = None

        assert self._view._process({'key': 's1', 'lang': 'en'}) == {
            'settlement': self._expected_settlement,
            'station': self._expected_station,
            'region': self._expected_region,
            'country': self._expected_country,
        }

        # no station.region_id
        self._test_station.country_id = None
        self._test_settlement.country_id = 1
        self._expected_station['countryId'] = None
        self._expected_settlement['countryId'] = self._test_settlement.country_id

        assert self._view._process({'key': 's1', 'lang': 'en'}) == {
            'settlement': self._expected_settlement,
            'station': self._expected_station,
            'region': self._expected_region,
            'country': self._expected_country,
        }

        # no station.region_id, no station.region_id
        self._test_station.country_id = None
        self._test_settlement.country_id = None
        self._expected_station['countryId'] = None
        self._expected_settlement['countryId'] = None

        assert self._view._process({'key': 's1', 'lang': 'en'}) == {
            'settlement': self._expected_settlement,
            'station': self._expected_station,
            'region': self._expected_region,
            'country': None,
        }
