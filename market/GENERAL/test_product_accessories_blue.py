#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Currency,
    MnPlace,
    Model,
    Offer,
    RegionalModel,
    Shop,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
    YamarecPlace,
    YamarecSettingPartition,
)
from core.testcase import main
from core.types.sku import MarketSku, BlueOffer
from core.types.taxes import Vat, Tax
from simple_testcase import SimpleTestCase, create_model_with_default_offer


class _Offers(object):
    sku1_offer1 = BlueOffer(price=5, vat=Vat.VAT_10, offerid='Shop1_sku1', feedid=100, waremd5='Sku1Price5-IiLVm1Goleg')
    sku2_offer2 = BlueOffer(price=50, vat=Vat.VAT_0, offerid='Shop1_sku2', feedid=100, waremd5='Sku1Price50-iLVm1Goleg')


class T(SimpleTestCase):
    """
    Набобр тестов для place=product_accessories_blue
    """

    @classmethod
    def prepare(cls):
        cls.settings.rgb_blue_is_cpa = True

        cls.index.models += [
            Model(hyperid=1, hid=101),
            Model(hyperid=2, hid=101),
            Model(hyperid=3, hid=101),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=1, offers=1, rids=[213]),
            RegionalModel(hyperid=2, offers=1, rids=[213]),
            RegionalModel(hyperid=3, offers=1, rids=[213]),
        ]

        cls.index.shops += [
            Shop(
                fesh=431782,
                datafeed_id=1,
                priority_region=213,
                name='blue_shop_1',
                currency=Currency.RUR,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                tax_system=Tax.OSN,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=2,
                priority_region=213,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=100,
                datafeed_id=100,
                priority_region=213,
                name='blue_shop_2',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                blue=Shop.BLUE_REAL,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title="blue offer sku2",
                hyperid=2,
                sku=2,
                waremd5='Sku2-wdDXWsIiLVm1goleg',
                blue_offers=[_Offers.sku2_offer2],
            ),
        ]

        # non blue offers
        cls.index.offers += [
            Offer(
                waremd5='pCl2on9YL4fCV8poq57hRg', hyperid=2, fesh=2, price=1, cpa=Offer.CPA_REAL, discount=5, ts=2002
            ),
            Offer(waremd5='xzFUFhFuAvI1sVcwDnxXPQ', hyperid=3, fesh=2, price=1, cpa=Offer.CPA_REAL, discount=5),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2002).respond(0.5)

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.EXTERNAL_PRODUCT_ACCESSORY_BLUE_MARKET,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(
                        params={
                            'version': '1',
                            'use-external': '1',
                            'use-local': '0',
                        },
                        splits=[{}],
                    ),
                ],
            ),
            YamarecPlace(
                name=YamarecPlace.Name.EXTERNAL_PRODUCT_ACCESSORY,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(
                        params={
                            'version': '1',
                            'use-external': '1',
                            'use-local': '0',
                        },
                        splits=[{}],
                    ),
                ],
            ),
        ]

        cls.recommender.on_request_accessory_models(model_id=1, item_count=1000, version='1').respond(
            {'models': ['2', '3']}
        )

    def test_default_offer(self):
        """
        Проверка дефолтного оффера: должны быть только синие офферы
        В индексе для модели #1 более дешёвый - зелёный оффер, проверяем, что всё-таки
            вместо него отображается синий дефолтный оффер
        Проверяем также, что для модели #3 нет синего оффера
        """

        response = self.report.request_json(
            'place=product_accessories&rgb=blue&rearr-factors=market_blue_buybox_dsbs_conversion_coef=1;market_disable_product_accessories=0&hyperid=1&rids=213'
        )
        self.assertFragmentIn(
            response,
            {
                "total": 2,
                "results": [
                    create_model_with_default_offer(
                        model_id=2, waremd5='Sku1Price50-iLVm1Goleg', price=50, fesh=431782
                    ),
                ],
            },
        )

        response = self.report.request_json(
            'place=product_accessories&fesh=431782&rearr-factors=market_blue_buybox_dsbs_conversion_coef=1;market_disable_product_accessories=0&hyperid=1&rids=213'
        )
        self.assertFragmentIn(
            response,
            {
                "total": 1,
                "results": [
                    create_model_with_default_offer(
                        model_id=2, waremd5='Sku1Price50-iLVm1Goleg', price=50, fesh=431782
                    ),
                ],
            },
        )

        """
        А зелёном тоже должен быть синий оффер из-за приоритета CPA
        """
        response = self.report.request_json(
            'place=product_accessories&hyperid=1&rids=213&rgb=green&rearr-factors=prefer_do_with_sku=0;market_blue_buybox_dsbs_conversion_coef=1;market_disable_product_accessories=0'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    create_model_with_default_offer(model_id=2, waremd5='pCl2on9YL4fCV8poq57hRg', price=1, fesh=2),
                ]
            },
        )

    def test_disabled_accessories(self):
        """
        Проверка что по умолчанию аксессуары отключены
        """

        for place_id in ['place=product_accessories&rgb=blue', 'place=product_accessories&fesh=431782']:
            self.assertResponseIsEmpty(query='{}&hyperid=1&rids=213'.format(place_id))

    @classmethod
    def prepare_warehouse(cls):
        cls.index.models += [
            # key model
            Model(hyperid=11),
            # accessories
            Model(hyperid=12),
            Model(hyperid=13),
            Model(hyperid=14),
        ]
        # accessorirs config
        cls.recommender.on_request_accessory_models(model_id=11, item_count=1000, version='1').respond(
            {'models': ['13', '12', '14']}
        )
        cls.index.shops += [
            Shop(
                fesh=80001,
                datafeed_id=70001,
                priority_region=213,
                currency=Currency.RUR,
                # tax_system=Tax.OSN,
                blue=Shop.BLUE_REAL,
                warehouse_id=147,
            ),
            Shop(
                fesh=80002,
                datafeed_id=70002,
                priority_region=213,
                currency=Currency.RUR,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
            ),
        ]
        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(
                regions=[213, 225],
                warehouse_with_priority=[
                    WarehouseWithPriority(
                        100, 1
                    ),  # необходим хоть какой-то приоретет, чтобы связки вообще создались. Для всех остальных будет выставлен минимальный приоритет
                ],
            )
        ]

        # key model offers
        sku_offer_11_1 = BlueOffer(model_title='Key model #11', price=1000, vat=Vat.VAT_10, feedid=70001)
        # buybox
        sku_offer_11_2 = BlueOffer(model_title='Key model #11', price=1100, vat=Vat.VAT_10, feedid=70001)

        # accessories
        # accessory offer from target warehouse
        sku_offer_12_1 = BlueOffer(model_title='Accessory model #12', price=210, vat=Vat.VAT_10, feedid=70001)
        # offer from different warehouse
        sku_offer_12_2 = BlueOffer(model_title='Accessory model #12', price=100, vat=Vat.VAT_10, feedid=70002)
        # best offer from different warehouse
        sku_offer_12_3 = BlueOffer(model_title='Accessory model #12', price=200, vat=Vat.VAT_10, feedid=70002)
        # second accessory offer from different warehouse
        sku_offer_13 = BlueOffer(model_title='Accessory model #13', price=300, vat=Vat.VAT_10, feedid=70002)
        # yet another accessory offer from target warehouse
        sku_offer_14 = BlueOffer(model_title='Accessory model #14', price=300, vat=Vat.VAT_10, feedid=70001)

        cls.index.mskus += [
            # key sku
            MarketSku(hyperid=11, sku=10011, blue_offers=[sku_offer_11_1, sku_offer_11_2]),
            # accessory sku without offers in target warehouse
            MarketSku(hyperid=12, sku=10012, blue_offers=[sku_offer_12_1, sku_offer_12_2, sku_offer_12_3]),
            # accessory sku with offers in target warehouse
            MarketSku(hyperid=13, sku=10013, blue_offers=[sku_offer_13]),
            MarketSku(hyperid=14, sku=10014, blue_offers=[sku_offer_14]),
        ]

    def test_warehouse(self):
        """
        Аксессуары, недоступные на складе байбокса ключевой модели, пессимизируются
        (Если передан идентификатор sku ключевой модели)
        """

        """Запрос без указания sku: аксессуары в порядке от рекомендаций c последующей пессимизацией
        аксессуара 13, для которого в индексе нет оффера из склада 147 (на этом складе байбокс модели 11)
        При этом байбоксы для аксессуаров по-прежнему честные и могут быть с другого склада
        """
        response = self.report.request_json(
            'place=product_accessories&rgb=blue&hyperid=11&rids=213&debug=1&rearr-factors=market_disable_product_accessories=0'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 3,
                    'results': [
                        {
                            'entity': 'product',
                            'id': 12,
                            'offers': {
                                'items': [
                                    {'prices': {'value': '100'}, 'supplier': {'warehouseId': 145}},
                                ]
                            },
                        },
                        {
                            'entity': 'product',
                            'id': 14,
                            'offers': {'items': [{'prices': {'value': '300'}, 'supplier': {'warehouseId': 147}}]},
                        },
                        {
                            'entity': 'product',
                            'id': 13,
                            'offers': {'items': [{'prices': {'value': '300'}, 'supplier': {'warehouseId': 145}}]},
                        },
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        """Запрос с sku: аксессуары в порядке от рекомендаций c последующей пессимизацией
        аксессуара 13, для которого в индексе нет оффера из склада 147 (на этом складе байбокс модели 11)
        При этом байбоксы для аксессуаров по-прежнему честные и могут быть с другого склада
        """
        response = self.report.request_json(
            'place=product_accessories&rgb=blue&hyperid=11&rids=213&market-sku=10011&debug=1&rearr-factors=market_disable_product_accessories=0'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 3,
                    'results': [
                        {
                            'entity': 'product',
                            'id': 12,
                            'offers': {
                                'items': [
                                    {'prices': {'value': '100'}, 'supplier': {'warehouseId': 145}},
                                ]
                            },
                        },
                        {
                            'entity': 'product',
                            'id': 14,
                            'offers': {'items': [{'prices': {'value': '300'}, 'supplier': {'warehouseId': 147}}]},
                        },
                        {
                            'entity': 'product',
                            'id': 13,
                            'offers': {'items': [{'prices': {'value': '300'}, 'supplier': {'warehouseId': 145}}]},
                        },
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        """
        Запрос зеленого маркета, на котором пессимизации аксессуара 13 быть не должно.
        """
        response = self.report.request_json(
            'place=product_accessories&rgb=green&hyperid=11&rids=213&debug=1&rearr-factors=market_disable_product_accessories=0'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 3,
                    'results': [
                        {
                            'entity': 'product',
                            'id': 13,
                            'offers': {'items': [{'prices': {'value': '300'}, 'supplier': {'warehouseId': 145}}]},
                        },
                        {
                            'entity': 'product',
                            'id': 12,
                            'offers': {
                                'items': [
                                    {'prices': {'value': '100'}, 'supplier': {'warehouseId': 145}},
                                ]
                            },
                        },
                        {
                            'entity': 'product',
                            'id': 14,
                            'offers': {'items': [{'prices': {'value': '300'}, 'supplier': {'warehouseId': 147}}]},
                        },
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        """
        Запрос синего маркета, на котором пессимизации аксессуара 13 быть не должно ввиду наличия флага, отключающего буст.
        """
        response = self.report.request_json(
            'place=product_accessories&rgb=blue&hyperid=11&rids=213&debug=1&rearr-factors=market_disable_product_accessories=0;market_disable_warehouse_boosting_for_product_accessories=1'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 3,
                    'results': [
                        {
                            'entity': 'product',
                            'id': 13,
                            'offers': {'items': [{'prices': {'value': '300'}, 'supplier': {'warehouseId': 145}}]},
                        },
                        {
                            'entity': 'product',
                            'id': 12,
                            'offers': {
                                'items': [
                                    {'prices': {'value': '100'}, 'supplier': {'warehouseId': 145}},
                                ]
                            },
                        },
                        {
                            'entity': 'product',
                            'id': 14,
                            'offers': {'items': [{'prices': {'value': '300'}, 'supplier': {'warehouseId': 147}}]},
                        },
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
