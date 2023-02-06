#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import BlueOffer, MarketSku, Model, Offer, Region, Shop
from core.matcher import NoKey, NotEmpty


class T(TestCase):
    """
    https://st.yandex-team.ru/MARKETOUT-36211
    Протестить, что при включенном расхлопывании/схлопывании по business_id и приоритезации, работает следующая

    логика CPC и CPA оффера, имеющие один business_id, не схлопываются, MD с DSBS схлопывается приоритет MD>DSBS,

    внутри MD схлопываем по cpm.
    """

    @classmethod
    def prepare(cls):
        cls.index.regiontree += [
            Region(rid=213, name='Москва'),
        ]

        cls.index.shops += [
            Shop(
                fesh=12,
                business_fesh=3,
                datafeed_id=12,
                priority_region=213,
                regions=[225],
                name="1P поставщик",
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
            Shop(
                fesh=31,
                business_fesh=3,
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
                fesh=32,
                business_fesh=3,
                datafeed_id=32,
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
                fesh=111,
                business_fesh=3,
                datafeed_id=110,
                priority_region=213,
                regions=[225],
                name="простой белый магазин Пети",
            ),
            Shop(
                fesh=1110,
                business_fesh=3,
                datafeed_id=1100,
                priority_region=213,
                regions=[225],
                name="dsbs белый магазин Пети",
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=112,
                business_fesh=4,
                datafeed_id=120,
                priority_region=213,
                regions=[225],
                name="второй простой белый магазин Пети",
            ),
            Shop(
                fesh=1120,
                business_fesh=4,
                datafeed_id=1200,
                priority_region=213,
                regions=[225],
                name="второй dsbs белый магазин Пети",
                cpa=Shop.CPA_REAL,
            ),
        ]

        cls.index.models += [
            Model(hid=101, hyperid=105, title="Model with msku"),
        ]

        cls.index.mskus += [
            MarketSku(
                hid=101,
                hyperid=105,
                sku=103,
                title="MSKU-103",
                blue_offers=[
                    BlueOffer(price=300, feedid=12, fesh=12, fee=30, waremd5='OFF2_3000_SKU1_SUP12_Q', business_id=3),
                    BlueOffer(price=295, feedid=31, fesh=31, fee=50, waremd5='OFF2_3000_SKU1_SUP31_Q', business_id=3),
                    BlueOffer(price=300, feedid=32, fesh=32, fee=40, waremd5='OFF2_3000_SKU1_SUP32_Q', business_id=3),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                price=300, feedid=110, fesh=111, waremd5='OFF_3000_HID1_WHITE__Q', hyperid=105, hid=101, business_id=3
            ),
            Offer(
                price=300, feedid=120, fesh=112, waremd5='OFF_3000_HID2_WHITE__Q', hyperid=105, hid=101, business_id=4
            ),
        ]
        cls.index.offers += [
            Offer(
                price=300,
                feedid=1100,
                sku=103,
                fesh=1110,
                waremd5='OFF_DSBS_HID1_WHITE__Q',
                hyperid=105,
                hid=101,
                business_id=3,
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                price=300,
                feedid=1200,
                sku=103,
                fesh=1120,
                waremd5='OFF_DSBS_HID2_WHITE__Q',
                hyperid=105,
                hid=101,
                business_id=4,
                cpa=Offer.CPA_REAL,
            ),
        ]

    def test_without_uncollapse(self):
        """
        Сначала проверяем запрос без расхлопывания.
        Все оффера на месте. Среди синих офферов и DSBS оффера разыгрывается байбокс
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=105&hid=101&fesh=3&grhow=supplier&rearr-factors=market_blue_buybox_courier_delivery_priority=0;market_blue_buybox_disable_old_buybox_algo=0;market_uncollapse_supplier=0;enable_business_id=1;market_enable_buybox_by_business=0;&debug=da'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 2,
                    'results': [
                        {
                            'entity': 'offer',
                            'cpa': 'real',
                            'isFulfillment': True,
                            'debug': {
                                'buyboxDebug': {
                                    'Offers': [
                                        {'WareMd5': 'OFF_DSBS_HID1_WHITE__Q'},
                                        {'WareMd5': 'OFF2_3000_SKU1_SUP12_Q'},
                                        {'WareMd5': 'OFF2_3000_SKU1_SUP31_Q'},
                                        {'WareMd5': 'OFF2_3000_SKU1_SUP32_Q'},
                                    ]
                                }
                            },
                        },
                        {'entity': 'offer', 'cpa': NoKey('cpa'), 'cpc': NotEmpty(), 'wareId': 'OFF_3000_HID1_WHITE__Q'},
                    ],
                }
            },
        )

    def test_with_uncollapse(self):
        """
        Делаем запрос с расхлопыванием.
        Проверяем все условия задачи:
        1) CPC и CPA оффера, имеющие один business_id, не схлопываются
        2) BlueOffer и DSBS схлопывается, остается BlueOffer
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=105&hid=101&fesh=3&grhow=supplier&rearr-factors=market_uncollapse_supplier=1;enable_business_id=1;market_enable_buybox_by_business=0;&debug=da'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 2,
                    'results': [
                        {
                            'entity': 'offer',
                            'cpa': 'real',
                            'isFulfillment': True,
                            'debug': {
                                'buyboxDebug': {'WonMethod': 'SINGLE_OFFER_BEFORE_BUYBOX_FILTERS'}
                            },  # Нет байбокс-контеста
                            'wareId': 'OFF2_3000_SKU1_SUP31_Q',
                        },
                        # DSBS оффер схлопнулся и исчез из выдачи
                        # CPC оффер остался
                        {'entity': 'offer', 'cpa': NoKey('cpa'), 'cpc': NotEmpty(), 'wareId': 'OFF_3000_HID1_WHITE__Q'},
                    ],
                }
            },
        )

    def test_with_uncollapse_with_bb_group_by_business_id(self):
        """
        Делаем запрос с расхлопыванием.
        Проверяем все условия задачи:
        1) CPC и CPA оффера, имеющие один business_id, не схлопываются
        2) BlueOffer и DSBS схлопывается, остается BlueOffer
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=105&hid=101&fesh=3&grhow=supplier&rearr-factors=market_blue_buybox_disable_old_buybox_algo=0;market_uncollapse_supplier=1;enable_business_id=1;market_enable_buybox_by_business=1;&debug=da'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 2,
                    'results': [
                        {
                            'entity': 'offer',
                            'cpa': 'real',
                            'isFulfillment': True,
                            'debug': {'buyboxDebug': {'WonMethod': 'OLD_BUYBOX'}},
                            'wareId': 'OFF2_3000_SKU1_SUP31_Q',
                        },
                        # DSBS оффер схлопнулся и исчез из выдачи
                        # CPC оффер остался
                        {'entity': 'offer', 'cpa': NoKey('cpa'), 'cpc': NotEmpty(), 'wareId': 'OFF_3000_HID1_WHITE__Q'},
                    ],
                }
            },
        )

    def test_with_uncollapse_dsbs_cpc(self):
        """
        Делаем запрос с расхлопыванием.
        Проверяем все условия задачи:
        1) DSBS и CPC оффера не схлопываются
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=105&hid=101&fesh=4&grhow=supplier&rearr-factors=market_uncollapse_supplier=1;enable_business_id=1;market_enable_buybox_by_business=1;&debug=da'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 2,
                    'results': [
                        {
                            'entity': 'offer',
                            'cpa': 'real',
                            'debug': {'buyboxDebug': {'WonMethod': 'SINGLE_OFFER_BEFORE_BUYBOX_FILTERS'}},
                            'wareId': 'OFF_DSBS_HID2_WHITE__Q',
                        },
                        {'entity': 'offer', 'cpa': NoKey('cpa'), 'cpc': NotEmpty(), 'wareId': 'OFF_3000_HID2_WHITE__Q'},
                    ],
                }
            },
        )


if __name__ == '__main__':
    main()
