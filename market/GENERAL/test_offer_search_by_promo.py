#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import DynamicBlueGenericBundlesPromos, Promo, PromoType, Shop, PromoCheapestAsGift
from core.types.sku import MarketSku, BlueOffer
from core.types.offer_promo import OffersMatchingRules, PromoBlueSet, make_generic_bundle_content
from core.types.autogen import b64url_md5

from itertools import count

nummer = count()


class _Rids:
    moscow = 213


class _Shops:
    class _Feeds:
        feed_1 = 100

    shop_1 = Shop(
        fesh=_Feeds.feed_1,
        datafeed_id=_Feeds.feed_1,
        priority_region=_Rids.moscow,
        regions=[_Rids.moscow],
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
    )


class _Requests:
    base = 'place=offer_search&rids=213&regset=1'
    promo_type = base + '&promo-type={promo_type}'
    promo_id = base + '&promoid={promo_id}'


class _BlueOffers:
    def __blue_offer():
        num = next(nummer)
        return BlueOffer(
            waremd5=b64url_md5(num),
            fesh=_Shops.shop_1.fesh,
            feedid=_Shops.shop_1.fesh,
            offerid='offer_{}'.format(num),
        )

    blue_set_1 = __blue_offer()
    blue_set_2 = __blue_offer()
    generic_bundle_1 = __blue_offer()
    generic_bundle_2 = __blue_offer()
    cheapest_as_gift_1 = __blue_offer()
    cheapest_as_gift_2 = __blue_offer()


class _Mskus:
    def __msku(offers):
        num = next(nummer)
        return MarketSku(sku=num, hyperid=num, blue_offers=([offers] if not isinstance(offers, list) else offers))

    blue_set_1 = __msku(_BlueOffers.blue_set_1)
    blue_set_2 = __msku(_BlueOffers.blue_set_2)
    generic_bundle_1 = __msku(_BlueOffers.generic_bundle_1)
    generic_bundle_2 = __msku(_BlueOffers.generic_bundle_2)
    cheapest_as_gift_1 = __msku(_BlueOffers.cheapest_as_gift_1)
    cheapest_as_gift_2 = __msku(_BlueOffers.cheapest_as_gift_2)


class _Promos:
    blue_set = Promo(
        promo_type=PromoType.BLUE_SET,
        feed_id=_Shops.shop_1.fesh,
        key=b64url_md5(next(nummer)),
        url='http://яндекс.рф/',
        shop_promo_id='promo_blue_set_1',
        blue_set=PromoBlueSet(
            sets_content=[
                {
                    'items': [
                        {'offer_id': _BlueOffers.blue_set_1.offerid, 'discount': 0},
                        {'offer_id': _BlueOffers.blue_set_2.offerid, 'discount': 0},
                    ],
                    'linked': True,
                },
            ],
            restrict_refund=True,
            allow_berubonus=False,
            allow_promocode=False,
        ),
        offers_matching_rules=[OffersMatchingRules(mskus=[_Mskus.blue_set_1, _Mskus.blue_set_2])],
    )

    generic_bundle = Promo(
        promo_type=PromoType.GENERIC_BUNDLE,
        feed_id=_Shops.shop_1.fesh,
        key=b64url_md5(next(nummer)),
        url='http://яндекс.рф/',
        shop_promo_id='promo_generic_bundle_1',
        generic_bundles_content=[
            make_generic_bundle_content(
                _BlueOffers.generic_bundle_1.offerid,
                _BlueOffers.generic_bundle_2.offerid,
            ),
        ],
        offers_matching_rules=[OffersMatchingRules(mskus=[_Mskus.generic_bundle_1, _Mskus.generic_bundle_2])],
    )

    cheapest_as_gift = Promo(
        promo_type=PromoType.CHEAPEST_AS_GIFT,
        feed_id=_Shops.shop_1.fesh,
        key=b64url_md5(next(nummer)),
        url='http://яндекс.рф/',
        shop_promo_id='promo_cheapest_as_gift_1',
        cheapest_as_gift=PromoCheapestAsGift(
            offer_ids=[
                (_Shops.shop_1.fesh, _BlueOffers.cheapest_as_gift_1.offerid),
                (_Shops.shop_1.fesh, _BlueOffers.cheapest_as_gift_2.offerid),
            ],
            count=3,
            promo_url='http://cag_promo_url.com',
            link_text='cag_link_text',
            allow_berubonus=False,
            allow_promocode=False,
        ),
        offers_matching_rules=[OffersMatchingRules(mskus=[_Mskus.cheapest_as_gift_1, _Mskus.cheapest_as_gift_2])],
    )


class T(TestCase):
    @classmethod
    def prepare_offers(cls):
        _BlueOffers.blue_set_1.promo = [_Promos.blue_set]
        _BlueOffers.blue_set_1.blue_promo_key = _Promos.blue_set.key
        _BlueOffers.blue_set_2.promo = [_Promos.blue_set]
        _BlueOffers.blue_set_2.blue_promo_key = _Promos.blue_set.key

        _BlueOffers.generic_bundle_1.promo = [_Promos.generic_bundle]
        _BlueOffers.generic_bundle_1.blue_promo_key = _Promos.generic_bundle.key
        _BlueOffers.generic_bundle_2.promo = [_Promos.generic_bundle]
        _BlueOffers.generic_bundle_2.blue_promo_key = _Promos.generic_bundle.key

        _BlueOffers.cheapest_as_gift_1.promo = [_Promos.cheapest_as_gift]
        _BlueOffers.cheapest_as_gift_1.blue_promo_key = _Promos.cheapest_as_gift.key
        _BlueOffers.cheapest_as_gift_2.promo = [_Promos.cheapest_as_gift]
        _BlueOffers.cheapest_as_gift_2.blue_promo_key = _Promos.cheapest_as_gift.key

    @classmethod
    def prepare_shops(cls):
        cls.index.shops += [_Shops.shop_1]

    @classmethod
    def prepare_mskus(cls):
        cls.index.mskus += [
            _Mskus.blue_set_1,
            _Mskus.blue_set_2,
            _Mskus.generic_bundle_1,
            _Mskus.generic_bundle_2,
            _Mskus.cheapest_as_gift_1,
            _Mskus.cheapest_as_gift_2,
        ]

    @classmethod
    def prepare_promos(cls):
        cls.index.promos += [
            _Promos.blue_set,
            _Promos.generic_bundle,
            _Promos.cheapest_as_gift,
        ]

        cls.settings.loyalty_enabled = True
        cls.dynamic.loyalty += [
            DynamicBlueGenericBundlesPromos(
                whitelist=[
                    _Promos.blue_set.key,
                    _Promos.generic_bundle.key,
                    _Promos.cheapest_as_gift.key,
                ]
            )
        ]

    def check_offers_in_response(self, response, offers):
        if not isinstance(offers, list):
            offers = [offers]

        self.assertFragmentIn(
            response,
            {
                "totalOffers": len(offers),
                "results": [
                    {
                        "wareId": offer.waremd5,
                    }
                    for offer in offers
                ],
            },
        )

    def test_find_offer_with_promo_blue_set(self):
        request = _Requests.promo_type.format(promo_type=PromoType.BLUE_SET)
        response = self.report.request_json(request)
        self.check_offers_in_response(
            response,
            [
                _BlueOffers.blue_set_1,
                _BlueOffers.blue_set_2,
            ],
        )

        request = _Requests.promo_id.format(promo_id=_Promos.blue_set.key)
        response = self.report.request_json(request)
        self.check_offers_in_response(
            response,
            [
                _BlueOffers.blue_set_1,
                _BlueOffers.blue_set_2,
            ],
        )

    def test_find_offer_with_promo_generic_bundle(self):
        request = _Requests.promo_type.format(promo_type=PromoType.GENERIC_BUNDLE)
        response = self.report.request_json(request)
        self.check_offers_in_response(
            response,
            [
                _BlueOffers.generic_bundle_1,
            ],
        )

        request = _Requests.promo_id.format(promo_id=_Promos.generic_bundle.key)
        response = self.report.request_json(request)
        self.check_offers_in_response(
            response,
            [
                _BlueOffers.generic_bundle_1,
                _BlueOffers.generic_bundle_2,
            ],
        )

    def test_find_offer_with_promo_type_cheapest_as_gift(self):
        request = _Requests.promo_type.format(promo_type=PromoType.CHEAPEST_AS_GIFT)
        response = self.report.request_json(request)
        self.check_offers_in_response(
            response,
            [
                _BlueOffers.cheapest_as_gift_1,
                _BlueOffers.cheapest_as_gift_2,
            ],
        )

        request = _Requests.promo_id.format(promo_id=_Promos.cheapest_as_gift.key)
        response = self.report.request_json(request)
        self.check_offers_in_response(
            response,
            [
                _BlueOffers.cheapest_as_gift_1,
                _BlueOffers.cheapest_as_gift_2,
            ],
        )


if __name__ == '__main__':
    main()
