# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from common.models.geo import CityMajority
from common.tester.factories import create_country, create_settlement
from common.tester.testcase import TestCase
from travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_serialization import point_country_json


class TestPointCountryJson(TestCase):
    def test_empty_country(self):
        assert point_country_json(None, language='ru') == {}

    def test_ukraina(self):
        country = create_country(code='UA', title_uk='Россiя')
        create_settlement(majority=CityMajority.CAPITAL_ID, time_zone='Europe/Kiev', country=country)

        assert point_country_json(country, language='uk') == {
            'code': 'UA',
            'title': 'Россiя',
            'railwayTimezone': 'Europe/Kiev'
        }
