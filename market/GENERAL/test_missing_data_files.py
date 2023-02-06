#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Currency, HyperCategory, HyperCategoryType, Model, Offer, Region, Shop
from core.testcase import TestCase, main
from core.types.min_bids import MinBidsCategory, MinBidsModel, MinBidsPriceGroup, MinBidsSwitchOff

from core.types.taxes import Tax
from core.types.sku import MarketSku, BlueOffer

# Предполагается, что этот модуль будет использоваться для тестирования загрузки репорта с отсутствующими
# входными данными - файлами индекса или report-data.
# Дело в том, что если для тестирования каждой ситуации с отсутствием в индексе какого-либо файла
# тестировать в отдельном лайтовом модуле, то это приведёт медленной работе лайта, так как при
# запуске такого модуля каждый раз происходит загрузка репорта, а это долго. А тестировать это в том
# модуле, где тестируется функционал связанный с этим файлом, тоже нельзя, так как входные данные
# в рамках одного тестового модуля неизменны (за исключением динамических).


class T(TestCase):
    @classmethod
    def prepare_min_bids2(cls):
        cls.index.regiontree += [
            Region(rid=213, name='Москва'),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=100, output_type=HyperCategoryType.GURU),
        ]
        cls.index.models += [
            Model(hyperid=1000, hid=100, title='min_bids model1123'),
        ]
        cls.index.offers += [
            Offer(title='min_bids offer1', hid=100, hyperid=1000, price=10100, bid=1000),
            Offer(title='min_bids offer2', hid=100, hyperid=1000, price=10000, bid=900),
        ]
        cls.index.min_bids_category_stats += [
            MinBidsCategory(
                category_id=100,
                geo_group_id=0,
                price_group_id=100,
                drr=0.22,
                search_conversion=0.233,
                card_conversion=1.0,
                full_card_conversion=1.0,
            )
        ]
        cls.index.min_bids_model_stats += [
            MinBidsModel(
                model_id=1000,
                geo_group_id=0,
                drr=0.01,
                search_clicks=1,
                search_orders=3,
                card_clicks=144,
                card_orders=9,
                full_card_orders=0,
                full_card_clicks=0,
            )
        ]
        cls.index.min_bids_price_groups += [MinBidsPriceGroup(0), MinBidsPriceGroup(100)]
        cls.index.min_bids_config += [MinBidsSwitchOff()]

    def test_min_bids2_is_not_used(self):
        '''
        Запрашиваем плейс productoffers

        Проверяем, что значение минимального бида соответствует старой формуле.
        '''

        self.report.request_json('place=productoffers&hyperid=1000&show-urls=external&how=aprice')

        self.show_log.expect(min_bid=13, title="min_bids offer1")

    @classmethod
    def prepare_default_blue_fee(cls):
        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='virtual_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                cpa=Shop.CPA_REAL,
            ),
            Shop(fesh=2, datafeed_id=2, priority_region=213, name='blue_shop', warehouse_id=145, cpa=Shop.CPA_REAL),
        ]
        cls.index.mskus += [
            MarketSku(
                title="blue offer sku1",
                hyperid=1,
                sku=1,
                waremd5='Sku1-wdDXWsIiLVm1goleg',
                blue_offers=[BlueOffer(offerid='Shop1_sku1')],
            ),
        ]

    def test_default_blue_fee(self):
        """
        Проверяем, что при отсутствии файла с данными по fee голубого маркета, используется fee равный
        трём процентам
        """
        response = self.report.request_json('place=sku_offers&market-sku=1&show-urls=direct&rids=213&rgb=blue')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "sku",
                        "offers": {
                            "items": [
                                {
                                    "entity": "offer",
                                    "fee": "0.0000",
                                }
                            ]
                        },
                    }
                ]
            },
        )


if __name__ == '__main__':
    main()
