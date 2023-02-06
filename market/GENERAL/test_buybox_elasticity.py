#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    DynamicDeliveryRestriction,
    DynamicWarehouseDelivery,
    DynamicWarehouseLink,
    Elasticity,
    Model,
    MonetOfferEntry,
    MonetSkuEntry,
    Offer,
    Region,
    Shop,
    WebErfEntry,
    WebErfFeatures,
    WebHerfEntry,
    WebHerfFeatures,
)
from core.types.sku import MarketSku, BlueOffer
from core.matcher import NoKey, Not, Equal


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
            Shop(fesh=4801, business_fesh=3, name="dsbs магазин Пети", regions=[213], cpa=Shop.CPA_REAL),
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
                    BlueOffer(waremd5='BH8EPLtKmdLQhLUasgaOnA', price=20, offerid='shop_sku_eq_ref_min', feedid=2)
                ],
            ),
            MarketSku(
                title='blue market sku3',
                hyperid=1,
                sku=3,
                ref_min_price=200,
                buybox_elasticity=None,
                blue_offers=[
                    BlueOffer(
                        waremd5='KXGI8T3GP_pqjgdd7HfoHQ',
                        price=10,
                        offerid='shop_sku_lt_ref_min',
                        feedid=31,
                        randx=3100,
                    )
                ],
            ),
            MarketSku(
                title='blue market sku4',
                hyperid=1,
                sku=4,
                ref_min_price=200,
                blue_offers=[
                    BlueOffer(
                        buybox_elasticity=[
                            Elasticity(price_variant=100, demand_mean=200),
                            Elasticity(price_variant=200, demand_mean=80),
                            Elasticity(price_variant=300, demand_mean=10),
                        ],
                        price=201,
                        feedid=31,
                        randx=3100,
                        waremd5='cK-_Ilm0RJ2K4UKeBU3xsQ',
                    ),
                    BlueOffer(
                        buybox_elasticity=[
                            Elasticity(price_variant=100, demand_mean=200),
                            Elasticity(price_variant=200, demand_mean=83),
                            Elasticity(price_variant=300, demand_mean=10),
                        ],
                        price=200,
                        feedid=31,
                        waremd5='Ti7Ou_r-J6fADgRi-tCHOg',
                    ),
                    BlueOffer(buybox_elasticity=False, price=199, feedid=31, waremd5='6e2crVbJJlImOVN1WFK-2w'),
                ],
            ),
            MarketSku(
                title='msku to test default elasticities',
                hyperid=1,
                sku=5,
                ref_min_price=100,
                blue_offers=[
                    BlueOffer(
                        title='blue offer msku 5 normal els',
                        buybox_elasticity=[
                            Elasticity(price_variant=198, demand_mean=200),
                            Elasticity(price_variant=200, demand_mean=198),
                        ],
                        price=199,
                        feedid=31,
                        waremd5='cK-_Ilm0RJ2K4UKeBU5xsQ',
                    ),
                    BlueOffer(
                        title='blue offer msku 5 left border els',
                        buybox_elasticity=[
                            Elasticity(price_variant=198, demand_mean=200),
                            Elasticity(price_variant=200, demand_mean=198),
                        ],
                        price=197,
                        feedid=31,
                        waremd5='Ti7Ou_r-J5fADgRi-tCHOg',
                    ),
                    BlueOffer(
                        title='blue offer msku 5 right border els',
                        buybox_elasticity=[
                            Elasticity(price_variant=198, demand_mean=200),
                            Elasticity(price_variant=200, demand_mean=198),
                        ],
                        price=201,
                        feedid=31,
                        waremd5='6e5crVbJJlImOVN1WFK-2w',
                    ),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                title="market DSBS Offer",
                hid=1,
                hyperid=1,
                price=198,
                fesh=4801,
                business_id=3,
                sku=4,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-lbqQ',
            )
        ]

        cls.index.web_erf_features += [
            WebErfEntry(
                url='http://pokupki.market.yandex.ru/product/1?offerid=EpnWVxDQxj4wg7vVI1ElnA',
                features=WebErfFeatures(title_comm=1, f_title_idf_sum=0.5),
            ),
        ]

        cls.index.web_herf_features += [
            WebHerfEntry(host='http://pokupki.market.yandex.ru', features=WebHerfFeatures(owner_enough_clicked=1)),
        ]

        cls.index.monet_offer_entries += [
            MonetOfferEntry('http://pokupki.market.yandex.ru/product/1?offerid=EpnWVxDQxj4wg7vVI1ElnA', elasticity=0.52)
        ]

        cls.index.monet_sku_entries += [MonetSkuEntry(sku='1', predict_items_today=1.5, predict_items_in_5d=1.25)]

        cls.settings.generate_old_erf = False  # explicitly, is false by default

        cls.index.models += [
            Model(hyperid=1, hid=1, title='blue and green model'),
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

    def test_print_doc(self):
        """Проверяем, что print_doc выводит эластичность"""
        response = self.report.request_json('place=print_doc&market-sku=1')
        self.assertFragmentIn(
            response,
            {
                "elasticity": "[{ PriceVariant: 100, DemandMean: 200 }, { PriceVariant: 200, DemandMean: 80 }, { PriceVariant: 300, DemandMean: 10 }]"
            },
        )

    def test_offerinfo(self):
        """Проверяем, place=prime."""
        response = self.report.request_json('place=prime&text=market&pp=1&market-sku=1')
        self.assertFragmentIn(
            response,
            {
                "elasticity": [
                    {"demandMean": "200", "priceVariant": {"currency": "RUR", "value": "100"}},
                    {"demandMean": "80", "priceVariant": {"currency": "RUR", "value": "200"}},
                    {"demandMean": "10", "priceVariant": {"currency": "RUR", "value": "300"}},
                ]
            },
        )

    def test_default_elasticity(self):
        """Проверяем, эластичность по умолчанию"""
        response = self.report.request_json('place=prime&rgb=blue&text=market&pp=1&market-sku=3')
        self.assertFragmentIn(
            response,
            {
                "elasticity": [
                    {"demandMean": "100", "priceVariant": {"currency": "RUR", "value": "100"}},
                    {"demandMean": "30", "priceVariant": {"currency": "RUR", "value": "200"}},
                    {"demandMean": "20", "priceVariant": {"currency": "RUR", "value": "300"}},
                ]
            },
        )

    def test_empty_elasticity(self):
        """Проверяем, когда эластичность пустая"""
        response = self.report.request_json('place=prime&text=market&pp=1&&market-sku=2')
        self.assertFragmentNotIn(response, {"elasticity": []})

    def test_msku_elasticity(self):
        """
        https://st.yandex-team.ru/MARKETOUT-38635
        Если у оффера нет эластичности, то она будет взята с соседнего синего оффера
        У оффера '6e2crVbJJlImOVN1WFK-2w' эластичности нет, но в байбоксе была учтена эластичность с оффера 'cK-_Ilm0RJ2K4UKeBU3xsQ'
        При этом у оффера 'Ti7Ou_r-J6fADgRi-tCHOg' используется его собственная эластичность
        """
        response = self.report.request_json(
            'place=prime&text=market&pp=1&rgb=blue&market-sku=4&debug=da&yandexuid=1001&rearr-factors=market_blue_buybox_max_gmv_rel=1.4;market_blue_buybox_disable_dsbs_pessimisation=1'
            ';market_nordstream_buybox=0'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': 'sgf1xWYFqdGiLh4TT-lbqQ',
                'elasticity': NoKey("elasticity"),
                'debug': {
                    'buyboxDebug': {
                        'WonMethod': 'WON_BY_EXCHANGE',
                        'Offers': [
                            {'WareMd5': 'sgf1xWYFqdGiLh4TT-lbqQ', 'PredictedElasticity': {'Value': 82.4}},
                            {'WareMd5': 'cK-_Ilm0RJ2K4UKeBU3xsQ', 'PredictedElasticity': {'Value': 79.3}},
                            {'WareMd5': 'Ti7Ou_r-J6fADgRi-tCHOg', 'PredictedElasticity': {'Value': 83}},
                            {'WareMd5': '6e2crVbJJlImOVN1WFK-2w', 'PredictedElasticity': {'Value': 81.2}},
                        ],
                    }
                },
            },
        )
        self.assertFragmentIn(response, "Elasticity from MSKU: sgf1xWYFqdGiLh4TT-lbqQ")
        self.assertFragmentIn(response, "Elasticity from MSKU: 6e2crVbJJlImOVN1WFK-2w")

    def test_always_default_elasticity_flag_normal(self):
        """
        https://st.yandex-team.ru/MARKETOUT-43658
        Проверяю флаг market_blue_buybox_always_use_default_elasticity, который насильно включает дефолтную эластичность
        Проверка, что NORMAL эластичности становятся DEFAULT
        """
        base_request = 'place=productoffers&market-sku=4&pp=6&debug=da&rearr-factors=market_blue_buybox_always_use_default_elasticity='
        els_values_for_offers = {  # Эластичности для офферов
            'cK-_Ilm0RJ2K4UKeBU3xsQ': {
                '0': 79.3,  # Значение без флага (NORMAL)
                '1': 0.964753,  # Значение с флагом (DEFAULT-эластичность)
            },
            'Ti7Ou_r-J6fADgRi-tCHOg': {
                '0': 83,
                '1': 0.982249,
            },
            '6e2crVbJJlImOVN1WFK-2w': {
                '0': 81.2,
                '1': 1,
            },
        }
        for flag_value, els_type in zip(("0", "1"), ("NORMAL", "DEFAULT")):
            response = self.report.request_json(base_request + flag_value)
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'debug': {
                                'buyboxDebug': {
                                    'Offers': [
                                        {
                                            'WareMd5': 'sgf1xWYFqdGiLh4TT-lbqQ',  # This offer has no elasticity in index
                                            'PredictedElasticity': {
                                                'Type': 'DEFAULT',
                                                'Value': 1,
                                            },
                                        },
                                    ],
                                },
                            },
                        },
                        {
                            'debug': {
                                'buyboxDebug': {
                                    'Offers': [
                                        {
                                            'WareMd5': curr_ware_md5,
                                            'PredictedElasticity': {
                                                'Type': els_type,
                                                'Value': els_values_for_offers[curr_ware_md5][flag_value],
                                            },
                                        }
                                        for curr_ware_md5 in [
                                            'cK-_Ilm0RJ2K4UKeBU3xsQ',
                                            'Ti7Ou_r-J6fADgRi-tCHOg',
                                            '6e2crVbJJlImOVN1WFK-2w',
                                        ]
                                    ],
                                },
                            },
                        },
                    ]
                },
            )

    def test_always_default_elasticity_flag_borders(self):
        """
        https://st.yandex-team.ru/MARKETOUT-43658
        Проверяю флаг market_blue_buybox_always_use_default_elasticity, который насильно включает дефолтную эластичность
        Проверка, что эластичности всех типов (LEFT_BORDER, RIGHT_BORDER) становятся DEFAULT
        """
        base_request = 'place=productoffers&market-sku=5&pp=6&debug=da&rearr-factors=market_blue_buybox_always_use_default_elasticity='

        response = self.report.request_json(base_request + '0')  # without flag
        self.assertFragmentIn(
            response,
            {
                'buyboxDebug': {
                    'Offers': [
                        {
                            'WareMd5': 'cK-_Ilm0RJ2K4UKeBU5xsQ',
                            'PredictedElasticity': {
                                'Type': 'NORMAL',
                                'Value': 199,
                            },
                        },
                        {
                            'WareMd5': 'Ti7Ou_r-J5fADgRi-tCHOg',
                            'PredictedElasticity': {
                                'Type': 'LEFT_BORDER',
                                'Value': 200,
                            },
                        },
                        {
                            'WareMd5': '6e5crVbJJlImOVN1WFK-2w',
                            'PredictedElasticity': {
                                'Type': 'RIGHT_BORDER',
                                'Value': 0,
                            },
                        },
                    ],
                },
            },
        )

        response = self.report.request_json(base_request + '1')  # with flag
        self.assertFragmentIn(
            response,
            {
                'buyboxDebug': {
                    'Offers': [
                        {
                            'WareMd5': 'cK-_Ilm0RJ2K4UKeBU5xsQ',
                            'PredictedElasticity': {
                                'Type': 'DEFAULT',
                                'Value': 0.964401,
                            },
                        },
                        {
                            'WareMd5': 'Ti7Ou_r-J5fADgRi-tCHOg',
                            'PredictedElasticity': {
                                'Type': 'DEFAULT',
                                'Value': 1,
                            },
                        },
                        {
                            'WareMd5': '6e5crVbJJlImOVN1WFK-2w',
                            'PredictedElasticity': {
                                'Type': 'DEFAULT',
                                'Value': 0.929827,
                            },
                        },
                    ],
                },
            },
        )

    def test_old_buybox_enabling(self):
        """
        Проверяю, что флаг market_blue_buybox_by_gmv_ue=0 включает старый байбокс везде (и что другие флаги для включения старого байбокса не требуются).
        Логика такая - флаг market_blue_buybox_by_gmv_ue=0 "в начале" байбокса ставит EnableFlag=False, из-за чего включается старый байбокс
        """
        # Тут важно проверять не только для ДО, но и для топ 6
        base_request = 'place=productoffers&market-sku=4&pp=6&debug=da&rids=213&offers-set=defaultList,listCpa&rearr-factors=market_blue_buybox_by_gmv_ue='
        won_methods_for_new_buybox = ['WON_BY_EXCHANGE', 'SINGLE_OFFER_BEFORE_BUYBOX_FILTERS', 'WON_BY_EXCHANGE']
        for flag_value in [0, 1]:
            response = self.report.request_json(base_request + str(flag_value))
            # Проверяем, что везде выбрался правильный WonMethod и EnableFlag
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': won_methods_for_new_buybox[i] if flag_value else 'OLD_BUYBOX',
                                    'Settings': {'EnableFlag': True if flag_value else False},
                                },
                            },
                        }
                        for i in range(3)  # Вообще результат содержит 4 элемента, но один из них - regionalDelimeter
                    ],
                },
            )
            # Проверяем, что нигде не выбрался неверный EnableFlag
            self.assertFragmentNotIn(
                response,
                {
                    'Settings': {'EnableFlag': False if flag_value else True},
                },
            )

    def test_old_buybox_enabling_in_prime(self):
        """
        Проверяю, что флаг market_blue_buybox_by_gmv_ue=0 включает старый байбокс везде (и что другие флаги для включения старого байбокса не требуются).
        Проверяю для прайма
        Логика такая - флаг market_blue_buybox_by_gmv_ue=0 "в начале" байбокса ставит EnableFlag=False, из-за чего включается старый байбокс
        """
        base_request = 'place=prime&text=sku4&pp=7&debug=da&rids=213&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=market_blue_buybox_by_gmv_ue='
        for flag_value in [0, 1]:
            response = self.report.request_json(base_request + str(flag_value))
            # Проверяем, что везде выбрался правильный WonMethod и EnableFlag
            self.assertFragmentIn(
                response,
                {
                    'offers': {
                        'items': [
                            {
                                'debug': {
                                    'buyboxDebug': {
                                        'WonMethod': Not(Equal('OLD_BUYBOX')) if flag_value else 'OLD_BUYBOX',
                                        'Settings': {'EnableFlag': True if flag_value else False},
                                    },
                                },
                            },
                        ],
                    }
                },
            )
            # Проверяем, что нигде не выбрался неверный EnableFlag
            self.assertFragmentNotIn(
                response,
                {
                    'Settings': {'EnableFlag': False if flag_value else True},
                },
            )


if __name__ == '__main__':
    main()
