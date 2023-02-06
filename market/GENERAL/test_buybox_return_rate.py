#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    DynamicDeliveryRestriction,
    DynamicWarehouseDelivery,
    DynamicWarehouseLink,
    Elasticity,
    MarketSku,
    Region,
    Shop,
)
from core.types.return_rate import ReturnRate


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.regiontree += [
            Region(
                rid=1,
                name='Московская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=213, name='Москва'),
                ],
            )
        ]

        cls.index.shops += [
            Shop(
                fesh=13,
                datafeed_id=13,
                priority_region=213,
                regions=[225],
                name="3P поставщик Петя",
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
            Shop(
                fesh=31,
                datafeed_id=31,
                priority_region=213,
                regions=[225],
                name="3P поставщик Вася",
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
            Shop(
                fesh=17,
                datafeed_id=17,
                priority_region=213,
                regions=[225],
                name="Кроссдок поставщик",
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
                direct_shipping=False,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=1,
                title='blue market sku1',
                sku=1,
                ref_min_price=20,
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=200),
                    Elasticity(price_variant=200, demand_mean=80),
                    Elasticity(price_variant=300, demand_mean=10),
                ],
                blue_offers=[
                    BlueOffer(
                        waremd5='EpnWVxDQxj4wg7vVI1ElnA',
                        price=30,
                        offerid='shop_sku_gt_ref_min',
                        feedid=31,
                        randx=3100,
                    )
                ],
            ),
            MarketSku(
                title='blue market sku2',
                hyperid=1,
                sku=2,
                blue_offers=[
                    BlueOffer(waremd5='BH8EPLtKmdLQhLUasgaOnA', price=20, offerid='shop_sku_eq_ref_min', feedid=13)
                ],
            ),
            MarketSku(
                title='blue market sku3',
                hyperid=1,
                sku=3,
                blue_offers=[BlueOffer(waremd5='KXGI8T3GP_pqjgdd7HfoHQ', price=11, offerid='qwerty', feedid=17)],
            ),
        ]

        cls.index.return_rate += [
            ReturnRate(
                supplier_id=5,
                type='FF',
                return_rate=0.95,
                late_ship_rate=-1,
                cancellation_rate=-1,
            ),
            ReturnRate(
                supplier_id=13,
                type='FF',
                return_rate=0.77,
                late_ship_rate=-1,
                cancellation_rate=-1,
            ),
            ReturnRate(
                supplier_id=17,
                type='Crossdock',
                return_rate=0.13,
                late_ship_rate=-1,
                cancellation_rate=-1,
            ),
        ]

    @classmethod
    def prepare_nordstream(cls):
        cls.settings.nordstream_autogenerate = False
        for warehouse_id in [145]:
            cls.dynamic.nordstream += [DynamicWarehouseLink(warehouse_id, [warehouse_id])]
            cls.dynamic.nordstream += [
                DynamicWarehouseDelivery(
                    warehouse_id,
                    {
                        225: [
                            DynamicDeliveryRestriction(min_days=1, max_days=2, cost=15),
                        ],
                    },
                ),
            ]

    def test_buybox_return_rate_not_in_file(self):
        """Проверяем протаскивание return_rate в BuyboxContest.
        Проверка, когда в файле отсутствует значение для поставшика.
        """
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&text=market&pp=1&market-sku=1&debug=da&rearr-factors=market_debug_buybox=1;market_blue_buybox_by_gmv_ue=0'
        )
        self.assertFragmentIn(
            response,
            {
                "wareId": "EpnWVxDQxj4wg7vVI1ElnA",
                'debug': {
                    'buyboxDebug': {
                        'WonMethod': 'OLD_BUYBOX',
                        'Offers': [
                            {
                                'WareMd5': 'EpnWVxDQxj4wg7vVI1ElnA',
                                'ShopOperationalRating': {
                                    'ReturnRate': 0.001,
                                },
                            },
                        ],
                    }
                },
            },
        )


if __name__ == '__main__':
    main()
