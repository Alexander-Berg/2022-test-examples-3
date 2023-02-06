#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

"""
В тестах проверяется функиональность подмены устаревших msku-id на новые. Один из вариантов применения:
пользователь добавил ссылку на страницу товара на Беру в закладки браузера, но через некоторое время msku-id
был удален (например, контенты склеили 2 товара в 1, а старые msku-id удалили). Чтобы пользователь получал корректный
результат, необходимо делать редирект. Со стороны репорта это выглядит как добавление нового поля deletedId в ответ.

Тикеты, связанные с этими изменениями:
https://st.yandex-team.ru/MCPROJECT-132 -- общий тикет всего проекта.
https://st.yandex-team.ru/MARKETINDEXER-30325 -- общий тикет задачи (схожая функциональность уже была для белого,
                                                 но лишь для кластеров, в рамках проекта она была чуть расширена, чтобы
                                                 уметь обрабатывать модели).

https://st.yandex-team.ru/BLUEMARKET-7616 -- обсуждение с синими фронтами (плейсы sku_offers).
https://st.yandex-team.ru/MARKETOUT-26852 -- обсуждение с чекаутером (плейсы sku_offers, consolidate).

https://st.yandex-team.ru/MARKETFRONT-1964 -- старый тикет со схожими хотелками (был найден уже после начала данного
                                              проекта), нужен для СЕО.
"""

from core.matcher import ElementCount, EmptyList, Contains
from core.types import (
    BlueOffer,
    MarketSku,
    MarketSkuTransition,
    Shop,
    Vat,
    Model,
)
from core.testcase import TestCase, main


def _make_offer(shop_sku, waremd5, supplier_id):
    return BlueOffer(
        price=42,
        offerid=shop_sku,
        waremd5=waremd5,
        supplier_id=supplier_id,
        vat=Vat.VAT_10,
        stock_store_count=1,
        feedid=1,
    )


class _Offers(object):
    offer_1 = _make_offer('offer_10', 'offer_10_s1__________g', 100500)
    offer_2 = _make_offer('offer_20', 'offer_20_s1__________g', 100500)
    offer_3 = _make_offer('offer_30', 'offer_30_s1__________g', 100500)
    offer_4 = _make_offer('offer_40', 'offer_40_s1__________g', 100500)

    offer_5 = _make_offer('offer_50', 'offer_50_s1__________g', 100500)
    offer_6 = _make_offer('offer_60', 'offer_60_s1__________g', 100500)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += [
            Shop(
                fesh=100500,
                priority_region=213,
                warehouse_id=312,
                blue=Shop.BLUE_REAL,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                sku=10,
                hyperid=110,
                blue_offers=[
                    _Offers.offer_1,
                ],
            ),
            MarketSku(
                sku=20,
                blue_offers=[
                    _Offers.offer_2,
                ],
            ),
            MarketSku(
                sku=30,
                blue_offers=[
                    _Offers.offer_3,
                    _Offers.offer_4,
                ],
            ),
            MarketSku(
                sku=50,
                blue_offers=[
                    _Offers.offer_5,
                    _Offers.offer_6,
                ],
            ),
        ]

        cls.index.models += [
            Model(hyperid=110, title='model110'),
        ]

        cls.index.msku_transitions += [
            MarketSkuTransition(src_id=1, dst_id=10),
            MarketSkuTransition(src_id=2, dst_id=20),
            # these MSKUs were merged into a new one
            MarketSkuTransition(src_id=3, dst_id=30),
            MarketSkuTransition(src_id=4, dst_id=30),
            # transition of MSKU with promo
            MarketSkuTransition(src_id=5, dst_id=50),
        ]

    def test_modelinfo_enabled(self):
        """
        Тестируем, что плейс корректно делает подмену msku_id, если есть флаг with-rebuilt-model=1.
        """
        response = self.report.request_json('place=sku_offers&market-sku=1&with-rebuilt-model=1')
        self.assertFragmentIn(
            response,
            {
                'entity': 'sku',
                'id': '10',
                'deletedId': '1',
            },
            allow_different_len=False,
        )

    def test_modelinfo_disabled(self):
        """
        Тестируем, что если нет флага with-rebuilt-model=1 (по умолчанию он = 0), то репорт работает по-старому.
        """
        response = self.report.request_json('place=sku_offers&market-sku=1')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 0,
                    'results': EmptyList(),
                }
            },
            allow_different_len=False,
        )

    def test_modelinfo_multiple_msku(self):
        """
        Тестируем, что функциональность отработает корректно, если передано несколько market-sku.
        """
        response = self.report.request_json('place=sku_offers&market-sku=1&market-sku=2&with-rebuilt-model=1')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 2,
                    'results': [
                        {
                            'entity': 'sku',
                            'id': '10',
                            'deletedId': '1',
                        },
                        {
                            'entity': 'sku',
                            'id': '20',
                            'deletedId': '2',
                        },
                    ],
                }
            },
            allow_different_len=False,
        )

    def test_productoffers_msku_transitions(self):
        """
        Тестируем, что плейс productoffers корректно делает подмену msku_id, если есть флаг with-rebuilt-model=1.
        А при выключенном флаге выдача пустая
        """
        response = self.report.request_json(
            "place=productoffers&market-sku=1&with-rebuilt-model=1&hid-by-market-sku=1&client=frontend&debug=da"
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'modelId': 110,
                    'marketSku': "10",
                    'deletedMskuId': "1",
                    'results': [
                        {
                            'entity': 'offer',
                            'marketSku': "10",
                        },
                    ],
                }
            },
            allow_different_len=False,
        )
        self.assertFragmentIn(
            response,
            {
                'logicTrace': [
                    Contains('ActualizeHyperId(): Actualize &hyperid if need'),
                    Contains('ActualizeMskuId(): Actualize &market-sku if need'),
                    Contains('ActualizeMskuId(): Msku 1 replaced on 10'),
                    Contains('AddHyperAndHid(): Add &hyperid and &hid if not exists'),
                    Contains('Add hyperid=110 for market-sku=10'),
                ],
            },
        )

        response = self.report.request_json(
            "place=productoffers&market-sku=10&with-rebuilt-model=1&client=frontend&hid-by-market-sku=1"
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'modelId': 110,
                    'marketSku': "10",
                    'results': [
                        {
                            'entity': 'offer',
                            'marketSku': "10",
                        },
                    ],
                }
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            "place=productoffers&market-sku=1&hid-by-market-sku=1&client=frontend&debug=da"
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': ElementCount(0),
                }
            },
            allow_different_len=False,
        )
        # Проверяем, что при with-rebuilt-model=1 замены msku не происходит
        self.assertFragmentNotIn(
            response,
            {
                'logicTrace': [
                    Contains('ActualizeMskuId(): Msku 1 replaced on 10'),
                ],
            },
        )


if __name__ == '__main__':
    main()
