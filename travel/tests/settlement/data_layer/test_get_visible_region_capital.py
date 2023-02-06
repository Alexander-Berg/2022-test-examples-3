# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from common.models.geo import CityMajority
from common.tester.factories import create_settlement, create_region
from common.tester.testcase import TestCase
from travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement import get_visible_region_capital


class TestGetVisibleRegionCapital(TestCase):
    def test_no_region(self):
        """
        Населенный пункт не привязан ни к какому региону.
        """
        settlement = create_settlement(majority=CityMajority.COMMON_CITY_ID)

        assert get_visible_region_capital(settlement) is None

    def test_region_without_capital(self):
        """
        Населенный пункт привязан к региону, у которого не определна столица.
        """
        region = create_region()
        settlement = create_settlement(region=region, majority=CityMajority.COMMON_CITY_ID)

        assert get_visible_region_capital(settlement) is None

    def test_region_hidden_capital(self):
        """
        Населенный пункт привязан к региону, у которого столица скрытая.
        """
        region = create_region()
        create_settlement(region=region, majority=CityMajority.REGION_CAPITAL_ID, hidden=True)
        settlement = create_settlement(region=region, majority=CityMajority.COMMON_CITY_ID)

        assert get_visible_region_capital(settlement) is None

    def test_region_visible_capital(self):
        """
        Населенный пункт привязан к региону, у которого столица видимая.
        """
        region = create_region()
        region_capital = create_settlement(region=region, majority=CityMajority.REGION_CAPITAL_ID)
        settlement = create_settlement(region=region, majority=CityMajority.COMMON_CITY_ID)

        assert get_visible_region_capital(settlement) == region_capital
