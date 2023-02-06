#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa
import base64

from core.testcase import TestCase, main
from core.types import Shop, Region, HyperCategory
from core.types.sku import MarketSku, BlueOffer
from core.types.autogen import b64url_md5

from itertools import count

nummer = count()


class _Rids:
    moscow = 213


class _HyperCategory:
    class _Ids:
        id_1 = 1
        id_2 = 2

    hyper_category_1 = HyperCategory(hid=_Ids.id_1)
    hyper_category_2 = HyperCategory(hid=_Ids.id_2)


class _Shops:
    class _Feeds:
        feed_1 = 100
        feed_2 = 200

    def __shop(feed, datafeed_id, priority_region, regions):
        return Shop(
            fesh=feed,
            datafeed_id=datafeed_id,
            priority_region=priority_region,
            regions=regions,
            supplier_type=Shop.THIRD_PARTY,
            blue=Shop.BLUE_REAL,
        )

    shop_1 = __shop(_Feeds.feed_1, _Feeds.feed_1, _Rids.moscow, [_Rids.moscow])

    shop_2 = __shop(_Feeds.feed_2, _Feeds.feed_2, _Rids.moscow, [_Rids.moscow])


class _Requests:
    base = 'place=offer_search&rids={rid}&regset={rid}'
    msku = base + '&market-sku={msku}'
    offer = base + '&offerid={offerid}'
    shoffer = base + '&feed_shoffer_id={shofferid}'
    shoffer_64 = base + '&feed_shoffer_id_base64={shofferid}'
    hid = base + '&hid={hid}'


class _BlueOffers:
    def __blue_offer(feed):
        num = next(nummer)
        return BlueOffer(
            waremd5=b64url_md5(num),
            fesh=feed,
            feedid=feed,
            offerid='offer_{}'.format(num),
        )

    offer_1_1 = __blue_offer(_Shops.shop_1.fesh)
    offer_1_2 = __blue_offer(_Shops.shop_1.fesh)

    offer_2_1 = __blue_offer(_Shops.shop_2.fesh)
    offer_2_2 = __blue_offer(_Shops.shop_2.fesh)


class _Mskus:
    def __msku(offers, hid):
        num = next(nummer)
        return MarketSku(sku=num, hid=hid, hyperid=num, blue_offers=offers)

    msku_1 = __msku(offers=[_BlueOffers.offer_1_1, _BlueOffers.offer_2_1], hid=_HyperCategory._Ids.id_1)
    msku_2 = __msku(offers=[_BlueOffers.offer_1_2, _BlueOffers.offer_2_2], hid=_HyperCategory._Ids.id_2)


# +-----------+---+---+
# |   Shop    | 1 | 2 |
# +-----------+---+---+
# | offer_1_1 | + | - |
# | offer_1_2 | + | - |
# | offer_2_1 | - | + |
# | offer_2_2 | - | + |
# +-----------+---+---+

# +-----------+---+---+
# |   Msku    | 1 | 2 |
# +-----------+---+---+
# | offer_1_1 | + | - |
# | offer_1_2 | - | + |
# | offer_2_1 | + | - |
# | offer_2_2 | - | + |
# +-----------+---+---+


class T(TestCase):
    @classmethod
    def prepare_regions(cls):
        cls.index.regiontree += [
            Region(rid=_Rids.moscow),
        ]

    @classmethod
    def prepare_shops(cls):
        cls.index.shops += [_Shops.shop_1, _Shops.shop_2]

    @classmethod
    def prepare_mskus(cls):
        cls.index.mskus += [
            _Mskus.msku_1,
            _Mskus.msku_2,
        ]

    @classmethod
    def prepare_HyperCategory(cls):
        cls.index.hypertree += [_HyperCategory.hyper_category_1, _HyperCategory.hyper_category_2]

    def check_no_offers_in_response(self, response):
        self.assertFragmentIn(
            response,
            {
                "totalOffers": 0,
            },
        )

    def check_offers_in_response(self, response, offers=None):
        if not offers:
            return self.check_no_offers_in_response(response)

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
            allow_different_len=False,
        )

    def assert_find_offers_by_msku(self, rid, msku):
        request = _Requests.msku.format(rid=rid, msku=msku.sku)
        response = self.report.request_json(request)
        self.check_offers_in_response(response, msku.get_blue_offers())

    def test_find_offers_by_msku(self):
        for msku in [_Mskus.msku_1, _Mskus.msku_2]:
            self.assert_find_offers_by_msku(rid=_Rids.moscow, msku=msku)

    def assert_find_offers_by_shop_offer_id(self, rid, shopid, offerid, result):
        def encode(feed_shoffer):
            return base64.b64encode(feed_shoffer)

        shofferid = "{}-{}".format(shopid, offerid)
        request_base = _Requests.shoffer.format(rid=rid, shofferid=shofferid)
        request_64 = _Requests.shoffer_64.format(rid=rid, shofferid=encode(shofferid))
        for request in [request_base, request_64]:
            response = self.report.request_json(request)
            self.check_offers_in_response(response, result)

    def assert_find_all_offers_in_shop_by_shop_offer_id(self, rid, shopid, offers_to_find, offers_to_drop):
        self.assert_find_offers_by_shop_offer_id(rid, shopid, '*', offers_to_find)

        if offers_to_find:
            for offer in offers_to_find:
                self.assert_find_offers_by_shop_offer_id(rid, shopid, offer.offerid, [offer])

        if offers_to_drop:
            for offer in offers_to_drop:
                self.assert_find_offers_by_shop_offer_id(rid, shopid, offer.offerid, None)

    def test_find_offers_by_shop_offer_id(self):
        self.assert_find_all_offers_in_shop_by_shop_offer_id(
            rid=_Rids.moscow,
            shopid=_Shops.shop_1.fesh,
            offers_to_find=[_BlueOffers.offer_1_1, _BlueOffers.offer_1_2],
            offers_to_drop=[_BlueOffers.offer_2_1, _BlueOffers.offer_2_2],
        )

        self.assert_find_all_offers_in_shop_by_shop_offer_id(
            rid=_Rids.moscow,
            shopid=_Shops.shop_2.fesh,
            offers_to_find=[_BlueOffers.offer_2_1, _BlueOffers.offer_2_2],
            offers_to_drop=[_BlueOffers.offer_1_1, _BlueOffers.offer_1_2],
        )

    def assert_find_offers_by_waremd5(self, rid, offers):
        if offers:
            for offer in offers:
                request = _Requests.offer.format(rid=rid, offerid=offer.waremd5)
                response = self.report.request_json(request)
                self.check_offers_in_response(response, offer)

    def test_find_offers_by_waremd5(self):
        self.assert_find_offers_by_waremd5(
            rid=_Rids.moscow,
            offers=[_BlueOffers.offer_1_1, _BlueOffers.offer_1_2, _BlueOffers.offer_2_1, _BlueOffers.offer_2_2],
        )

    def assert_find_offers_by_hid(self, rid, hid, offers):
        if offers:
            request = _Requests.hid.format(rid=rid, hid=hid)
            response = self.report.request_json(request)
            self.check_offers_in_response(response, offers)

    def test_find_offers_by_hid(self):
        self.assert_find_offers_by_hid(
            rid=_Rids.moscow,
            hid=_HyperCategory._Ids.id_1,
            offers=[_BlueOffers.offer_1_1, _BlueOffers.offer_2_1],
        )

        self.assert_find_offers_by_hid(
            rid=_Rids.moscow,
            hid=_HyperCategory._Ids.id_2,
            offers=[_BlueOffers.offer_1_2, _BlueOffers.offer_2_2],
        )


if __name__ == '__main__':
    main()
