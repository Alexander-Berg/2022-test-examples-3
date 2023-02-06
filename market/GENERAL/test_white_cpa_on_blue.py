#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    BucketInfo,
    CreditTemplate,
    DeliveryBucket,
    DeliveryOption,
    Dimensions,
    HyperCategory,
    MarketSku,
    Model,
    NewPickupBucket,
    NewPickupOption,
    Offer,
    OfferDeliveryInfo,
    OfferDimensions,
    Outlet,
    OutletDeliveryOption,
    PickupRegionGroup,
    Region,
    RegionalDelivery,
    Shop,
    Vat,
)
from core.types.delivery import OutletType
from core.matcher import Absent, Contains, NoKey, Not, NotEmpty
from core.combinator import DeliveryStats, make_offer_id
from unittest import skip


class T(TestCase):
    """
    Проверяем что белые cpa офера адекватно пролезают в rgb=blue запросы
    """

    shop_dsbs = Shop(
        fesh=42,
        datafeed_id=4240,
        priority_region=213,
        regions=[213],
        name='Наковальни International',
        client_id=11,
        cpa=Shop.CPA_REAL,
        warehouse_id=111,
    )

    shop_dsbs_1 = Shop(
        fesh=52,
        datafeed_id=4243,
        priority_region=213,
        name='Наковальни Russia',
        client_id=21,
        cpa=Shop.CPA_REAL,
        warehouse_id=222,
    )

    shop_dsbs_2 = Shop(
        fesh=62,
        datafeed_id=4244,
        priority_region=213,
        name='Наковальни USA',
        client_id=31,
        cpa=Shop.CPA_REAL,
        warehouse_id=333,
    )

    shop_blue = Shop(
        fesh=103,
        datafeed_id=1031,
        priority_region=213,
        name='Поставщик верстаков #1',
        client_id=12,
        cpa=Shop.CPA_REAL,
        blue=Shop.BLUE_REAL,
        warehouse_id=444,
    )

    shop_blue_alternative = Shop(
        fesh=104,
        datafeed_id=1032,
        priority_region=213,
        name='Поставщик верстаков #2',
        client_id=13,
        cpa=Shop.CPA_REAL,
        blue=Shop.BLUE_REAL,
        warehouse_id=555,
    )

    shop_dsbs_buybox = Shop(
        fesh=44,
        datafeed_id=4444,
        priority_region=213,
        name='Столы России',
        client_id=14,
        cpa=Shop.CPA_REAL,
        warehouse_id=666,
    )

    msku_for_dsbs = MarketSku(title="Nokovalnya", hyperid=100, sku=101)

    # msku только с dsbs офером
    offer_dsbs = Offer(
        offerid='NoAlternative',
        title="Наковальня #100 с доставкой",
        hyperid=100,
        fesh=shop_dsbs.fesh,
        waremd5='DsbsNoAlternative____g',
        price=100000,
        sku=msku_for_dsbs.sku,
        cpa=Offer.CPA_REAL,
        delivery_buckets=[4240],
        dimensions=OfferDimensions(width=1, height=1, length=1),
        delivery_info=OfferDeliveryInfo(pickup_buckets=[BucketInfo(bucket_id=1313)]),
        credit_template_id=1,
    )

    offer_blue = BlueOffer(
        fesh=shop_blue.fesh,
        feedid=shop_blue.datafeed_id,
        offerid='Shop1_sku_22',
        vat=Vat.VAT_10,
        waremd5='Blue101______________Q',
        price=10000,
        weight=0.3,
        dimensions=OfferDimensions(length=1, width=22, height=33.3),
        delivery_buckets=[4343],
    )

    # msku с синим и dsbs офером
    msku_midxed = MarketSku(title="Verstak", hyperid=200, sku=201, blue_offers=[offer_blue])

    # оффер для участия в buybox вместе с другими синими оферами
    offer_dsbs_alternative = Offer(
        offerid='Alternative',
        cpa=Offer.CPA_REAL,
        fesh=shop_dsbs.fesh,
        feedid=shop_dsbs.datafeed_id,
        hyperid=22,
        sku=22,  # but was msku_midxed.sku,
        waremd5='Dsbs201______________g',
        delivery_buckets=[4240],
    )

    offer_blue_multi = BlueOffer(
        fesh=shop_blue.fesh,
        feedid=shop_blue.datafeed_id,
        offerid='Shop1_sku_301',
        vat=Vat.VAT_10,
        waremd5='Blue301______________Q',
        price=10000,
        delivery_buckets=[4343],
    )

    # msku с синим байбоксом и несколькими альтернативами
    msku_multioffer_blue = MarketSku(title="Stol_blue", hyperid=300, sku=301, blue_offers=[offer_blue_multi])

    offer_dsbs_multi_faster_delivery = Offer(
        offerid='Faster',
        hyperid=300,
        fesh=shop_dsbs.fesh,
        feedid=shop_dsbs.datafeed_id,
        waremd5='DsbsFaster___________g',
        price=100500,
        sku=msku_multioffer_blue.sku,
        cpa=Offer.CPA_REAL,
        delivery_buckets=[4240],
    )

    offer_dsbs_multi_cheapest = Offer(
        offerid='Cheapest',
        hyperid=300,
        fesh=shop_dsbs_1.fesh,
        feedid=shop_dsbs_1.datafeed_id,
        waremd5='DsbsCheapest_________g',
        price=100,
        sku=msku_multioffer_blue.sku,
        cpa=Offer.CPA_REAL,
        delivery_buckets=[4241],
    )

    # этот оффер дешевле, чем байбокс, но не самый дешевый,
    # поэтому его не будет в мультиоффере
    offer_dsbs_multi_cheaper = Offer(
        offerid='Cheaper',
        hyperid=300,
        fesh=shop_dsbs_2.fesh,
        feedid=shop_dsbs_2.datafeed_id,
        waremd5='DsbsCheaper__________g',
        price=200,
        sku=msku_multioffer_blue.sku,
        cpa=Offer.CPA_REAL,
        delivery_buckets=[4242],
    )

    # msku с dsbs байбоксом и несколькими альтернативами
    msku_multioffer_dsbs = MarketSku(title="Stol_dsbs", hyperid=400, sku=401)

    offer_dsbs_multi_buybox = Offer(
        offerid='BuyBox',
        hyperid=400,
        fesh=shop_dsbs_buybox.fesh,
        feedid=shop_dsbs_buybox.datafeed_id,
        waremd5='DsbsBuyBox___________g',
        price=100,
        sku=msku_multioffer_dsbs.sku,
        cpa=Offer.CPA_REAL,
        delivery_buckets=[4244],
    )
    # Этот оффер не появится в альтернативных, не смотря на более быструю доставук,
    # тк у них с байбоксом одинаковый поставщик
    offer_dsbs_multi_same_supplier = Offer(
        offerid='SameSupplier',
        hyperid=400,
        fesh=shop_dsbs_buybox.fesh,
        feedid=shop_dsbs_buybox.datafeed_id,
        waremd5='DsbsSameSupplier_____g',
        price=400,
        sku=msku_multioffer_dsbs.sku,
        cpa=Offer.CPA_REAL,
        delivery_buckets=[4245],
    )

    offer_dsbs_multi_faster_delivery_1 = Offer(
        offerid='Faster1',
        hyperid=400,
        fesh=shop_dsbs.fesh,
        feedid=shop_dsbs.datafeed_id,
        waremd5='DsbsFaster1__________g',
        price=100500,
        sku=msku_multioffer_dsbs.sku,
        cpa=Offer.CPA_REAL,
        delivery_buckets=[4240],
    )

    offer_blue_combinator = BlueOffer(
        fesh=shop_blue.fesh,
        feedid=shop_blue.datafeed_id,
        offerid='Shop1_sku_501',
        vat=Vat.VAT_10,
        waremd5='Blue501______________Q',
        price=500,
        delivery_buckets=[4343],
    )

    offer_blue_alternative = BlueOffer(
        fesh=shop_blue_alternative.fesh,
        feedid=shop_blue_alternative.datafeed_id,
        offerid='Shop1_sku_502',
        vat=Vat.VAT_10,
        waremd5='Blue502______________Q',
        price=1000,
        delivery_buckets=[4346],  # быстрее чем у байбокса
    )

    # msku с синим байбоксом и несколькими cиними и dsbs альтернативами
    msku_multioffer_all = MarketSku(
        title="Stol_all",
        hyperid=500,
        sku=501,
        blue_offers=[
            offer_blue_combinator,
            offer_blue_alternative,
        ],
    )

    # у этого dsbs доставка по скорости такая же, как у Blue502
    # но сам оффер дешевле, поэтому он должен попасть в мультиоффер
    offer_dsbs_multi_fast_like_blue = Offer(
        offerid='Faster500',
        hyperid=500,
        fesh=shop_dsbs.fesh,
        feedid=shop_dsbs.datafeed_id,
        waremd5='DsbsFaster500________g',
        price=900,
        sku=msku_multioffer_all.sku,
        cpa=Offer.CPA_REAL,
        delivery_buckets=[4240],
    )

    combinator_sku_test_data = (
        (
            offer_dsbs_multi_cheapest,
            shop_dsbs_1,
            DeliveryStats(cost=100, day_from=1, day_to=3),  # delivery option according to DeliveryBucket
        ),
        (
            offer_dsbs_multi_faster_delivery,
            shop_dsbs,
            DeliveryStats(cost=100500, day_from=1, day_to=1),  # delivery option according to DeliveryBucket
        ),
        (
            offer_dsbs_multi_cheaper,
            shop_dsbs_2,
            DeliveryStats(cost=200, day_from=1, day_to=3),  # delivery option according to DeliveryBucket
        ),
        (
            offer_blue_combinator,
            shop_blue,
            DeliveryStats(cost=500, day_from=1, day_to=2),  # delivery option according to DeliveryBucket
        ),
        (
            offer_dsbs_multi_buybox,
            shop_dsbs_buybox,
            DeliveryStats(cost=100, day_from=1, day_to=3),  # delivery option according to DeliveryBucket
        ),
    )

    @classmethod
    def prepare(cls):
        cls.settings.check_combinator_errors = True
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

        cls.index.hypertree += [
            HyperCategory(hid=1, name='Все для кузен'),
        ]

        cls.index.models += [
            Model(hyperid=100, hid=1, title='Наковальня #100'),
            Model(hyperid=200, hid=1, title='Верстак дубовый'),
            Model(hyperid=300, hid=1, title='Стол стеклянный'),
            Model(hyperid=400, hid=1, title='Стол березовый'),
        ]

        cls.index.shops += [
            T.shop_dsbs,
            T.shop_dsbs_1,
            T.shop_dsbs_2,
            T.shop_blue,
            T.shop_dsbs_buybox,
        ]

        cls.index.offers += [
            T.offer_dsbs,
            T.offer_dsbs_alternative,
            T.offer_dsbs_multi_faster_delivery,
            T.offer_dsbs_multi_cheaper,
            T.offer_dsbs_multi_cheapest,
            T.offer_dsbs_multi_buybox,
            T.offer_dsbs_multi_same_supplier,
            T.offer_dsbs_multi_faster_delivery_1,
            T.offer_dsbs_multi_fast_like_blue,
        ]

        cls.index.mskus += [
            T.msku_for_dsbs,
            T.msku_midxed,
            T.msku_multioffer_blue,
            T.msku_multioffer_dsbs,
            T.msku_multioffer_all,
        ]

        cls.index.credit_templates += [
            CreditTemplate(template_id=1, bank="MMM", url="mmm.ru", term=36, rate=5, min_price=2),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=4240,
                fesh=T.shop_dsbs.fesh,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
            DeliveryBucket(
                bucket_id=4241,
                fesh=T.shop_dsbs_1.fesh,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=3)])],
            ),
            DeliveryBucket(
                bucket_id=4242,
                fesh=T.shop_dsbs_2.fesh,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=3)])],
            ),
            DeliveryBucket(
                bucket_id=4343,
                fesh=T.shop_blue.fesh,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=2)])],
            ),
            DeliveryBucket(
                bucket_id=4244,
                fesh=T.shop_dsbs_buybox.fesh,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=3)])],
            ),
            DeliveryBucket(
                bucket_id=4245,
                fesh=T.shop_dsbs_buybox.fesh,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
            DeliveryBucket(
                bucket_id=4346,
                fesh=T.shop_blue.fesh,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=1,
                dimensions=Dimensions(width=100, height=90, length=80),
                region=213,
                delivery_option=OutletDeliveryOption(day_from=0, day_to=1, order_before=2, work_in_holiday=False),
                working_days=[i for i in range(10)],
                delivery_service_id=101,
                point_type=Outlet.FOR_PICKUP,
            )
        ]

        cls.index.new_pickup_buckets += [
            NewPickupBucket(
                bucket_id=1313,
                region_groups=[
                    PickupRegionGroup(options=[NewPickupOption(price=220, day_from=1, day_to=3)], outlets=[1])
                ],
            ),
        ]

    @classmethod
    def prepare_combinator(cls):
        for offer, shop, delivery in cls.combinator_sku_test_data:
            cls.combinator.add_offer_delivery(
                offer_ids=make_offer_id(offer, shop),
                pickup_stats=DeliveryStats(*delivery),
                outlet_types=[OutletType.FOR_PICKUP],
            )

    def test_blue_offer_from_white_shard(self):
        """Под флагом market_search_in_white_offer_shard_all_cpa_docs=1 все документы берутся из белых шардов"""

        response = self.report.request_json(
            'place=prime&rids=213&regset=0&hid=1&numdoc=30&debug=1'
            '&rearr-factors=market_search_in_white_offer_shard_all_cpa_docs=1;market_metadoc_search=no'
        )

        self.assertFragmentIn(
            response,
            {
                "debug": {
                    'report': {
                        'context': {
                            'collections': {'MODEL': NotEmpty(), 'SHOP': NotEmpty(), 'SHOP_BLUE': NoKey('SHOP_BLUE')}
                        }
                    }
                }
            },
        )

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'offer',
                            'offerColor': 'blue',
                            'cpa': 'real',
                            'debug': {'tech': {"originBase": Not(Contains("basesearch-blue"))}},
                        }
                    ]
                }
            },
        )

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'offer',
                            'offerColor': 'white',
                            'cpa': 'real',
                            'debug': {'tech': {"originBase": Not(Contains("basesearch-blue"))}},
                        }
                    ]
                }
            },
        )

        response = self.report.request_json(
            'place=prime&rids=213&regset=0&hid=1&numdoc=30&debug=1&rgb=blue&allow-collapsing=0'
            '&rearr-factors=market_search_in_white_offer_shard_all_cpa_docs=1;market_metadoc_search=no'
        )

        self.assertFragmentIn(
            response,
            {"debug": {'report': {'context': {'collections': {'SHOP': NotEmpty(), 'SHOP_BLUE': NoKey('SHOP_BLUE')}}}}},
        )

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'offer',
                            'offerColor': 'blue',
                            'cpa': 'real',
                            'debug': {'tech': {"originBase": Not(Contains("basesearch-blue"))}},
                        }
                    ]
                }
            },
        )

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'offer',
                            'offerColor': 'white',
                            'cpa': 'real',
                            'debug': {'tech': {"originBase": Not(Contains("basesearch-blue"))}},
                        }
                    ]
                }
            },
        )

    def test_prime_positive(self):
        """Проверяем что в плейсе prime в buybox DSBS офера пролезазют только если у msku нет синих оферов
        и только под флагом"""

        base_request = 'place=prime&rgb=blue&rids=213&regset=2&show-urls=cpa,promotion'

        response = self.report.request_json(
            base_request
            + '&hyperid={}&debug=1'.format(T.msku_for_dsbs.hyperid)
            + '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "product",
                "id": int(T.msku_for_dsbs.hyperid),
                "offers": {
                    "count": 1,
                    "items": [
                        {
                            "entity": "offer",
                            "model": {"id": int(T.msku_for_dsbs.hyperid)},
                            "wareId": T.offer_dsbs.ware_md5,
                            "cpa": "real",
                            "marketSku": "101",
                            "marketSkuCreator": "market",
                            "modelAwareTitles": {
                                "highlighted": [{"value": "Наковальня #100 с доставкой"}],
                                "raw": "Наковальня #100 с доставкой",
                            },
                            "urls": {
                                "cpa": NotEmpty(),
                                "direct": NotEmpty(),
                                "promotion": Absent(),  # У dsbs офферов должен отсутсвовать promotion url
                            },
                            "shop": {
                                "id": T.shop_dsbs.fesh,
                            },
                            "supplier": {
                                "id": T.shop_dsbs.fesh,
                            },
                            "realShop": {
                                "id": T.shop_dsbs.fesh,
                            },
                        }
                    ],
                },
            },
            allow_different_len=False,
        )

        # Проверяем, что promotion url не создается для dsbs,
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

        response = self.report.request_json(base_request + '&hyperid={}'.format(T.msku_midxed.hyperid))
        self.assertFragmentIn(
            response,
            {
                "entity": "product",
                "id": int(T.msku_midxed.hyperid),
                "offers": {
                    "count": 1,
                    "items": [
                        {
                            "entity": "offer",
                            "wareId": T.offer_blue.waremd5,
                        }
                    ],
                },
            },
            allow_different_len=False,
        )

    def test_prime_new_buckets(self):
        """Проверяем что новые бакеты у белых оферов не ломают синюю выдачу
        что проверяется: нет ошибок репорта когда на базовых обрабатываются белые офера с новым бакетами
        при args.set_skip_delivery_info_calculation(true)"""

        base_request = 'place=prime&rgb=blue&rids=213&regset=2&show-urls=cpa,promotion'
        for calculate_delivery in [0, 1]:

            response = self.report.request_json(
                base_request
                + '&calculate-delivery={}&hyperid={}&debug=1'.format(calculate_delivery, T.msku_for_dsbs.hyperid)
            )
            self.assertFragmentIn(
                response,
                {
                    "entity": "product",
                    "id": int(T.msku_for_dsbs.hyperid),
                    "offers": {
                        "count": 1,
                        "items": [
                            {
                                "entity": "offer",
                                "model": {"id": int(T.msku_for_dsbs.hyperid)},
                            }
                        ],
                    },
                },
            )

    def test_sku_offers_multioffer(self):
        """Проверяем работу multioffers в sku_offers с dsbs"""

        base_request = 'place=sku_offers&rgb=blue&rids=213&regset=2'

        # Сначала проверим при синем байбоксе
        response = self.report.request_json(base_request + '&market-sku=301&enable_multioffer=1')
        self.assertFragmentIn(
            response,
            {
                "offers": {
                    "items": [
                        {
                            "entity": "offer",
                            "wareId": T.offer_dsbs_multi_cheapest.waremd5,
                        }
                    ]
                },
                "additionalOffers": Absent(),
            },
            allow_different_len=False,
        )

        # Если запросить самый дешевый dsbs,
        # то в альтернативах не будет синего оффера, тк он хуже по все параметрам
        response = self.report.request_json(
            base_request + '&market-sku=301&enable_multioffer=1&offerid=' + T.offer_dsbs_multi_cheapest.ware_md5
        )
        self.assertFragmentIn(
            response,
            {
                "offers": {
                    "items": [
                        {
                            "entity": "offer",
                            "wareId": T.offer_dsbs_multi_cheapest.ware_md5,
                        }
                    ]
                },
                "additionalOffers": Absent(),
            },
            allow_different_len=False,
        )

        # Если запросить самый бастрый, то в мультиоффере останется только дешевый
        response = self.report.request_json(
            base_request + '&market-sku=301&enable_multioffer=1&offerid=' + T.offer_dsbs_multi_faster_delivery.ware_md5
        )
        self.assertFragmentIn(
            response,
            {
                "offers": {
                    "items": [
                        {
                            "entity": "offer",
                            "wareId": T.offer_dsbs_multi_faster_delivery.ware_md5,
                        }
                    ]
                },
                "additionalOffers": Absent(),
            },
            allow_different_len=False,
        )

        # Если запросить средний по цене, то будут и самый дешевый и самый быстрый
        response = self.report.request_json(
            base_request + '&market-sku=301&enable_multioffer=1&offerid=' + T.offer_dsbs_multi_cheaper.ware_md5
        )
        self.assertFragmentIn(
            response,
            {
                "offers": {
                    "items": [
                        {
                            "entity": "offer",
                            "wareId": T.offer_dsbs_multi_cheaper.ware_md5,
                        }
                    ]
                },
                "additionalOffers": Absent(),
            },
            allow_different_len=False,
        )

        # проверка, что при совпадении скорости доставки победит тот,
        # у кого другая характеристика лучше (в данном случае цена)
        response = self.report.request_json(base_request + '&market-sku=501&enable_multioffer=1')
        self.assertFragmentIn(
            response,
            {
                "offers": {
                    "items": [
                        {
                            "entity": "offer",
                            "wareId": T.offer_blue_combinator.waremd5,
                        }
                    ]
                },
                "additionalOffers": Absent(),
            },
            allow_different_len=False,
        )

        # Теперь для dsbs байбокса
        response = self.report.request_json(base_request + '&market-sku=401&enable_multioffer=1')
        self.assertFragmentIn(
            response,
            {
                "offers": {
                    "items": [
                        {
                            "entity": "offer",
                            "wareId": T.offer_dsbs_multi_buybox.ware_md5,
                        }
                    ]
                },
                "additionalOffers": Absent(),
            },
            allow_different_len=False,
        )

        # Без enable_multioffer не должно быть альтернатив
        response = self.report.request_json(base_request + '&market-sku=401')
        self.assertFragmentIn(
            response,
            {
                "offers": {
                    "items": [
                        {
                            "entity": "offer",
                            "wareId": T.offer_dsbs_multi_buybox.ware_md5,
                        }
                    ]
                },
                "additionalOffers": Absent(),
            },
            allow_different_len=False,
        )

    def test_offerinfo_positive(self):
        base_request = 'place=offerinfo&rgb=blue&rids=213&regset=2&show-urls=cpa'
        response = self.report.request_json(base_request + '&offerid={}'.format(T.offer_dsbs.ware_md5))
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "wareId": T.offer_dsbs.ware_md5,
                "model": {"id": int(T.msku_for_dsbs.hyperid)},
                "modelAwareTitles": {
                    "highlighted": [{"value": "Наковальня #100 с доставкой"}],
                    "raw": "Наковальня #100 с доставкой",
                },
            },
        )

    @skip('white credits will be deleted soon')
    def test_credit_options(self):
        """Проверяем что кредитные опции не вылезают в фильтры"""
        base_request = 'place=prime&rids=213&regset=2&show-urls=cpa'
        flags = '&rearr-factors=market_calculate_credits=1;market_return_credits=1'
        response = self.report.request_json(
            base_request + flags + '&rgb=blue&hyperid={}&debug=1'.format(T.msku_for_dsbs.hyperid)
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "product",
                "id": int(T.msku_for_dsbs.hyperid),
                "offers": {
                    "count": 1,
                    "items": [
                        {
                            "entity": "offer",
                            "wareId": T.offer_dsbs.ware_md5,
                        }
                    ],
                },
            },
        )
        self.assertFragmentNotIn(
            response,
            {"filters": [{"id": "credit-type"}]},
        )

        # при этом в белом запросе тот же офер пролезает, и кредиты есть
        response = self.report.request_json(
            base_request + flags + '&hyperid={}&debug=1'.format(T.msku_for_dsbs.hyperid)
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "wareId": T.offer_dsbs.ware_md5,
            },
        )
        self.assertFragmentIn(
            response,
            {"filters": [{"id": "credit-type"}]},
        )

    @skip('Некорректный market-sku у offer_dsbs_alternative')
    def test_combinator_sku_offers(self):
        """Проверка того, что в комбинатор не ходим за доставкой для белых офферов"""
        base_request = 'place=sku_offers&rgb=blue&rids=213&regset=2&debug=1' + '&offerid={}&market-sku={}'.format(
            T.offer_dsbs_alternative.ware_md5, T.msku_midxed.sku
        )
        response = self.report.request_json(base_request)
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "wareId": T.offer_dsbs_alternative.ware_md5,
            },
        )
        self.error_log.not_expect(code=3732)  # combinator request failure due to DSBS offer having no size


if __name__ == '__main__':
    main()
