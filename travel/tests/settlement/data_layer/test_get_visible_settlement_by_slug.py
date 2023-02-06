# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from common.tester.factories import create_settlement
from common.tester.testcase import TestCase
from travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement import get_visible_settlement_by_slug


class TestGetVisibleSettlementBySlug(TestCase):
    def __init__(self, *args, **kwargs):
        super(TestGetVisibleSettlementBySlug, self).__init__(*args, **kwargs)
        self.settlement_id = 777
        self.settlement_slug = 'slug777'

    def test_found_by_id(self):
        """
        Город найден по slug.
        """
        settlement = create_settlement(id=self.settlement_id, slug=self.settlement_slug)
        assert get_visible_settlement_by_slug(self.settlement_slug) == settlement

    def test_found_hidden(self):
        """
        Город найден по slug, но он скрытый. Найдена ближайшая столица.
        """
        settlement = create_settlement(id=self.settlement_id, slug=self.settlement_slug, hidden=True)
        assert get_visible_settlement_by_slug(self.settlement_slug) == settlement

    def test_not_found_by_id(self):
        """
        Город не найден по id.
        """
        assert get_visible_settlement_by_slug(self.settlement_slug) is None
