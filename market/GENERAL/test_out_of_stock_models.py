#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import MarketSku, Model, Offer, Region, RegionalModel, Shop
from core.testcase import TestCase, main


class _C:
    rid_unknown = 3


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.disable_check_empty_output()

        cls.index.regional_models += [
            RegionalModel(hyperid=175941311, rids=[54, 194], has_good_cpa=False, has_cpa=True),
        ]

        cls.index.models += [
            Model(hyperid=175941311, hid=44),
            Model(hyperid=10, hid=44),
            Model(hyperid=200, hid=55),
        ]

        for i in range(20):
            cls.index.models.append(Model(hyperid=201 + i, hid=55))

        cls.index.regiontree = [
            Region(rid=194, name='Саратов'),
            Region(
                rid=54,
                name='Екб',
                children=[
                    Region(rid=100500, name='улица в ЕКб'),
                ],
            ),
        ]

        cls.index.shops += [
            Shop(fesh=4, priority_region=54, regions=[54, 194], cpa=Shop.CPA_REAL, name='СPA Екб'),
            Shop(fesh=5, priority_region=54, regions=[54, 194], cpa=Shop.CPA_NO, cpc=Shop.CPC_REAL, name='CPC Екб'),
        ]

        cls.index.mskus += [
            MarketSku(hyperid=175941311, hid=44, sku=1010),
            MarketSku(hyperid=665306170, hid=44, sku=1020),
            MarketSku(hyperid=10, hid=44, sku=1030),
            MarketSku(hyperid=200, hid=55, sku=1050),
        ]

        cls.index.offers += [
            Offer(fesh=4, title="CPA офер #2", cpa=Offer.CPA_REAL, hyperid=175941311, price=200, sku=1010, hid=44),
            Offer(
                fesh=5,
                title="CPA_NO офер, модель из конфига",
                cpa=Offer.CPA_NO,
                price=100,
                hyperid=175941311,
                sku=1010,
                hid=44,
            ),
            Offer(fesh=5, title="CPA_NO 10", cpa=Offer.CPA_NO, hyperid=10, sku=1030, hid=44),
            Offer(fesh=4, title="CPA_REAL 55", cpa=Offer.CPA_REAL, hyperid=200, price=555, sku=1050, hid=55),
        ]

    # Несколько моделей в магазинах в Екатеринбурге
    # У модели 665306170 нет офферов
    # У модели 10 есть только cpc
    # В выдаче должны быть только эти две модели, у остальных есть cpa
    # Параметр use-default-offers не должен повлиять
    def test_out_of_stock_cpa_model_filter(self):
        """Проверяем выдачу моделей с фильтрацией по отсутствию cpa-офферов"""
        for do in ('0', '1'):
            request = (
                'place=prime&pp=18&entities=product&local-offers-first=0&rids=54&hid=44'
                + '&cpa-out-of-stock-models=1&use-default-offers=%s' % do
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 2,
                        "totalModels": 2,
                        "results": [
                            {"id": 10, "type": "model", "offers": {"count": 0}},
                            {"id": 665306170, "type": "model", "offers": {"count": 0}},
                        ],
                    }
                },
                allow_different_len=False,
            )

    # https://st.yandex-team.ru/MARKETOUT-44507 - не отдаем модели "не в продаже" для экспресса
    def test_out_of_stock_cpa_model_filter_with_express(self):
        """Проверяем выдачу моделей с фильтрацией по отсутствию cpa-офферов + с фильтром по экспресс-доставке.
        Условия аналогичны test_out_of_stock_cpa_model_filter, но не должны найти ничего, хотя у моделей нет cpa-офферов."""

        for do in ('0', '1'):
            request = (
                'place=prime&pp=18&entities=product&local-offers-first=0&&rids=54&hid=44'
                + '&cpa-out-of-stock-models=1&filter-express-delivery=1&use-default-offers=%s' % do
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {"search": {"total": 0, "totalModels": 0, "results": []}},
                allow_different_len=False,
            )

    def test_out_of_stock_cpa_model_filter_shutdown(self):
        """Проверяем отключение выдачи через rearr"""

        for do in ('0', '1'):
            request = (
                'place=prime&pp=18&entities=product&local-offers-first=0&&rids=54&hid=44&cpa-out-of-stock-models=1'
                + '&rearr-factors=market_disable_out_of_stock_models_search=1&use-default-offers=%s' % do
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {"search": {"total": 0, "totalModels": 0, "results": []}},
                allow_different_len=False,
            )

    # Проверяем выдачу моделей не в продаже внутри основной выдачи на прайме
    # Условия теста - см. test_out_of_stock_cpa_model_filter + нужен cpa=real
    # В выдаче должны быть две модели не в продаже + обычные результаты
    def test_out_of_stock_models_on_prime(self):
        """Проверяем подмешивание моделей с фильтрацией по отсутствию cpa-офферов в основную выдачу; требуется cpa=only"""
        request = (
            'place=prime&pp=18&local-offers-first=0&rids=54&hid=44'
            + '&use-default-offers=1&cpa=real'
            + '&rearr-factors=market_oos_enable=1'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"id": 10, "entity": "product", "offers": {"count": 0}},
                        {"id": 665306170, "entity": "product", "offers": {"count": 0}},
                    ],
                }
            },
            allow_different_len=True,
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"entity": "offer"},
                    ],
                }
            },
            allow_different_len=True,
        )

    # Проверяем ограничение на количество моделей (не в продаже) в основной выдаче
    # Условия теста - см. test_out_of_stock_cpa_model_filter + нужен cpa=real + нужны схлопывание с разгруппировкой
    # В выдаче должны быть 5 моделей не в продаже (hid=55, hyperid>200) + тестовый схлопнутый cpa-оффер (hid=55, hyperid=200)
    def test_out_of_stock_models_limit_on_prime(self):
        # с how=random используется plain-итератор, без - rearrangeable
        for extra_args in ('', '&how=random'):
            request = (
                'place=prime&pp=18&local-offers-first=0&rids=54&hid=55'
                + '&use-default-offers=1&cpa=real'
                + '&allow-collapsing=1&allow-ungrouping=1'
                + '&rearr-factors=market_oos_enable=1;market_oos_limit=5;market_oos_limit_per_shard=10'
                + extra_args
            )

            response = self.report.request_json(request)

            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {"id": 200, "entity": "product", "offers": {"count": 1}},
                        ],
                    }
                },
                allow_different_len=True,
            )

            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {"entity": "product", "offers": {"count": 0}},
                        ]
                        * 5,
                    }
                },
                allow_different_len=True,
            )

            assert len(response.root['search']['results']) == 6


if __name__ == '__main__':
    main()
