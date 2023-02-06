#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    DynamicBlueGenericBundlesPromos,
    HyperCategory,
    Model,
    Offer,
    OfferDimensions,
    Promo,
    PromoType,
    Shop,
)
from core.matcher import Absent, NotEmpty
from core.types.dynamic_filters import DynamicQPromos
from core.types.offer_promo import (
    PromoCheapestAsGift,
    make_generic_bundle_content,
    OffersMatchingRules,
)


class T(TestCase):
    shop_dsbs = Shop(
        fesh=42,
        datafeed_id=4240,
        priority_region=213,
        regions=[213],
        name='dsbs_shop_1',
        client_id=11,
        cpa=Shop.CPA_REAL,
    )

    DSBS_OFFER_CAG = 'dsbs_offer_cag'
    DSBS_OFFER_GB_PRIMARY = 'dsbs_offer_gb_primary'
    DSBS_OFFER_GB_SECONDARY = 'dsbs_offer_gb_secondary'
    DSBS_OFFER_FAST = 'dsbs_offer_fast'

    promo_cag = Promo(
        promo_type=PromoType.CHEAPEST_AS_GIFT,
        shop_promo_id='cag-dsbs',
        feed_id=shop_dsbs.datafeed_id,
        key='JVvklyUgdnawSJPG4UhZ-1',
        url='http://cag.com',
        cheapest_as_gift=PromoCheapestAsGift(
            offer_ids=[
                (shop_dsbs.datafeed_id, DSBS_OFFER_CAG),
            ],
            count=3,
            promo_url='http://cag_promo_url.com',
            link_text='cag_link_text',
            allow_berubonus=False,
            allow_promocode=False,
        ),
    )

    promo_gb = Promo(
        promo_type=PromoType.GENERIC_BUNDLE,
        shop_promo_id='gb-dsbs',
        feed_id=shop_dsbs.datafeed_id,
        key='JVvklyUgdnawSJPG4UhZ-2',
        url='http://gb.com',
        generic_bundles_content=[
            make_generic_bundle_content(
                DSBS_OFFER_GB_PRIMARY,
                DSBS_OFFER_GB_SECONDARY,
            ),
        ],
    )

    dsbs_offer_cag = Offer(
        title=DSBS_OFFER_CAG,
        offerid=DSBS_OFFER_CAG,
        hyperid=100,
        fesh=shop_dsbs.fesh,
        waremd5='DsbsOfferCAG_________g',
        price=1000,
        cpa=Offer.CPA_REAL,
        dimensions=OfferDimensions(width=10, height=20, length=15),
        promo=promo_cag,
        blue_promo_key=promo_cag.shop_promo_id,
    )

    dsbs_offer_gb_primary = Offer(
        title=DSBS_OFFER_GB_PRIMARY,
        offerid=DSBS_OFFER_GB_PRIMARY,
        hyperid=100,
        fesh=shop_dsbs.fesh,
        waremd5='DsbsOfferGBPrimary___g',
        price=1100,
        cpa=Offer.CPA_REAL,
        dimensions=OfferDimensions(width=10, height=20, length=15),
        promo=promo_gb,
        blue_promo_key=promo_gb.shop_promo_id,
    )

    dsbs_offer_gb_secondary = Offer(
        title=DSBS_OFFER_GB_SECONDARY,
        offerid=DSBS_OFFER_GB_SECONDARY,
        hyperid=100,
        fesh=shop_dsbs.fesh,
        waremd5='DsbsOfferGBSecondary_g',
        price=1200,
        cpa=Offer.CPA_REAL,
        dimensions=OfferDimensions(width=10, height=20, length=15),
        promo=promo_gb,
    )

    dsbs_offer_fast = Offer(
        title=DSBS_OFFER_FAST,
        offerid=DSBS_OFFER_FAST,
        hyperid=100,
        fesh=shop_dsbs.fesh,
        waremd5='DsbsOffer_FAST_______g',
        price=1000,
        cpa=Offer.CPA_REAL,
        dimensions=OfferDimensions(width=10, height=20, length=15),
    )

    promo_fast = Promo(
        promo_type=PromoType.CHEAPEST_AS_GIFT,
        shop_promo_id='promo_fast',
        feed_id=shop_dsbs.datafeed_id,
        key='JVvklyUgdnawSJPG4UhZ-3',
        url='http://fast.promo',
        cheapest_as_gift=PromoCheapestAsGift(
            offer_ids=[
                (shop_dsbs.datafeed_id, DSBS_OFFER_FAST),
            ],
            count=3,
            promo_url='http://fast_promo_url.com',
            link_text='fast_link_text',
            allow_berubonus=False,
            allow_promocode=False,
        ),
        offers_matching_rules=[
            OffersMatchingRules(feed_offer_ids=[[shop_dsbs.datafeed_id, DSBS_OFFER_FAST]]),
        ],
        generation_ts=1,
    )

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.hypertree += [
            HyperCategory(hid=1, name='hid1'),
        ]

        cls.index.models += [
            Model(hyperid=100, hid=1, title='hyperid1'),
        ]

        cls.index.shops += [
            T.shop_dsbs,
        ]

        cls.index.offers += [
            T.dsbs_offer_cag,
            T.dsbs_offer_gb_primary,
            T.dsbs_offer_gb_secondary,
            T.dsbs_offer_fast,
        ]

        cls.index.promos += [
            T.promo_cag,
            T.promo_gb,
        ]

        cls.settings.loyalty_enabled = True

        cls.dynamic.loyalty += [
            DynamicBlueGenericBundlesPromos(
                whitelist=[
                    T.promo_cag.key,
                    T.promo_gb.key,
                    T.promo_fast.key,
                ]
            ),
        ]

    def check_cag_promo(self, response, offer, promo):
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': offer.ware_md5,
                'promos': [
                    {
                        'type': promo.type_name,
                        'key': promo.key,
                        'url': promo.url,
                        'startDate': NotEmpty() if promo.start_date is not None else Absent(),
                        'endDate': NotEmpty() if promo.end_date is not None else Absent(),
                        'itemsInfo': {
                            'count': promo.cheapest_as_gift.count,
                            'promo_url': promo.cheapest_as_gift.promo_url,
                            'link_text': promo.cheapest_as_gift.link_text,
                            'constraints': {
                                'allow_berubonus': promo.cheapest_as_gift.allow_berubonus,
                                'allow_promocode': promo.cheapest_as_gift.allow_promocode,
                            },
                        },
                    }
                ],
            },
            allow_different_len=False,
        )

    def check_gb_promo(self, response, offer_primary, offer_secondary, promo):
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': offer_primary.ware_md5,
                'promos': [
                    {
                        'type': promo.type_name,
                        'key': promo.key,
                        'url': promo.url,
                        'startDate': NotEmpty() if promo.start_date is not None else Absent(),
                        'endDate': NotEmpty() if promo.end_date is not None else Absent(),
                        'itemsInfo': {
                            'additionalOffers': [
                                {
                                    'totalOldPrice': {
                                        'value': str(offer_primary.price + offer_secondary.price),
                                        'currency': 'RUR',
                                    },
                                    'totalPrice': {
                                        'value': str(offer_primary.price),
                                        'currency': 'RUR',
                                    },
                                    'primaryPrice': {
                                        'value': '1095',
                                        'currency': 'RUR',
                                    },
                                    'offer': {
                                        'price': {
                                            'value': '5',
                                            'currency': 'RUR',
                                        },
                                        'offerId': offer_secondary.waremd5,
                                        'showUid': NotEmpty(),
                                        'feeShow': NotEmpty(),
                                        'entity': 'showPlace',
                                        'urls': NotEmpty(),
                                    },
                                }
                            ],
                            'constraints': NotEmpty(),
                        },
                    }
                ],
            },
            allow_different_len=False,
        )

    def test_cag_promo(self):
        base_request = 'place=offerinfo&pp=18&rgb=green_with_blue&rids=213&regset=2&show-urls=cpa'
        response = self.report.request_json(base_request + '&offerid={}'.format(T.dsbs_offer_cag.ware_md5))
        self.check_cag_promo(response, T.dsbs_offer_cag, T.promo_cag)

    def test_gb_promo(self):
        base_request = 'place=offerinfo&pp=18&rgb=green_with_blue&rids=213&regset=2&show-urls=cpa'
        response = self.report.request_json(base_request + '&offerid={}'.format(T.dsbs_offer_gb_primary.ware_md5))
        self.check_gb_promo(response, T.dsbs_offer_gb_primary, T.dsbs_offer_gb_secondary, T.promo_gb)

    def test_cag_landing(self):
        base_request = 'place=prime&pp=18&rgb=green_with_blue&rids=213&regset=2&show-urls=cpa&shop-promo-id={}'.format(
            T.promo_cag.shop_promo_id
        )
        response = self.report.request_json(base_request)
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': T.dsbs_offer_cag.ware_md5,
                    'promos': [
                        {
                            'type': T.promo_cag.type_name,
                            'key': T.promo_cag.key,
                        }
                    ],
                }
            ],
            allow_different_len=False,
        )

    def test_gb_landing(self):
        base_request = 'place=prime&pp=18&rgb=green_with_blue&rids=213&regset=2&show-urls=cpa&shop-promo-id={}'.format(
            T.promo_gb.shop_promo_id
        )
        response = self.report.request_json(base_request)
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': T.dsbs_offer_gb_primary.ware_md5,
                    'promos': [
                        {
                            'type': T.promo_gb.type_name,
                            'key': T.promo_gb.key,
                        }
                    ],
                }
            ],
            allow_different_len=False,
        )

    def test_fast_promo_dsbs(self):
        self.dynamic.qpromos += [DynamicQPromos([T.promo_fast])]
        base_request = (
            'place=offerinfo&pp=18&rgb=green_with_blue&rids=213&regset=2&show-urls=cpa'
            + '&rearr-factors=enable_fast_promo_matcher=1;enable_fast_promo_new_promos=1'
        )
        response = self.report.request_json(base_request + '&offerid={}'.format(T.dsbs_offer_fast.ware_md5))
        self.check_cag_promo(response, T.dsbs_offer_fast, T.promo_fast)


if __name__ == '__main__':
    main()
