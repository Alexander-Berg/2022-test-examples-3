# -*- coding: utf-8 -*-
from __future__ import absolute_import, unicode_literals

from datetime import datetime
from logging import Logger

from marshmallow import ValidationError
from mock import Mock
from typing import cast

from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.backend.main.rest.country_restrictions.covid_info import CovidInfoView
from travel.avia.backend.repository.country import CountryRepository
from travel.avia.backend.repository.covid_info import CovidInfoRepository
from travel.avia.backend.repository.region import RegionRepository
from travel.avia.backend.repository.settlement import SettlementRepository
from travel.avia.backend.repository.station import StationRepository
from travel.avia.backend.repository.translations import TranslatedTitleRepository
from travel.avia.library.python.common.utils.date import MSK_TZ
from travel.avia.library.python.tester.factories import (
    create_translated_title,
    create_settlement,
    create_station,
    create_country,
    create_region,
    create_covid_info
)


class CovidInfoViewTest(TestCase):
    COUNTRY_TITLE = 'ru_страна'
    NON_COUNTRY_TITLE = 'ru_non_country'

    def setUp(self):
        self._environment = Mock()
        self._environment.now_aware = Mock(
            return_value=MSK_TZ.localize(datetime(2017, 9, 1))
        )
        self._translated_title_repository = TranslatedTitleRepository()
        self.country_repo = CountryRepository(self._translated_title_repository)
        self.region_repo = RegionRepository(self._translated_title_repository)
        self.settlement_repo = SettlementRepository(
            translated_title_repository=self._translated_title_repository,
            environment=self._environment,
        )
        self.station_repo = StationRepository(self._translated_title_repository)
        self.covid_info_repo = CovidInfoRepository()

        self.covid_info_view = CovidInfoView(
            country_repository=self.country_repo,
            region_repository=self.region_repo,
            settlement_repository=self.settlement_repo,
            station_repository=self.station_repo,
            covid_info_repository=self.covid_info_repo,
            logger=cast(Logger, Mock()),
        )

    def create_country(self, **kwargs):
        title = create_translated_title(ru_nominative=self.COUNTRY_TITLE)
        return create_country(new_L_title_id=title.id, **kwargs)

    def test_covid_info_view(self):
        c = self.create_country(
            id=100,
            code='XY',
            _geo_id=12345678,
        )
        create_region(id=200, title=self.NON_COUNTRY_TITLE, country=c)
        create_settlement(id=300, country=c)
        create_station(id=400, country=c)
        create_covid_info(quarantine_days=8, quarantine=True, visa=False, country=c)

        self.country_repo.pre_cache()
        self.region_repo.pre_cache()
        self.settlement_repo.pre_cache()
        self.station_repo.pre_cache()
        self.covid_info_repo.pre_cache()

        expected = {
            'avia': 'unknown',
            'country_id': 100L,
            'country_title_ru': self.COUNTRY_TITLE,
            'quarantine': 'yes',
            'quarantine_days': 8L,
            'tourism': 'unknown',
            'visa': 'no',
        }

        actual = self.covid_info_view._process({
            'point_key': 'l100',
        })
        assert actual == expected

        actual = self.covid_info_view._process({
            'point_key': 'r200',
        })
        assert actual == expected

        actual = self.covid_info_view._process({
            'point_key': 'c300',
        })
        assert actual == expected

        actual = self.covid_info_view._process({
            'point_key': 's400',
        })
        assert actual == expected

        # incorrect format shall raise ValidationError
        try:
            actual = self.covid_info_view._process({
                'point_key': 'c2_10035',
            })
            raise Exception('must throw exception')
        except ValidationError:
            pass
