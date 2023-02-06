#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    DynamicDeliveryRestriction,
    DynamicWarehouseDelivery,
    DynamicWarehouseLink,
    OfferDimensions,
    Region,
    Shop,
)
from core.types.sku import MarketSku, BlueOffer
from core.matcher import NoKey

from core.types.delivery import BlueDeliveryTariff


class T(TestCase):
    @classmethod
    def prepare_blue_delivery_modifiers(cls):
        '''
        В тарифах синих оферов находится настройка КГТ
        '''
        cls.index.blue_delivery_modifiers.set_default_modifier(
            tariffs=[
                BlueDeliveryTariff(user_price=100),
            ],
            large_size_weight=23,
        )

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
                blue_offers=[
                    BlueOffer(
                        waremd5='EpnWVxDQxj4wg7vVI1ElnA',
                        price=30,
                        weight=21,
                        blue_weight=21,
                        dimensions=OfferDimensions(length=20, width=30, height=10),
                        blue_dimensions=OfferDimensions(length=20, width=30, height=10),
                        offerid='shop_sku_gt_ref_min',
                        feedid=31,
                        randx=3100,
                        cargo_types=[44, 55, 66, 300],
                    )  # CargoType 300 теперь не влияет на КГТ
                ],
            ),
            MarketSku(
                title='blue market sku2',
                hyperid=1,
                sku=2,
                blue_offers=[
                    BlueOffer(
                        waremd5='BH8EPLtKmdLQhLUasgaOnA',
                        price=20,
                        weight=22,
                        blue_weight=22,
                        dimensions=OfferDimensions(length=20, width=30, height=10),
                        blue_dimensions=OfferDimensions(length=20, width=30, height=10),
                        offerid='shop_sku_eq_ref_min',
                        feedid=31,
                    )
                ],
            ),
            MarketSku(
                title='blue market sku3',
                hyperid=1,
                sku=3,
                blue_offers=[
                    BlueOffer(
                        waremd5='KXGI8T3GP_pqjgdd7HfoHQ',
                        price=10,
                        weight=23,  # Этот вес достиг порога КГТ
                        blue_weight=23,
                        dimensions=OfferDimensions(length=20, width=30, height=10),
                        blue_dimensions=OfferDimensions(length=20, width=30, height=10),
                        offerid='shop_sku_lt_ref_min',
                        feedid=31,
                        randx=3100,
                        cargo_types=[44, 300, 66],
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

    def test_not_kgt(self):
        """
        Проверяем не крупногабаритный оффер
        """

        response = self.report.request_json(
            'place=prime&text=market&pp=1&market-sku=1&debug=da&rearr-factors=market_buybox_by_supplier_on_white=1;market_blue_buybox_by_gmv_ue=0;market_debug_buybox=1'
        )

        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'wareId': 'EpnWVxDQxj4wg7vVI1ElnA',
                    'buyboxDebug': {
                        'WonMethod': 'OLD_BUYBOX',
                        'Offers': [
                            {'WareMd5': 'EpnWVxDQxj4wg7vVI1ElnA', 'IsKGT': False},
                        ],
                    },
                    'factors': {'BUYBOX_IS_KGT': NoKey("BUYBOX_IS_KGT")},
                }
            },
        )

    def test_empty_cargo_types(self):
        """
        Проверяем оффер, когда cargo_types не проставлены
        """

        response = self.report.request_json(
            'place=prime&text=market&pp=1&market-sku=2&debug=da&rearr-factors=market_buybox_by_supplier_on_white=1;market_blue_buybox_by_gmv_ue=0;market_debug_buybox=1'
        )

        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'wareId': 'BH8EPLtKmdLQhLUasgaOnA',
                    'buyboxDebug': {
                        'WonMethod': 'OLD_BUYBOX',
                        'Offers': [
                            {'WareMd5': 'BH8EPLtKmdLQhLUasgaOnA', 'IsKGT': False},
                        ],
                    },
                    'factors': {'BUYBOX_IS_KGT': NoKey("BUYBOX_IS_KGT")},
                }
            },
        )

    def test_kgt(self):
        """
        Проверяем крупногабаритный оффер
        Крупногабаритный, когда в cargo_types есть 300
        """

        response = self.report.request_json(
            'place=prime&text=market&pp=1&market-sku=3&debug=da&rearr-factors=market_buybox_by_supplier_on_white=1;market_blue_buybox_by_gmv_ue=0;market_debug_buybox=1'
        )

        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'wareId': 'KXGI8T3GP_pqjgdd7HfoHQ',
                    'buyboxDebug': {
                        'WonMethod': 'OLD_BUYBOX',
                        'Offers': [
                            {'WareMd5': 'KXGI8T3GP_pqjgdd7HfoHQ', 'IsKGT': True},
                        ],
                    },
                    'factors': {'BUYBOX_IS_KGT': '1'},
                }
            },
        )


if __name__ == '__main__':
    main()
