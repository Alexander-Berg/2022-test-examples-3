# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from common.tester.factories import create_settlement, create_region
from common.tester.testcase import TestCase
from travel.rasp.train_api.data_layer.settlement import get_visible_district_capital


class TestGetVisibleDistrictCapital(TestCase):
    def test_no_district(self):
        """
        Населенный пункт не привязан ни к какому району.
        """
        settlement = create_settlement()

        assert get_visible_district_capital(settlement) is None

    def test_district_without_capital(self):
        """
        Населенный пункт привязан к району, у которого не определна столица.
        """
        settlement = create_settlement(district=dict())

        assert get_visible_district_capital(settlement) is None

    def test_district_hidden_capital(self):
        """
        Населенный пункт привязан к району, у которого столица скрытая.
        """
        region = create_region()
        district_capital = create_settlement(region=region, hidden=True)
        settlement = create_settlement(district=dict(region=region, settlement=district_capital))

        assert get_visible_district_capital(settlement) is None

    def test_district_visible_capital(self):
        """
        Населенный пункт привязан к району, у которого столица видимая.
        """
        region = create_region()
        district_capital = create_settlement(region=region)
        settlement = create_settlement(district=dict(region=region, settlement=district_capital))

        assert get_visible_district_capital(settlement) == district_capital
