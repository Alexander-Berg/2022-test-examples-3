#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    DeliveryBucket,
    DeliveryOption,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    HyperCategory,
    MarketSku,
    Model,
    NavCategory,
    Offer,
    Region,
    RegionalDelivery,
    Shop,
    Vat,
)
from core.types.autogen import Const
from core.matcher import Absent, Contains, GreaterEq, NotEmpty


class T(TestCase):
    """
    Проверяем что белые cpa офера без msku пролезают в rgb=blue запросы под флагом market_enable_dsbs_without_msku
    """

    shop_dsbs = Shop(
        fesh=42,
        datafeed_id=4240,
        priority_region=213,
        name='Мечи и Луки',
        client_id=11,
        cpa=Shop.CPA_REAL,
    )

    shop_dsbs_1 = Shop(
        fesh=43,
        datafeed_id=4242,
        priority_region=213,
        name='Мечи и Луки 1',
        client_id=12,
        cpa=Shop.CPA_REAL,
    )

    shop_blue = Shop(
        fesh=44,
        datafeed_id=3232,
        priority_region=213,
        name='Синие Мечи и Луки',
        client_id=13,
        cpa=Shop.CPA_REAL,
        blue=Shop.BLUE_REAL,
    )

    offer_dsbs = Offer(
        title="Лук с доставкой",
        hyperid=100,
        fesh=shop_dsbs.fesh,
        waremd5='DsbsWithoutMsku______g',
        price=100500,
        cpa=Offer.CPA_REAL,
        delivery_buckets=[4240],
    )

    offer_dsbs_same_model_id_0 = Offer(
        title="Меч #0 с доставкой",
        hyperid=200,
        fesh=shop_dsbs.fesh,
        waremd5='DsbsWithoutMsku0_____g',
        price=100,
        cpa=Offer.CPA_REAL,
        delivery_buckets=[4240],
    )

    offer_dsbs_same_model_id_1 = Offer(
        title="Меч #1 с доставкой",
        hyperid=200,
        fesh=shop_dsbs_1.fesh,
        waremd5='DsbsWithoutMsku1_____g',
        price=110,
        cpa=Offer.CPA_REAL,
        delivery_buckets=[4242],
    )

    offer_dsbs_outside_moscow = Offer(
        title="Лук с доставкой в замкадье",
        hyperid=100,
        fesh=shop_dsbs.fesh,
        waremd5='DsbsWithoutMskuNoMoscg',
        price=100500,
        cpa=Offer.CPA_REAL,
        delivery_buckets=[3131],
    )

    offer_blue = BlueOffer(
        fesh=shop_blue.fesh,
        feedid=shop_blue.datafeed_id,
        offerid='ShopBlue_sku_',
        vat=Vat.VAT_10,
        waremd5='Blue101______________Q',
        price=400,
        delivery_buckets=[3232],
    )

    msku_with_one_blue_offer = MarketSku(title="Красный Складной Лук", hyperid=300, sku=101, blue_offers=[offer_blue])

    offer_dsbs_300 = Offer(
        title="Складной Лук с доставкой",
        hyperid=300,
        fesh=shop_dsbs.fesh,
        waremd5='DsbsWithoutMsku300___g',
        price=100,
        cpa=Offer.CPA_REAL,
        delivery_buckets=[4240],
    )

    msku_with_one_dsbs_offer = MarketSku(title="Синий Железный Меч", hyperid=400, sku=201)

    offer_dsbs_with_msku = Offer(
        title="Синий Железный Меч с доставкой",
        hyperid=400,
        fesh=shop_dsbs.fesh,
        waremd5='DsbsWithMsku400______g',
        price=300,
        sku=msku_with_one_dsbs_offer.sku,
        cpa=Offer.CPA_REAL,
        delivery_buckets=[4242],
    )

    offer_dsbs_400 = Offer(
        title="Железный Меч с доставкой",
        hyperid=400,
        fesh=shop_dsbs_1.fesh,
        waremd5='DsbsWithoutMsku400___g',
        price=200,
        cpa=Offer.CPA_REAL,
        delivery_buckets=[4240],
    )

    offer_blue_with_hid = BlueOffer(
        title='big bow blue',
        fesh=shop_blue.fesh,
        feedid=shop_blue.datafeed_id,
        hid=50,
        vat=Vat.VAT_10,
        waremd5='Blue301______________Q',
        price=400,
        delivery_buckets=[3232],
    )

    msku_one_blue_offer_with_hid = MarketSku(
        title="Красный Большой Лук", hyperid=500, sku=301, blue_offers=[offer_blue_with_hid]
    )

    offer_dsbs_with_hid = Offer(
        title='big bow dsbs',
        hyperid=500,
        hid=50,
        fesh=shop_dsbs.fesh,
        waremd5='DsbsWithoutMskuHid___g',
        price=100,
        cpa=Offer.CPA_REAL,
        delivery_buckets=[4240],
    )

    offer_dsbs_no_msku_no_model1 = Offer(
        title='dsbs cheburek',
        hid=50,
        fesh=shop_dsbs.fesh,
        waremd5='DsbsNoMskuNoModel1___g',
        price=100,
        cpa=Offer.CPA_REAL,
        delivery_buckets=[4240],
    )

    offer_dsbs_no_msku_no_model2 = Offer(
        title='dsbs varenic',
        hid=50,
        fesh=shop_dsbs.fesh,
        waremd5='DsbsNoMskuNoModel2___g',
        price=100,
        cpa=Offer.CPA_REAL,
        delivery_buckets=[4240],
    )

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.regiontree += [
            Region(
                rid=1,
                name='Московская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=213, name='Москва'),
                    Region(rid=312, name='Точно не Москва'),
                ],
            )
        ]

        cls.index.hypertree += [
            HyperCategory(hid=1, name='Все мечи'),
        ]

        cls.index.models += [
            Model(hyperid=100, hid=10, title='Лук'),
            Model(hyperid=200, hid=20, title='Меч'),
            Model(hyperid=300, hid=30, title='Складной Лук'),
            Model(hyperid=400, hid=40, title='Меч железный'),
            Model(hyperid=500, hid=50, title='Большой Лук'),
        ]

        cls.index.shops += [
            T.shop_dsbs,
            T.shop_dsbs_1,
        ]

        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
        ]

        cls.index.offers += [
            T.offer_dsbs,
            T.offer_dsbs_same_model_id_0,
            T.offer_dsbs_same_model_id_1,
            T.offer_dsbs_outside_moscow,
            T.offer_dsbs_300,
            T.offer_dsbs_with_msku,
            T.offer_dsbs_400,
            T.offer_dsbs_with_hid,
            T.offer_dsbs_no_msku_no_model1,
            T.offer_dsbs_no_msku_no_model2,
        ]

        cls.index.mskus += [
            T.msku_with_one_blue_offer,
            T.msku_with_one_dsbs_offer,
            T.msku_one_blue_offer_with_hid,
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=4240,
                fesh=T.shop_dsbs.fesh,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
            DeliveryBucket(
                bucket_id=4242,
                fesh=T.shop_dsbs_1.fesh,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=3)])],
            ),
            DeliveryBucket(
                bucket_id=3232,
                fesh=T.shop_blue.fesh,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=3)])],
            ),
            DeliveryBucket(
                bucket_id=3131,
                fesh=T.shop_dsbs.fesh,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=312, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
        ]

        cls.index.navtree = [NavCategory(hid=50, nid=51, name='Bows And Swords')]

    def test_prime_positive(self):
        """
        Проверяем что в плейсе prime dsbs оффера без msku пролезают в выдачу как офферы
        """

        base_request = 'place=prime&rgb=blue&rids=213&regset=2&show-urls=cpa,promotion'
        flags = '&rearr-factors=market_nordstream=0'

        def dsbs_offer_entity(wareId, shop_fesh, model_id=None, msku=Absent()):
            return {
                "entity": "offer",
                "model": {"id": int(model_id)} if model_id else Absent(),
                "offerColor": "white",
                "wareId": wareId,
                "cpa": "real",
                "marketSku": msku,
                "marketSkuCreator": "market",
                "modelAwareTitles": NotEmpty(),
                "urls": {
                    "cpa": NotEmpty(),
                    "direct": NotEmpty(),
                    "promotion": Absent(),  # У dsbs офферов должен отсутсвовать promotion url
                },
                "shop": {
                    "id": shop_fesh,
                },
                "supplier": {
                    "id": shop_fesh,
                },
                "realShop": {
                    "id": shop_fesh,
                },
            }

        # Одиночный dsbs, на выдаче один оффер
        response = self.report.request_json(base_request + flags + '&hyperid={}&debug=1'.format(T.offer_dsbs.hyperid))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    dsbs_offer_entity(T.offer_dsbs.ware_md5, T.shop_dsbs.fesh),
                ]
            },
            allow_different_len=False,
        )

        # Проверяем, что promotion url не создается для dsbs без msku,
        # и это логируется
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'report': {
                        'logicTrace': [
                            Contains('Promotion url is unavailable for dsbs on blue market'),
                        ],
                    },
                },
            },
        )

        # Запрос для hyperid, где два dsbs оффера без msku
        # На выдаче два оффера
        response = self.report.request_json(
            base_request + flags + '&hyperid={}'.format(T.offer_dsbs_same_model_id_0.hyperid)
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    dsbs_offer_entity(T.offer_dsbs_same_model_id_0.ware_md5, T.shop_dsbs.fesh),
                    dsbs_offer_entity(T.offer_dsbs_same_model_id_1.ware_md5, T.shop_dsbs_1.fesh),
                ]
            },
            allow_different_len=False,
        )

        # Запрос для hyperid, где есть msku с синим оффером и dsbs без msku
        # На выдаче dsbs оффер и product с синим оффером
        response = self.report.request_json(base_request + flags + '&hyperid={}'.format(T.offer_dsbs_300.hyperid))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": int(T.msku_with_one_blue_offer.hyperid),
                        "offers": {
                            "count": 1,
                            "items": [
                                {
                                    "entity": "offer",
                                    "wareId": T.offer_blue.waremd5,
                                    "urls": {
                                        "cpa": NotEmpty(),
                                        "direct": NotEmpty(),
                                        "promotion": NotEmpty(),
                                    },
                                }
                            ],
                        },
                    },
                    dsbs_offer_entity(T.offer_dsbs_300.ware_md5, T.shop_dsbs.fesh),
                ]
            },
            allow_different_len=False,
        )

        # Запрос для hyperid, где есть msku с dsbs и dsbs без msku
        # На выдаче dsbs оффер без msku и product с dsbs оффером
        response = self.report.request_json(base_request + flags + '&hyperid={}'.format(T.offer_dsbs_400.hyperid))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": int(T.msku_with_one_dsbs_offer.hyperid),
                        "offers": {
                            "count": 1,
                            "items": [
                                dsbs_offer_entity(
                                    T.offer_dsbs_with_msku.ware_md5,
                                    T.shop_dsbs.fesh,
                                    T.offer_dsbs_with_msku.hyperid,
                                    msku=T.msku_with_one_dsbs_offer.sku,
                                )
                            ],
                        },
                    },
                    dsbs_offer_entity(T.offer_dsbs_400.ware_md5, T.shop_dsbs_1.fesh),
                ]
            },
            allow_different_len=False,
        )

        requests = [
            '&text=big+bow&hid={}&nid=51'.format(T.offer_dsbs_with_hid.category),  # с hid-ом и nid-ом
            '&text=big+bow&hid={}'.format(T.offer_dsbs_with_hid.category),  # только с hid-ом
            '&text=big+bow&hid={}&nid=51'.format(T.offer_dsbs_with_hid.category),  # только с nid-ом
        ]
        for req in requests:
            response = self.report.request_json(base_request + flags + req)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "product",
                            "id": int(T.msku_one_blue_offer_with_hid.hyperid),
                            "offers": {
                                "count": 1,
                                "items": [
                                    {
                                        "entity": "offer",
                                        "wareId": T.offer_blue_with_hid.waremd5,
                                        "urls": {
                                            "cpa": NotEmpty(),
                                            "direct": NotEmpty(),
                                            "promotion": NotEmpty(),
                                        },
                                    }
                                ],
                            },
                        },
                        dsbs_offer_entity(T.offer_dsbs_with_hid.ware_md5, T.shop_dsbs.fesh),
                    ]
                },
                allow_different_len=False,
            )

    def test_prime_delivery_positive(self):
        """Проверяем что мы не показываем офера без msku в регионах где нет доставки"""

        base_request = 'place=prime&rgb=blue&regset=2&show-urls=cpa,promotion&hid=10'

        response = self.report.request_json(base_request + '&rids=213')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": T.offer_dsbs.ware_md5,
                    }
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(base_request + '&rids=312')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": T.offer_dsbs_outside_moscow.ware_md5,
                    }
                ]
            },
            allow_different_len=True,
        )

    def test_white_dsts(self):
        """Проверяем что на белом dsbs офер без msku но с model сохроняет блок model"""
        base_request = 'place=prime&rgb=white&rids=213&regset=2'

        # Одиночный dsbs, на выдаче один оффер
        response = self.report.request_json(base_request + '&hyperid={}&debug=1'.format(T.offer_dsbs.hyperid))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "model": {"id": int(T.offer_dsbs.hyperid)},
                        "offerColor": "white",
                        "wareId": T.offer_dsbs.ware_md5,
                        "cpa": "real",
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_prime_no_msku_no_model_positive(self):
        base_request = 'place=prime&rgb=blue&rids=213&regset=2&show-urls=cpa,promotion'
        flags = '&rearr-factors=market_nordstream=0'
        response = self.report.request_json(
            base_request + flags + '&hid={}'.format(T.offer_dsbs_no_msku_no_model1.category)
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": GreaterEq(Const.VMID_START),
                        "offers": {
                            "count": 1,
                            "items": [
                                {
                                    "offerColor": "white",
                                    "wareId": T.offer_dsbs_no_msku_no_model1.ware_md5,
                                    "cpa": "real",
                                    "marketSku": GreaterEq(Const.VMID_START),
                                }
                            ],
                        },
                    },
                    {
                        "entity": "product",
                        "id": GreaterEq(Const.VMID_START),
                        "offers": {
                            "count": 1,
                            "items": [
                                {
                                    "offerColor": "white",
                                    "wareId": T.offer_dsbs_no_msku_no_model2.ware_md5,
                                    "cpa": "real",
                                    "marketSku": GreaterEq(Const.VMID_START),
                                }
                            ],
                        },
                    },
                    {
                        "entity": "offer",
                        "model": Absent(),
                        "offerColor": "white",
                        "wareId": T.offer_dsbs_with_hid.ware_md5,
                        "cpa": "real",
                        "marketSku": Absent(),
                    },
                    {
                        "entity": "product",
                        "id": 500,
                        "offers": {
                            "count": 1,
                            "cutPriceCount": 0,
                            "items": [
                                {
                                    "entity": "offer",
                                    "model": {"id": 500},
                                    "offerColor": "blue",
                                    "wareId": T.offer_blue_with_hid.waremd5,
                                }
                            ],
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_search_literals_for_white_cpa_docs(self):
        """Проверяем какие литералы используются в запросах на базовых
        синие и белые с мску и без: b + w_cpa"""

        base_request = 'place=prime&rgb=blue&rids=213&regset=2&debug=da&hyperid=400'

        def collection_text(condition):
            return {'debug': {'report': {'context': {'collections': {'SHOP': {'text': [condition]}}}}}}

        response = self.report.request_json(base_request)
        self.assertFragmentIn(response, collection_text(Contains('(blue_doctype:"b" | blue_doctype:"w_cpa")')))
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'product',
                            'offers': {'items': [{'offerColor': 'white', 'cpa': 'real', 'marketSku': NotEmpty()}]},
                        },
                        {'entity': 'offer', 'offerColor': 'white', 'cpa': 'real', 'marketSku': Absent()},
                    ]
                }
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
