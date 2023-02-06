# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from common.tester.factories import create_settlement
from common.tester.testcase import TestCase
from travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement import get_visible_settlement_by_id


class TestGetVisibleSettlementById(TestCase):
    def test_found_by_id(self):
        """
        Город найден по id.
        """
        settlement_id = 777
        settlement = create_settlement(id=settlement_id)
        assert get_visible_settlement_by_id(settlement_id) == settlement

    def test_found_hidden(self):
        """
        Город найден по id, но он скрытый. Найдена ближайшая столица.
        """
        settlement_id = 777
        settlement = create_settlement(id=settlement_id, hidden=True)
        assert get_visible_settlement_by_id(settlement_id) == settlement

    def test_not_found_by_id(self):
        """
        Город не найден по id.
        """
        settlement_id = 777
        assert get_visible_settlement_by_id(settlement_id) is None
