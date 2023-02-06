#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    Offer,
    Shop,
    OfferDimensions,
)

from core.types.express_partners import EatsWarehousesEncoder
from core.logs import ErrorCodes
from core.matcher import Absent, EmptyList
from core.types.hypercategory import (
    EATS_CATEG_ID,
    CategoryStreamRecord,
    Stream,
)

from market.pylibrary import slug

MODEL_ID_1 = 1
CATEGORY_FOOD = EATS_CATEG_ID

CATEGORY_NON_FOOD = 15
MODEL_ID_NON_FOOD = 3
MSK_RIDS = 213

PEPSI_BUSINESS = 15
PRINGLES_BUSINESS = 20
NON_FOOD_BUSINESS = 25
UNUSED_BUSINESS = 26

PEPSI_SKU = '25'

# Shops
PEPSI_SHOP_172 = Shop(
    fesh=1,
    datafeed_id=1,
    business_fesh=PEPSI_BUSINESS,
    priority_region=MSK_RIDS,
    regions=[MSK_RIDS],
    name="Поставщик пепси со 172 склада",
    cpa=Shop.CPA_REAL,
    warehouse_id=172,
    is_eats=True,
)

PEPSI_SHOP_147 = Shop(
    fesh=2,
    datafeed_id=2,
    business_fesh=PEPSI_BUSINESS,
    priority_region=MSK_RIDS,
    regions=[MSK_RIDS],
    name="Поставщик пепси со 147 склада",
    cpa=Shop.CPA_REAL,
    warehouse_id=147,
    is_eats=True,
)

PRINGLES_SHOP_155 = Shop(
    fesh=3,
    datafeed_id=3,
    business_fesh=PRINGLES_BUSINESS,
    priority_region=MSK_RIDS,
    regions=[MSK_RIDS],
    name="Поставщик принглс со 155 склада",
    cpa=Shop.CPA_REAL,
    warehouse_id=155,
    is_eats=True,
)

PRINGLES_SHOP_170 = Shop(
    fesh=4,
    datafeed_id=4,
    business_fesh=PRINGLES_BUSINESS,
    priority_region=MSK_RIDS,
    regions=[MSK_RIDS],
    name="Поставщик принглс со 170 склада",
    cpa=Shop.CPA_REAL,
    warehouse_id=170,
    is_eats=True,
)

USUAL_SHOP_180 = Shop(
    fesh=5,
    datafeed_id=5,
    business_fesh=NON_FOOD_BUSINESS,
    priority_region=MSK_RIDS,
    regions=[MSK_RIDS],
    name="Поставщик не еды со 180 склада",
    cpa=Shop.CPA_REAL,
    warehouse_id=180,
)

UNUSED_SHOP_111 = Shop(
    fesh=6,
    datafeed_id=6,
    business_fesh=UNUSED_BUSINESS,
    priority_region=MSK_RIDS,
    regions=[MSK_RIDS],
    name="Другой поставщик",
    cpa=Shop.CPA_REAL,
    warehouse_id=111,
    is_eats=True,
)

# Offers
PEPSI_172 = Offer(
    price=200,
    offerid='PepsiShop_sku25',
    waremd5='Sku25Price200-172wh-eg',
    hid=CATEGORY_FOOD,
    sku=PEPSI_SKU,
    shop=PEPSI_SHOP_172,
    is_eda_retail=True,
    is_express=True,
    weight=1,
    dimensions=OfferDimensions(length=1, width=1, height=1),
)

PEPSI_147 = Offer(
    price=200,
    offerid='PepsiShop_sku25',
    waremd5='Sku25Price200-147wh-eg',
    hid=CATEGORY_FOOD,
    sku=PEPSI_SKU,
    shop=PEPSI_SHOP_147,
    is_eda_retail=True,
    is_express=True,
)

PEPSI_LITE_172 = Offer(
    price=250,
    offerid='PepsiShop_sku27',
    waremd5='Sku25Price250-172wh-eg',
    hid=CATEGORY_FOOD,
    sku='27',
    shop=PEPSI_SHOP_172,
    is_eda_retail=True,
    is_express=True,
)

PEPSI_LITE_147 = Offer(
    price=250,
    offerid='PepsiShop_sku27',
    waremd5='Sku25Price250-147wh-eg',
    hid=CATEGORY_FOOD,
    sku='27',
    shop=PEPSI_SHOP_147,
    is_eda_retail=True,
    is_express=True,
)

PEPSI_VANILLA_147 = Offer(
    price=250,
    offerid='PepsiShop_sku47',
    waremd5='Sku47Price250-147wh-eg',
    hid=CATEGORY_FOOD,
    sku='47',
    shop=PEPSI_SHOP_147,
    is_eda_retail=True,
    is_express=True,
    has_gone=True,
)

PRINGLES_155 = Offer(
    price=300,
    offerid='PringlesShop_sku35',
    waremd5='Sku35Price300-155wh-eg',
    hid=CATEGORY_FOOD,
    sku='35',
    shop=PRINGLES_SHOP_155,
    is_eda_retail=True,
    is_express=True,
)

PRINGLES_170 = Offer(
    price=300,
    offerid='PringlesShop_sku35',
    waremd5='Sku35Price300-170wh-eg',
    hid=CATEGORY_FOOD,
    sku='35',
    shop=PRINGLES_SHOP_170,
    is_eda_retail=True,
    is_express=True,
)

PRINGLES_BIG_155 = Offer(
    price=400,
    offerid='PringlesShop_sku37',
    waremd5='Sku37Price400-155wh-eg',
    hid=CATEGORY_FOOD,
    sku='37',
    shop=PRINGLES_SHOP_155,
    is_eda_retail=True,
    is_express=True,
)

PRINGLES_BIG_170 = Offer(
    price=400,
    offerid='PringlesShop_sku37',
    waremd5='Sku37Price400-170wh-eg',
    hid=CATEGORY_FOOD,
    sku='37',
    shop=PRINGLES_SHOP_170,
    is_eda_retail=True,
    is_express=True,
    has_gone=True,
)

NON_FOOD_180 = Offer(
    price=300,
    offerid='NonFoodShop_sku45',
    waremd5='Sku45Price300-180wh-eg',
    hid=CATEGORY_NON_FOOD,
    sku='45',
    shop=USUAL_SHOP_180,
)

PEPSI_OFFER_IN_PRINGLES_SHOP = Offer(
    sku=PEPSI_SKU,
    hid=CATEGORY_FOOD,
    shop=PRINGLES_SHOP_170,
    is_eda_retail=True,
)


def create_sample_shop(shops, shop_id):
    for shop in shops:
        if shop.fesh == shop_id:
            return {
                "id": shop.fesh,
                "business_id": shop.business_fesh,
                "business_name": shop.business_name,
                "slug": slug.translit(shop.name),
                "name": shop.name,
            }
    raise Exception("Unknown shop id {}".format(shop_id))


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.market_access_settings.enabled = True
        cls.settings.market_access_settings.download_catstreams = True
        cls.index.category_streams += [
            CategoryStreamRecord(CATEGORY_FOOD, Stream.FMCG.value),
        ]
        cls.index.shops += [
            PEPSI_SHOP_172,
            PEPSI_SHOP_147,
            PRINGLES_SHOP_155,
            PRINGLES_SHOP_170,
            USUAL_SHOP_180,
            UNUSED_SHOP_111,
        ]
        cls.index.offers += [
            PEPSI_172,
            PEPSI_147,
            PEPSI_LITE_172,
            PEPSI_LITE_147,
            PRINGLES_155,
            PRINGLES_170,
            PRINGLES_BIG_155,
            PRINGLES_BIG_170,
            NON_FOOD_180,
            PEPSI_OFFER_IN_PRINGLES_SHOP,  # не стирать, используется для проверки MARKETOUT-45918
        ]

    def test_eda_offers_replacement(self):
        """
        Проверяется логика подмены офферов еды.
        Сначала по офферу вычисляем бизнес, затем по бизнесу вычисляем доступный склад из гиперлокального контекста
        и кладем в посылку оффер с этого склада.
        """

        # Не стал делать моки комбинатора и КД, так как они для проверки нужной функциональности не требуются
        self.error_log.ignore(code=ErrorCodes.EXTREQUEST_DELIVERY_CALC_FATAL_ERROR)
        self.error_log.ignore(code=ErrorCodes.EXTREQUEST_COMBINATOR_REQUEST_FAILED)

        request = "place=combine&rids={rids}&pp=18&offers-list={offer_list}&eats-warehouses-compressed={warehouses}&enable-foodtech-offers=eda_retail,lavka"

        for offers in [
            [(PEPSI_147, PEPSI_172), (PRINGLES_155, PRINGLES_170)],
            [(PEPSI_172, PEPSI_147), (PRINGLES_170, PRINGLES_155)],
            [(PEPSI_172, PEPSI_172), (PRINGLES_170, PRINGLES_170)],
            [(PEPSI_147, PEPSI_147), (PRINGLES_155, PRINGLES_155)],
            [
                (PEPSI_147, PEPSI_172),
            ],
        ]:
            wh_encoder = EatsWarehousesEncoder()
            request_offers = []
            for original_offer, local_offer in offers:
                wh_encoder.add_warehouse(wh_id=local_offer.shop.warehouse_id)
                request_offers += [
                    '{ware_id}:{count};msku:{msku};cart_item_id:{cart_id};tag:eats;supplier_id:{shop}'.format(
                        ware_id=original_offer.waremd5,
                        count=1,
                        msku=original_offer.sku,
                        cart_id=len(request_offers) + 1,
                        shop=original_offer.shop.fesh,
                    )
                ]

            response = self.report.request_json(
                request.format(rids=MSK_RIDS, offer_list=','.join(request_offers), warehouses=wh_encoder.encode())
            )

            self.assertFragmentIn(
                response,
                {
                    "total": len(offers),
                    "results": [
                        {
                            "entity": "split-strategy",
                            "buckets": [
                                {
                                    "warehouseId": local_offer.shop.warehouse_id,
                                    "shopId": local_offer.shop.fesh,
                                    "offers": [
                                        {
                                            "wareId": local_offer.waremd5,
                                            "replacedId": original_offer.waremd5,
                                            "count": 1,
                                        },
                                    ],
                                }
                                for original_offer, local_offer in offers
                            ],
                        },
                    ],
                    "offers": {"items": [{"wareId": local_offer.waremd5} for _, local_offer in offers]},
                    "shops": [create_sample_shop(self.index.shops, local_offer.shop.fesh) for _, local_offer in offers],
                },
                allow_different_len=False,
            )

    def test_eda_offers_without_msku(self):
        """
        Проверяется логика подмены офферов еды без msku. Если в запросе не пришло msku, то оффер не подменяем, просто проверяем доступность.
        """

        # Не стал делать моки комбинатора и КД, так как они для проверки нужной функциональности не требуются
        self.error_log.ignore(code=ErrorCodes.EXTREQUEST_DELIVERY_CALC_FATAL_ERROR)
        self.error_log.ignore(code=ErrorCodes.EXTREQUEST_COMBINATOR_REQUEST_FAILED)

        request = "place=combine&rids={rids}&pp=18&offers-list={offer_list}&eats-warehouses-compressed={warehouses}&enable-foodtech-offers=eda_retail,lavka"

        for offers in [(PEPSI_147, PRINGLES_155), (PEPSI_147,), (PRINGLES_155,)]:
            wh_encoder = EatsWarehousesEncoder()
            request_offers = []
            for offer in offers:
                wh_encoder.add_warehouse(wh_id=offer.shop.warehouse_id)
                request_offers += [
                    '{ware_id}:{count};cart_item_id:{cart_id};tag:eats;supplier_id:{shop}'.format(
                        ware_id=offer.waremd5,
                        count=1,
                        cart_id=len(request_offers) + 1,
                        shop=offer.shop.fesh,
                    )
                ]

            response = self.report.request_json(
                request.format(rids=MSK_RIDS, offer_list=','.join(request_offers), warehouses=wh_encoder.encode())
            )

            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "split-strategy",
                            "buckets": [
                                {
                                    "warehouseId": offer.shop.warehouse_id,
                                    "shopId": offer.shop.fesh,
                                    "offers": [
                                        {
                                            "wareId": offer.waremd5,
                                            "replacedId": offer.waremd5,
                                            "count": 1,
                                        },
                                    ],
                                }
                                for offer in offers
                            ],
                        },
                    ],
                    "offers": {"items": [{"wareId": offer.waremd5} for offer in offers]},
                },
                allow_different_len=False,
            )

    def test_offers_from_one_shop(self):
        """Проверяется, что офферы одного бизнеса будут сложены в одну посылку,
        даже, если они будут отфильтрованы"""

        # Не стал делать моки комбинатора и КД, так как они для проверки нужной функциональности не требуются
        self.error_log.ignore(code=ErrorCodes.EXTREQUEST_DELIVERY_CALC_FATAL_ERROR)
        self.error_log.ignore(code=ErrorCodes.EXTREQUEST_COMBINATOR_REQUEST_FAILED)

        request = "place=combine&rids={rids}&pp=18&offers-list={offer_list}&eats-warehouses-compressed={warehouses}&enable-foodtech-offers=eda_retail,lavka"

        for original_one, original_two, local_one, local_two, reason_two in [
            (PEPSI_147, PEPSI_LITE_147, PEPSI_172, PEPSI_LITE_172, ""),
            (PEPSI_172, PEPSI_LITE_172, PEPSI_147, PEPSI_LITE_147, ""),
            (PEPSI_147, PEPSI_LITE_172, PEPSI_172, PEPSI_LITE_172, ""),
            (PEPSI_172, PEPSI_LITE_147, PEPSI_172, PEPSI_LITE_172, ""),
            (PRINGLES_155, PRINGLES_BIG_155, PRINGLES_170, PRINGLES_BIG_170, "Offer doesn`t exists"),
        ]:
            wh_encoder = EatsWarehousesEncoder()
            request_offers = []
            wh_encoder.add_warehouse(wh_id=local_one.shop.warehouse_id)
            request_offers += [
                '{ware_id}:{count};msku:{msku};cart_item_id:{cart_id};tag:eats;supplier_id:{shop}'.format(
                    ware_id=offer.waremd5,
                    count=1,
                    msku=offer.sku,
                    cart_id=cart_id,
                    shop=offer.shop.fesh,
                )
                for offer, cart_id in [(original_one, 1), (original_two, 2)]
            ]

            response = self.report.request_json(
                request.format(rids=MSK_RIDS, offer_list=','.join(request_offers), warehouses=wh_encoder.encode())
            )

            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "split-strategy",
                            "buckets": [
                                {
                                    "warehouseId": local_one.shop.warehouse_id,
                                    "shopId": local_one.shop.fesh,
                                    "offers": [
                                        {
                                            "wareId": local.waremd5 if len(reason) == 0 else "",
                                            "replacedId": original.waremd5,
                                            "count": 1,
                                            "reason": Absent() if len(reason) == 0 else reason,
                                            "cartItemIds": [cart_id],
                                        }
                                        for original, local, reason, cart_id in [
                                            (original_one, local_one, "", 1),
                                            (original_two, local_two, reason_two, 2),
                                        ]
                                    ],
                                }
                            ],
                        },
                    ],
                    "offers": {
                        "items": [
                            {"wareId": offer.waremd5}
                            for offer in ([local_one] + ([local_two] if len(reason_two) == 0 else []))
                        ]
                    },
                },
                allow_different_len=False,
            )

    def test_errors(self):
        """Проверяются, что в случае каких-либо ошибок, причина будет выведена в поле reason"""

        request = "place=combine&rids={rids}&pp=18&offers-list={offer_list}&eats-warehouses-compressed={warehouses}&enable-foodtech-offers=eda_retail,lavka"
        wh_encoder = EatsWarehousesEncoder()
        wh_encoder.add_warehouse(wh_id=PEPSI_147.shop.warehouse_id)
        warehouses = wh_encoder.encode()

        waremd5, msku, supplier = PEPSI_147.waremd5, PEPSI_147.sku, PEPSI_147.shop.fesh
        for request_offer, set_warehouses, reason, in [
            ('{}:1;msku:{};cart_item_id:1;tag:eats'.format(waremd5, msku), True, "Оffer doesn`t have supplier id"),
            (
                '{}:1;msku:{};cart_item_id:1;tag:eats;supplier_id:10'.format(waremd5, msku),
                True,
                "Failed to get business id",
            ),
            (
                '{}:1;msku:{};tag:eats;supplier_id:{}'.format(waremd5, msku, supplier),
                True,
                "Оffer doesn`t have cart item id",
            ),
            (
                '{}:1;msku:15;cart_item_id:1;tag:eats;supplier_id:{}'.format(waremd5, supplier),
                True,
                "Offer doesn`t exists",
            ),
        ]:

            response = self.report.request_json(
                request.format(
                    rids=MSK_RIDS, offer_list=request_offer, warehouses=(warehouses if set_warehouses else "")
                )
            )

            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "split-strategy",
                            "buckets": [
                                {
                                    "offers": [
                                        {
                                            "wareId": "",
                                            "replacedId": waremd5,
                                            "count": 1,
                                            "reason": reason,
                                        }
                                    ],
                                }
                            ],
                        },
                    ]
                },
                allow_different_len=False,
            )

    def test_eda_offers_with_usual_parcels(self):
        """
        Проверяется, что посылки еды нормально взаимодействуют с обычными посылками
        """

        # Не стал делать моки комбинатора и КД, так как они для проверки нужной функциональности не требуются
        self.error_log.ignore(code=ErrorCodes.EXTREQUEST_DELIVERY_CALC_FATAL_ERROR)
        self.error_log.ignore(code=ErrorCodes.EXTREQUEST_COMBINATOR_REQUEST_FAILED)

        request = "place=combine&rids={rids}&pp=18&offers-list={offer_list}&eats-warehouses-compressed={warehouses}&enable-foodtech-offers=eda_retail,lavka"

        wh_encoder = EatsWarehousesEncoder()
        wh_encoder.add_warehouse(wh_id=PEPSI_172.shop.warehouse_id)
        request_offers = []
        for offer, tag in [(PEPSI_147, "eats"), (NON_FOOD_180, "")]:
            request_offers += [
                '{ware_id}:{count};msku:{msku};cart_item_id:{cart_id};tag:{tag};supplier_id:{shop}'.format(
                    ware_id=offer.waremd5,
                    count=1,
                    msku=offer.sku,
                    cart_id=len(request_offers) + 1,
                    tag=tag,
                    shop=offer.shop.fesh,
                )
            ]

        response = self.report.request_json(
            request.format(rids=MSK_RIDS, offer_list=','.join(request_offers), warehouses=wh_encoder.encode())
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "split-strategy",
                        "buckets": [
                            {
                                "offers": [
                                    {
                                        "wareId": local_offer.waremd5,
                                        "replacedId": original_offer.waremd5,
                                        "count": 1,
                                    },
                                ],
                            }
                            for original_offer, local_offer in [(PEPSI_147, PEPSI_172), (NON_FOOD_180, NON_FOOD_180)]
                        ],
                    },
                ],
                "offers": {"items": [{"wareId": offer.waremd5} for offer in [PEPSI_172, NON_FOOD_180]]},
            },
            allow_different_len=False,
        )

    def test_unavailable_business(self):
        """
        Проверяем, что оферы одного бизнеса складываются в один бакет.
        Даже если нет ни одного доступного офера, бакет отмечен идентификатором магазина.
        По идентификатору магазина можно найти информацию в коллекции shops
        """
        # Не стал делать моки комбинатора и КД, так как они для проверки нужной функциональности не требуются
        self.error_log.ignore(code=ErrorCodes.EXTREQUEST_DELIVERY_CALC_FATAL_ERROR)
        self.error_log.ignore(code=ErrorCodes.EXTREQUEST_COMBINATOR_REQUEST_FAILED)

        request = "place=combine&rids={rids}&pp=18&offers-list={offer_list}&eats-warehouses-compressed={warehouses}&enable-foodtech-offers=eda_retail,lavka"

        wh_encoder = EatsWarehousesEncoder()
        wh_encoder.add_warehouse(wh_id=UNUSED_SHOP_111.warehouse_id)
        request_offers = []
        for offer in [PEPSI_LITE_147, PRINGLES_155, PEPSI_172]:
            request_offers += [
                '{ware_id}:{count};msku:{msku};cart_item_id:{cart_id};tag:eats;supplier_id:{shop}'.format(
                    ware_id=offer.waremd5,
                    count=1,
                    msku=offer.sku,
                    cart_id=len(request_offers) + 1,
                    shop=offer.shop.fesh,
                )
            ]

        response = self.report.request_json(
            request.format(rids=MSK_RIDS, offer_list=','.join(request_offers), warehouses=wh_encoder.encode())
        )

        def create_bucket(offers):
            return {
                "warehouseId": 0,
                "shopId": offers[0].shop.fesh,
                "offers": [
                    {
                        "wareId": "",
                        "replacedId": offer.waremd5,
                        "count": 1,
                        "reason": 'Offer doesn`t exists',
                    }
                    for offer in offers
                ],
            }

        bucket1 = create_bucket([PEPSI_LITE_147, PEPSI_172])
        bucket2 = create_bucket([PRINGLES_155])

        shop1 = create_sample_shop(self.index.shops, PEPSI_LITE_147.shop.fesh)
        shop2 = create_sample_shop(self.index.shops, PRINGLES_155.shop.fesh)

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "split-strategy",
                        "buckets": [bucket1, bucket2],
                    },
                ],
                "offers": {"items": EmptyList()},
                "shops": [shop1, shop2],
            },
            allow_different_len=False,
        )

    def test_do_not_replace_usual_offer_on_eats_offer(self):
        """
        Оферы Еды не подменяют обычные оферы
        """
        request = "place=combine&rids={rids}&pp=18&offers-list={offer_list}&eats-warehouses-compressed={warehouses}&enable-foodtech-offers=eda_retail"

        requested_offer_id = Offer.generate_waremd5('UnavailableOffer')

        wh_encoder = EatsWarehousesEncoder()
        wh_encoder.add_warehouse(wh_id=PEPSI_SHOP_172.warehouse_id)

        def check(tag, expected_offer):
            request_offers = [
                '{ware_id}:{count};msku:{msku};cart_item_id:{cart_id};tag:{tag};supplier_id:{shop}'.format(
                    ware_id=requested_offer_id,
                    count=1,
                    msku=PEPSI_SKU,
                    cart_id=1,
                    tag=tag,
                    shop=PEPSI_SHOP_172.fesh,
                )
            ]

            response = self.report.request_json(
                request.format(rids=MSK_RIDS, offer_list=','.join(request_offers), warehouses=wh_encoder.encode())
            )

            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "split-strategy",
                            "buckets": [
                                {
                                    "offers": [
                                        {
                                            "wareId": expected_offer,
                                            "replacedId": requested_offer_id,
                                            "count": 1,
                                        }
                                    ],
                                }
                            ],
                        },
                    ],
                },
                allow_different_len=False,
            )

        check('', '')  # Обычный офер не подменяется
        check('eats', PEPSI_172.ware_md5)  # Офер еды подменился

    def test_actual_delivery(self):
        # Для фудтеха не должно быть походов в комбинатор. Проверяем, что если всё же пришел запрос с едой, то выдаётся ошибка
        self.error_log.expect(code=ErrorCodes.ACD_FOODTECH_OFFER).times(2)
        for place in ('actual_delivery', 'delivery_route'):
            req = (
                'enable-foodtech-offers=eda_retail'
                '&rearr-factors=use_dsbs_combinator_response_in_actual_delivery=1'
                '&place={}'
                '&pp=18'
                '&rids=213'
                '&offers-list={}:1'
                '&delivery-type=courier'.format(place, PEPSI_172.waremd5)
            )

            response = self.report.request_json(req)
            self.assertFragmentIn(
                response,
                {
                    'total': 0,
                    'offerProblems': [
                        {
                            'problems': ['FOODTECH_IN_ACTUAL_DELIVERY'],
                            'wareId': PEPSI_172.waremd5,
                        }
                    ],
                },
            )

    def test_hyperlocal_shop_is_available(self):
        # Для бакета с недоступными офферами еды пишем, доступен ли гиперлокальный магазин по адресу пользователя
        # чтобы можно было понять, не в адресе ли проблема
        request = "place=combine&rids={rids}&pp=18&offers-list={offer_list}&eats-warehouses-compressed={warehouses}&enable-foodtech-offers=eda_retail,lavka"

        request_offers = []
        for offer in [PEPSI_VANILLA_147, PRINGLES_155]:
            request_offers += [
                '{ware_id}:{count};msku:{msku};cart_item_id:{cart_id};tag:eats;supplier_id:{shop}'.format(
                    ware_id=offer.waremd5,
                    count=1,
                    msku=offer.sku,
                    cart_id=len(request_offers) + 1,
                    shop=offer.shop.fesh,
                )
            ]

        wh_encoder = EatsWarehousesEncoder()
        wh_encoder.add_warehouse(wh_id=PEPSI_SHOP_147.warehouse_id)

        response = self.report.request_json(
            request.format(rids=MSK_RIDS, offer_list=','.join(request_offers), warehouses=wh_encoder.encode())
        )

        # PRINGLES_155 недоступен по гиперлокальности; PEPSI_VANILLA_147 доступен, но закончился
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "split-strategy",
                        "buckets": [
                            {
                                "shopId": 3,
                                "isHyperlocalShopAvailable": False,
                                "offers": [
                                    {
                                        "replacedId": PRINGLES_155.waremd5,
                                        "wareId": "",
                                        "count": 1,
                                        "reason": 'Offer doesn`t exists',
                                    }
                                ],
                            },
                            {
                                "shopId": 2,
                                "isHyperlocalShopAvailable": True,
                                "offers": [
                                    {
                                        "replacedId": PEPSI_VANILLA_147.waremd5,
                                        "wareId": "",
                                        "count": 1,
                                        "reason": 'Offer doesn`t exists',
                                    }
                                ],
                            },
                        ],
                    },
                ],
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
