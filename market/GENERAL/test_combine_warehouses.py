#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    Currency,
    MarketSku,
    Shop,
    Tax,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
)

from core.logs import ErrorCodes

MODEL_ID_1 = 1
CATEGORY_ID_1 = 1
MODEL_ID_2 = 2
CATEGORY_ID_2 = 2
MSK_RIDS = 213

# Shops
PEPSI_SHOP_1_172 = Shop(
    fesh=1,
    datafeed_id=1,
    priority_region=MSK_RIDS,
    regions=[MSK_RIDS],
    name="Поставщик пепси со 172 склада",
    currency=Currency.RUR,
    tax_system=Tax.OSN,
    supplier_type=Shop.FIRST_PARTY,
    blue=Shop.BLUE_REAL,
    cpa=Shop.CPA_REAL,
    warehouse_id=172,
    fulfillment_program=True,
)

PEPSI_SHOP_1_147 = Shop(
    fesh=1,
    datafeed_id=2,
    priority_region=MSK_RIDS,
    regions=[MSK_RIDS],
    name="Поставщик пепси со 147 склада",
    currency=Currency.RUR,
    tax_system=Tax.OSN,
    supplier_type=Shop.FIRST_PARTY,
    blue=Shop.BLUE_REAL,
    cpa=Shop.CPA_REAL,
    warehouse_id=147,
    fulfillment_program=True,
)

PRINGLES_SHOP_2_155 = Shop(
    fesh=3,
    datafeed_id=3,
    priority_region=MSK_RIDS,
    regions=[MSK_RIDS],
    name="Поставщик принглс со 155 склада",
    currency=Currency.RUR,
    tax_system=Tax.OSN,
    supplier_type=Shop.FIRST_PARTY,
    blue=Shop.BLUE_REAL,
    cpa=Shop.CPA_REAL,
    warehouse_id=155,
    fulfillment_program=True,
)

# Offers
PEPSI_1P_172 = BlueOffer(
    price=11000, offerid='PepsiShop1_sku25', waremd5='Sku25Price11k-172wh-eg', feedid=1, stock_store_count=1
)
PEPSI_1P_147 = BlueOffer(
    price=11000, offerid='PepsiShop1_sku25', waremd5='Sku25Price11k-147wh-eg', feedid=2, stock_store_count=1
)
PRINGLES_1P_155 = BlueOffer(
    price=12000, offerid='PringlesShop2_sku35', waremd5='Sku35Price12k-155wh-eg', feedid=3, stock_store_count=1
)

# MSKUs
PEPSI_MSKU = MarketSku(
    title="MSKU 1P",
    hid=CATEGORY_ID_1,
    hyperid=MODEL_ID_1,
    sku='25',
    blue_offers=[PEPSI_1P_172, PEPSI_1P_147],
)

PRINGLES_MSKU = MarketSku(
    title="MSKU 1P",
    hid=CATEGORY_ID_2,
    hyperid=MODEL_ID_2,
    sku='35',
    blue_offers=[PRINGLES_1P_155],
)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.mskus += [PEPSI_MSKU, PRINGLES_MSKU]
        cls.index.shops += [PEPSI_SHOP_1_172, PEPSI_SHOP_1_147, PRINGLES_SHOP_2_155]
        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(
                regions=[225],
                warehouse_with_priority=[
                    WarehouseWithPriority(172, 1),
                    WarehouseWithPriority(147, 1),
                    WarehouseWithPriority(155, 1),
                ],
            ),
        ]

    def test_no_offer_replace(self):
        """
        Проверяется, что при наличии на разных складом одинаковых МСКУ от одного поставщика и
        равнозначности этих складов, выбирается склад оффер с которого положили в корзину
        """

        # Не стал делать моки комбинатор и КД, так как они для проверки нужной функциональности не требуются
        self.error_log.ignore(code=ErrorCodes.EXTREQUEST_DELIVERY_CALC_FATAL_ERROR)
        self.error_log.ignore(code=ErrorCodes.EXTREQUEST_COMBINATOR_REQUEST_FAILED)

        request = "place=combine&rids={rids}&pp=18&offers-list={offer_list}&rearr-factors=market_use_global_warehouse_priorities_filtering=0"

        for no_replace_offer, warehouse in [(PEPSI_1P_147, 147), (PEPSI_1P_172, 172)]:
            request_offers = []
            for cart_item_id, (offer, msku) in enumerate(
                [(no_replace_offer, PEPSI_MSKU), (PRINGLES_1P_155, PRINGLES_MSKU)]
            ):
                request_offers += [
                    '{}:{};msku:{};cart_item_id:{}'.format(
                        offer.waremd5,
                        1,
                        msku.sku,
                        cart_item_id + 1,
                    )
                ]
            response = self.report.request_json(request.format(rids=MSK_RIDS, offer_list=','.join(request_offers)))

            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "split-strategy",
                            "name": "consolidate-without-crossdock",
                            "buckets": [
                                {
                                    "warehouseId": warehouse,
                                    "offers": [
                                        {
                                            "wareId": no_replace_offer.waremd5,
                                        },
                                    ],
                                },
                                {
                                    "warehouseId": 155,
                                    "offers": [
                                        {
                                            "wareId": PRINGLES_1P_155.waremd5,
                                        },
                                    ],
                                },
                            ],
                        },
                    ]
                },
                allow_different_len=False,
            )


if __name__ == '__main__':
    main()
