#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import (
    BlueOffer,
    DynamicDeliveryRestriction,
    DynamicWarehouseDelivery,
    DynamicWarehouseLink,
    MarketSku,
    MnPlace,
    Offer,
    OfferDimensions,
    Shop,
)
from core.testcase import TestCase, main
from core.matcher import Absent


class T(TestCase):
    @classmethod
    def prepare_on_demand_delivery(cls):
        cls.index.shops += [
            Shop(
                fesh=20,
                datafeed_id=20,
                priority_region=213,
                regions=[225],
                supplier_type=Shop.THIRD_PARTY,
                blue='REAL',
                name='3p shop',
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                sku=2001,
                title="on demand delivery offer 1",
                blue_offers=[BlueOffer(ts=100401, feedid=20)],
            ),
            MarketSku(
                sku=2002,
                title="not on demand delivery offer 2",
                blue_offers=[
                    BlueOffer(ts=100402, feedid=20, dimensions=OfferDimensions(length=100, width=100, height=100))
                ],
            ),
            MarketSku(
                sku=2003,
                title="not on demand delivery offer 3",
                blue_offers=[
                    BlueOffer(ts=100403, feedid=20, dimensions=OfferDimensions(length=100, width=100, height=100))
                ],
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100401).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100402).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100402).respond(0.7)

        cls.index.offers += [
            Offer(title="on demand delivery white 1"),
            Offer(title="on demand delivery white 2"),
            Offer(title="on demand delivery white 3"),
            Offer(title="on demand delivery white 4"),
        ]

        warehouse_id = 145
        cls.settings.nordstream_autogenerate = False
        cls.dynamic.nordstream += [DynamicWarehouseLink(warehouse_id, [warehouse_id])]
        cls.dynamic.nordstream += [
            DynamicWarehouseDelivery(
                warehouse_id,
                {
                    213: [
                        DynamicDeliveryRestriction(
                            min_days=0,
                            max_days=100,
                            delivery_subtype=1,
                            tariff_user_mask=3,
                            max_dimensions=[50, 50, 50],
                        ),
                        DynamicDeliveryRestriction(
                            min_days=0, max_days=100, tariff_user_mask=3, max_dimensions=[10000, 10000, 10000]
                        ),
                    ],
                },
            ),
        ]

    def test_on_demand_delivery(self):
        """Проверяем, что под флагом market_parallel_on_demand_delivery=1 если у 50% CPA офферов есть доставка по клику,
        то в выдачу добавляется признак cpaItemsOnDemandDelivery
        https://st.yandex-team.ru/MARKETOUT-42049
        """
        request = 'place=parallel&rids=213&text=on+demand+delivery&ignore-all-filters-except-dynamic=1'
        request += '&rearr-factors=market_offers_incut_align=0;market_cpa_offers_incut_threshold=0;market_cpa_offers_incut_hide_duplicates=0;'

        # Доставка по клику есть у 1 из 2 офферов, признак добавляется
        response = self.report.request_bs_pb(
            request + 'market_parallel_on_demand_delivery=1;market_cpa_offers_incut_count=2'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "cpaItemsOnDemandDelivery": True,
                    "showcase": {
                        "cpaItems": [
                            {
                                "title": {"text": {"__hl": {"text": "on demand delivery offer 1", "raw": True}}},
                                "delivery": {"onDemand": True},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "not on demand delivery offer 2", "raw": True}}},
                                "delivery": {"onDemand": Absent()},
                            },
                        ]
                    },
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Доставка по клику есть у 1 из 3 офферов, признак не добавляется
        response = self.report.request_bs_pb(
            request + 'market_parallel_on_demand_delivery=1;market_cpa_offers_incut_count=3'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "cpaItemsOnDemandDelivery": Absent(),
                    "showcase": {
                        "cpaItems": [
                            {
                                "title": {"text": {"__hl": {"text": "on demand delivery offer 1", "raw": True}}},
                                "delivery": {"onDemand": True},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "not on demand delivery offer 2", "raw": True}}},
                                "delivery": {"onDemand": Absent()},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "not on demand delivery offer 3", "raw": True}}},
                                "delivery": {"onDemand": Absent()},
                            },
                        ]
                    },
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
