# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from hamcrest import assert_that, has_entries, has_entry

from common.tester.factories import create_settlement, create_station, create_country, create_region
from common.tester.testcase import TestCase
from travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_serialization import (
    result_without_errors_point_json
)


TIMEZONE = 'Asia/Yakutsk'


class TestResultWithoutErrorsPointJson(TestCase):
    """
    Тесты на JSON-сериализацию пункта отправления или прибытия из результата разбора поискового контекста,
    не содержащего ошибок.
    """
    def setUp(self):
        self.original_title = 'original title'
        self.parsed_title = 'parsed title'
        self.original_key = 'original key'
        self.language = 'uk'
        self.country = create_country(title_uk=self.parsed_title, code='UK')
        self.region = create_region(title_uk='Регiон', country=self.country)
        self.settlement = create_settlement(title_uk='ГородСоСтанцией', time_zone=TIMEZONE, region=self.region)

    def test_settlement(self):
        """
        Тип сериализуемого пункта - город.
        """
        settlement = create_settlement(title_uk=self.parsed_title, country=self.country, time_zone=TIMEZONE,
                                       region=self.region)

        assert_that(result_without_errors_point_json(settlement, self.language), has_entries({
            'key': settlement.point_key,
            'slug': settlement.slug,
            'title': self.parsed_title,
            'timezone': TIMEZONE,
            'country': has_entry('code', 'UK'),
            'region': has_entry('title', 'Регiон'),
            'settlement': has_entries('title', self.parsed_title),
        }))

    def test_station(self):
        """
        Тип сериализуемого пункта - станция.
        """
        station = create_station(title_uk=self.parsed_title, country=self.country, time_zone=TIMEZONE,
                                 region=self.region, settlement=self.settlement)

        assert_that(result_without_errors_point_json(station, self.language), has_entries({
            'key': station.point_key,
            'slug': station.slug,
            'title': self.parsed_title,
            'timezone': TIMEZONE,
            'country': has_entry('code', 'UK'),
            'region': has_entry('title', 'Регiон'),
            'settlement': has_entries('title', 'ГородСоСтанцией'),
        }))

    def test_country(self):
        assert_that(result_without_errors_point_json(self.country, self.language), has_entries({
            'key': self.country.point_key,
            'title': self.parsed_title,
            'timezone': None,
            'country': has_entry('code', 'UK')
        }))
