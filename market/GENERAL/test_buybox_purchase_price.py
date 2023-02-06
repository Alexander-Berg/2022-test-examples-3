#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import DynamicDeliveryRestriction, DynamicWarehouseDelivery, DynamicWarehouseLink, Region, Shop
from core.types.sku import MarketSku, BlueOffer


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
            Shop(fesh=13, priority_region=213, fulfillment_virtual=True, virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE),
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
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=1,
                title='blue market sku1',
                sku=1,
                purchase_price=20.33,
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

    def test_buybox_purchase_price(self):
        """Проверяем закупочную цену в BuyboxContest."""
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
                            {'WareMd5': 'EpnWVxDQxj4wg7vVI1ElnA', 'PurchasePrice': 20.33},
                        ],
                    }
                },
            },
        )


if __name__ == '__main__':
    main()
