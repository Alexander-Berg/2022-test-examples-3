#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.testcase import TestCase, main
from core.report import REQUEST_TIMESTAMP
from core.types import BlueOffer, DynamicBlueGenericBundlesPromos, DynamicSkuOffer, MarketSku, Promo, PromoType, Shop
from core.types.offer_promo import PromoBlueSet, make_generic_bundle_content
from datetime import datetime, timedelta

now = datetime.fromtimestamp(REQUEST_TIMESTAMP)  # нет рандома - нет нормального времени
delta_big = timedelta(days=1)


class ShopWithBundle:
    FESH = 777
    DESCR = "shop_with_bundle"


class ShopWithBlueSet:
    FESH = 888
    DESCR = "shop_with_blue_set"


def get_shop_blue_offer(shopInfo, info):
    return BlueOffer(price=1000, feedid=shopInfo.FESH, offerid=shopInfo.DESCR + "_" + info)


primary_in_bundle1 = get_shop_blue_offer(ShopWithBundle, "primaryBundle1")
primary_in_bundle2 = get_shop_blue_offer(ShopWithBundle, "primaryBundle2")
primary_in_bundle3 = get_shop_blue_offer(ShopWithBundle, "primaryBundle3")
secondary_in_bundle = get_shop_blue_offer(ShopWithBundle, "secondaryBundle1")
another_secondary_in_bundle = get_shop_blue_offer(ShopWithBundle, "secondaryBundle2")
gb_secondary_preorder = get_shop_blue_offer(ShopWithBundle, "gb_secondary_preorder")

offer_in_blue_set = get_shop_blue_offer(ShopWithBlueSet, "blueSet1")
another_offer_in_blue_set = get_shop_blue_offer(ShopWithBlueSet, "blueSet2")

set2_offer1 = get_shop_blue_offer(ShopWithBlueSet, "offer1")
set2_offer2 = get_shop_blue_offer(ShopWithBlueSet, "offer2")
set2_offer3 = get_shop_blue_offer(ShopWithBlueSet, "offer3")


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += [
            Shop(
                fesh=shopInfo.FESH,
                datafeed_id=shopInfo.FESH,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
            )
            for shopInfo in (ShopWithBundle, ShopWithBlueSet)
        ]

        cls.index.mskus += [
            MarketSku(sku=11, blue_offers=[primary_in_bundle1, primary_in_bundle2, primary_in_bundle3]),
            MarketSku(sku=12, blue_offers=[secondary_in_bundle]),
            MarketSku(sku=13, blue_offers=[another_secondary_in_bundle]),
            MarketSku(sku=14, blue_offers=[offer_in_blue_set]),
            MarketSku(sku=15, blue_offers=[another_offer_in_blue_set]),
            MarketSku(sku=18, blue_offers=[set2_offer1]),
            MarketSku(sku=19, blue_offers=[set2_offer2]),
            MarketSku(sku=20, blue_offers=[set2_offer3]),
            MarketSku(sku=21, blue_offers=[gb_secondary_preorder]),
        ]

        cls.index.promos += [
            Promo(
                promo_type=PromoType.GENERIC_BUNDLE,
                feed_id=ShopWithBundle.FESH,
                key='JVvklxUgdnawSJPG4UhZ-1',
                url='http://localhost.ru/',
                start_date=now - delta_big,
                end_date=now + delta_big,
                generic_bundles_content=[
                    make_generic_bundle_content(primary_in_bundle1.offerid, secondary_in_bundle.offerid, 1),
                    make_generic_bundle_content(primary_in_bundle2.offerid, another_secondary_in_bundle.offerid, 1),
                    make_generic_bundle_content(primary_in_bundle3.offerid, gb_secondary_preorder.offerid, 1),
                ],
            ),
            Promo(
                promo_type=PromoType.BLUE_SET,
                feed_id=ShopWithBlueSet.FESH,
                key='JVvklxUgdnawSJPG4UhZ-2',
                url='http://яндекс.рф/',
                start_date=now - delta_big,
                end_date=now + delta_big,
                blue_set=PromoBlueSet(
                    sets_content=[
                        {
                            'items': [
                                {'offer_id': offer_in_blue_set.offerid},
                                {'offer_id': another_offer_in_blue_set.offerid},
                            ],
                            'linked': True,
                        }
                    ],
                ),
            ),
            Promo(
                promo_type=PromoType.BLUE_SET,
                feed_id=ShopWithBlueSet.FESH,
                key='JVvklxUgdnawSJPG4UhZ-3',
                url='http://яндекс.рф/',
                start_date=now - delta_big,
                end_date=now + delta_big,
                blue_set=PromoBlueSet(
                    sets_content=[
                        {
                            'items': [
                                {'offer_id': set2_offer1.offerid},
                                {'offer_id': set2_offer2.offerid},
                                {'offer_id': set2_offer3.offerid},
                            ],
                            'linked': False,
                        }
                    ],
                ),
            ),
        ]

        cls.settings.loyalty_enabled = True
        # выдача должна работать независимо от наличия акции в белом списке, добавляем только 1 акцию
        cls.dynamic.loyalty += [DynamicBlueGenericBundlesPromos(whitelist=[cls.index.promos[0].key])]

        cls.dynamic.preorder_sku_offers += [
            DynamicSkuOffer(shop_id=ShopWithBundle.FESH, sku=gb_secondary_preorder.offerid)
        ]

    def test_collect_promos_secondary(self):
        # выдача должна работать независимо от rearr флагов, для проверки сбрасываем все акционные флаги
        response = self.report.request_json("place=collect_promo_secondary_items")
        self.assertFragmentIn(
            response,
            {
                "result": [
                    {
                        "feed": feed,
                        "offers": offers,
                    }
                    for feed, offers in (
                        (
                            ShopWithBundle.FESH,
                            [
                                secondary_in_bundle.offerid,
                                another_secondary_in_bundle.offerid,
                                gb_secondary_preorder.offerid,
                            ],
                        ),
                        (
                            ShopWithBlueSet.FESH,
                            [
                                offer_in_blue_set.offerid,
                                another_offer_in_blue_set.offerid,
                                # здесь нет оффера-1, т.к. комплект не связанный (не надо показывать комплект для вторичных товаров)
                                set2_offer2.offerid,
                                set2_offer3.offerid,
                            ],
                        ),
                    )
                ],
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
