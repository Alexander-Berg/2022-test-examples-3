#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Absent
from core.testcase import (
    main,
    TestCase,
)
from core.types import Promo, PromoType
from core.types.dynamic_filters import DynamicPromoSecondaries
from core.types.offer_promo import PromoBlueCashback, PromoDirectDiscount, PromoBlueSet
from core.types.sku import (
    BlueOffer,
    MarketSku,
)
from itertools import count


nummer = count()

DEFAULT_FEED_ID = 777
DEFAULT_SHOP_ID = 7770

OFFERID1 = "offer1"
OFFERID2 = "offer2"
OFFERID2_SECONDARY = "offer2_sec"


def build_ware_md5(offerid):
    insert = offerid.ljust(14, "_")
    return "Sku{}Goleg".format(insert)


def build_promo_ware_md5(id):
    return id.ljust(22, "_")


def create_blue_offer(offerid, price, promo, fesh=DEFAULT_SHOP_ID, feed=DEFAULT_FEED_ID):
    params = dict(
        waremd5=build_ware_md5(offerid),
        price=price,
        fesh=fesh,
        feedid=feed,
        offerid=offerid,
    )
    if promo is not None:
        params["promo"] = promo
    return BlueOffer(**params)


def create_msku(sku, offers_list, hid=None):
    assert isinstance(offers_list, list)
    msku_args = dict(sku=sku, hyperid=sku, blue_offers=offers_list)
    if hid is not None:
        msku_args.update(hid=hid)
    return MarketSku(**msku_args)


blue_cashback_1 = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    description='blue_cashback_1_description',
    key=build_promo_ware_md5('cashback'),
    blue_cashback=PromoBlueCashback(share=0.2, version=10, priority=1),
)

promo_direct_discount = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=DEFAULT_FEED_ID,
    key=build_promo_ware_md5('direct_discount'),
    url='http://direct_discount.com/',
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': DEFAULT_FEED_ID,
                'offer_id': OFFERID1,
                'discount_price': {
                    'value': 95,
                    'currency': 'RUR',
                },
                'old_price': {
                    'value': 12345,
                    'currency': 'RUR',
                },
            },
            {
                'feed_id': DEFAULT_FEED_ID,
                'offer_id': OFFERID2,
                'discount_price': {
                    'value': 96,
                    'currency': 'RUR',
                },
                'old_price': {
                    'value': 12345,
                    'currency': 'RUR',
                },
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
)

promo_blue_set = Promo(
    promo_type=PromoType.BLUE_SET,
    feed_id=DEFAULT_FEED_ID,
    key=build_promo_ware_md5('blue_set'),
    url='http://яндекс.рф/',
    blue_set=PromoBlueSet(
        sets_content=[
            {
                'items': [
                    {'offer_id': OFFERID1},
                    {'offer_id': OFFERID2},
                ],
                'linked': True,
            }
        ],
    ),
)

blue_offer_1 = create_blue_offer(OFFERID1, price=100, promo=[blue_cashback_1, promo_direct_discount])
blue_offer_2 = create_blue_offer(OFFERID2, price=100, promo=[blue_cashback_1, promo_direct_discount])
blue_offer_2_secondary = create_blue_offer(OFFERID2_SECONDARY, price=100, promo=None)

msku_1 = create_msku(111, [blue_offer_1])
msku_2 = create_msku(112, [blue_offer_2])
msku_3 = create_msku(113, [blue_offer_2_secondary])


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['enable_fast_promo_matcher=0;enable_fast_promo_matcher_test=0']

        cls.index.mskus += [
            msku_1,
            msku_2,
            msku_3,
        ]
        cls.index.promos += [promo_blue_set, promo_direct_discount]
        secondaries_exclude = frozenset([(blue_offer_2_secondary.feedid, blue_offer_2_secondary.offerid)])
        cls.dynamic.promo_secondaries += [
            DynamicPromoSecondaries(
                promos=[
                    promo_blue_set,
                ],
                excludes=secondaries_exclude,
            ),
        ]

    def build_common_request_part(self, place, rgb, rearr_flags):
        result = "place={place}&rgb={rgb}&perks=yandex_cashback".format(place=place, rgb=rgb)
        result += '&rearr-factors={}'.format(";".join("{}={}".format(key, val) for key, val in rearr_flags.items()))
        return result

    # Белые проверки

    def build_white_request(self, place, msku, offer, color, rearr_flags):
        request = self.build_common_request_part(place=place, rgb=color, rearr_flags=rearr_flags)
        if place == 'offerinfo':
            request += '&offerid={}'.format(offer.waremd5)
        if place == 'productoffers':
            request += '&market-sku={}'.format(msku.sku)
        if place == 'prime':
            request += '&hyperid={}'.format(str(msku.sku))
        if place == 'multi_category':
            request += '&hid={}'.format(msku.category)
        request += '&use-default-offers=1&rids=0&regset=2&pp=28'
        return request

    def white_offer_checker_promos(self, msku, offer, place, color, promos, rearr_flag):
        response = self.report.request_json(self.build_white_request(place, msku, offer, color, rearr_flag))
        # Блок promo должен отсутствовать
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': offer.waremd5,
                'promos': promos or Absent(),
            },
            allow_different_len=False,
        )

    def white_checker_promos(self, msku, offer, place, color, promos, rearr_flag):
        response = self.report.request_json(self.build_white_request(place, msku, offer, color, rearr_flag))
        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': int(msku.sku),
                'offers': {
                    'items': [
                        {
                            'entity': 'offer',
                            'wareId': offer.waremd5,
                            'promos': promos or Absent(),
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

    def check_white(self, msku, offer, values, flags):
        WHITE_PLACES = (
            'prime',
            'multi_category',
        )
        WHITE_OFFERS_PLACE = (
            'productoffers',
            'offerinfo',
        )

        color = "green_with_blue"

        for place in WHITE_PLACES:
            self.white_checker_promos(msku, offer, place, color, values, flags)

        for place in WHITE_OFFERS_PLACE:
            self.white_offer_checker_promos(msku, offer, place, color, values, flags)

    # синие проверки

    def build_blue_request(self, place, msku, offer, color, rearr_flags):
        request = self.build_common_request_part(place=place, rgb=color, rearr_flags=rearr_flags)
        request += "&market-sku={msku}".format(msku=msku.sku)
        return request

    def blue_offer_checker(self, msku, offer, place, color, promos, rearr_flag):
        response = self.report.request_json(self.build_blue_request(place, msku, offer, color, rearr_flag))
        # Блок promo должен отсутствовать
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': offer.waremd5,
                'promos': promos or Absent(),
            },
            allow_different_len=False,
        )

    def check_blue(self, msku, offer, values, flags):
        BLUE_PLACES = (
            'sku_offers',
            'prime',
        )

        color = "blue"

        for place in BLUE_PLACES:
            self.blue_offer_checker(msku, offer, place, color, values, flags)

    def check_promos(self, msku, offer, values, flag):
        flags = {
            "market_promo_blue_cashback": 1,
            "market_metadoc_search": "no",
        }

        if flag is not None:
            flags["market_enable_multipromo"] = flag

        self.check_white(msku, offer, values, flags)
        self.check_blue(msku, offer, values, flags)

    def test_promos_absent_without_flag(self):
        self.check_promos(msku_1, blue_offer_1, None, 0)

    def test_promos_with_flag(self):
        promos = [
            {
                "type": "direct-discount",
                "key": promo_direct_discount.key,
            },
            {
                "type": "blue-cashback",
                "key": blue_cashback_1.key,
            },
        ]
        self.check_promos(msku_1, blue_offer_1, promos, 1)
        self.check_promos(msku_1, blue_offer_1, promos, None)

    def test_promos_reordering(self):
        # Проверяем нарушение порядка акций, которое было возможно в старой схеме
        promos = [
            {
                "type": "direct-discount",
                "key": promo_direct_discount.key,
            },
            {
                "type": "blue-cashback",
                "key": blue_cashback_1.key,
            },
        ]
        self.check_promos(msku_2, blue_offer_2, promos, 1)


if __name__ == '__main__':
    main()
