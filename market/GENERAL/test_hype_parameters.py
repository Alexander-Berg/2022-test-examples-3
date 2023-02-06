#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.testcase import TestCase, main
from core.types import MarketSku, Model, Offer, Region
from core.matcher import Absent

# Тест для 'hype' параметров, которые пробрасываются с индексатора до фронта в виде bool-параметра для отображения определённых бейджей.
# "Новинка", "Эксклюзив", "Редкий товар"

outParams = ["exclusive", "hypeGoods", "rareItem"]


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.models += [
            Model(title="has params", hyperid=101, hid=11, is_exclusive=True, hype_goods=True, rare_item=True),
            Model(title="has not params", hyperid=100, hid=10, is_exclusive=False, hype_goods=False, rare_item=False),
            Model(title="miss params", hyperid=99, hid=9),
        ]
        cls.index.regiontree += [
            Region(rid=777, name='region for place=modelinfo'),
        ]
        cls.index.offers += [
            Offer(title="has params", hyperid=1001, is_exclusive=True, hype_goods=True, rare_item=True),
            Offer(title="has not params", hyperid=1000, is_exclusive=False, hype_goods=False, rare_item=False),
            Offer(title="miss params", hyperid=999),
        ]
        cls.index.mskus += [
            MarketSku(title="has params", hyperid=1001, sku=50, is_exclusive=True, hype_goods=True, rare_item=True),
            MarketSku(
                title="has not params", hyperid=1000, sku=51, is_exclusive=False, hype_goods=False, rare_item=False
            ),
            MarketSku(title="miss params", hyperid=999, sku=49),
        ]

    # Тесты для офферов
    def test_offer_has_hype(self):
        response = self.report.request_json('place=prime&hyperid=1001')
        for paramName in outParams:
            self.assertFragmentIn(response, {"results": [{"entity": "offer", paramName: True}]})

    def test_offer_has_not_hype(self):
        response = self.report.request_json('place=prime&hyperid=1000')
        for paramName in outParams:
            self.assertFragmentIn(response, {"results": [{"entity": "offer", paramName: Absent()}]})

    def test_offer_misses_hype(self):
        response = self.report.request_json('place=prime&hyperid=999')
        for paramName in outParams:
            self.assertFragmentIn(response, {"results": [{"entity": "offer", paramName: Absent()}]})

    # Тесты для моделек
    def test_model_has_hype(self):
        response = self.report.request_json('place=modelinfo&hyperid=101&rids=777')
        for paramName in outParams:
            self.assertFragmentIn(response, {"results": [{"entity": "product", paramName: True}]})

    def test_model_has_not_hype(self):
        response = self.report.request_json('place=modelinfo&hyperid=100&rids=777')
        for paramName in outParams:
            self.assertFragmentIn(response, {"results": [{"entity": "product", paramName: Absent()}]})

    def test_model_misses_hype(self):
        response = self.report.request_json('place=modelinfo&hyperid=99&rids=777')
        for paramName in outParams:
            self.assertFragmentIn(response, {"results": [{"entity": "product", paramName: Absent()}]})

    # Тесты для sku
    def test_sku_has_hype(self):
        response = self.report.request_json('place=modelinfo&hyperid=1001&rids=777&market-sku=50')
        for paramName in outParams:
            self.assertFragmentIn(response, {"results": [{"entity": "sku", paramName: True}]})

    def test_sku_has_not_hype(self):
        response = self.report.request_json('place=modelinfo&hyperid=1000&rids=777&market-sku=51')
        for paramName in outParams:
            self.assertFragmentIn(response, {"results": [{"entity": "sku", paramName: Absent()}]})

    def test_sku_misses_hype(self):
        response = self.report.request_json('place=modelinfo&hyperid=999&rids=777&market-sku=49')
        for paramName in outParams:
            self.assertFragmentIn(response, {"results": [{"entity": "sku", paramName: Absent()}]})


if __name__ == '__main__':
    main()
