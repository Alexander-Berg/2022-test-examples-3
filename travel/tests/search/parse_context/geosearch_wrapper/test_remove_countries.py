# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from common.tester.factories import create_country, create_settlement
from common.tester.testcase import TestCase
from geosearch.views.pointlist import PointList
from travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_wrapper import remove_countries


class TestRemoveCountries(TestCase):
    def test_main_point_is_country(self):
        country = create_country()
        settlement = create_settlement()
        point_list = PointList(country, [settlement], exact_variant=True)

        new_point_list = remove_countries(point_list)

        assert new_point_list.point is None
        assert not new_point_list.exact_variant

    def test_country_in_variants(self):
        country = create_country()
        settlement1 = create_settlement()
        settlement2 = create_settlement()
        point_list = PointList(settlement1, [settlement2, country], exact_variant=True)

        new_point_list = remove_countries(point_list)

        assert new_point_list.point == settlement1
        assert new_point_list.variants == [settlement2]
        assert new_point_list.exact_variant

    def test_only_countries_in_point_list(self):
        country1 = create_country()
        country2 = create_country()
        country3 = create_country()
        point_list = PointList(country1, [country2, country3], exact_variant=True)

        new_point_list = remove_countries(point_list)

        assert new_point_list is None
